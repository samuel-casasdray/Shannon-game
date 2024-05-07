package fr.projet.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import fr.projet.Callback;
import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.gui.Gui;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

@ClientEndpoint
@Slf4j
public class WebSocketClient {
    private Session session;
    @Getter
    private String response = null;
    @Getter
    private boolean closed = true;
    @Getter
    @Setter
    private Callback callback;
    @Getter
    private Timer timer;
    private static final String SERVER_HOSTNAME = "wss://cryp.tf/";
    private static final String SERVER_URI = SERVER_HOSTNAME +"ws/";
    private static final String CREATE_GAME_URI = SERVER_HOSTNAME +"create_game/"; // Le nom de domaine de mon serveur
    private static final String JOIN_GAME_URI = SERVER_HOSTNAME +"join_game/";
    private boolean joiner;
    @Getter
    private long id;
    @Getter
    private int nbVertices;
    private Game game;
    @Getter
    @Setter
    private String waiting;
    @Getter
    @Setter
    private static String pseudoCUT = "A";
    @Getter
    @Setter
    private static String pseudoSHORT = "B";

    public WebSocketClient(int nbVertices, Turn turn) throws IOException, URISyntaxException, InterruptedException {
        this.joiner = false;
        int creatorTurn = turn == Turn.CUT ? 0 : 1;
        if (!this.isClosed())
            this.close();
        this.connectServer(CREATE_GAME_URI +creatorTurn+"/"+nbVertices+"/"+pseudoCUT);
        createConnection();
    }

    public WebSocketClient(long id) throws IOException, URISyntaxException, InterruptedException {
        this.joiner = true;
        if (!this.isClosed())
            this.close();
        this.connectServer(JOIN_GAME_URI + id + "/"+pseudoSHORT);
        createConnection();
    }

    private boolean doServerRespond() throws InterruptedException {
        int count = 0;
        while(response == null) {
            if (count > 1000) {
                return false; // Le serveur ne répond pas
            }
            Thread.sleep(1); // On attend que le serveur réponde
            count++;
        }
        return true;
    }

    private void createConnection() throws InterruptedException, IOException {
        if (!doServerRespond()) throw new IOException();
        if (response.equals("Not found")) {
            Platform.runLater(() -> Gui.popupMessage("Le code rentré est incorrect", "Aucune partie trouvée."));
            throw new IOException();
        }
        JsonElement jsonElement = JsonParser.parseString(response);
        this.id = jsonElement.getAsJsonObject().get("id").getAsLong();
        this.nbVertices = jsonElement.getAsJsonObject().get("nb_vertices").getAsInt();
        // Permet d'envoyer à intervalle régulier des ping au serveur pour garder en vie les
        // connexions websocket qui ont une durée de vie de 120s ou 180s
        if (timer != null)
            timer.cancel(); // Cas qui peut se produire si l'on fait plusieurs parties à la suite
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                if (game != null && (game.getCutWon() || game.getShortWon())) // Si la game est finie, on cancel le timer
                    getTimer().cancel();
                if (session != null && session.isOpen())
                    sendMessage("Ping");
            }
        },0,60000);
    }

    public WebSocketClient() {}
    public void reConnect(String serverUri) throws URISyntaxException, IOException {
        if (isClosed()) {
            connectServer(serverUri);
        }
    }

    public Game connect(Callback function) throws IOException, TimeoutException {
        try {
            JsonElement jsonElement = JsonParser.parseString(response);
            long seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            this.connectServer(SERVER_URI + this.id);
            Turn turn = jsonElement.getAsJsonObject().get("creator_turn").getAsString().equals("Short") ? Turn.SHORT : Turn.CUT;
            this.game = new Game(this.nbVertices, this.id, joiner, this, SERVER_URI + this.id, seed, turn);
            this.setCallback(function);
            return game;
        } catch (URISyntaxException | NullPointerException e) {
            this.close();
            return null;
        }
    }

    public void connectServer(String serverUri) throws URISyntaxException, IOException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        try {
            container.connectToServer(this, new URI(serverUri));
        }
        catch (DeploymentException handshakeError) {
            closed = true;
            return;
        }
        catch (Exception e) {
            closed = true;
            throw new IOException();
        }
        closed = false;
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        }
        catch (Exception e) {
            if (closed) {
                log.error("Déconnexion inattendue");
            }
        }
    }

    public static void getHandshake() {
        try {
            WebSocketClient clientHandshake = new WebSocketClient();
            clientHandshake.connectServer(SERVER_HOSTNAME);
            if (!clientHandshake.isClosed())
                clientHandshake.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    public void setGame(Game game) {
        if (this.game == null)
            this.game = game;
    }

    public void close() throws IOException {
        if (session != null && session.isOpen())
        {
            session.close();
            closed = true;
            if (timer != null) timer.cancel();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        closed = false;
        log.info("Connected to server");
    }

    @OnMessage
    public void onMessage(String message) {
        if (closed) return;
        response = message;
        log.info("Received message: " + message);
        try {
            JsonElement jsonElement = JsonParser.parseString(message);
            JsonElement stats = jsonElement.getAsJsonObject().get("stats");
            if (!stats.isJsonNull()) return;
        }
        catch (JsonSyntaxException | NullPointerException | IllegalStateException ignored) {}
        if (message.startsWith("{")) {
            if (callback != null)
                callback.call();
            return;
        }
        else if (message.equals("Not found")) {
            return;
        }
        if (!message.equals("Pong"))
        {
            if (game != null)
                try {
                    game.play1vs1(message);
                }
                catch (IOException e) {
                    log.error("Erreur lors de la réception du message");
                }
            else
                waiting = message;
        }
    }
    @OnClose
    public void onClose() {
        if (timer != null)
            timer.cancel();
        closed = true;
        log.info("Connection closed");
    }
}

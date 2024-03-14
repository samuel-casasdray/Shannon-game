package fr.projet.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
    private static Timer timer;
    private static final String SERVER_HOSTNAME = "wss://cryp.tf/";
    private static final String SERVER_URI = SERVER_HOSTNAME +"ws/";
    private static final String CREATE_GAME_URI = SERVER_HOSTNAME +"create_game/"; // Le nom de domaine de mon serveur
    private static final String JOIN_GAME_URI = SERVER_HOSTNAME +"join_game/";
    private boolean joiner;
    @Getter
    private long id;
    @Getter
    private int nbVertices;
    public WebSocketClient(int nbVertices, long id, boolean joiner, Turn turn) throws IOException, URISyntaxException, InterruptedException {
        int creatorTurn = turn == Turn.CUT ? 0 : 1;
        if (joiner) {
            if (!this.isClosed())
                this.close();
            this.connectServer(JOIN_GAME_URI + id);
        }
        else {
            if (!this.isClosed())
                this.close();
            this.connectServer(CREATE_GAME_URI +creatorTurn+"/"+nbVertices);
        }
        this.joiner = joiner;
        int count = 0;
        while(response == null) {
            if (count > 50) {
                return; // Le serveur ne répond pas
            }
            response = this.getResponse();
            if (response == null)
                Thread.sleep(100); // On attend que le serveur réponde
            count++;
        }
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
                    WebSocketClient.getTimer().cancel();
                if (session != null && session.isOpen())
                    sendMessage("Ping");
            }
        },0,60000);
    }

    public WebSocketClient() {}
    private Game game = null;

    public void reConnect(String serverUri) throws URISyntaxException, IOException {
        if (isClosed()) {
            connectServer(serverUri);
        }
    }

    public Game connect(Callback function) throws IOException {
        try {
            JsonElement jsonElement = JsonParser.parseString(response);
            long seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            this.connectServer(SERVER_URI + this.id);
            Turn turn = jsonElement.getAsJsonObject().get("creator_turn").getAsString().equals("Short") ? Turn.SHORT : Turn.CUT;
            this.game = new Game(this.nbVertices, this.id, joiner, this, SERVER_URI + this.id, seed, turn);
            this.setCallback(function);
            return game;
        } catch (Exception e) {
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
    public void onMessage(String message) throws IOException {
        response = message;
        log.info("Received message: " + message);
        if (message.startsWith("{")) {
            if (callback != null)
                callback.call();
            return;
        }
        else if (message.equals("Not found")) {
            return;
        }
        if (!message.equals("Pong"))
            game.play1vs1(message);
    }
    @OnClose
    public void onClose() {
        if (timer != null)
            timer.cancel();
        closed = true;
        log.info("Connection closed");
    }
}

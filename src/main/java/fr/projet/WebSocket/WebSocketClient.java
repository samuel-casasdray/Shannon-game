package fr.projet.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.projet.Callback;
import fr.projet.game.Game;
import fr.projet.game.Turn;
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
    private static final String serverHostname = "wss://cryp.tf/";
    private static final String serverUri = serverHostname+"ws/";
    private static final String CreateGameUri = serverHostname+"create_game/"; // Le nom de domaine de mon serveur
    private static final String JoinGameUri = serverHostname+"join_game/";
    private boolean joiner;
    @Getter
    private long id;
    public WebSocketClient(long id, boolean joiner, Turn turn) throws IOException, URISyntaxException, InterruptedException {
        int creatorTurn = turn == Turn.CUT ? 0 : 1;
        if (joiner) {
            if (!this.isClosed())
                this.close();
            this.connectServer(JoinGameUri + id);
        }
        else {
            if (!this.isClosed())
                this.close();
            this.connectServer(CreateGameUri+creatorTurn);
        }
        this.joiner = joiner;
        int count = 0;
        while(response == null) {
            if (count > 50) {
                return; // Le serveur ne répond pas
            }
            response = this.getResponse();
            Thread.sleep(100); // On attend que le serveur réponde
            count++;
        }
        JsonElement jsonElement = JsonParser.parseString(response);
        this.id = jsonElement.getAsJsonObject().get("id").getAsLong();
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

    public void reConnect(String serverUri) throws DeploymentException, URISyntaxException, IOException, InterruptedException {
        if (isClosed()) {
            connectServer(serverUri);
        }
    }

    public Game connect(Callback function) throws IOException {
        try {
            JsonElement jsonElement = JsonParser.parseString(response);
            long seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            this.connectServer(serverUri + this.id);
            Turn turn = jsonElement.getAsJsonObject().get("creator_turn").getAsString().equals("Short") ? Turn.SHORT : Turn.CUT;
            Game game = new Game(this.id, joiner, this, serverUri + this.id, seed, turn);
            this.setGame(game);
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
            clientHandshake.connectServer(serverHostname);
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
            session.close();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        closed = false;
        log.info("Connected to server");
    }

    @OnMessage
    public void onMessage(String message) throws IOException, DeploymentException, URISyntaxException, InterruptedException {
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

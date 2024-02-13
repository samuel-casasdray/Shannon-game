package fr.projet.WebSocket;

import fr.projet.Callback;
import fr.projet.game.Game;
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
    public WebSocketClient(boolean timer) {
        if (timer) {
            // Permet d'envoyer à intervalle régulier des ping au serveur pour garder en vie les
            // connexions websocket qui ont une durée de vie de 120s ou 180s
            WebSocketClient.timer = new Timer();
            WebSocketClient.timer.scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    if (game != null && (game.getCutWon() || game.getShortWon())) WebSocketClient.getTimer().cancel();
                    if (session != null && session.isOpen())
                        sendMessage("Ping");
                }
            },0,60000);
        }
    }

    private Game game = null;

    public void reConnect(String serverUri) throws DeploymentException, URISyntaxException, IOException, InterruptedException {
        if (isClosed()) {
            connect(serverUri);
        }
    }

    public void connect(String serverUri) throws URISyntaxException, DeploymentException, InterruptedException, IOException {
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

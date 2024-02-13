package fr.projet.serverClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.projet.Callback;
import fr.projet.WebSocket.WebSocketClient;
import fr.projet.game.Game;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;

@Setter
@Getter
@Slf4j
public class Client {
    @Getter
    private long id;
    private static final String serverHostname = "wss://cryp.tf/";
    private static final String serverUri = serverHostname+"ws/";
    private static final String CreateGameUri = serverHostname+"create_game"; // Le nom de domaine de mon serveur
    private static final String JoinGameUri = serverHostname+"join_game/";
    private final WebSocketClient client = new WebSocketClient(true);
    private final boolean joiner;
    private String response = null;
    public Client(long id, boolean joiner) throws DeploymentException, URISyntaxException, IOException, InterruptedException {
        if (joiner) {
            if (!client.isClosed())
                client.close();
            client.connect(JoinGameUri + id);
        }
        else {
            if (!client.isClosed())
                client.close();
            client.connect(CreateGameUri);
        }
        this.joiner = joiner;
        int count = 0;
        while(response == null) {
            if (count > 50) {
                return; // Le serveur ne répond pas
            }
            response = client.getResponse();
            Thread.sleep(100);
            count++;
        }
        JsonElement jsonElement = JsonParser.parseString(response);
        this.id = jsonElement.getAsJsonObject().get("id").getAsLong();
    }

    public static void getHandshake() {
        try {
            WebSocketClient client = new WebSocketClient(false);
            client.connect(serverHostname);
            if (!client.isClosed())
                client.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    public Game connect(Callback function) throws IOException {
        try {
            JsonElement jsonElement = JsonParser.parseString(response);
            long seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            client.connect(serverUri + this.id);
            Game game = new Game(this.id, joiner, client, serverUri + this.id, seed);
            client.setGame(game);
            client.setCallback(function);
            return game;
        } catch (Exception e) {
            client.close();
            return null;
        }
    }

}

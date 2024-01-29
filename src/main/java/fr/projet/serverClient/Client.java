package fr.projet.serverClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.projet.Callback;
import fr.projet.WebSocket.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Turn;
import lombok.Getter;
import lombok.Setter;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Function;

@Setter
@Getter
public class Client {
    @Getter
    private long id;
    private final String serverUri = "ws://51.75.126.59:2999/ws/:";
    private final String CreateGameUri = "ws://51.75.126.59:2999/create_game";
    private final String JoinGameUri = "ws://51.75.126.59:2999/join_game/:";
    private final WebSocketClient client = new WebSocketClient();
    private final boolean joiner;
    public Client(long id, boolean joiner) throws DeploymentException, URISyntaxException, IOException, InterruptedException {
        if (joiner) {
            client.connect(JoinGameUri + id);
        }
        else {
            client.connect(CreateGameUri);
        }
        this.joiner = joiner;
        while (client.getResponse() == null) {
            Thread.sleep(1);
        }
        JsonElement jsonElement = JsonParser.parseString(client.getResponse());
        this.id = jsonElement.getAsJsonObject().get("id").getAsInt();
    }

    public Game connect(Callback function) {
        try {
            JsonElement jsonElement = JsonParser.parseString(client.getResponse());
            long seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            client.connect(serverUri + this.id);
            Game game = new Game(this.id, joiner, client, serverUri + this.id, seed);
            client.setGame(game);
            client.setCallback(function);
            return game;
        } catch (Exception e) {
            return null;
        }
    }

}

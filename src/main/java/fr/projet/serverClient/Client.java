package fr.projet.serverClient;
import fr.projet.game.Game;
import lombok.Getter;
import lombok.Setter;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;
@Setter
@Getter
public class Client {
    private long id;
    public Client()  {

    }
    public void CreateGame() {
        try {
            new Game(0, false);
        }
        catch (NumberFormatException | URISyntaxException e) {
            System.out.println("error");
        } catch (IOException | InterruptedException ignored) {}
        catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    public void JoinGame(long id) {
        try {
            new Game(id, true);
        }
        catch (NumberFormatException | URISyntaxException e) {
            System.out.println("error");
        } catch (IOException | InterruptedException | DeploymentException e) {
            throw new RuntimeException(e);
        }
    }
}

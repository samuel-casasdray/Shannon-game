package fr.projet;

import fr.projet.gui.GuiScene;
import fr.projet.server.WebSocketClient;
import fr.projet.gui.Gui;
import javafx.application.Application;

import java.util.Timer;

public class Main {
    public static void main(String[] args) {
        Application.launch(Gui.class);
        Timer timer = WebSocketClient.getTimer();
        if (timer != null) timer.cancel();
        System.exit(0);
    }
}

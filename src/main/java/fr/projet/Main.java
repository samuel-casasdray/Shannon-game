package fr.projet;

import fr.projet.WebSocket.WebSocketClient;
import fr.projet.gui.Gui;
import javafx.application.Application;

import java.util.Timer;

public class Main {
    public static void main(String[] args) {
        Application.launch(Gui.class);
        Timer timer = WebSocketClient.getTimer();
        if (timer != null) timer.cancel();
    }
}

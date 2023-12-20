package fr.projet;

import fr.projet.Gui.Gui;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        Gui.setNbVertex(8);
        Application.launch(Gui.class, args);
    }
}
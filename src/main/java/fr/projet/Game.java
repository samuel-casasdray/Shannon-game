package fr.projet;

import fr.projet.Gui.Gui;
import javafx.application.Application;

public class Game {
    Graph graph;
    Turn turn = Turn.CUT;

    public Game() {
        graph = new Graph(8);
        Gui.setGraph(graph);
        Application.launch(Gui.class);
    }

    public void Play() {
        if (turn == Turn.CUT) {
            turn = Turn.SHORT;
        } else {
            turn = Turn.CUT;
        }
    }
}

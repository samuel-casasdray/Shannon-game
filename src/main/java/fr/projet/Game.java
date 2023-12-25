package fr.projet;

import fr.projet.Graph.Graph;
import fr.projet.Graph.Vertex;
import fr.projet.Gui.Gui;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;

public class Game {
    Graph graph;
    Turn turn = Turn.CUT;
    EventHandler<MouseEvent> handler = mouseEvent -> {
        if (mouseEvent.getSource() instanceof Line) {
            Pair<Vertex, Vertex> pair = (Pair<Vertex, Vertex>) ((Line) mouseEvent.getSource()).getProperties().get("pair");
            for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                if (Vertex.isSameCouple(pair, neighbors.getKey())) {
                    if (pair.getKey().isCutOrPanted(pair.getValue())) return;
                    if (turn == Turn.CUT) {
                        pair.getKey().cut(pair.getValue());
                        neighbors.getValue().getStrokeDashArray().addAll(25d, 10d);
                    } else {
                        pair.getKey().paint(pair.getValue());
                        neighbors.getValue().setStroke(Color.RED);
                    }
                    turn = turn.flip();
                    return;
                }
            }
        }
    };

    public Game() {
        graph = new Graph(8);
        Gui.setGraph(graph);
        Gui.setHandler(handler);
        new Thread(() -> Application.launch(Gui.class)).start();
    }
}

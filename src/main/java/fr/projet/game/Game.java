package fr.projet.game;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;

public class Game {
    private Graph graph;
    private Turn turn = Turn.CUT;
    private boolean cutWon = false;
    public Game() {
        int nbVertices = 8;
        graph = new Graph(nbVertices);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices);
        }
        Gui.setGraph(graph);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }

    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon) return;
        if (mouseEvent.getSource() instanceof Line line &&
                line.getProperties().get("pair") instanceof Pair<?, ?> pair1 &&
                pair1.getKey() instanceof Vertex key && pair1.getValue() instanceof Vertex value) {
            for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                if (Vertex.isSameCouple(new Pair<>(key, value), neighbors.getKey())) {
                    if (key.isCutOrPanted(value)) {
                        return;
                    }
                    if (turn == Turn.CUT) {
                        key.cut(value);
                        neighbors.getValue().getStrokeDashArray().addAll(25D, 10D);
                    } else {
                        key.paint(value);
                        neighbors.getValue().setStroke(Color.RED);
                    }
                    turn = turn.flip();
                    if (graph.cutWon()) {
                        System.out.println("CUT a gagné");
                        cutWon = true;
                    }
                    return;
                }
            }
        }
        if (graph.cutWon()) {
            System.out.println("CUT a gagné");
            cutWon = true;
        }
    }
}

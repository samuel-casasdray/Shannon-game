package fr.projet.game;

import fr.projet.IA.BasicAI;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Game {
    private Graph graph;
    private Turn turn = Turn.CUT;
    @Setter
    private boolean cutWon = false;
    private boolean againstAI = true;
    private BasicAI ia;

    public Game() {
        int nbVertices = 8;
        graph = new Graph(nbVertices);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices);
        }
        ia = new BasicAI(this, turn);
        if (againstAI)
        Gui.setIa(ia);
        Gui.setGraph(graph);
        Gui.setGame(this);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }

    private void play(Vertex key, Vertex value) {
        if (cutWon) return;
        if (againstAI && turn == Turn.CUT) {
            Pair<Vertex, Vertex> v = ia.play();
            v.getKey().cut(v.getValue());
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(v)) {
                    element.getValue().getStrokeDashArray().addAll(25D, 10D);
                    break;
                }
            }
            turn = turn.flip();
            if (graph.cutWon()) {
                cutWon = true;
                return;
            }
        } else {
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
                        cutWon = true;
                    }
                    if (againstAI) {
                        play(key, value);
                    }
                    return;
                }
            }
        }

    }

    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon)
            return;
        if (mouseEvent.getSource() instanceof Line line &&
                line.getProperties().get("pair") instanceof Pair<?, ?> pair1 &&
                pair1.getKey() instanceof Vertex key && pair1.getValue() instanceof Vertex value) {
            play(key, value);
        }
        if (cutWon) {
            System.out.println("CUT a gagné");
            cutWon = true;
        }

    }
}

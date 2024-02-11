package fr.projet.IA;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import fr.projet.game.Game;
import fr.projet.game.Turn;

public class BasicAI implements InterfaceIA {
    Game game;
    Turn plays;
    Graph graph;

    public BasicAI(Game game, Turn plays) {
        this.game = game;
        this.plays = plays;
        this.graph = game.getGraph();
    }

    public Pair<Vertex, Vertex> playCUT() {
        boolean cutWonState = game.getCutWon();
        for (int i = graph.getNeighbors().size() - 1; i > 0; i--) {
            var element = graph.getNeighbors().get(i);
            graph.removeNeighbor(element);
            if (game.cutWon()) {
                game.setCutWon(cutWonState);
                for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                    if (Vertex.isSameCouple(new Pair<>(element.getKey(), element.getValue()), neighbors.getKey())) {
                        if (!element.getKey().isCut(element.getValue())
                                && !element.getKey().isPainted(element.getValue()))
                            return element;
                        else
                            graph.addNeighbor(element);
                    }
                }
            } else {
                graph.addNeighbor(element);
            }
        }
        game.setCutWon(cutWonState);
        for (var x : graph.getNeighbors()) {
            if (!x.getKey().isCut(x.getValue()) && !x.getKey().isPainted(x.getValue()))
                return x;
        }
        return graph.getNeighbors().getFirst();
    }

    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        return null;
    }

}

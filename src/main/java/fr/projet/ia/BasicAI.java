package fr.projet.ia;

import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import fr.projet.game.Game;
import fr.projet.game.Turn;

public class BasicAI extends InterfaceIA {

    public BasicAI(Game game, Turn plays) {
        super(game, plays);
    }

    public Pair<Vertex, Vertex> playCUT() {
        boolean cutWonState = game.getCutWon();
        for (int i = graph.getNeighbors().size() - 1; i > 0; i--) {
            var element = graph.getNeighbors().get(i);
            graph.removeNeighbor(element);
            if (game.cutWon()) {
                graph.addNeighbor(element);
                game.setCutWon(cutWonState);
                for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                    if (Vertex.isSameCouple(new Pair<>(element.getKey(), element.getValue()), neighbors.getKey())) {
                        if (!element.getKey().isCut(element.getValue())
                            && !element.getKey().isPainted(element.getValue()))
                        {
                            game.setCutWon(cutWonState);
                            return element;
                        }
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
        return playCUT(); // Move de génie
    }

}
package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

import java.util.List;
import java.util.Set;

public class WinnerStrat extends InterfaceIA {
    private List<Graph> stratGagnante;
    private Set<Pair<Vertex, Vertex>> toCut;
    public WinnerStrat(Game game, Turn plays, List<Graph> stratGagnante) {
        super(game, plays);
        this.stratGagnante = stratGagnante;
        this.toCut = stratGagnante.getFirst().getNeighbors();
    }

    public Pair<Vertex, Vertex> playCUT() {
        if (!toCut.isEmpty()) {
            Pair<Vertex, Vertex> theEdge = null;
            for (Pair<Vertex, Vertex> edge : toCut) {
                if (!game.getCutted().contains(edge) && !game.getSecured().contains(edge)) {
                    theEdge = edge;
                    break;
                }
            }
            if (theEdge != null) {
                toCut.remove(theEdge);
                return theEdge;
            }
        }
        return null;
    }

    public Pair<Vertex, Vertex> playSHORT() {
        return null;
    }
}

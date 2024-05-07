package fr.projet.ia;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;
import fr.projet.game.Game;
import fr.projet.game.Turn;
import java.util.HashSet;
import java.util.Set;

public class BasicAI extends InterfaceIA {

    public BasicAI(Game game, Turn plays) {
        super(game, plays);
    }

    public Pair<Vertex, Vertex> playCUT() {
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();
        Set<Pair<Vertex, Vertex>> notCutted = new HashSet<>();
        for (Pair<Vertex, Vertex> edge : game.getGraph().getNeighbors()) {
            if (!game.getCutted().contains(edge) && !game.getSecured().contains(edge)) {
                edges.add(edge);
            }
            if (!game.getCutted().contains(edge))
                notCutted.add(edge);
        }
        Graph notCuttedGraph = new Graph(notCutted);
        for (Pair<Vertex, Vertex> edge : edges) {
            notCuttedGraph.removeNeighbor(edge);
            if (!notCuttedGraph.estConnexe()) return edge;
            notCuttedGraph.addNeighbor(edge);
        }
        return edges.iterator().next();
    }

    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();
        Set<Pair<Vertex, Vertex>> redEdges = game.getSecured();
        for (Pair<Vertex, Vertex> edge : game.getGraph().getNeighbors()) {
            if (!game.getCutted().contains(edge) && !game.getSecured().contains(edge)) {
                edges.add(edge);
            }
        }
        Graph redGraph = new Graph(redEdges);
        for (Pair<Vertex, Vertex> edge : edges) {
            redGraph.addNeighbor(edge);
            if (game.getGraph().estCouvrant(redGraph)) return edge;
            redGraph.removeNeighbor(edge);
        }
        return playCUT();
    }
}
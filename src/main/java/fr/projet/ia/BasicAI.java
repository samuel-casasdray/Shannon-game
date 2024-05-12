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
        for (Pair<Vertex, Vertex> edge : game.getGraph().getEdges()) {
            if (!game.isCutted(edge) && !game.isSecured(edge)) {
                edges.add(edge);
            }
            if (!game.isCutted(edge)) {
                notCutted.add(edge);
            }
        }
        Graph notCuttedGraph = new Graph(notCutted);
        for (Pair<Vertex, Vertex> edge : edges) {
            notCuttedGraph.removeNeighbor(edge);
            if (!notCuttedGraph.estConnexe()) return edge;
            notCuttedGraph.addEdge(edge);
        }
        return edges.iterator().next();
    }

    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();
        Set<Pair<Vertex, Vertex>> redEdges = game.getSecured();
        for (Pair<Vertex, Vertex> edge : game.getGraph().getEdges()) {
            if (!game.isCutted(edge) && !game.isSecured(edge)) {
                edges.add(edge);
            }
        }
        Graph redGraph = new Graph(redEdges);
        for (Pair<Vertex, Vertex> edge : edges) {
            redGraph.addEdge(edge);
            if (game.getGraph().estCouvrant(redGraph)) return edge;
            redGraph.removeNeighbor(edge);
        }
        return playCUT();
    }
}
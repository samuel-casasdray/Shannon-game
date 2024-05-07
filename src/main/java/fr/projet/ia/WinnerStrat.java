package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WinnerStrat extends InterfaceIA {
    private Set<Pair<Vertex, Vertex>> toCut;
    private Graph T1;
    private Graph T2;
    public WinnerStrat(Game game, Turn plays, List<Graph> stratGagnante) {
        super(game, plays);
        this.toCut = stratGagnante.getFirst().getNeighbors();
        this.T1 = stratGagnante.getFirst();
        this.T2 = stratGagnante.get(1);
    }

    public Pair<Vertex, Vertex> playCUT() {
        if (!toCut.isEmpty()) {
            Pair<Vertex, Vertex> theEdge = null;
            for (Pair<Vertex, Vertex> edge : toCut) {
                if (!game.isCutted(edge) && !game.isSecured(edge)) {
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


    public Graph modEgdeGraph (Graph g, Set<Pair<Vertex,Vertex>> edgesAdd, Set<Pair<Vertex,Vertex>> edgesDel) {
        for (Pair<Vertex,Vertex> e : edgesAdd) {
            if (!g.getNeighbors().contains(e) && !g.getNeighbors().contains(Graph.reverseEdge(e))) {
                g.addNeighbor(e);
            }
        }
        for (Pair<Vertex,Vertex> e : edgesDel) {
            if (g.getNeighbors().contains(e)) {
                g.removeNeighbor(e);
            }
            if (g.getNeighbors().contains(Graph.reverseEdge(e))) {
                g.removeNeighbor(Graph.reverseEdge(e));
            }
        }
        for (Vertex v : this.graph.getVertices()) {
            if (!g.getVertices().contains(v)) {
                g.addVertex(v);
            }
        }
        return g;
    }

    public Pair<Vertex, Vertex> playSHORT() {
        Set<Pair<Vertex, Vertex>> securedInit = new HashSet<>(game.getSecured());
        Set<Pair<Vertex, Vertex>> cuttedInit = new HashSet<>(game.getCutted());
        Graph T1mod = modEgdeGraph(T1.copy(),securedInit,cuttedInit);
        Graph T2mod = modEgdeGraph(T2.copy(),securedInit,cuttedInit);
        if (T1mod.estConnexe() && T2mod.estConnexe()) {
            for (Pair<Vertex,Vertex> e : graph.getNeighbors()) {
                if (!securedInit.contains(e) && !cuttedInit.contains(e)) {
                    return e;
                }
            }
        }
        if (!T1mod.estConnexe()) {
            for (Pair<Vertex, Vertex> e : T2mod.getNeighbors()) {
                boolean allReadyIn = T1mod.getNeighbors().contains(e) || T1mod.getNeighbors().contains(Graph.reverseEdge(e));
                if (!allReadyIn) {
                    T1mod.addNeighbor(e);
                }
                if (T1mod.estConnexe() && !securedInit.contains(e) && !cuttedInit.contains(e) && !securedInit.contains(Graph.reverseEdge(e)) && !cuttedInit.contains(Graph.reverseEdge(e))) {
                    if (graph.getNeighbors().contains(Graph.reverseEdge(e))) {
                        e = Graph.reverseEdge(e);
                    }
                    return e;
                }
                if (!allReadyIn) {
                    T1mod.removeNeighbor(e);
                }
            }
        }
        if (!T2mod.estConnexe()) {
            for (Pair<Vertex, Vertex> e : T1mod.getNeighbors()) {
                boolean allReadyIn = T2mod.getNeighbors().contains(e) || T2mod.getNeighbors().contains(Graph.reverseEdge(e));
                if (!allReadyIn) {
                    T2mod.addNeighbor(e);
                }
                if (T2mod.estConnexe() && !securedInit.contains(e) && !cuttedInit.contains(e) && !securedInit.contains(Graph.reverseEdge(e)) && !cuttedInit.contains(Graph.reverseEdge(e))) {
                    if (graph.getNeighbors().contains(Graph.reverseEdge(e))) {
                        e = Graph.reverseEdge(e);
                    }
                    return e;
                }
                if (!allReadyIn) {
                    T2mod.removeNeighbor(e);
                }
            }
        }
        return null;
    }
}
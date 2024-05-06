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
    private List<Graph> stratGagnante;
    private Set<Pair<Vertex, Vertex>> toCut;
    private Graph T1;
    private Graph T2;

    public WinnerStrat(Game game, Turn plays, List<Graph> stratGagnante) {
        super(game, plays);
        this.stratGagnante = stratGagnante;
        this.toCut = stratGagnante.getFirst().getNeighbors();
        this.T1 = stratGagnante.getFirst();
        this.T2 = stratGagnante.get(1);
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

    public static Graph modEgdeGraph (Graph g, HashSet<Pair<Vertex,Vertex>> edgesAdd, HashSet<Pair<Vertex,Vertex>> edgesDel) {
        for (Pair<Vertex,Vertex> e : edgesAdd) {
            g.addNeighbor(e);
        }
        for (Pair<Vertex,Vertex> e : edgesDel) {
            if (g.getNeighbors().contains(e)) {
                g.removeNeighbor(e);
            }
            if (g.getNeighbors().contains(Graph.reverseEdge(e))) {
                g.removeNeighbor(Graph.reverseEdge(e));
            }
        }
        return g;
    }

    public Pair<Vertex, Vertex> playSHORT() {
        HashSet<Pair<Vertex, Vertex>> securedInit = new HashSet<>(game.getSecured());
        HashSet<Pair<Vertex, Vertex>> cuttedInit = new HashSet<>(game.getCutted());
        Graph T1mod = modEgdeGraph(T1.copy(),securedInit,cuttedInit);
        Graph T2mod = modEgdeGraph(T2.copy(),securedInit,cuttedInit);
        if (T1mod.estConnexe() && T2mod.estConnexe()) {
            for (Pair<Vertex,Vertex> e : graph.getNeighbors()) {
                if (!securedInit.contains(e) && !cuttedInit.contains(e) && !game.getCutted().contains(e) && !game.getSecured().contains(e)){
                    return e;
                }
            }
        }
        if (!T1mod.estConnexe())
            for (Pair<Vertex,Vertex> e : T2mod.getNeighbors()) {
                T1mod.addNeighbor(e);
                if (T1mod.estConnexe() && !game.getCutted().contains(e) && !game.getSecured().contains(e)) {
                    return e;
                }
                T1mod.removeNeighbor(e);
            }
        if (!T2mod.estConnexe())
            for (Pair<Vertex,Vertex> e : T1mod.getNeighbors()) {
                T2mod.addNeighbor(e);
                if (T2mod.estConnexe() && !game.getCutted().contains(e) && !game.getSecured().contains(e)) {
                    return e;
                }
                T2mod.removeNeighbor(e);
            }
        System.out.println("Erreur : pas d'arête à couper");
        return null;
    }
}

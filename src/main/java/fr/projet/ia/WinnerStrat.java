package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

import java.sql.PseudoColumnUsage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WinnerStrat extends InterfaceIA {
    private List<Graph> stratGagnante;
    private List<Pair<Vertex, Vertex>> toCut;
    private Graph T1;
    private Graph T2;
    public WinnerStrat(Game game, Turn plays, List<Graph> stratGagnante) {
        super(game, plays);
        this.stratGagnante = stratGagnante;
        this.toCut = new ArrayList<>(stratGagnante.getFirst().getNeighbors());
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
                System.out.println("The edge : " + theEdge);
                return theEdge;
            }
        }
        System.out.println("No more edges to cut");
        return null;
    }


    public Graph modEgdeGraph (Graph g, HashSet<Pair<Vertex,Vertex>> edgesAdd, HashSet<Pair<Vertex,Vertex>> edgesDel) {
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
        System.out.println("========================================");
        HashSet<Pair<Vertex, Vertex>> securedInit = new HashSet<>(game.getSecured());
        HashSet<Pair<Vertex, Vertex>> cuttedInit = new HashSet<>(game.getCutted());
        Graph T1mod = modEgdeGraph(T1.copy(),securedInit,cuttedInit);
        Graph T2mod = modEgdeGraph(T2.copy(),securedInit,cuttedInit);
        System.out.println(securedInit+"\n"+cuttedInit);
        System.out.println("ET : "+T1mod.estConnexe() +" "+ T2mod.estConnexe());
        System.out.println("test : ");
        for (Pair<Vertex,Vertex> e : graph.getNeighbors()) {
            if ((T1mod.getNeighbors().contains(e) && T2mod.getNeighbors().contains(e)) || (T1mod.getNeighbors().contains(Graph.reverseEdge(e)) && T2mod.getNeighbors().contains(Graph.reverseEdge(e)))) {
                System.out.println(e+" et "+securedInit.contains(e)+" "+securedInit.contains(Graph.reverseEdge(e)));
            }
        }
        if (T1mod.estConnexe() && T2mod.estConnexe()) {
            System.out.println("lol");
            for (Pair<Vertex,Vertex> e : graph.getNeighbors()) {
                if (!securedInit.contains(e) && !cuttedInit.contains(e)) {
                    return e;
                }
            }
        }
        if (!T1mod.estConnexe()) {
            System.out.println("T1");
            for (Pair<Vertex, Vertex> e : T2mod.getNeighbors()) {
                Boolean allReadyIn = true;
                if (!T1mod.getNeighbors().contains(e) && !T1mod.getNeighbors().contains(Graph.reverseEdge(e))) {
                    allReadyIn = false;
                }
                if (!allReadyIn) {
                    T1mod.addNeighbor(e);
                }
                if (T1mod.estConnexe() && !securedInit.contains(e) && !cuttedInit.contains(e) && !securedInit.contains(Graph.reverseEdge(e)) && !cuttedInit.contains(Graph.reverseEdge(e))) {
                    System.out.println("T1Valide");
                    //System.out.println(graph.getNeighbors().contains(e)+" "+graph.getNeighbors().contains(Graph.reverseEdge(e)));
                    if (graph.getNeighbors().contains(Graph.reverseEdge(e))) {
                        e = Graph.reverseEdge(e);
                    }
                    System.out.println(e);
                    return e;
                }
                if (!allReadyIn) {
                    T1mod.removeNeighbor(e);
                }
            }
        }
        if (!T2mod.estConnexe()) {
            System.out.println("T2");
            for (Pair<Vertex, Vertex> e : T1mod.getNeighbors()) {
                Boolean allReadyIn = true;
                if (!T2mod.getNeighbors().contains(e) && !T2mod.getNeighbors().contains(Graph.reverseEdge(e))) {
                    allReadyIn = false;
                }
                if (!allReadyIn) {
                    T2mod.addNeighbor(e);
                }
                //System.out.println(T2mod.estConnexe());
                if (T2mod.estConnexe() && !securedInit.contains(e) && !cuttedInit.contains(e) && !securedInit.contains(Graph.reverseEdge(e)) && !cuttedInit.contains(Graph.reverseEdge(e))) {
                    System.out.println("T2Valide");
                    //System.out.println(graph.getNeighbors().contains(e)+" "+graph.getNeighbors().contains(Graph.reverseEdge(e)));
                    if (graph.getNeighbors().contains(Graph.reverseEdge(e))) {
                        e = Graph.reverseEdge(e);
                    }
                    System.out.println(e);
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

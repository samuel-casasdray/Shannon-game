package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.max;
import static java.util.Collections.min;

public class Minimax extends InterfaceIA {

    //Pair<Vertex, Vertex> bestMove;

    int depth;
    int compteur=0;

    Graph graph;

    public Minimax(Game game, Turn plays, int depth) {
        super(game, plays);
        this.depth = depth;
        this.graph=game.getGraph();
    }


    public int evaluate2(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted) {
        //if (this.graph.difference(cutted)) System.out.println("OOFEJOZEJIOEIOZ");
        Graph testSecured = new Graph(secured);
        if (this.graph.difference(cutted)) return 10;
        if (this.graph.estCouvrant(testSecured)) return -10;
        return 0;
    }

    public int evaluate(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted) {
        int res = this.graph.getVertices().size();
        HashSet<Pair<Vertex, Vertex>> toTest = new HashSet<>(this.graph.getNeighbors());
        toTest.removeAll(cutted);
        toTest.removeAll(secured);
        for (Vertex v : this.graph.getVertices()) {
            int nbNeib=0;
            boolean notSec = true;
            for (Pair<Vertex,Vertex> s : secured) {
                if (s.getValue()==v || s.getKey()==v) {
                    notSec=false;
                }
            }
            for (Pair<Vertex, Vertex> edge : toTest) {
                if (edge.getValue()==v || edge.getKey()==v) {
                    nbNeib+=1;
                }
            }
            if (nbNeib<res) res=nbNeib;
        }
        //System.out.println(-res);
        return -res;
    }




    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        return null;
    }

    public Pair<Vertex, Vertex> playCUTtest() {
        HashSet<Pair<Vertex, Vertex>> secured = new HashSet<>(game.getSecured());
        Pair<Vertex, Vertex> solution = null;
        int res = -100000;
        for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
            solution = edge;
        }
        System.out.println(this.graph.difference(secured));
        return solution;
    }



    public Pair<Vertex, Vertex> playCUT() {
        HashSet<Pair<Vertex, Vertex>> securedInit = new HashSet<>(game.getSecured());
        HashSet<Pair<Vertex, Vertex>> cuttedInit = new HashSet<>(game.getCutted());
        int d=this.depth-1;
        Pair<Vertex, Vertex> solution = null;
        int res = -100000;
        //System.out.println("--------------------------------------------------------");
        for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
            if (!securedInit.contains(edge) && !cuttedInit.contains(edge)) {
                HashSet<Pair<Vertex, Vertex>> cuttedModif = new HashSet<>(cuttedInit);
                cuttedModif.add(edge);
                int call = minMaxCUT(securedInit, cuttedModif, d, 0);
                //System.out.println(call+" - "+edge+"--------    "+res);
                if (call>res) {
                    res=call;
                    solution=edge;
                }
            }
        }
        //System.out.println("--------------------------------------------------------\n"+solution);
        return solution;
    }


    public int minMaxCUT(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted, int d, int player) { //1 pour CUT 0 pour SHORT
        int eval = evaluate(secured, cutted);
        //System.out.println(eval);
        //System.out.println(cutted.size()+"   "+secured.size());
        //System.out.println(cutted+"    "+secured);
        //System.out.println(eval+" lol "+eval* (d + 1) * (d + 1));
        if (d == 0 || eval == -1) {
            return eval * (d + 1) * (d + 1);
        }
        int val = 0;
        if (player == 1) {
            val = -10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> cuttedSuite = new HashSet<>(cutted);
                    cuttedSuite.add(edge);
                    val = Math.max(minMaxCUT(secured, cuttedSuite, d - 1, 0), val);
                }
            }
        } else {
            val = 10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> securedSuite = new HashSet<>(secured);
                    securedSuite.add(edge);
                    val = Math.min(minMaxCUT(securedSuite, cutted, d - 1, 1), val);
                }
            }
        }
        return val;
    }














}



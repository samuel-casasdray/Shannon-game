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


    public int evaluate(Graph graph, HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted) {
        Graph testSecured = new Graph(secured);
        if (graph.difference(cutted)) return 10;
        if (graph.estCouvrant(testSecured)) return -10;
        return 0;
    }

    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        return null;
    }

    public Pair<Vertex, Vertex> playCUT() {
        HashSet<Pair<Vertex, Vertex>> secured = game.getSecured();
        HashSet<Pair<Vertex, Vertex>> cutted = game.getCutted();
        List<Pair<Integer, Pair<Vertex, Vertex>>> scores = new ArrayList<>();
        int depth=this.depth-1;
        for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
            if (!secured.contains(edge) && !cutted.contains(edge)) {
                HashSet<Pair<Vertex, Vertex>> cuttedSuite = new HashSet<>(cutted);
                cuttedSuite.add(edge);
                int res =minMaxCUT(secured, cuttedSuite, depth, 0);
                System.out.println("res : "+res);
                Pair<Integer, Pair<Vertex, Vertex>> scoreEdge = new Pair<>(res, edge);
                scores.add(scoreEdge);
            }
        }
        System.out.println(scores+"\n"+this.compteur+" "+this.depth);
        if (scores.isEmpty()) return null; // Cas à gérer quand l'IA ne peut plus jouer
        Pair<Integer, Pair<Vertex, Vertex>> max = scores.getFirst();
        for (Pair<Integer, Pair<Vertex, Vertex>> element : scores) {
            if (element.getKey() > max.getKey()) max = element;
        }
        return max.getValue();
    }


    public int minMaxCUT(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted, int depth, int player) { //1 pour CUT 0 pour SHORT
        //System.out.println(this.graph.getNeighbors().size());
        int eval = evaluate(this.graph, secured, cutted);
        if (this.graph.difference(cutted)) {
            System.out.println(cutted);
        }
        this.compteur+=1;
        System.out.println("depth : "+depth+" - "+eval*(depth+1)*(depth+1)*(depth+1)*(depth+1));
        if (depth <= 0 || eval == 10 || eval == -10) {
            return eval*(depth+1)*(depth+1)*(depth+1)*(depth+1);
        }
        //System.out.println(cutted.size()+secured.size()==this.graph.getNeighbors().size());
        int val;
        if (player == 1) {
            val = -100000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!secured.contains(edge) && !cutted.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> cuttedSuite = new HashSet<>(cutted);
                    cuttedSuite.add(edge);
                    depth-=1;
                    int res=minMaxCUT(secured, cuttedSuite, depth, 0);
                    if (res>val) val=res;
                    //System.out.println(score+" "+cutted.size());
                    depth+=1;
                }
            }
        }
        else {
            val = 100000;
            //System.out.println(secured+"             "+cutted);
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!secured.contains(edge) && !cutted.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> securedSuite = new HashSet<>(secured);
                    securedSuite.add(edge);
                    depth-=1;
                    int res=minMaxCUT(secured, securedSuite, depth, 0);
                    if (res<val) val=res;
                    depth+=1;
                }
            }
        }
        return val;
    }



}



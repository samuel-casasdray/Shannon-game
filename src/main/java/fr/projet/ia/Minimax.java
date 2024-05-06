package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Set;

public class Minimax extends InterfaceIA {

    //Pair<Vertex, Vertex> bestMove;
    int compteur=0;

    public Minimax(Game game, Turn plays, int depth) {
        super(game, plays, depth);
    }


    public int evaluate(Set<Pair<Vertex, Vertex>> secured, Set<Pair<Vertex, Vertex>> cutted) {
        //if (this.graph.difference(cutted)) System.out.println("OOFEJOZEJIOEIOZ");
        Graph testSecured = new Graph(secured);
        if (this.graph.difference(cutted)) return 1000;
        if (this.graph.estCouvrant(testSecured)) return -1000;
        return 0;
    }


    public int evaluateDegree (Set<Pair<Vertex, Vertex>> secured, Set<Pair<Vertex, Vertex>> cutted) {
        HashMap<Vertex,Integer> tab = new HashMap<>();
        HashMap<Vertex,Integer> link = new HashMap<>();
        int i=0;
        for (Vertex v : this.graph.getVertices()) {
            tab.put(v,0);
            link.put(v,i);
            i++;
        }
        for (Pair<Vertex,Vertex> e : this.graph.getNeighbors()) {
            if (secured.contains(e)) {
                int val = link.get(e.getKey());
                int toChange = link.get(e.getValue());
                for (Map.Entry<Vertex, Integer> entry : link.entrySet()) {
                    if (entry.getValue()==toChange) {
                        link.put(entry.getKey(),val);
                    }
                }
            }
            if (!cutted.contains(e) && !secured.contains(e)) {
                tab.put(e.getKey(), tab.get(e.getKey()) + 1);
                tab.put(e.getValue(), tab.get(e.getValue()) + 1);
            }
        }
        HashMap<Integer,Integer> scoreTot = new HashMap<>();
        for (Map.Entry<Vertex, Integer> entry : link.entrySet()) {
            if (!scoreTot.containsKey(entry.getValue())) {
                scoreTot.put(entry.getValue(),0);
                for (Map.Entry<Vertex, Integer> entry2 : link.entrySet()) {
                    if (Objects.equals(entry2.getValue(), entry.getValue())) {
                        scoreTot.put(entry.getValue(),scoreTot.get(entry.getValue())+tab.get(entry2.getKey()));
                    }
                }
            }
        }
        //System.out.println(scoreTot);
        //System.out.println(link);
        //System.out.println(tab);
        int minNombre=1000;
        for (Map.Entry<Integer, Integer> entry : scoreTot.entrySet()) {
            if (entry.getValue() < minNombre) {
                minNombre = entry.getValue();
            }
        }
        //System.out.println(minNombre);
        //System.out.println(v+" "+minNombre);
        return -minNombre*10;
    }




//    public int evaluate2(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted) {
//        int res = this.graph.getVertices().size();
//        HashSet<Pair<Vertex, Vertex>> toTest = new HashSet<>(this.graph.getNeighbors());
//        toTest.removeAll(cutted);
//        toTest.removeAll(secured);
//        for (Vertex v : this.graph.getVertices()) {
//            int nbNeib = 0;
//            boolean notSec = true;
//            for (Pair<Vertex, Vertex> s : secured) {
//                if (s.getValue() == v || s.getKey() == v) {
//                    notSec = false;
//                }
//            }
//            for (Pair<Vertex, Vertex> edge : toTest) {
//                if (edge.getValue() == v || edge.getKey() == v) {
//                    nbNeib += 1;
//                }
//            }
//            if (nbNeib < res) res = nbNeib;
//        }
//        //System.out.println(-res);
//        return -res;
//    }



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
        long res = -1000000000;
        //System.out.println("--------------------------------------------------------");
        for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
            if (!securedInit.contains(edge) && !cuttedInit.contains(edge)) {
                HashSet<Pair<Vertex, Vertex>> cuttedModif = new HashSet<>(cuttedInit);
                cuttedModif.add(edge);
                //int call = minMax(securedInit, cuttedModif, d, 0);
                int call = alpha_beta(securedInit, cuttedModif, d, 0,-10000,10000);
                //System.out.println(call+" - "+edge+"--------    "+res);
                if (call>res) {
                    res=call;
                    solution=edge;
                }
            }
        }
        //System.out.println("--------------------------------------------------------\n"+solution);
        evaluateDegree(game.getSecured(),game.getCutted());
        return solution;
    }


    public Pair<Vertex, Vertex> playSHORT() {
        HashSet<Pair<Vertex, Vertex>> securedInit = new HashSet<>(game.getSecured());
        HashSet<Pair<Vertex, Vertex>> cuttedInit = new HashSet<>(game.getCutted());
        int d=this.depth-1;
        Pair<Vertex, Vertex> solution = null;
        int res = 100000;
        //System.out.println("--------------------------------------------------------");
        for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
            if (!securedInit.contains(edge) && !cuttedInit.contains(edge)) {
                HashSet<Pair<Vertex, Vertex>> securedModif = new HashSet<>(securedInit);
                securedModif.add(edge);
                //int call = minMax(securedModif, cuttedInit, d, 1);
                int call = alpha_beta(securedModif, cuttedInit, d, 1,-10000,10000);
                //System.out.println(call+" - "+edge+"--------    "+res);
                if (call<res) {
                    res=call;
                    solution=edge;
                }
            }
        }
        //System.out.println("--------------------------------------------------------\n"+solution);
        return solution;
    }





    public int minMax(HashSet<Pair<Vertex, Vertex>> secured, Set<Pair<Vertex, Vertex>> cutted, int d, int player) { //1 pour CUT 0 pour SHORT
        int eval = evaluate(secured, cutted);
        //System.out.println(eval);
        //System.out.println(cutted.size()+"   "+secured.size());
        //System.out.println(cutted+"    "+secured);
        //System.out.println(eval+" lol "+eval* (d + 1) * (d + 1));
        if (d == 0 || eval != 0) {
            return eval;
        }
        int val = 0;
        if (player == 1) {
            val = -10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> cuttedSuite = new HashSet<>(cutted);
                    cuttedSuite.add(edge);
                    //val+=minMaxCUT(secured, cuttedSuite, d - 1, 0);
                    val = Math.max(minMax(secured, cuttedSuite, d - 1, 0), val);
                }
            }
        } else {
            val = 10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> securedSuite = new HashSet<>(secured);
                    securedSuite.add(edge);
                    val = Math.min(minMax(securedSuite, cutted, d - 1, 1), val);
                    //val+=minMaxCUT(securedSuite, cutted, d - 1, 1);
                }
            }
        }
        return val;
    }




    public int alpha_beta (Set<Pair<Vertex, Vertex>> secured, Set<Pair<Vertex, Vertex>> cutted, int d, int player, int alpha, int beta) { //1 pour CUT 0 pour SHORT
        int eval = evaluateDegree(secured, cutted);
        int eval2 = evaluate(secured, cutted);
        //int eval2 = evaluateDegree(secured, cutted);
        if (d == 0 || eval2 != 0) {
            return (int) (eval + eval2*Math.pow(2,(d+1)));
            //return (int) (eval*Math.pow(2,(d+1)))+eval2;
            //return (int) (eval*Math.pow(2,(d+1)))+eval2;
        }
        int val = 0;
        if (player == 1) {
            val = -10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> cuttedSuite = new HashSet<>(cutted);
                    cuttedSuite.add(edge);
                    val = Math.max(alpha_beta(secured, cuttedSuite, d - 1, 0,alpha,beta), val);
                    if (val>=beta) {
                        return val;
                    }
                    alpha=Math.max(alpha,val);
                }
            }
        } else {
            val = 10000;
            for (Pair<Vertex, Vertex> edge : this.graph.getNeighbors()) {
                if (!cutted.contains(edge) && !secured.contains(edge)) {
                    HashSet<Pair<Vertex, Vertex>> securedSuite = new HashSet<>(secured);
                    securedSuite.add(edge);
                    val = Math.min(alpha_beta(securedSuite, cutted, d - 1, 1,alpha,beta), val);
                    if (alpha>=val) {
                        return val;
                    }
                    beta=Math.min(beta,val);
                }
            }
        }
        return val;
    }













}





//public int evaluate(HashSet<Pair<Vertex, Vertex>> secured, HashSet<Pair<Vertex, Vertex>> cutted) {
//    int res = this.graph.getVertices().size();
//    HashSet<Pair<Vertex, Vertex>> toTest = new HashSet<>(this.graph.getNeighbors());
//    toTest.removeAll(cutted);
//    toTest.removeAll(secured);
//    for (Vertex v : this.graph.getVertices()) {
//        int nbNeib=0;
//        boolean notSec = true;
//        for (Pair<Vertex,Vertex> s : secured) {
//            if (s.getValue()==v || s.getKey()==v) {
//                notSec=false;
//            }
//        }
//        for (Pair<Vertex, Vertex> edge : toTest) {
//            if (edge.getValue()==v || edge.getKey()==v) {
//                nbNeib+=1;
//            }
//        }
//        if (nbNeib<res) res=nbNeib;
//    }
//    //System.out.println(-res);
//    return -res;
//}


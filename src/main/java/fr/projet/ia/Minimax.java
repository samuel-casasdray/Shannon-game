package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Minimax extends InterfaceIA {

    //Pair<Vertex, Vertex> bestMove;

    int depth;

    public Minimax(Game game, Turn plays, int depth) {
        super(game, plays);
        this.depth = depth;
    }

    public int sum (List<Pair<Integer, Pair<Vertex, Vertex>>> liste) {
        int sum = 0;
        for (Pair<Integer, Pair<Vertex, Vertex>> element : liste){
            sum+=element.getKey();
        }
        return sum;
    }

//    public Pair<Vertex, Vertex> playCUT() {
//        List<Pair<Vertex, Vertex>> AllEdges = this.graph.getNeighbors();
//        ArrayList<Pair<Vertex, Vertex>> secured = game.getSecured();
//        ArrayList<Pair<Vertex, Vertex>> cutted = game.getCutted();
//        List<Pair<Vertex, Vertex>> edges = new ArrayList<>();
//        for (Pair<Vertex, Vertex> toUse : AllEdges) {
//            if (!secured.contains(toUse) && !cutted.contains(toUse)) {
//                edges.add(toUse);
//            }
//        }

//        int value = 0;
//        Pair<Vertex, Vertex> res = edges.getFirst();
//        for (Pair<Vertex, Vertex> toCut : edges) {
//            ArrayList<Pair<Vertex, Vertex>> copieCutted = new ArrayList<>(cutted);
//            copieCutted.add(toCut);
//            int call = MinMax(secured, copieCutted, d - 1, false);
//            if (value < call) {
//                value = call;
//                res = toCut;
//            }
//        }
//        return res;
//    }

//    public int MinMax (ArrayList<Pair<Vertex, Vertex>> secured, ArrayList<Pair<Vertex, Vertex>> cutted, int depth, Boolean maximizingPlayer) {
//        List<Pair<int, Pair<Vertex, Vertex>>> score = new ArrayList<>();
//        List<Pair<Vertex, Vertex>> AllEdges = this.graph.getNeighbors();
//        List<Pair<Vertex, Vertex>> edges = new ArrayList<>();
//        for (Pair<Vertex, Vertex> toUse : AllEdges) {
//            if (!secured.contains(toUse) && !cutted.contains(toUse)) {
//                edges.add(toUse);
//            }
//        }
//        int value = 0;
//        if (depth == 0 || terminalNode(secured,cutted)) {
//            return evaluate(this.graph, secured, cutted);
//        }
//        if (maximizingPlayer) {
//            value = -1000000000;
//            for (Pair<Vertex, Vertex> toCut : edges) {
//                ArrayList<Pair<Vertex, Vertex>> copieCutted = new ArrayList<>(cutted);
//                copieCutted.add(toCut);
//                value = Math.max(value,MinMax(secured,copieCutted, depth-1, false));
//            }
//        }
//        else {
//            value = 1000000000;
//            for (Pair<Vertex, Vertex> toSecure : edges) {
//                ArrayList<Pair<Vertex, Vertex>> copieSecure = new ArrayList<>(secured);
//                copieSecure.add(toSecure);
//                value = Math.min(value,MinMax(copieSecure,cutted, depth-1, true));
//            }
//        }
//        return value;
//    }

//    public Boolean terminalNode (ArrayList<Pair<Vertex, Vertex>> secured, ArrayList<Pair<Vertex, Vertex>> cutted) {
//        Graph testSecured = new Graph(secured);
//        return this.graph.estCouvrant(testSecured) || this.graph.difference(cutted);
//    }

    public int evaluate (Graph graph, List<Pair<Vertex, Vertex>> secured, List<Pair<Vertex, Vertex>> cutted) {
        Graph testSecured = new Graph(secured);
        if (graph.estCouvrant(testSecured)) return -10;
        if (graph.difference(cutted)) return 10;
        return 0;
    }

    @Override
    public Pair<Vertex, Vertex> playSHORT() {
        return null;
    }

    public Pair<Vertex, Vertex> playCUT () {
        List<Pair<Vertex, Vertex>> secured = game.getSecured();
        List<Pair<Vertex, Vertex>> cutted = game.getCutted();
        List<Pair<Integer, Pair<Vertex, Vertex>>> score = minMaxF(secured, cutted, depth);
        if (score.isEmpty()) return null; // Cas à gérer quand l'IA ne peut plus jouer
        Pair<Integer, Pair<Vertex, Vertex>> max = score.getFirst();
        for (Pair<Integer, Pair<Vertex, Vertex>> element : score){
            if (element.getKey()>max.getKey()) max=element;
        }
        return max.getValue();
    }

    public List<Pair<Integer, Pair<Vertex, Vertex>>> minMaxF(List<Pair<Vertex, Vertex>> secured, List<Pair<Vertex, Vertex>> cutted, int depth) {
        //score contient en fait des paire qui corresponde à une arrete le score qu'on lui a calculé
        List<Pair<Integer, Pair<Vertex, Vertex>>> score = new ArrayList<>(); //Les scores associé aux arrete
        List<Pair<Vertex, Vertex>> allEdges = this.graph.getNeighbors();
        List<Pair<Vertex, Vertex>> edges = new ArrayList<>();
        for (Pair<Vertex, Vertex> toCut : allEdges) { // Liste des arrete jouables
            if (!secured.contains(toCut) && !cutted.contains(toCut)) {
                edges.add(toCut);
            }
        }
        if (depth==0) {
            //si on ne regarde plus en avant, alors toute les arretes ont le meme score qui est celui que la fonction d'evaluation donne au graphe
            int value = evaluate(this.graph,secured,cutted);
            for (Pair<Vertex, Vertex> toCut : edges) {
                score.add(new Pair<>(value,toCut));
            }
            return score;
        }
        //Sinon :
        for (Pair<Vertex, Vertex> toCut : edges) { //On parcours toutes les arrêtes du graphes pour essayer de couper

            List<Integer> scoreEdge = new ArrayList<>(); //Le score associé à une arrete
            ArrayList<Pair<Vertex, Vertex>> copieSecure = new ArrayList<>(secured); //Comme on veut simuler des coups on recupere les deux tableau de secure et cut
            ArrayList<Pair<Vertex, Vertex>> copieCutted = new ArrayList<>(cutted);

            copieCutted.add(toCut);
            for (Pair<Vertex, Vertex> toSecure : edges) { //On a joué maintenant on simule le coup adverse donc on parcours les arretes et on essaie de les sécuriser
                if (!secured.contains(toSecure) && !cutted.contains(toSecure)) {
                    copieSecure.add(toSecure);
                    //Une fois qu'on a immaginé notre coup et celui de l'adversaire c'est à nous de jouer à nouveau donc on fait l'appel récursif
                    //On ajoute donc au tableau scoreEdge les sommes des evalutations des arretes restante en fonction de chaque securisation
                    scoreEdge.add(sum (minMaxF(copieSecure,copieCutted, depth-1) ));
                }
            }
            //Maintenant scoreEdge contient pour chaque securisation de CUT, les valeur des evaluations de nos coup suivant, le score de l'arrete est donc la somme de ces valeurs
            score.add(new Pair<>(scoreEdge.stream().mapToInt(Integer::intValue).sum(),toCut));
        }
        return score;
    }

//    public Pair<Vertex, Vertex> playSHORT () {
//        ArrayList<Pair<Vertex, Vertex>> secured = game.getSecured();
//        ArrayList<Pair<Vertex, Vertex>> cutted = game.getCutted();
//        List<Pair<int, Pair<Vertex, Vertex>>> score = MinMaxF2(secured, cutted, d);
//        Pair<int, Pair<Vertex, Vertex>> max = score.getFirst();
//        for (Pair<int, Pair<Vertex, Vertex>> element : score){
//            if (element.getKey()>max.getKey()) max=element;
//        }
//        return max.getValue();
//    }
////
//    public List<Pair<int, Pair<Vertex, Vertex>>> MinMaxF2 (ArrayList<Pair<Vertex, Vertex>> secured, ArrayList<Pair<Vertex, Vertex>> cutted, int depth) {
//        List<Pair<int, Pair<Vertex, Vertex>>> score = new ArrayList<>();
//        List<Pair<Vertex, Vertex>> AllEdges = this.graph.getNeighbors();
//        List<Pair<Vertex, Vertex>> edges = new ArrayList<>();
//        for (Pair<Vertex, Vertex> toCut : AllEdges) {
//            if (!secured.contains(toCut) && !cutted.contains(toCut)) {
//                edges.add(toCut);
//            }
//        }
//        if (depth==0) {
//            int value = -evaluate(this.graph,secured,cutted);
//            for (Pair<Vertex, Vertex> toCut : edges) {
//                score.add(new Pair<>(value,toCut));
//            }
//            return score;
//        }
//        for (Pair<Vertex, Vertex> toSecure : edges) {
//            List<int> scoreEdge = new ArrayList<>();
//            ArrayList<Pair<Vertex, Vertex>> copieSecure = new ArrayList<>(secured);
//            ArrayList<Pair<Vertex, Vertex>> copieCutted = new ArrayList<>(cutted);
//            copieSecure.add(toSecure);
//            for (Pair<Vertex, Vertex> toCut : edges) {
//                if (!secured.contains(toCut) && !cutted.contains(toCut)) {
//                    copieCutted.add(toCut);
//                    scoreEdge.add(sum (MinMaxF2(copieSecure,copieCutted, depth-1) ));
//                }
//            }
//            score.add(new Pair<>(scoreEdge.stream().mapToInt(int::intValue).sum(),toSecure));
//        }
//        return score;
//    }























    // public int minimax(int depth, Turn turn) {
    //     if (depth == 0) {
    //         if (game.cutWon()) return 1;
    //         else return -1;
    //     }
    //     if (turn == Turn.CUT) {
    //         int value = -1;
    //         for 
    //     }
    // }
    // public int play(Turn turn) {
    //     if (game.cutWon()) {
    //         return 1;
    //     }
    //     else if (game.shortWon()) {
    //         return -1;
    //     }
    //     if (turn == Turn.CUT) {
    //         int meilleur_score = -2;
    //         Pair<Vertex, Vertex> edge = null;
    //         for (int i = graph.getNeighbors().size() - 1; i > 0; i--) {
    //             var element = graph.getNeighbors().get(i);
    //             for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
    //                 if (Vertex.isSameCouple(new Pair<>(element.getKey(), element.getValue()), neighbors.getKey())) {
    //                     if (!element.getKey().isCut(element.getValue())
    //                             && !element.getKey().isPainted(element.getValue()))
    //                         edge = element;
    //                 }
    //             }
    //             if (edge == null) {
    //                 edge = graph.getNeighbors().get(0);
    //             }
    //             graph.removeNeighbor(edge);
    //             int score = play(turn);
    //             if (score > meilleur_score) {
    //                 meilleur_score = score;
    //                 bestMove = edge;
    //             }
    //             if (game.cutWon()) {
    //                 graph.addNeighbor(edge);
    //                 return 1;
    //             }
    //             else {
    //                 graph.addNeighbor(edge);
    //                 return -1;
    //             }
    //         }
    //     }
    //     else {
    //         int meilleur_score = 2;
    //         for (int i = graph.getNeighbors().size() - 1; i > 0; i--) {
    //             var edge = graph.getNeighbors().get(i);
    //             graph.removeNeighbor(edge);
    //             int score = play(turn);
    //             if (score < meilleur_score) {
    //                 meilleur_score = score;
    //                 bestMove = edge;
    //             }
    //             if (game.cutWon()) {
    //                 graph.addNeighbor(edge);
    //                 return 1;
    //             }
    //             else {
    //                 graph.addNeighbor(edge);
    //                 return -1;
    //             }
    //         }
    //     }
    //     return 0;
    // }
}

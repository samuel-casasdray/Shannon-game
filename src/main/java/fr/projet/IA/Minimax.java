package fr.projet.IA;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

public class Minimax {
    Game game;
    Turn plays;
    Graph graph;
    Pair<Vertex, Vertex> bestMove;

    public Minimax(Game game, Turn plays) {
        this.game = game;
        this.plays = plays;
        this.graph = game.getGraph();
    }

    public Pair<Vertex, Vertex>  getBestMove() {
        return bestMove;
    }
    
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

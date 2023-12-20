package fr.projet;

public class Game {
    Graph graph;
    Turn turn = Turn.CUT;
    public Game() {

    }

    public void Play() {
        if (turn == Turn.CUT) {
            turn = Turn.SHORT;
        }
        else {
            turn = Turn.CUT;
        }
    }
}

package fr.projet.IA;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;

public abstract class InterfaceIA {
    Game game;
    Turn plays;
    Graph graph;
    public abstract Pair<Vertex, Vertex> playCUT();
    public abstract Pair<Vertex, Vertex> playSHORT();

    InterfaceIA(Game game, Turn plays) {
        this.game = game;
        this.plays = plays;
        this.graph = game.getGraph();
    }
}

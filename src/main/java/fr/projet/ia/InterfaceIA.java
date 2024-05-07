package fr.projet.ia;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

public abstract class InterfaceIA {
    Game game;
    Turn plays;
    Graph graph;

    @Getter
    @Setter
    int depth;
    public abstract Pair<Vertex, Vertex> playCUT();
    public abstract Pair<Vertex, Vertex> playSHORT();

    InterfaceIA(Game game, Turn plays) {
        this.game = game;
        this.plays = plays;
        this.graph = game.getGraph();
    }

    InterfaceIA(Game game, Turn plays, int depth) {
        this(game, plays);
        this.depth = depth;
    }
}

package fr.projet;

import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Setter
@Getter
public class Vertex {
    private ArrayList<Vertex> listNeighbors;
    private boolean isInitialState;
    private boolean isFinalState;
    private Pair<Integer, Integer> coords;
    public Vertex(ArrayList<Vertex> vertices, boolean isInitialState, boolean isFinalState, int x, int y) {
        this.listNeighbors = vertices;
        this.isInitialState = isInitialState;
        this.isFinalState = isFinalState;
        this.coords = new Pair<>(x, y);
    }
    public Vertex(boolean isInitialState, boolean isFinalState, int x, int y) {
        this.listNeighbors = new ArrayList<>();
        this.isInitialState = isInitialState;
        this.isFinalState = isFinalState;
        this.coords = new Pair<>(x, y);
    }
    public void addNeighborVertex(Vertex v) {
        listNeighbors.add(v);
    }
}

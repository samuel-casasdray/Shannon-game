package fr.projet;

import javafx.util.Pair;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;

@Accessors(chain = true)
@Data
public class Vertex {
    private ArrayList<Vertex> listNeighbors;
    private Pair<Integer, Integer> coords;

    public Vertex(ArrayList<Vertex> vertices, int x, int y) {
        this.listNeighbors = vertices;
        this.coords = new Pair<>(x, y);
    }

    public Vertex(int x, int y) {
        this.listNeighbors = new ArrayList<>();
        this.coords = new Pair<>(x, y);
    }

    public void addNeighborVertex(Vertex v) {
        listNeighbors.add(v);
    }

    public void removeNeighborVertex(Vertex v) {
        listNeighbors.remove(v);
    }

    public void removeNeighborVertex(int i) {
        listNeighbors.remove(i);
    }

    public static boolean isSameCouple(Pair<Vertex, Vertex> vertex1, Pair<Vertex, Vertex> vertex2) {
        return (vertex1.getKey() == vertex2.getKey() && vertex1.getValue() == vertex2.getValue()) ||
                (vertex1.getKey() == vertex2.getValue() && vertex1.getValue() == vertex2.getKey());
    }
}

package fr.projet.graph;

import javafx.util.Pair;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class Vertex {
    private Pair<Integer, Integer> coords;
    private int color = 0;

    public Vertex(int x, int y) {
        this.coords = new Pair<>(x, y);
    }


    public int getX() {
        return getCoords().getKey();
    }

    public int getY() {
        return getCoords().getValue();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }


    @Override
    public String toString() {
        return "Vertex(" + coords.getKey() + ", " + coords.getValue() + ')';
    }

    public static boolean isSameCouple(Pair<Vertex, Vertex> vertex1, Pair<Vertex, Vertex> vertex2) {
        return (vertex1.getKey().equals(vertex2.getKey()) && vertex1.getValue().equals(vertex2.getValue())) ||
                (vertex1.getKey().equals(vertex2.getValue()) && vertex1.getValue().equals(vertex2.getKey()));
    }

    public double distance(Vertex v) {
        return Math.sqrt(Math.pow(getX() - v.getX(), 2)
                + Math.pow(getY() - v.getY(), 2));
    }
}


package fr.projet.graph;

import javafx.util.Pair;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;

@Accessors(chain = true)
@Data
public class Vertex {
    private HashSet<Vertex> neighbors;
    private HashSet<Vertex> neighborsCut = new HashSet<>(); // Pour avoir un contains en O(1) et aucune répétition ?
    private HashSet<Vertex> neighborsPaint = new HashSet<>();
    private Pair<Integer, Integer> coords;

    public Vertex(List<Vertex> vertices, int x, int y) {
        this.neighbors = new HashSet<>(vertices);
        this.coords = new Pair<>(x, y);
    }

    public Vertex(int x, int y) {
        this.neighbors = new HashSet<>();
        this.coords = new Pair<>(x, y);
    }

    public void addNeighborVertex(Vertex v) {
        neighbors.add(v);
        v.getNeighbors().add(this);
    }

    public void removeNeighborVertex(Vertex v) {
        neighbors.remove(v);
        v.getNeighbors().remove(this);
    }

    public int getX() {
        return getCoords().getKey();
    }

    public int getY() {
        return getCoords().getValue();
    }

    @Override
    public boolean equals(Object o) { 
        // à mon avis il faut garder le equals par défaut pour comparer les instances
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

    public void cut(Vertex v) {
        neighborsCut.add(v);
        v.neighborsCut.add(this); // si c'est cut, c'est cut dans les deux sens
    }

    public boolean isCut(Vertex v) {
        return neighborsCut.contains(v)
        || v.getNeighborsCut().contains(this);
    }

    public void paint(Vertex v) {
        neighborsPaint.add(v);
        v.getNeighborsPaint().add(this);
    }

    public boolean isPainted(Vertex v) {
        return neighborsPaint.contains(v) || v.getNeighborsPaint().contains(this);
    }

    public boolean isCutOrPanted(Vertex v) {
        return isCut(v) || isPainted(v);
    }

    public double distance(Vertex v) {
        return Math.sqrt(Math.pow(getCoords().getKey() - v.getCoords().getKey(), 2)
                + Math.pow(getCoords().getValue() - v.getCoords().getValue(), 2));
    }
}


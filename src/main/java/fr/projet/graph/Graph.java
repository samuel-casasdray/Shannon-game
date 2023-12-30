package fr.projet.graph;

import fr.projet.gui.Gui;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Accessors(chain = true)
@Data
public class Graph {

    // nombre de vertex
    private int nbVertices = 5;

    private boolean aroundCircle = true;

    private double proba = 0.5;

    private final Random random = new Random();

    private List<Vertex> vertices;

    @Getter
    private List<Pair<Vertex, Vertex>> neighbors = new ArrayList<>();

    public Graph(List<Vertex> vertices) {
        this.vertices = new ArrayList<>(vertices);
    }

    public Graph(int nbVertices) {
        this.nbVertices = nbVertices;
        this.generateGraph();
    }

    public Graph() {
        this.generateGraph();
    }

    private void generateGraph() {
        this.vertices = new ArrayList<>();
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        for (int i = 0; i < nbVertices; i++) {
            Pair<Integer, Integer> coord;
            if (aroundCircle) {
                // Coord autour d'un cercle
                double toRad = Math.toRadians((double) i * 360F / nbVertices);
                coord = new Pair<>(
                        (int) (Math.cos(toRad) * (Gui.WINDOW_SIZE / 2D - Gui.WINDOW_MARGE) + Gui.WINDOW_SIZE / 2D),
                        (int) (Math.sin(toRad) * (Gui.WINDOW_SIZE / 2D - Gui.WINDOW_MARGE) + Gui.WINDOW_SIZE / 2D));
            } else {
                // Coord aléatoire
                coord = new Pair<>(
                        random.nextInt(Gui.WINDOW_MARGE, Gui.WINDOW_SIZE - Gui.WINDOW_MARGE),
                        random.nextInt(Gui.WINDOW_MARGE, Gui.WINDOW_SIZE - Gui.WINDOW_MARGE));
            }
            // On ajoute le sommet au graphe
            vertices.add(new Vertex(coord.getKey(), coord.getValue()));
        }
        // Instantiation des aretes selon une probabilité (proba) entre 2 sommets
        for (int i = 0; i < nbVertices; i++) {
            for (int j = i + 1; j < nbVertices; j++) {
                float p = random.nextFloat();
                if (p > proba) {
                    this.vertices.get(i).addNeighborVertex(this.vertices.get(j));
                    this.vertices.get(j).addNeighborVertex(this.vertices.get(i));
                    neighbors.add(new Pair<>(this.vertices.get(i), this.vertices.get(j)));
                }
            }
        }
    }

    public void setNbVertices(int nbVertices) {
        this.nbVertices = nbVertices;
        this.generateGraph();
    }

    public boolean estConnexe() {
        HashSet<Vertex> marked = new HashSet<>();
        ArrayList<Vertex> pile = new ArrayList<>();
        if (vertices.size() == 0) {
            return true;
        }
        pile.add(vertices.get(0));
        while (pile.size() > 0) {
            var s = pile.getLast();
            pile.remove(pile.size()-1);
            if (!marked.contains(s)) {
                marked.add(s);
                for (var t: s.getListNeighbors()) {
                    if (!marked.contains(t)) {
                        pile.add(t);
                    }
                }
            }
        }
        return marked.size() == vertices.size();
    }
}

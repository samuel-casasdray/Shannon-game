package fr.projet.Graph;

import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Accessors(chain = true)
@Data
public class Graph {

    private int nbVertices = 5; // nombre de vertex

    private boolean aroundCircle = true;

    private double proba = 0.5;

    private final Random random = new Random();

    private List<Vertex> vertices;

    @Getter
    private List<Pair<Vertex, Vertex>> neighbors = new ArrayList<>();

    public Graph(List<Vertex> vertices) {
        this.vertices = vertices;
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
            if (aroundCircle) { // Coord autour d'un cercle
                double toRad = (i * 360f / nbVertices) * (Math.PI / 180);
                coord = new Pair<>((int) (Math.cos(toRad) * 300 + 400), (int) (Math.sin(toRad) * 300 + 400));
            } else // Coord aléatoire
                coord = new Pair<>(random.nextInt(0, 780), random.nextInt(0, 780));
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
}

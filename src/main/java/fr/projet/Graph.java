package fr.projet;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class Graph {
    private ArrayList<Vertex> vertices;
    public Graph(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
    }
    public Graph() {
        this.vertices = new ArrayList<>();
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }
}

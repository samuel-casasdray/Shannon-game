package fr.projet;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.util.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void testPrincipal() {
        assertTrue(true);
    }
    @Test
    void testGrapheCompletEstConnexe() {
        Graph g = new Graph();
        int k = 100;
        // Création graphe complet à k sommets
        for (int i = 0; i < k; i++) {
            Vertex v = new Vertex(0, 0);
            g.addVertex(v);
            for (int j = 0; j < i; j++)
                g.addEdge(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        assertEquals(g.getEdges().size(), k * (k - 1) / 2);
        assertTrue(g.estConnexe());
    }

    @Test
    void testGrapheNonConnexe() {
        Graph g = new Graph();
        Vertex v1 = new Vertex(0, 0);
        Vertex v2 = new Vertex(0, 0);
        g.addVertex(v1);
        g.addVertex(v2);
        g.addVertex(new Vertex(0, 0));
        g.addEdge(new Pair<>(v1, v2));
        assertFalse(g.estConnexe());
    }

    @Test
    void testGrapheCompletNonConnexe() {
        Graph g = new Graph();
        int k = 100;
        // Création graphe complet à k sommets avec un sommet isolé (k+1 sommets au total)
        for (int i = 0; i < k; i++) {
            Vertex v = new Vertex(0, 0);
            g.addVertex(v);
            for (int j = 0; j < i; j++)
                g.addEdge(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        g.addVertex(new Vertex(0, 0));
        assertEquals(g.getEdges().size(), k * (k - 1) / 2);
        assertFalse(g.estConnexe());
    }

    @Test
    void testSpanningTreeGrapheComplet() {
        Graph g = new Graph();
        int k = 6;
        // Création graphe complet à k sommets
        for (int i = 0; i < k; i++) {
            Vertex v = new Vertex(0, 0);
            g.addVertex(v);
            for (int j = 0; j < i; j++)
                g.addEdge(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        Graph spanningTree = g.getSpanningTree();
        assertTrue(spanningTree.getNbVertices() == g.getNbVertices() &&
                spanningTree.getEdges().size() < g.getEdges().size() && spanningTree.estConnexe());
    }
}
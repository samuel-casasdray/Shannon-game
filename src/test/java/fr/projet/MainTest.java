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
                g.addNeighbor(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        assertEquals(g.getNeighbors().size(), k * (k - 1) / 2);
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
        g.addNeighbor(new Pair<>(v1, v2));
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
                g.addNeighbor(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        g.addVertex(new Vertex(0, 0));
        assertEquals(g.getNeighbors().size(), k * (k - 1) / 2);
        assertFalse(g.estConnexe());
    }

    @Test
    void testSpanningTreesGenerator() {
        Graph g = new Graph();
        int k = 6;
        // Création graphe complet à k sommets
        for (int i = 0; i < k; i++) {
            Vertex v = new Vertex(0, 0);
            g.addVertex(v);
            for (int j = 0; j < i; j++)
                g.addNeighbor(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        assertEquals(g.getNbVertices(), k);
        assertEquals(g.getTwoDistinctSpanningTrees().size(), 2);
    }

    @Test
    void testKruskalGrapheComplet() {
        Graph g = new Graph();
        int k = 6;
        // Création graphe complet à k sommets
        for (int i = 0; i < k; i++) {
            Vertex v = new Vertex(0, 0);
            g.addVertex(v);
            for (int j = 0; j < i; j++)
                g.addNeighbor(new Pair<>(g.getVertices().get(i), g.getVertices().get(j)));
        }
        Graph kruskal = g.kruskal();
        assertTrue(kruskal.getNbVertices() == g.getNbVertices() &&
                kruskal.getNeighbors().size() < g.getNeighbors().size() && kruskal.estConnexe());
    }
}
package fr.projet.graph;

import fr.projet.gui.Gui;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Accessors(chain = true)
@Data
@Slf4j
public class Graph {

    // nombre de vertex
    private int nbVertices = 5;

    private boolean aroundCircle = true;

    private double proba = 0.2;

    private Random random = new Random();

    private List<Vertex> vertices;

    @Getter
    private List<Pair<Vertex, Vertex>> neighbors = new ArrayList<>();
    public Graph(List<?> liste) {
        if (liste.isEmpty()) {
            this.nbVertices=0;
            this.vertices= new ArrayList<>();
            this.neighbors= new ArrayList<>();
        }
        else {
            if (liste.getFirst() instanceof Vertex) {
                //log.info("création a partir de sommet");
                this.vertices = (List<Vertex>) new ArrayList<>(liste);
                this.nbVertices = liste.size();
            }
            if (liste.getFirst() instanceof Pair) {
                //log.info("création a partir d'arretes");
                this.neighbors = (List<Pair<Vertex, Vertex>>) new ArrayList<>(liste);
                ;
                this.nbVertices = liste.size();
                List<Vertex> vertices = new ArrayList<>();
                for (Pair<Vertex, Vertex> element : neighbors) {
                    if (!vertices.contains(element.getKey())) vertices.add(element.getKey());
                    if (!vertices.contains(element.getValue())) vertices.add(element.getValue());
                }
                this.vertices = vertices;
            }
        }
        //this.printGraph();
    }

    public Graph(int nbVertices) {
        this.nbVertices = nbVertices;
        this.vertices = new ArrayList<>();
        this.generateGraph();
    }

    public Graph(int nbVertices, long seed) {
        this.nbVertices = nbVertices;
        this.vertices = new ArrayList<>();
        random = new Random(seed);
        this.generateGraph();
    }


//    public Graph (List<Pair<Vertex, Vertex>> neighbors) {
//        this.neighbors=neighbors;
//        this.nbVertices= neighbors.size();
//        List<Vertex> vertices = new ArrayList<>();
//        for (Pair<Vertex, Vertex> element : neighbors) {
//            if (!vertices.contains(element.getKey())) vertices.add(element.getKey());
//            if (!vertices.contains(element.getValue())) vertices.add(element.getValue());
//        }
//        this.vertices=vertices;
//    }


    public Graph() {
        this.vertices = new ArrayList<>();
        this.generateGraph();
    }

    public void addVertice(Vertex v) {
        vertices.add(v);
    }

    private void generateGraph() {
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
            addVertice(new Vertex(coord.getKey(), coord.getValue()));
        }
        //Instantiation des aretes selon une probabilité (proba) entre 2 sommets
        for (int i = 0; i < nbVertices; i++) {
            for (int j = i + 1; j < nbVertices; j++) {
                float p = random.nextFloat();
                if (p < proba) {
                    addNeighbor(new Pair<>(this.vertices.get(i), this.vertices.get(j)));
                }
            }
        }
        //ajout d'aretes si jamais le sommet a moins de 2 voisins
        for (int i=0; i< nbVertices; i++) {
            while(VertexDegree(i)<2){
                int r;
                do{
                    r=random.nextInt(nbVertices);
                } while(r==i || this.vertices.get(i).getListNeighbors().contains(this.vertices.get(r)));
                addNeighbor(new Pair<>(this.vertices.get(i), this.vertices.get(r)));
            }
        } // Réfléchir à régénérer le graphe tant qu'il existe des sommets de degré < 2 (pour avoir moins d'arêtes)
    }

    public int VertexDegree(int indexVertex) {
        Vertex vertex= this.vertices.get(indexVertex);
        int count =0;
        for(Pair<Vertex,Vertex> p: neighbors){
            if (p.getKey().equals(vertex)){
                count++;
            }
        }
        return count;
    }

    public void setNbVertices(int nbVertices) {
        this.nbVertices = nbVertices;
        this.generateGraph();
    }

    public boolean estConnexe() {
        HashSet<Vertex> marked = new HashSet<>();
        ArrayList<Vertex> pile = new ArrayList<>();
        if (vertices.isEmpty()) {
            return true;
        }
        pile.add(vertices.getFirst());
        while (!pile.isEmpty()) {
            var s = pile.getLast();
            pile.removeLast();
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

    public void removeNeighbor(Pair<Vertex, Vertex> edge) {
        neighbors.remove(edge);
        edge.getKey().removeNeighborVertex(edge.getValue());
        edge.getValue().removeNeighborVertex(edge.getKey());
    }

    public void addNeighbor(Pair<Vertex, Vertex> edge) {
        neighbors.add(edge);
        edge.getKey().addNeighborVertex(edge.getValue());
        edge.getValue().addNeighborVertex(edge.getKey());
    }


    public void printGraph () { //Affiche le graphe dans la console (pour debuguer surtout)
        for (Pair<Vertex, Vertex> element : neighbors) {
            log.info("Arrete : "+element.getKey().toString()+element.getValue().toString());
        }
        log.info(ToStringBuilder.reflectionToString(this.vertices));
    }


    public boolean estCouvrant (Graph G) { //true si G est couvrant de this
        boolean couvrant = true;
        List<Vertex> VerticeG = G.getVertices();
        for (Vertex v : this.vertices) {
            if (!VerticeG.contains(v)) {
                couvrant = false;
                break;
            }
        }
        return couvrant && G.estConnexe();
    }

    public boolean difference (List<Pair<Vertex, Vertex>> cutted) { //true si this\cutted est non connexe
        List<Pair<Vertex, Vertex>> VerticeNew = new ArrayList<>();
        for (Pair<Vertex, Vertex> edge : neighbors) {
            if (!cutted.contains(edge)) {
                VerticeNew.add(edge);
            }
        }
        Graph toTest = new Graph(VerticeNew);
        return !toTest.estConnexe();
    }
}

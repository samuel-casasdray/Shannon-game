package fr.projet.graph;

import fr.projet.gui.Gui;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;

@Accessors(chain = true)
@Data
@Slf4j
public class Graph {

    // nombre de vertex
    private int nbVertices = 5;

    private boolean aroundCircle = false;

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
        this.generateGraphPlanaire();
    }

    public Graph(int nbVertices, long seed) {
        this.nbVertices = nbVertices;
        this.vertices = new ArrayList<>();
        random = new Random(seed);
        this.generateGraphPlanaire();
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
        this.generateGraphPlanaire();
    }

    public void addVertice(Vertex v) {
        vertices.add(v);
    }

    private boolean det(double x1, double y1, double x2, double y2, double x3, double y3) {
        return (y3-y1)*(x2-x1) > (y2-y1)*(x3-x1);
    }

    private boolean intersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        return det(x1, y1, x3, y3, x4, y4) != det(x2, y2, x3, y3, x4, y4) && det(x1, y1, x2, y2, x3, y3) != det(x1, y1, x2, y2, x4, y4);
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
    private void generateGraphPlanaire() {
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        int i = 0;
        double minDist = 130;
        while (vertices.size() != nbVertices) {
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
            Vertex newVertex = new Vertex(coord.getKey(), coord.getValue());
            if (getVertices().size() >= 2) {
                for (int j = 0; j < getVertices().size(); j++)  {
                    Vertex v = getVertices().get(random.nextInt(getVertices().size())); // Sommet aléatoire déjà créé
                    boolean intersect = false;
                    if (!v.equals(newVertex)) {
                        for (int k = 0; k < getNeighbors().size(); k++) {
                            Pair<Vertex, Vertex> neighbor = getNeighbors().get(k);
                            if (intersect(newVertex.getCoords().getKey(), newVertex.getCoords().getValue(), v.getCoords().getKey(), v.getCoords().getValue(),
                                    neighbor.getKey().getCoords().getKey(), neighbor.getKey().getCoords().getValue(), neighbor.getValue().getCoords().getKey(), neighbor.getValue().getCoords().getValue())) {
                                intersect = true; // Si ya au moins une intersection, intersect = true
                            }
                        }
                        if (!intersect) { // S'il n'y a aucune intersection, on place le sommet
                            boolean distanceOk = true;
                            for (Vertex v1: getVertices()) {
                                if (newVertex.distance(v1) < minDist) { // Si la distance entre le sommet à placer est >= minDist de tous ses voisins, on le place
                                    distanceOk = false;
                                    break;
                                }
                            }
                            if (distanceOk)
                            {
                                addVertice(newVertex);
                                addNeighbor(new Pair<>(newVertex, v));
                                i++;
                                break;
                            }
                        }
                    }
                }
            }
            else { // Ce else est dans le cas où on place les deux premiers sommets (dans ce cas aucun risque d'intersection)
                i++;
                if (getVertices().isEmpty() || newVertex.distance(getVertices().getFirst()) >= minDist)
                    addVertice(newVertex);
                if (getVertices().size() == 2) {
                    addNeighbor(new Pair<>(newVertex, getVertices().getFirst()));
                }
            }
            // Ces boucles imbriquées permettent d'éviter au maximum les sommets isolés
                for (Vertex v: getVertices()) {
                    for (Vertex v2: getVertices()) {
                        if (!v.equals(v2) && v.getListNeighbors().size() == 1 && !v.getListNeighbors().contains(v2)) { // Si le sommet v est isolé (degré == 1) et que v2 n'est pas voisin de v
                            boolean intersect = false;
                            for (i = 0; i < neighbors.size(); i++) {
                                Pair<Vertex, Vertex> neighbor = neighbors.get(i);
                                if (intersect(v.getCoords().getKey(), v.getCoords().getValue(), v2.getCoords().getKey(), v2.getCoords().getValue(),
                                        neighbor.getKey().getCoords().getKey(), neighbor.getKey().getCoords().getValue(), neighbor.getValue().getCoords().getKey(), neighbor.getValue().getCoords().getValue())) {
                                    intersect = true;
                                }
                            }
                            if (!intersect) {
                                addNeighbor(new Pair<>(v, v2));
                                break;
                            }
                        }
                    }
                }
        }
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

    public int minDeg() {
        int minDeg = getVertices().getFirst().getListNeighbors().size();
        for (Vertex v: getVertices()) {
            if (v.getListNeighbors().size() < minDeg) {
                minDeg = v.getListNeighbors().size();
            }
        }
        return minDeg;
    }

    public void setNbVertices(int nbVertices) {
        this.nbVertices = nbVertices;
        this.generateGraphPlanaire();
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

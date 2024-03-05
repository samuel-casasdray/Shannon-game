package fr.projet.graph;

import fr.projet.gui.UtilsGui;
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
    public Graph(List<Pair<Vertex, Vertex>> neighbors) {
        this.neighbors = new ArrayList<>(neighbors);
        HashSet<Vertex> vertexSet = new HashSet<>();
        for (Pair<Vertex, Vertex> element : this.neighbors) {
            vertexSet.add(element.getKey());
            vertexSet.add(element.getValue());
        }
        this.vertices = new ArrayList<>(vertexSet);
        this.nbVertices = vertexSet.size();
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

    public Graph() {
        this.vertices = new ArrayList<>();
        this.generateGraphPlanaire();
    }

    public void addVertice(Vertex v) {
        vertices.add(v);
    }

    private static boolean det(int x1, int y1, int x2, int y2, int x3, int y3) {
        return (y3-y1)*(x2-x1) > (y2-y1)*(x3-x1);
    }

    private boolean intersect(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
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
                        (int) (Math.cos(toRad) * (UtilsGui.WINDOW_SIZE / 2D - UtilsGui.WINDOW_MARGE) + UtilsGui.WINDOW_SIZE / 2D),
                        (int) (Math.sin(toRad) * (UtilsGui.WINDOW_SIZE / 2D - UtilsGui.WINDOW_MARGE) + UtilsGui.WINDOW_SIZE / 2D));
            } else {
                // Coord aléatoire
                coord = new Pair<>(
                        random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE),
                        random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE));
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
            while(vertexDegree(i)<2){
                int r;
                do {
                    r=random.nextInt(nbVertices);
                } while(r==i || this.vertices.get(i).getListNeighbors().contains(this.vertices.get(r)));
                addNeighbor(new Pair<>(this.vertices.get(i), this.vertices.get(r)));
            }
        } // Réfléchir à régénérer le graphe tant qu'il existe des sommets de degré < 2 (pour avoir moins d'arêtes)
    }
    private void generateGraphPlanaire() {
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        double minDist = 100;
        double radius = UtilsGui.CIRCLE_SIZE*2;
        int maxIter = getNbVertices()*10000;
        int iterCount = 0;
        while (getVertices().size() < getNbVertices()) {
            iterCount++;
            // Coord aléatoire
            Pair<Integer, Integer> coord = new Pair<>(
                    random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE),
                    random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE));
            Vertex newVertex = new Vertex(coord.getKey(), coord.getValue());
            // Ce if est dans le cas où on place le premier sommet
            if (getVertices().isEmpty()) {
                addVertice(newVertex);
            }
            else {
                boolean distanceOk = true;
                for (Vertex v1: getVertices()) {
                    // Si la distance entre le sommet à placer est >= minDist de tous ses voisins, on le place
                    if (newVertex.distance(v1) < minDist) {
                        distanceOk = false;
                        break;
                    }
                }
                if (distanceOk)
                    addVertice(newVertex);
            }
            if (iterCount >= maxIter) return;
        }
        // Ces boucles imbriquées permettent de relier un maximum de sommets
        for (int i = 0; i < getVertices().size(); i++) {
            Vertex v = getVertices().get(i);
            for (int j = 0; j < i; j++) {
                Vertex v2 = getVertices().get(j);
                if (v.getListNeighbors().contains(v2)) continue;
                boolean intersect = false;
                for (Pair<Vertex, Vertex> neighbor : getNeighbors()) {
                    if (!neighbor.getValue().equals(v) && !neighbor.getValue().equals(v2) && !neighbor.getKey().equals(v) && !neighbor.getKey().equals(v2) && intersect(v.getX(), v.getY(), v2.getX(), v2.getY(),
                            neighbor.getKey().getX(), neighbor.getKey().getY(), neighbor.getValue().getX(), neighbor.getValue().getY())) {
                        intersect = true;
                        break; // S'il y a une intersection, pas la peine de continuer
                    }
                }
                if (!intersect && !thereAreACircleCollision(radius, v, v2)) {
                    addNeighbor(new Pair<>(v, v2));
                }
            }
        }
    }

    private boolean thereAreACircleCollision(double radius, Vertex v1, Vertex v2) {
        for (Vertex vertex: getVertices()) {
            if (!vertex.equals(v1) && !vertex.equals(v2)) {
                int xLeft = (int) (vertex.getX()-radius);
                int xRight = (int) (vertex.getX()+radius);
                int y = vertex.getY();
                int yTop = (int) (y+radius);
                int yBottom = (int) (y-radius);
                if (intersect(v1.getX(), v1.getY(), v2.getX(), v2.getY(),
                        xLeft, yTop, xRight, yBottom) || intersect(v1.getX(), v1.getY(), v2.getX(), v2.getY(),
                        xLeft, yBottom, xRight, yTop))
                    return true;
            }
        }
        return false;
    }
    public int vertexDegree(int indexVertex) {
        Vertex vertex= getVertices().get(indexVertex);
        int count = 0;
        for(Pair<Vertex,Vertex> p: getNeighbors()){
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

    public int maxDeg() {
        int maxDeg = getVertices().getFirst().getListNeighbors().size();
        for (Vertex v: getVertices()) {
            if (v.getListNeighbors().size() > maxDeg) {
                maxDeg = v.getListNeighbors().size();
            }
        }
        return maxDeg;
    }

    public void setNbVertices(int nbVertices) {
        this.nbVertices = nbVertices;
        this.generateGraphPlanaire();
    }

    public boolean estConnexe() {
        if (getVertices().isEmpty()) {
            return true;
        }
        HashSet<Vertex> marked = new HashSet<>();
        Stack<Vertex> pile = new Stack<>();
        pile.push(getVertices().getFirst());
        while (!pile.empty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                for (Vertex t: v.getListNeighbors()) {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                }
            }
        }
        return marked.size() == getVertices().size();
    }

    public void removeNeighbor(Pair<Vertex, Vertex> edge) {
        neighbors.remove(edge);
        edge.getKey().removeNeighborVertex(edge.getValue());
        edge.getValue().removeNeighborVertex(edge.getKey());
    }

    public void addNeighbor(Pair<Vertex, Vertex> edge) {
        if (!getNeighbors().contains(edge)) {
            neighbors.add(edge);
            edge.getKey().addNeighborVertex(edge.getValue());
        }
    }


    public void printGraph () { //Affiche le graphe dans la console (pour debuguer surtout)
        for (Pair<Vertex, Vertex> element : neighbors) {
            log.info("Arrete : "+element.getKey().toString()+element.getValue().toString());
        }
        log.info(ToStringBuilder.reflectionToString(this.vertices));
    }

    public boolean estCouvrant (Graph G) { //true si G est couvrant de this
//        boolean couvrant = true;
//        List<Vertex> VerticeG = G.getVertices();
//        for (Vertex v : this.vertices) {
//            if (!VerticeG.contains(v)) {
//                couvrant = false;
//                break;
//            }
//        }
//        return couvrant && G.estConnexe();

        // OU
        // Je pense que cela suffit, l'algo commenté juste au dessus devrait être équivalent à :
        return G.getNbVertices() == nbVertices && G.estConnexe();
    }

    public boolean difference (List<Pair<Vertex, Vertex>> cutted) { //true si this\cutted est non connexe
        List<Pair<Vertex, Vertex>> verticeNew = new ArrayList<>();
        for (Pair<Vertex, Vertex> edge : neighbors) {
            if (!cutted.contains(edge)) {
                verticeNew.add(edge);
            }
        }
        Graph toTest = new Graph(verticeNew);
        return !toTest.estConnexe();
    }

    private double triangleArea(Vertex a, Vertex b, Vertex c) { // Renvoie l'aire du triangle formé par les sommets A B et C
        Pair<Double, Double> ab = new Pair<>((double) (b.getX() - a.getX()), (double) (b.getY() - a.getY()));
        Pair<Double, Double> ac = new Pair<>((double) (c.getX() - a.getX()), (double) (c.getY() - a.getY()));
        double crossProduct = ab.getKey() * ac.getValue() - ab.getValue() * ac.getKey();
        return Math.abs(crossProduct)/2.0;
    }

    private boolean lineCircleCollision(double radius, Vertex O, Vertex P, Vertex Q) {
        double minimumDistance = (2*triangleArea(O, P, Q))/P.distance(Q);
        return minimumDistance <= radius;
    }
}

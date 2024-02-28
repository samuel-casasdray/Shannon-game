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
        double minDist = 80;
        double radius = Gui.CIRCLE_SIZE*3;
        int maxIter = nbVertices*10000;
        int iterCount = 0;
        while (vertices.size() < nbVertices) {
            iterCount++;
            Pair<Integer, Integer> coord;
            // Coord aléatoire
            coord = new Pair<>(
                    random.nextInt(Gui.WINDOW_MARGE, Gui.WINDOW_SIZE - Gui.WINDOW_MARGE),
                    random.nextInt(Gui.WINDOW_MARGE, Gui.WINDOW_SIZE - Gui.WINDOW_MARGE));
            Vertex newVertex = new Vertex(coord.getKey(), coord.getValue());
            if (getVertices().size() >= 2) {
                for (int j = 0; j < getVertices().size(); j++)  {
                    Vertex v = getVertices().get(random.nextInt(getVertices().size()));
                    boolean intersect = false;
                    if (!v.equals(newVertex)) {
                        for (int k = 0; k < getNeighbors().size(); k++) {
                            Pair<Vertex, Vertex> neighbor = getNeighbors().get(k);
                            if (intersect(newVertex.getX(), newVertex.getY(), v.getX(), v.getY(),
                                    neighbor.getKey().getX(), neighbor.getKey().getY(), neighbor.getValue().getX(), neighbor.getValue().getY())) {
                                intersect = true; // Si ya au moins une intersection, intersect = true
                                break; // S'il y a une intersection, pas la peine de continuer
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
                            if (distanceOk && !thereAreACircleCollision(radius, newVertex, v))
                            {
                                // Cette boucle permet de placer le sommet ssi il n'est pas traversé par une arête déjà présente
                                for (Pair<Vertex, Vertex> neighbor: neighbors) {
                                    Vertex v1 = neighbor.getKey();
                                    Vertex v2 = neighbor.getValue();
                                    int xLeft = (int) (newVertex.getX()-radius);
                                    int xRight = (int) (newVertex.getX()+radius);
                                    int y = newVertex.getY();
                                    int yTop = (int) (y+radius);
                                    int yBottom = (int) (y-radius);
                                    if (intersect(v1.getX(), v1.getY(), v2.getX(), v2.getY(),
                                            xLeft, yTop, xRight, yBottom) || intersect(v1.getX(), v1.getY(), v2.getX(), v2.getY(),
                                            xLeft, yBottom, xRight, yTop)) {
                                        intersect = true;
                                    }
                                }
                                if (!intersect) {
                                    addVertice(newVertex);
                                    addNeighbor(new Pair<>(newVertex, v));
                                    break;
                                }
                            }
                            else {
                                vertices.remove(newVertex);
                                break;
                            }
                        }
                    }
                }
            }
            else { // Ce else est dans le cas où on place les deux premiers sommets (dans ce cas aucun risque d'intersection)
                if (getVertices().isEmpty() || newVertex.distance(getVertices().getFirst()) >= minDist)
                    addVertice(newVertex);
            }
            if (iterCount >= maxIter) return;
        }
        // Ces boucles imbriquées permettent d'éviter au maximum les sommets isolés
        for (int i = 0; i < getVertices().size(); i++) {
            Vertex v = getVertices().get(i);
            for (int j = 0; j < i; j++) {
                Vertex v2 = getVertices().get(j);
                boolean intersect = false;
                for (Pair<Vertex, Vertex> neighbor : neighbors) {
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
                {
                    //System.out.println((getVertices().indexOf(v1)+1) + " " + v1+" -> " + (getVertices().indexOf(vertex)+1)+" " +vertex+" -> "+(getVertices().indexOf(v2)+1)+" " +v2);
                    return true;
                }
            }
        }
        //System.out.println((getVertices().indexOf(v1)+1) + " " + v1+" -> "+(getVertices().indexOf(v2)+1)+" " +v2);
        return false;
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

    private double triangleArea(Vertex A, Vertex B, Vertex C) { // Renvoie l'aire du triangle formé par les sommets A B et C
        Pair<Integer, Integer> AB = new Pair<>(B.getX() - A.getX(), B.getY() - A.getY());
        Pair<Integer, Integer> AC = new Pair<>(C.getX() - A.getX(), C.getY() - A.getY());
        double crossProduct = AB.getKey() * AC.getValue() - AB.getValue() * AC.getKey();
        return Math.abs(crossProduct)/2.0;
    }

    private boolean lineCircleCollision(double radius, Vertex O, Vertex P, Vertex Q) {
        double minimumDistance = (2*triangleArea(O, P, Q))/P.distance(Q);
        return minimumDistance <= radius;
    }
}

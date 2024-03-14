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
    private Map<Vertex, List<Vertex>> adjVertices;

    @Getter
    private HashSet<Pair<Vertex, Vertex>> neighbors = new HashSet<>();
    public Graph(Collection<Pair<Vertex, Vertex>> neighbors) {
        this.neighbors = new HashSet<>(neighbors);
        this.vertices = new ArrayList<>();
        this.adjVertices= new HashMap<>();
        HashSet<Vertex> vertexSet = new HashSet<>();
        for (Pair<Vertex, Vertex> element : this.neighbors) {
            vertexSet.add(element.getKey());
            vertexSet.add(element.getValue());
            addNeighbor(element);
        }
        this.vertices = new ArrayList<>(vertexSet);
        this.nbVertices = vertexSet.size();
    }

    public Graph(int nbVertices) {
        this.nbVertices = nbVertices;
        this.vertices = new ArrayList<>();
        this.adjVertices = new HashMap<>();
        this.generateGraphPlanaire();
    }

    public Graph(int nbVertices, long seed) {
        this.nbVertices = nbVertices;
        this.vertices = new ArrayList<>();
        this.adjVertices = new HashMap<>();
        random = new Random(seed);
        this.generateGraphPlanaire();
    }

    public Graph() {
        this.vertices = new ArrayList<>();
        this.adjVertices = new HashMap<>();
    }
    public void addVertex(Vertex v) {
        adjVertices.putIfAbsent(v, new ArrayList<>());
        vertices.add(v);
    }

    public int getNbVertices() {return getVertices().size();}

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
            addVertex(new Vertex(coord.getKey(), coord.getValue()));
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
                } while(r==i || adjVertices.get(this.vertices.get(i)).contains(this.vertices.get(r)));
                addNeighbor(new Pair<>(this.vertices.get(i), this.vertices.get(r)));
            }
        } // Réfléchir à régénérer le graphe tant qu'il existe des sommets de degré < 2 (pour avoir moins d'arêtes)
    }
    private void generateGraphPlanaire() {
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        double minDist = 100;
        double radius = UtilsGui.CIRCLE_SIZE*2;
        int maxIter = nbVertices*10000;
        int iterCount = 0;
        while (getNbVertices() < nbVertices) {
            iterCount++;
            // Coord aléatoire
            Pair<Integer, Integer> coord = new Pair<>(
                    random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE),
                    random.nextInt(UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_SIZE - UtilsGui.WINDOW_MARGE));
            Vertex newVertex = new Vertex(coord.getKey(), coord.getValue());
            // Ce if est dans le cas où on place le premier sommet
            if (getVertices().isEmpty()) {
                addVertex(newVertex);
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
                    addVertex(newVertex);
            }
            if (iterCount >= maxIter) return;
        }
        // Ces boucles imbriquées permettent de relier un maximum de sommets
        for (int i = 0; i < getVertices().size(); i++) {
            Vertex v = getVertices().get(i);
            for (int j = 0; j < i; j++) {
                Vertex v2 = getVertices().get(j);
                if (getAdjVertices().get(v).contains(v2)) continue;
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
        //int minDeg = getVertices().getFirst().getNeighbors().size();
        int minDeg = getAdjVertices().get(getVertices().getFirst()).size();
        for (Vertex v: getVertices()) {
            if (getAdjVertices().get(v).size() < minDeg) {
                minDeg = getAdjVertices().get(v).size();
            }
        }
        return minDeg;
    }

    public int maxDeg() {
        int maxDeg = getAdjVertices().get(getVertices().getFirst()).size();
        for (Vertex v: getVertices()) {
            if (getAdjVertices().get(v).size() > maxDeg) {
                maxDeg = getAdjVertices().get(v).size();
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
                for (Vertex t: getAdjVertices().get(v)) {
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
        adjVertices.get(edge.getKey()).remove(edge.getValue());
        adjVertices.get(edge.getValue()).remove(edge.getKey());
    }

    public void addNeighbor(Pair<Vertex, Vertex> edge) {
        if (!neighbors.contains(new Pair<>(edge.getValue(), edge.getKey())))
            neighbors.add(edge);
        if (!vertices.contains(edge.getKey()))
            vertices.add(edge.getKey());
        if (!vertices.contains(edge.getValue()))
            vertices.add(edge.getValue());
        getAdjVertices().putIfAbsent(edge.getKey(), new ArrayList<>());
        getAdjVertices().putIfAbsent(edge.getValue(), new ArrayList<>());
        if (!getAdjVertices().get(edge.getKey()).contains(edge.getValue()))
            getAdjVertices().get(edge.getKey()).add(edge.getValue());
        if (!getAdjVertices().get(edge.getValue()).contains(edge.getKey()))
            getAdjVertices().get(edge.getValue()).add(edge.getKey());
    }


    public void printGraph () { //Affiche le graphe dans la console (pour debuguer surtout)
        for (Pair<Vertex, Vertex> element : neighbors) {
            log.info("Arrete : "+element.getKey().toString()+element.getValue().toString());
        }
        log.info(ToStringBuilder.reflectionToString(this.vertices));
    }

    public boolean estCouvrant (Graph G) { //true si G est couvrant de this
        return G.getNbVertices() == nbVertices && G.estConnexe();
    }

    public boolean difference (HashSet<Pair<Vertex, Vertex>> cutted) { //true si this\cutted est non connexe
        List<Pair<Vertex, Vertex>> verticeNew = new ArrayList<>();
        for (Pair<Vertex, Vertex> edge : neighbors) {
            if (!cutted.contains(edge)) {
                verticeNew.add(edge);
            }
        }
        Graph toTest = new Graph(verticeNew);
        return !toTest.estConnexe();
    }

    private boolean areDisjoint(List<Integer> tree1, List<Integer> tree2, Map<Integer, Pair<Integer, Integer>> edgNum) {
        List<Pair<Integer, Integer>> edges1 = new ArrayList<>();
        List<Pair<Integer, Integer>> edges2 = new ArrayList<>();

        for (Integer edge : tree1) {
            edges1.add(edgNum.get(edge));
        }

        for (Integer edge : tree2) {
            edges2.add(edgNum.get(edge));
        }

        edges1.retainAll(edges2);
        return edges1.isEmpty();
    }
    private List<List<Integer>> cartesianProduct(List<List<Integer>> lists) {
        List<List<Integer>> resultLists = new ArrayList<>();
        if (lists.isEmpty()) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            List<Integer> firstList = lists.getFirst();
            List<List<Integer>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (Integer condition : firstList) {
                for (List<Integer> remainingList : remainingLists) {
                    ArrayList<Integer> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }
    private List<Graph> spanTrees(List<List<Integer>> trs, List<List<List<Integer>>> edg, List<List<Integer>> all_span_trees, int k, Map<Integer, Pair<Integer, Integer>> edgNum) {
        if (k == 0) {
            List<List<Integer>> productResult = cartesianProduct(trs);
            // Code pour renvoyer deux arbres couvrants par énumération
//            for (List<Integer> tree : productResult) {
//                for (List<Integer> existingTree : all_span_trees) {
//                    if (areDisjoint(tree, existingTree, edgNum)) {
//                        List<Graph> result = new ArrayList<>(2);
//                        for (List<Integer> element : Arrays.asList(tree, existingTree)) {
//                            Graph gr = new Graph();
//                            for (Integer t : element) {
//                                Pair<Integer, Integer> edgeIndices = edgNum.get(t);
//                                Vertex v1 = getVertices().get(edgeIndices.getKey());
//                                Vertex v2 = getVertices().get(edgeIndices.getValue());
//                                gr.addNeighbor(new Pair<>(v1, v2));
//                            }
//                            result.add(gr);
//                        }
//                        return result;
//                    }
//                }
//            }
            // On regarde si le graphe moins l'arbre couvrant est connexe (=diff),
            // si oui on prend un arbre couvrant grâce à Kruskal, s'il est couvrant du graphe de base
            // nous avons nos deux arbres couvrants disjoints
            for (List<Integer> tree : productResult) {
                Graph spanningTree = new Graph();
                for (Integer t : tree) {
                    Pair<Integer, Integer> edgeIndices = edgNum.get(t);
                    Vertex v1 = getVertices().get(edgeIndices.getKey());
                    Vertex v2 = getVertices().get(edgeIndices.getValue());
                    spanningTree.addNeighbor(new Pair<>(v1, v2));
                }
                Graph diff = new Graph();
                for (Pair<Vertex, Vertex> edge : getNeighbors()) {
                    if (!spanningTree.getNeighbors().contains(edge) && !spanningTree.getNeighbors().contains(new Pair<>(edge.getValue(), edge.getKey()))) {
                        diff.addNeighbor(edge);
                    }
                }
                if (diff.estConnexe()) {
                    // Second spanning tree ?
                    Graph kruskal = diff.Kruskal();
                    if (kruskal.getNbVertices() == getNbVertices()) // yes
                        return Arrays.asList(spanningTree, kruskal);
                }
            }
            all_span_trees.addAll(productResult);
        }
            for (int i = 0; i < k; i++) {
                if (edg.get(k).get(i).isEmpty()) continue;
                trs.add(edg.get(k).get(i));
                List<List<Integer>> concat = new ArrayList<>();
                for (int j = 0; j < i; j++) {
                    List<Integer> tempList = new ArrayList<>();
                    tempList.addAll(edg.get(i).get(j));
                    tempList.addAll(edg.get(k).get(j));
                    concat.add(tempList);
                }
                edg.set(i, concat);
                List<Graph> spanningTrees = spanTrees(trs, edg, all_span_trees, k - 1, edgNum);
                if (!spanningTrees.isEmpty()) return spanningTrees;
                trs.removeLast();
                for (int j = 0; j < i; j++) {
                    for (int m = 0; m < edg.get(k).get(j).size(); m++) {
                        edg.get(i).get(j).removeLast();
                    }
                }
            }
            return new ArrayList<>();
        }

    // Algorithme permettant d'énumérer tous les arbres couvrants trouvé ici :
    // Tag, M. A., & Mansour, M. E. (2019). Automatic computing of the grand potential in finite temperature many-body
    // perturbation theory. International Journal of Modern Physics C, 30(11), 1950100.
    public List<Graph> getTwoDistinctSpanningTrees() {
        if (!estConnexe()) return new ArrayList<>();
        int n = getNbVertices();
        List<List<List<Integer>>> edg = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            edg.add(new ArrayList<>(Collections.nCopies(n, new ArrayList<>())));
        }
        int mx = getNeighbors().size();
        Map<Integer, Pair<Integer, Integer>> edgNum = new HashMap<>();
        for (Pair<Vertex, Vertex> ed : getNeighbors()) {
            int index1 = getVertices().indexOf(ed.getKey());
            int index2 = getVertices().indexOf(ed.getValue());
            int i = Math.min(index1, index2);
            int j = Math.max(index1, index2);
            List<Integer> lMx = new ArrayList<>();
            lMx.add(mx);
            edg.get(j).set(i, lMx);
            edgNum.put(mx, new Pair<>(i, j));
            mx--;
        }
        return spanTrees(new ArrayList<>(), edg, new ArrayList<>(), n-1, edgNum);
    }

    private int find(List<Integer> parent, int i) {
        if (parent.get(i) == i) return i;
        return find(parent, parent.get(i));
    }

    private void union(List<Integer> parent, List<Integer> rank, int x, int y) {
        int xroot = find(parent, x);
        int yroot = find(parent, y);
        if (rank.get(xroot) < rank.get(yroot))
            parent.set(xroot, yroot);
        else if (rank.get(xroot) > rank.get(yroot))
            parent.set(yroot, xroot);
        else {
            parent.set(yroot, xroot);
            rank.set(xroot, rank.get(xroot)+1);
        }
    }

    public Graph Kruskal() {
        List<Pair<Vertex, Vertex>> neighbors = new ArrayList<>(getNeighbors());
        List<Pair<Vertex, Vertex>> result = new ArrayList<>();
        int i = 0;
        int e = 0;
        List<Integer> parent = new ArrayList<>();
        List<Integer> rank = new ArrayList<>();
        for (int node = 0; node < getNbVertices(); node++) {
            parent.add(node);
            rank.add(0);
        }
        while (e < getNbVertices() - 1) {
           Pair<Vertex, Vertex> uv = neighbors.get(i);
           i++;
           int x = find(parent, getVertices().indexOf(uv.getKey()));
           int y = find(parent, getVertices().indexOf(uv.getValue()));
           if (x != y) {
               e++;
               result.add(uv);
               union(parent, rank, x, y);
           }
        }
        return new Graph(result);
    }

    public String toString() {
        StringBuilder r = new StringBuilder();
        for (Vertex v : getVertices())
            r.append(getVertices().indexOf(v)).append(" ");
        return r.toString();
    }
}

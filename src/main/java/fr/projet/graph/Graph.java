package fr.projet.graph;

import fr.projet.gui.UtilsGui;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;
import java.util.function.BiPredicate;

@Accessors(chain = true)
@Data
@Slf4j
public class Graph {

    // nombre de vertex
    private int nbVertices = 5;

    private boolean aroundCircle = false;

    private double proba = 1;

    private Random random = new Random();

    private List<Vertex> vertices = new ArrayList<>();
    private Map<Vertex, List<Vertex>> adjVertices = new HashMap<>();

    @Getter
    private Set<Pair<Vertex, Vertex>> neighbors = new HashSet<>();
    public Graph(Collection<Pair<Vertex, Vertex>> neighbors) {
        this.neighbors = new HashSet<>(neighbors);
        Set<Vertex> vertexSet = new HashSet<>();
        for (Pair<Vertex, Vertex> element : this.neighbors) {
            vertexSet.add(element.getKey());
            vertexSet.add(element.getValue());
            addNeighbor(element);
        }
        this.vertices = new ArrayList<>(vertexSet);
        this.nbVertices = vertexSet.size();
    }

    public Graph(Collection<Vertex> vertices, Map<Vertex, List<Vertex>> adjVertices) {
        for (Vertex v : vertices) {
            addVertex(v);
        }
        for (Map.Entry<Vertex, List<Vertex>> entry : adjVertices.entrySet()) {
            for (Vertex v : entry.getValue()) {
                addNeighbor(new Pair<>(entry.getKey(), v));
            }
        }
    }

    public Graph(int nbVertices, int maxDeg, int minDeg) {
        this.nbVertices = nbVertices;
        this.adjVertices = new HashMap<>();
        this.generateGraphPlanaire(maxDeg, minDeg);
    }

    public Graph(int nbVertices, int maxDeg, int minDeg, long seed) {
        this.nbVertices = nbVertices;
        this.adjVertices = new HashMap<>();
        random = new Random(seed);
        this.generateGraphPlanaire(maxDeg, minDeg);
    }

    public Graph() {
        this.adjVertices = new HashMap<>();
    }
    public void addVertex(Vertex v) {
        adjVertices.putIfAbsent(v, new ArrayList<>());
        vertices.add(v);
    }

    public void removeVertex(Vertex v) {
        vertices.remove(v);
        getAdjVertices().remove(v);
        for (List<Vertex> listVertex : getAdjVertices().values()) {
            listVertex.remove(v);
        }
        neighbors.removeIf(neighbor -> neighbor.getKey().equals(v) || neighbor.getValue().equals(v));
    }

    public int getNbVertices() {return getVertices().size();}

    private static boolean det(int x1, int y1, int x2, int y2, int x3, int y3) {
        return (y3-y1)*(x2-x1) > (y2-y1)*(x3-x1);
    }

    private boolean intersect(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        return det(x1, y1, x3, y3, x4, y4) != det(x2, y2, x3, y3, x4, y4) && det(x1, y1, x2, y2, x3, y3) != det(x1, y1, x2, y2, x4, y4);
    }
  
    private void generateGraphPlanaire(int maxDeg, int minDeg) {
        double minDist = 100; // distance minimale entre deux sommets
        int maxSize = 1000; // taille de la fenêtre sur laquelle on place les sommets (fenêtre virtuelle qui ne dépend pas des écrans)
        double radius = UtilsGui.CIRCLE_SIZE*3; // rayon d'un sommet que l'on considère pour les collisions
        int maxIter = nbVertices*100; // nombre d'itérations max pour placer les sommets
        int iterCount = 0;
        List<Pair<Vertex, Vertex>> edges = new ArrayList<>();
        while (getNbVertices() < nbVertices) {
            iterCount++;
            // Coord aléatoire
            Vertex newVertex = new Vertex(random.nextInt(UtilsGui.WINDOW_MARGE, maxSize-UtilsGui.WINDOW_MARGE),
                                          random.nextInt(UtilsGui.WINDOW_MARGE, maxSize-UtilsGui.WINDOW_MARGE));
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
            if (iterCount >= maxIter) return;
        }
        // Ces boucles imbriquées permettent de relier un maximum de sommets
        for (int i = 0; i < getVertices().size(); i++) {
            Vertex v = getVertices().get(i);
            for (int j = 0; j < i; j++) {
                Vertex v2 = getVertices().get(j);
                if (getAdjVertices().get(v).contains(v2)) continue; // Si les sommets sont déjà reliés, on passe
                boolean intersect = false;
                for (Pair<Vertex, Vertex> neighbor : getNeighbors()) {
                    if (!neighbor.getValue().equals(v) && !neighbor.getValue().equals(v2) && !neighbor.getKey().equals(v) && !neighbor.getKey().equals(v2) && intersect(v.getX(), v.getY(), v2.getX(), v2.getY(),
                            neighbor.getKey().getX(), neighbor.getKey().getY(), neighbor.getValue().getX(), neighbor.getValue().getY())) {
                        intersect = true;
                        break; // S'il y a une intersection, pas la peine de continuer
                    }
                }
                if (!intersect && !thereAreACircleCollision(radius, v, v2) && (degree(v) < maxDeg && degree(v2) < maxDeg))
                {
                    Pair<Vertex, Vertex> neighbor = new Pair<>(v, v2);
                    addNeighbor(neighbor);
                    edges.add(neighbor); // Permet de synchroniser dans le cas des games en ligne
                }
            }
        }
        for (Pair<Vertex, Vertex> edge : edges) {
            float p = random.nextFloat();
            if (p > proba && degree(edge.getKey()) > minDeg && degree(edge.getValue()) > minDeg)
                removeNeighbor(edge);
        }
        // On converti les coordonnées des sommets pour les afficher sur l'écran
        for (Vertex v : getVertices()) {
            v.setCoords(new Pair<>((int) toScreenSize(v.getX(), 0, maxSize, UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_WIDTH-UtilsGui.WINDOW_MARGE),
                    (int) toScreenSize(v.getY(), 0, maxSize, UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_HEIGHT-UtilsGui.WINDOW_MARGE)));
        }
    }

    // Fonction qui permet de convertir les coordonnées des sommets pour les afficher sur l'écran
    private double toScreenSize(double x, double a, double b, double c, double d) {
        return (x-a)*(d-c)/(b-a)+c;
    }

    public int degree(Vertex v) {
        return getAdjVertices().get(v).size();
    }

    private void graphForCUT () {
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        int compteur=0;
        generateGraphPlanaire(7, 3);
        int tailleMax = 2*(this.vertices.size()-1)-1;
        while (this.neighbors.size()>tailleMax) {
            compteur+=1;
            //System.out.println(this.neighbors.size());
            int x=random.nextInt(this.neighbors.size());
            HashSet<Pair<Vertex, Vertex>> save = new HashSet<>(this.getNeighbors());
            List<Pair<Vertex, Vertex>> newVertice = new ArrayList<>(this.neighbors);
            newVertice.remove(x);
            this.neighbors= new HashSet<>(newVertice);
            System.out.println("A : "+this.minDeg());
            if (this.minDeg()<2) {
                this.neighbors=new HashSet<>(save);
            }
            System.out.println("B : "+this.minDeg());
            if (compteur>=this.neighbors.size()*10) {
                System.out.println("OOOOH");
                graphForCUT();
            }
        }
    }


    private void graphForSHORT () {
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        int compteur=0;
        generateGraphPlanaire(7, 3);
        Graph tree = getSpanningTree();
        if (difference((HashSet<Pair<Vertex, Vertex>>) tree.getNeighbors())) {
            graphForSHORT();
        }
    }




    private boolean thereAreACircleCollision(double radius, Vertex v1, Vertex v2) {
        // On simplifie en faisant une croix dans le cercle et on vérifie l'intersection avec les deux segments
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

    public int minDeg() {
        int minDeg = getAdjVertices().get(getVertices().getFirst()).size();
        for (Vertex v: getVertices()) {
            if (getAdjVertices().get(v).size() < minDeg) {
                minDeg = getAdjVertices().get(v).size();
            }
        }
        return minDeg;
    }

    public int maxDeg() {
        int maxDeg = degree(getVertices().getFirst());
        for (Vertex v: getVertices()) {
            int dv = degree(v);
            if (dv > maxDeg) {
                maxDeg = dv;
            }
        }
        return maxDeg;
    }

    public boolean estConnexe() {
        if (getVertices().isEmpty()) {
            return true;
        }
        HashSet<Vertex> marked = new HashSet<>();
        Deque<Vertex> pile = new ArrayDeque<>();
        pile.push(getVertices().getFirst());
        while (!pile.isEmpty()) {
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

    public Set<Vertex> getComponent(Vertex vertex, BiPredicate<Vertex, Vertex> predicate) {
        if (getVertices().isEmpty()) {
            return new HashSet<>();
        }
        HashSet<Vertex> marked = new HashSet<>();
        Deque<Vertex> pile = new ArrayDeque<>();
        pile.push(vertex);
        while (!pile.isEmpty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                for (Vertex t: getAdjVertices().get(v).stream().filter(x -> predicate.test(x, v)).toList()) {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                }
            }
        }
        return marked;
    }

    public void removeNeighbor(Pair<Vertex, Vertex> edge) {
        neighbors.remove(edge);
        neighbors.remove(new Pair<>(edge.getValue(), edge.getKey()));
        if (adjVertices.containsKey(edge.getKey()))
            adjVertices.get(edge.getKey()).remove(edge.getValue());
        if (adjVertices.containsKey(edge.getValue()))
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

    public boolean difference (Set<Pair<Vertex, Vertex>> cutted) { //true si this\cutted est non connexe
        Set<Pair<Vertex, Vertex>> verticeNew = new HashSet<>();
        for (Pair<Vertex, Vertex> edge : neighbors) {
            if (!cutted.contains(edge)) {
                verticeNew.add(edge);
            }
        }
        Graph toTest = new Graph(verticeNew);
        HashSet<Vertex> hVertices = new HashSet<>(toTest.getVertices());
        for (Vertex v : this.getVertices()) {
            if (!hVertices.contains(v)) return true;
        }

        return !toTest.estConnexe();
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
                    List<Integer> resultList = new ArrayList<>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }
    private List<Graph> spanTrees(List<List<Integer>> trs, List<List<List<Integer>>> edg, int k, Map<Integer, Pair<Integer, Integer>> edgNum) {
        if (k == 0) {
            List<List<Integer>> productResult = cartesianProduct(trs);
            // Code pour renvoyer deux arbres couvrants par énumération
            // On prend le graphe moins l'arbre couvrant (=diff)
            // On prend un arbre couvrant de diff
            // Si cet arbre est couvrant du graphe de base alors on a trouvé deux arbres couvrants disjoints
            for (List<Integer> tree : productResult) {
                Graph spanningTree = new Graph();
                for (Integer t : tree) {
                    Pair<Integer, Integer> edgeIndices = edgNum.get(t);
                    Vertex v1 = getVertices().get(edgeIndices.getKey());
                    Vertex v2 = getVertices().get(edgeIndices.getValue());
                    spanningTree.addNeighbor(new Pair<>(v1, v2));
                }
                boolean isNotPossibleToGetASecondSpanningTree = false;
                for (Vertex v : spanningTree.getVertices()) {
                    if (spanningTree.getAdjVertices().get(v).size() == getAdjVertices().get(v).size())
                    {
                        isNotPossibleToGetASecondSpanningTree = true; // Si un sommet d'un arbre couvrant a le même degré que le graphe de base,
                        break;  // cela veut dire qu'il ne peut pas y avoir de second arbre couvrant disjoint de celui-ci
                    }   // car le graphe complémentaire ne contiendrait pas ce sommet
                }
                if (isNotPossibleToGetASecondSpanningTree) continue;
                Graph diff = new Graph();
                for (Pair<Vertex, Vertex> edge : getNeighbors()) {
                    if (!spanningTree.getNeighbors().contains(edge) && !spanningTree.getNeighbors().contains(new Pair<>(edge.getValue(), edge.getKey()))) {
                        diff.addNeighbor(edge);
                    }
                }
                // Second spanning tree ?
                Graph kruskal = diff.getSpanningTree();
                if (kruskal.getNbVertices() == getNbVertices() && kruskal.estConnexe()) // yes
                    return Arrays.asList(spanningTree, kruskal);
            }
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
                List<Graph> spanningTrees = spanTrees(trs, edg, k - 1, edgNum);
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
        return spanTrees(new ArrayList<>(), edg, n-1, edgNum);
    }

    public Graph getSpanningTree() {
        List<Vertex> file = new ArrayList<>();
        Set<Vertex> marques = new HashSet<>();
        Set<Pair<Vertex, Vertex>> edges = new HashSet<>();
        file.add(getVertices().getFirst());
        while (!file.isEmpty()) {
            Vertex s = file.getFirst();
            file.removeFirst();
            for (Vertex t : getAdjVertices().get(s)) {
                if (!marques.contains(t)) {
                    file.add(t);
                    edges.add(new Pair<>(s, t));
                    marques.add(t);
                }
            }
        }

        return new Graph(edges);
    }



    public String toString() {
        StringBuilder r = new StringBuilder();
        for (Vertex v : getVertices())
            r.append(getVertices().indexOf(v)).append(" ");
        return r.toString();
    }

    public static double distancePointSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double a = x - x1;
        double b = y - y1;
        double c = x2 - x1;
        double d = y2 - y1;

        double dot = a * c + b * d;
        double len = c * c + d * d;
        double param = -1;
        if (len != 0) //in case of 0 length line
            param = dot / len;

        double xx;
        double yy;

        if (param < 0) { // P is between P1 and P2
            xx = x1;
            yy = y1;
        } else if (param > 1) { // closest point in segment is P2
            xx = x2;
            yy = y2;
        } else { // closest point in segment is P1
            xx = x1 + param * c;
            yy = y1 + param * d;
        }

        double dx = x - xx;
        double dy = y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    //#############################################################################################################
    //#############################################################################################################
    //#############################################################################################################
    //#############################################################################################################

    //Stratégie Gagnante :

    public static boolean sameEdge (Pair<Vertex,Vertex> e1, Pair<Vertex,Vertex> e2) {
        if ((e1.getKey()==e2.getKey() && e1.getValue()==e2.getValue()) || (e1.getKey()==e2.getValue() && e1.getValue()==e2.getKey())) {
            return true;
        }
        return false;
    }

    public static Pair<Vertex,Vertex> reverseEdge (Pair<Vertex,Vertex> e1) {
        Pair<Vertex,Vertex> e2 = new Pair<>(e1.getValue(),e1.getKey());
        return e2;
    }


    public Graph soustraction (Graph dif) {
        HashSet <Pair<Vertex, Vertex>> newNeib = new HashSet<>();
        for (Pair<Vertex,Vertex> e : this.getNeighbors()) {
            if (!dif.getNeighbors().contains(e) && !dif.getNeighbors().contains(reverseEdge(e))) {
                newNeib.add(e);
            }
        }
        Graph g1 = new Graph(newNeib);
        for (Vertex v : dif.getVertices()) {
            if (!g1.getVertices().contains(v)) {
                g1.addVertex(v);
            }
        }
        return g1;
    }


    public boolean endEvalutation (ArrayList<ArrayList<Vertex>> P) {
        boolean result = true;
        for (ArrayList<Vertex> partition : P) {
            HashSet<Pair<Vertex, Vertex>> newNeib = new HashSet<>(this.getNeighbors());
            for (Pair<Vertex, Vertex> edge : this.getNeighbors()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
            }
            Graph toTest = new Graph(newNeib);
            for (Vertex v : partition) {
                if (!toTest.getVertices().contains(v)) {
                    toTest.addVertex(v);
                }
            }
            if (!toTest.estConnexe()) {
                result=false;
            }
        }
        return result;
    }

    public ArrayList<Graph> graphInPartitions (ArrayList<ArrayList<Vertex>> P) {
        //prend en paramettre un partition et renvoie la liste qui contient les graphequi sont les sous graphe de this dans les partie de P

        ArrayList<Graph> res = new ArrayList<>();
        for (ArrayList<Vertex> partition : P) {
            HashSet<Pair<Vertex, Vertex>> newNeib = new HashSet<>();
            for (Pair<Vertex, Vertex> edge : this.getNeighbors()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
            }
            Graph toTest = new Graph(newNeib);
            for (Vertex v : partition) {
                if (!toTest.getVertices().contains(v)) {
                    toTest.addVertex(v);
                }
            }
            res.add(toTest);
        }
        return res;
    }




    public ArrayList<Vertex> allComponent(Vertex vertex) {
        if (getVertices().isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<Vertex> marked = new ArrayList<>();
        Deque<Vertex> pile = new ArrayDeque<>();
        pile.push(vertex);
        while (!pile.isEmpty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                for (Vertex t: this.getAdjVertices().get(v)) {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                }
            }
        }
        return marked;
    }


    public static boolean contientTab (ArrayList<ArrayList<Vertex>> tab, ArrayList<Vertex> s2) {
        for (ArrayList<Vertex> l : tab) {
            if (l.containsAll(s2) && s2.containsAll(l)) {
                return true;
            }
        }
        return false;
    }


    public static ArrayList<ArrayList<Vertex>> composantesConnexe (ArrayList<Graph> listG) {
        ArrayList<ArrayList<Vertex>> newP = new ArrayList<>();
        for (Graph G : listG) {
            for (Vertex v : G.getVertices()) {
                ArrayList<Vertex> comp = G.allComponent(v);
                if (!contientTab(newP,comp)) {
                    newP.add(comp);
                }
            }
        }
        return newP;
    }


    public Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> getT (ArrayList<ArrayList<Vertex>> P, Map<Pair<Vertex,Vertex>,Integer> levels, int actualLevel) {
        ArrayList<Graph> res = new ArrayList<>();
        HashSet<Pair<Vertex, Vertex>> newNeib = new HashSet<>();
        for (ArrayList<Vertex> partition : P) {
            for (Pair<Vertex, Vertex> edge : this.getNeighbors()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
                else {
                    levels.put(edge,actualLevel);
                }
            }
        }
        Graph toTest = new Graph(newNeib);
        for (Vertex v : this.getVertices()) {
            if (!toTest.getVertices().contains(v)) {
                toTest.addVertex(v);
            }
        }
        return new Pair<>(toTest,levels);
    }


    public Pair<Vertex,Vertex> cycle (ArrayList<ArrayList<Vertex>> P, Map<Pair<Vertex,Vertex>,Integer> levels) {
        Pair<Vertex,Vertex> res = this.getNeighbors().iterator().next();
        int level = -100000000;
        for (Pair<Vertex,Vertex> e : this.getNeighbors()) {
            HashSet<Pair<Vertex,Vertex>> newV = new HashSet(this.getNeighbors());
            newV.remove(e);
            Graph newG = new Graph(newV);
            if (newG.estConnexe()) {
                if (levels.get(e)<level) {
                    res=e;
                    level=levels.get(e);
                }
            }
        }
        return res;
    }

    public Pair<Vertex,Vertex> cycleForT1 (ArrayList<ArrayList<Vertex>> P, Map<Pair<Vertex,Vertex>,Integer> levels, Pair<Vertex,Vertex> stay) {
        Pair<Vertex,Vertex> res = this.getNeighbors().iterator().next();
        int level = -100000000;
        for (Pair<Vertex,Vertex> e : this.getNeighbors()) {
            HashSet<Pair<Vertex,Vertex>> newV = new HashSet(this.getNeighbors());
            newV.remove(e);
            Graph newG = new Graph(newV);
            if (newG.estConnexe()) {
                if (levels.get(e)<level && e!=stay) {
                    res=e;
                    level=levels.get(e);
                }
            }
        }
        return res;
    }


    public Pair<Boolean,Pair<Pair<Graph,Graph>,ArrayList<Pair<Vertex,Vertex>>>> winningStrat (Graph T1, Graph T2) { // true si SHORT win !!!!!!
        if (T2.estConnexe()) {
            System.out.println("sortie par T2 connexe");
            return new Pair<>(true, new Pair<>( new Pair<>(T1,T2), new ArrayList<>())); //Short Win
        }

        //On créé les levels et on dit que le level actuel est de 1
        Map<Pair<Vertex,Vertex>,Integer> levels = new HashMap<>();
        for (Pair<Vertex,Vertex> e : this.getNeighbors()) {
            levels.put(e,0);
        }
        int actualLevel=1;

        //On creer la premiere partiton qui contient tout
        ArrayList<Vertex> allVertice = new ArrayList<>(this.vertices);
        ArrayList<ArrayList<Vertex>> P = new ArrayList<>();
        P.add(allVertice);


        boolean turn2 = true;//Tour de T1 ou T2, true si tour de T2

        while (!T1.endEvalutation(P) && !T2.endEvalutation(P)) {
            System.out.println("On est dedans ! "+actualLevel);
            if (turn2) {
                ArrayList<Graph> GraphInP = T2.graphInPartitions(P);
                P = composantesConnexe(GraphInP);
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT1 = T1.getT(P,levels,actualLevel);
                T1 = pairT1.getKey();
                levels = new HashMap<>(pairT1.getValue());
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT2 = T2.getT(P,levels,actualLevel);
                T2 = pairT2.getKey();
                levels = new HashMap<>(pairT1.getValue());
            }
            else {
                ArrayList<Graph> GraphInP =T1.graphInPartitions(P);
                P = composantesConnexe(GraphInP);
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT1 = T1.getT(P,levels,actualLevel);
                T1 = pairT1.getKey();
                levels = new HashMap<>(pairT1.getValue());
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT2 = T2.getT(P,levels,actualLevel);
                T2 = pairT2.getKey();
                levels = new HashMap<>(pairT1.getValue());
            }
            actualLevel+=1;
        }

        //P est la partition finale

        //On regarde combien il y a d'arêtes à couper et on les stockes
        ArrayList<Pair<Vertex,Vertex>> toCut = new ArrayList<>();
        int compteur = 0;
        for (Pair<Vertex,Vertex> e : this.getNeighbors()) {
            boolean cutable = true;
            for (ArrayList<Vertex> partie : P) {
                if (partie.contains(e.getValue()) && partie.contains(e.getKey())) {
                    cutable=false;
                }
            }
            if (cutable==true) {
                compteur+=1;
                toCut.add(e);
            }
        }

        //On calcule maintenant si cut gagne
        if (compteur>2*P.size()-3) {
            System.out.println("sortie par toCut : compteur :"+compteur+" P size : "+P.size()+" P :\n"+P);
            return new Pair<>(false,new Pair<>(new Pair<Graph,Graph>(new Graph(0,0,0,0), new Graph(0,0,0,0)), toCut));
        }

        //là faut trouver le cycle et l'arrete on utilise une fonction qui trouve l'arrête
        Pair<Vertex,Vertex> toRemove = T2.cycle(P,levels);
        T2.removeNeighbor(toRemove);
        T1.addNeighbor(toRemove);
        T1.removeNeighbor(T1.cycleForT1(P,levels,toRemove));

        return winningStrat(T1,T2);
    }


    public ArrayList<Graph> appelStratGagnante () {
        Graph T1 = getSpanningTree();
        Graph T2 = this.soustraction(T1);

        ArrayList<Graph> ret = new ArrayList<>();
        //Pair<Boolean,Pair<Pair<Graph,Graph>,ArrayList<Pair<Vertex,Vertex>>>> res = this.winningStrat(T1,T2);
        //System.out.println("====================================================================================\n"+res.getKey());
        //System.out.println("====================================================================================\n");

        //Graph T3 = res.getValue().getKey().getKey();
        //Graph T4 = res.getValue().getKey().getValue();


        //######################################
        //########## SECTION DE TEST ###########
        //######################################

        ArrayList<Vertex> allVertice = new ArrayList<>(this.vertices);
        ArrayList<ArrayList<Vertex>> allVertice2 = new ArrayList<>();
        for (Vertex v : allVertice) {
            ArrayList<Vertex> vl = new ArrayList<>();
            vl.add(v);
            allVertice2.add(new ArrayList<>(vl));
        }
        ArrayList<ArrayList<Vertex>> P = new ArrayList<>();
        P.add(allVertice);
//
//        Graph T3 = this.getSpanningTree();
//
//        Graph T5 = new Graph();
//        for (Vertex v : this.getVertices()) {
//            T5.addVertex(v);
//        }
//        System.out.println("T5 e : "+T5.getNeighbors());
//
//
//        System.out.println(P+"\n"+T5.endEvalutation(P)+"\n");
//        System.out.println(allVertice2+"\n"+T5.endEvalutation(allVertice2));

        System.out.println(T1.endEvalutation(P));
        System.out.println(T2.endEvalutation(P));

        //Graph T4 = this.soustraction(T3);

        ret.add(T1);
        ret.add(T2);

        System.out.println("Pour T1 : ");
        ArrayList<Graph> GraphInP =T1.graphInPartitions(P);
        for (Graph g : GraphInP) {
            System.out.println(g.getVertices());
        }

        System.out.println("Pour T2 : ");
        ArrayList<Graph> GraphInP2 =T2.graphInPartitions(P);
        for (Graph g : GraphInP2) {
            System.out.println(g.getVertices());
        }


        return ret;
    }




}



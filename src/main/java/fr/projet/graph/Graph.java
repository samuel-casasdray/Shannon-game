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

    public static int globalVariable = 0;

    // nombre de vertex
    private int nbVertices = 5;

    private boolean aroundCircle = false;

    private double proba = 0.8; // Probabilité de garder l'arête

    private Random random = new Random();

    private List<Vertex> vertices = new ArrayList<>();
    private Map<Vertex, HashSet<Vertex>> adjVertices = new HashMap<>();

    @Getter
    private Set<Pair<Vertex, Vertex>> edges = new HashSet<>();
    public Graph(Collection<Pair<Vertex, Vertex>> neighbors) {
        Set<Vertex> vertexSet = new HashSet<>();
        for (Pair<Vertex, Vertex> element : neighbors) {
            vertexSet.add(element.getKey());
            vertexSet.add(element.getValue());
            addEdge(element);
        }
        this.vertices = new ArrayList<>(vertexSet);
        this.nbVertices = vertexSet.size();
    }

    public Graph(Collection<Vertex> vertices, Map<Vertex, HashSet<Vertex>> adjVertices) {
        for (Vertex v : vertices) {
            addVertex(v);
        }
        for (Map.Entry<Vertex, HashSet<Vertex>> entry : adjVertices.entrySet()) {
            for (Vertex v : entry.getValue()) {
                addEdge(new Pair<>(entry.getKey(), v));
            }
        }
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
        if (adjVertices.containsKey(v)) return;
        adjVertices.putIfAbsent(v, new HashSet<>());
        vertices.add(v);
    }

    public void removeVertex(Vertex v) {
        vertices.remove(v);
        getAdjVertices().remove(v);
        for (HashSet<Vertex> listVertex : getAdjVertices().values()) {
            listVertex.remove(v);
        }
        edges.removeIf(neighbor -> neighbor.getKey().equals(v) || neighbor.getValue().equals(v));
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
            Vertex newVertex = new Vertex(random.nextInt(0, maxSize),
                                          random.nextInt(0, maxSize));
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
                for (Pair<Vertex, Vertex> neighbor : getEdges()) {
                    if (!neighbor.getValue().equals(v) && !neighbor.getValue().equals(v2) && !neighbor.getKey().equals(v) && !neighbor.getKey().equals(v2) && intersect(v.getX(), v.getY(), v2.getX(), v2.getY(),
                            neighbor.getKey().getX(), neighbor.getKey().getY(), neighbor.getValue().getX(), neighbor.getValue().getY())) {
                        intersect = true;
                        break; // S'il y a une intersection, pas la peine de continuer
                    }
                }
                if (!intersect && !thereAreACircleCollision(radius, v, v2) && (degree(v) < maxDeg && degree(v2) < maxDeg))
                {
                    Pair<Vertex, Vertex> neighbor = new Pair<>(v, v2);
                    addEdge(neighbor);
                    edges.add(neighbor); // Permet de synchroniser dans le cas des games en ligne
                }
            }
        }
        for (Pair<Vertex, Vertex> edge : edges) {
            float p = random.nextFloat();
            if (p > proba && degree(edge.getKey()) > minDeg && degree(edge.getValue()) > minDeg)
                removeNeighbor(edge);
        }
        // On convertit les coordonnées des sommets pour les afficher sur l'écran
        for (Vertex v : getVertices()) {
            v.setCoords(new Pair<>((int) toScreenSize(v.getX(), 0, maxSize, UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_WIDTH-UtilsGui.WINDOW_MARGE),
                    (int) toScreenSize(v.getY(), 0, maxSize, UtilsGui.WINDOW_MARGE, UtilsGui.WINDOW_HEIGHT-UtilsGui.WINDOW_MARGE*2)));
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
        while (this.edges.size()>tailleMax) {
            compteur+=1;
            //System.out.println(this.neighbors.size());
            int x=random.nextInt(this.edges.size());
            HashSet<Pair<Vertex, Vertex>> save = new HashSet<>(this.getEdges());
            List<Pair<Vertex, Vertex>> newVertice = new ArrayList<>(this.edges);
            newVertice.remove(x);
            this.edges = new HashSet<>(newVertice);
            System.out.println("A : "+this.minDeg());
            if (this.minDeg()<2) {
                this.edges =new HashSet<>(save);
            }
            System.out.println("B : "+this.minDeg());
            if (compteur>=this.edges.size()*10) {
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
        if (difference((HashSet<Pair<Vertex, Vertex>>) tree.getEdges())) {
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
        edges.remove(edge);
        edges.remove(new Pair<>(edge.getValue(), edge.getKey()));
        if (adjVertices.containsKey(edge.getKey()))
            adjVertices.get(edge.getKey()).remove(edge.getValue());
        if (adjVertices.containsKey(edge.getValue()))
            adjVertices.get(edge.getValue()).remove(edge.getKey());
    }

    public void addEdge(Pair<Vertex, Vertex> edge) {
        if (!edges.contains(new Pair<>(edge.getValue(), edge.getKey())))
        {
            boolean contained = edges.add(edge);
            if (!contained) return;
        }
        else return;
        if (!adjVertices.containsKey(edge.getKey()))
        {
            vertices.add(edge.getKey());
            adjVertices.put(edge.getKey(), new HashSet<>());
        }
        if (!adjVertices.containsKey(edge.getValue()))
        {
            vertices.add(edge.getValue());
            adjVertices.put(edge.getValue(), new HashSet<>());
        }
        getAdjVertices().get(edge.getKey()).add(edge.getValue());
        getAdjVertices().get(edge.getValue()).add(edge.getKey());
    }


    public void printGraph () { //Affiche le graphe dans la console (pour debuguer surtout)
        for (Pair<Vertex, Vertex> element : edges) {
            log.info("Arrete : "+element.getKey().toString()+element.getValue().toString());
        }
        log.info(ToStringBuilder.reflectionToString(this.vertices));
    }

    public boolean estCouvrant (Graph G) { //true si G est couvrant de this
        return G.getNbVertices() == nbVertices && G.estConnexe();
    }

    public boolean difference (Set<Pair<Vertex, Vertex>> cutted) { //true si this\cutted est non connexe
        Set<Pair<Vertex, Vertex>> verticeNew = new HashSet<>();
        for (Pair<Vertex, Vertex> edge : edges) {
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
                    if (!edges.contains(new Pair<>(s,t)) && !edges.contains(new Pair<>(t,s))) {
                        edges.add(new Pair<>(s, t));
                    }
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

    public List<Pair<Integer,Integer>> printNeib () {
        List<Pair<Integer,Integer>> tab = new ArrayList<>();
        for (Pair<Vertex,Vertex> e : this.getEdges()) {
            Pair<Integer,Integer> p = new Pair<>(this.getVertices().indexOf(e.getKey()),this.getVertices().indexOf(e.getValue()));
            tab.add(p);
        }
        return tab;
    }

    public static boolean sameEdge (Pair<Vertex,Vertex> e1, Pair<Vertex,Vertex> e2) {
        return (e1.getKey() == e2.getKey() && e1.getValue() == e2.getValue()) ||
                (e1.getKey() == e2.getValue() && e1.getValue() == e2.getKey());
    }

    public static Pair<Vertex,Vertex> reverseEdge (Pair<Vertex,Vertex> e1) {
        return new Pair<>(e1.getValue(),e1.getKey());
    }


    public Graph soustraction (Graph dif) {
        HashSet <Pair<Vertex, Vertex>> newNeib = new HashSet<>();
        for (Pair<Vertex,Vertex> e : this.getEdges()) {
            if (!dif.getEdges().contains(e) && !dif.getEdges().contains(reverseEdge(e))) {
                newNeib.add(e);
            }
        }
        Graph g1 = new Graph(newNeib);
        for (Vertex v : dif.getVertices()) {
            if (!g1.getAdjVertices().keySet().contains(v)) {
                g1.addVertex(v);
            }
        }
        return g1;
    }


    public boolean endEvalutationAncien (ArrayList<ArrayList<Vertex>> P) {
        boolean result = true;
        for (ArrayList<Vertex> partition : P) {
            HashSet<Pair<Vertex, Vertex>> newNeib = new HashSet<>();
            for (Pair<Vertex, Vertex> edge : this.getEdges()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
            }
            Graph toTest = new Graph(newNeib);
            for (Vertex v : partition) {
                if (!toTest.getAdjVertices().keySet().contains(v)) {
                    toTest.addVertex(v);
                }
            }
            if (!toTest.estConnexe()) {
                result=false;
            }
        }
        return result;
    }

    public boolean endEvalutation (ArrayList<ArrayList<Vertex>> P) {
        boolean result = true;
        for (Graph G : this.graphInPartitions(P)) {
            if (!G.estConnexe()) {
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
            for (Pair<Vertex, Vertex> edge : this.getEdges()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
            }
            Graph toTest = new Graph(newNeib);
            for (Vertex v : partition) {
                if (!toTest.getAdjVertices().keySet().contains(v)) {
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

    public static ArrayList<Graph> composantesConnexeGraph (Graph G) {
        ArrayList<Graph> res = new ArrayList<>();
        ArrayList<ArrayList<Vertex>> newP = new ArrayList<>();
        for (Vertex v : G.getVertices()) {
            ArrayList<Vertex> comp = G.allComponent(v);
            if (!contientTab(newP,comp)) {
                newP.add(comp);
                ArrayList<Pair<Vertex,Vertex>> newNeib = new ArrayList<>();
                for (Pair<Vertex,Vertex> e : G.getEdges()) {
                    if (comp.contains(e.getKey()) && comp.contains(e.getValue())) {
                        newNeib.add(e);
                    }
                }
                Graph newG = new Graph(newNeib);
                for (Vertex ver : comp) {
                    if (!newG.getVertices().contains(ver)) {
                        newG.addVertex(v);
                    }
                }
                res.add(newG);
            }
        }
        return res;
    }


    public Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> getT (ArrayList<ArrayList<Vertex>> P, Map<Pair<Vertex,Vertex>,Integer> levels, int actualLevel) {
        HashSet<Pair<Vertex, Vertex>> newNeib = new HashSet<>();
        for (ArrayList<Vertex> partition : P) {
            for (Pair<Vertex, Vertex> edge : this.getEdges()) {
                if (partition.contains(edge.getValue()) && partition.contains(edge.getKey())) {
                    newNeib.add(edge);
                }
                else {
                    if (levels.containsKey(edge)) {
                        levels.put(edge, actualLevel);
                    }
                    else {
                        levels.put(reverseEdge(edge), actualLevel);
                    }
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
        Pair<Vertex,Vertex> res = this.getEdges().iterator().next();
        int level = 100000000;
        for (Graph G : composantesConnexeGraph(this)) {
            for (Pair<Vertex, Vertex> e : G.getEdges()) {
                HashSet<Pair<Vertex, Vertex>> newV = new HashSet(this.getEdges());
                newV.remove(e);
                boolean betweenTwo = true;
                for (ArrayList<Vertex> part : P) {
                    if (part.contains(e.getKey()) && part.contains(e.getValue())) {
                        betweenTwo = false;
                    }
                }

                Graph newG = new Graph(newV);
                for (Vertex v : G.getVertices()) {
                    if (!newG.getVertices().contains(v)) {
                        newG.addVertex(v);
                    }
                }
                if (newG.estConnexe() && betweenTwo) {
                    if (levels.containsKey(e)) {
                        if (levels.get(e) < level) {
                            res = e;
                            level = levels.get(e);
                        }
                    } else {
                        if (levels.get(reverseEdge(e)) < level) {
                            res = e;
                            level = levels.get(reverseEdge(e));
                        }
                    }
                }
            }
        }
        if (!this.getEdges().contains(res)) {
            res=reverseEdge(res);
        }
        return res;
    }

    public Pair<Vertex,Vertex> cycleForT1 (ArrayList<ArrayList<Vertex>> P, Map<Pair<Vertex,Vertex>,Integer> levels, Pair<Vertex,Vertex> stay) {
        Pair<Vertex,Vertex> res = this.getEdges().iterator().next();
        int level = 100000000;
        for (Pair<Vertex,Vertex> e : this.getEdges()) {
            HashSet<Pair<Vertex,Vertex>> newV = new HashSet(this.getEdges());
            newV.remove(e);
            Graph newG = new Graph(newV);
            for (Vertex v : this.getVertices()) {
                if (!newG.getVertices().contains(v)) {
                    newG.addVertex(v);
                }
            }
            if (newG.estConnexe()) {
                if (levels.containsKey(e)) {
                    if (levels.get(e)<level && e!=stay && !Objects.equals(e, reverseEdge(stay))) {
                        res=e;
                        level=levels.get(e);
                    }
                }
                else {
                    if (levels.get(reverseEdge(e))<level && e!=stay && !e.equals(reverseEdge(stay))) {
                        res=e;
                        level=levels.get(reverseEdge(e));
                    }
                }
            }
        }
        return res;
    }


    public Graph copy () {
        Graph res = new Graph(this.getEdges());
        for (Vertex v : this.getVertices()) {
            if (!res.getVertices().contains(v)) {
                res.addVertex(v);
            }
        }
        return res;
    }


    public Pair<Boolean,Pair<Pair<Graph,Graph>,ArrayList<Pair<Vertex,Vertex>>>> winningStrat (Graph T1, Graph T2) { // true si SHORT win !!!!!!
        Graph stockT1 = T1.copy();
        Graph stockT2 = T2.copy();
        if (T2.estConnexe()) {
            return new Pair<>(true, new Pair<>( new Pair<>(T1,T2), new ArrayList<>())); //Short Win
        }

        //On créé les levels et on dit que le level actuel est de 1
        Map<Pair<Vertex,Vertex>,Integer> levels = new HashMap<>();
        for (Pair<Vertex,Vertex> e : this.getEdges()) {
            levels.put(e,1000000);
        }
        int actualLevel=1;

        //On creer la premiere partiton qui contient tout
        ArrayList<Vertex> allVertice = new ArrayList<>(this.vertices);
        ArrayList<ArrayList<Vertex>> P = new ArrayList<>();
        P.add(allVertice);

        if (globalVariable>100) {
            return new Pair<>(true, new Pair<>( new Pair<>(null, null), new ArrayList<>())); //Short Win
        }
        globalVariable+=1;

        boolean turn2 = true;//Tour de T1 ou T2, true si tour de T2

        while (!T1.endEvalutation(P) || !T2.endEvalutation(P)) {
            if (turn2) {
                ArrayList<Graph> GraphInP = T2.graphInPartitions(P);
                P = composantesConnexe(GraphInP);
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT1 = T1.getT(P,levels,actualLevel);
                T1 = pairT1.getKey();
                levels = new HashMap<>(pairT1.getValue());
                turn2=false;
                T2.endEvalutation(P);
                T1.endEvalutation(P);
                T1.graphInPartitions(P);
            }
            else {
                ArrayList<Graph> GraphInP =T1.graphInPartitions(P);
                P = composantesConnexe(GraphInP);
                Pair<Graph,Map<Pair<Vertex,Vertex>,Integer>> pairT2 = T2.getT(P,levels,actualLevel);
                T2 = pairT2.getKey();
                levels = new HashMap<>(pairT2.getValue());
                turn2=true;
                T1.endEvalutation(P);
                T2.endEvalutation(P);
                T2.graphInPartitions(P);
            }
            actualLevel+=1;
        }
        //P est la partition finale
        T1 = stockT1.copy();
        T2 = stockT2.copy();

        //On regarde combien il y a d'arêtes à couper et on les stockes
        ArrayList<Pair<Vertex,Vertex>> toCut = new ArrayList<>();
        int compteur = 0;
        for (Pair<Vertex,Vertex> e : this.getEdges()) {
            boolean cutable = true;
            for (ArrayList<Vertex> partie : P) {
                if (partie.contains(e.getValue()) && partie.contains(e.getKey())) {
                    cutable=false;
                }
            }
            if (cutable && !toCut.contains(e) && !toCut.contains(reverseEdge(e))) {
                compteur+=1;
                toCut.add(e);
            }
        }

        //On calcule maintenant si cut gagne
        if (compteur<2*P.size()-2) {
            return new Pair<>(false,new Pair<>(new Pair<>(new Graph(0, 0, 0, 0), new Graph(0, 0, 0, 0)), toCut));
        }

        //là faut trouver le cycle et l'arrete on utilise une fonction qui trouve l'arrête
        Pair<Vertex,Vertex> toRemove = T2.cycle(P,levels);
        T2.removeNeighbor(toRemove);
        T1.addEdge(toRemove);
        Pair<Vertex,Vertex> toRemT1 = T1.cycleForT1(P,levels,toRemove);
        T1.removeNeighbor(toRemT1);
        T2.addEdge(toRemT1);
        return winningStrat(T1.copy(),T2.copy());
    }


    public ArrayList<Graph> appelStratGagnante () {
        globalVariable=0;

        Graph TA = getSpanningTree();
        Graph TB = this.soustraction(TA);
        ArrayList<Graph> ret = new ArrayList<>();
        var winningStrat = this.winningStrat(TA.copy(),TB.copy());
        if (winningStrat.getValue().getKey().getKey() == null ) {
            return new ArrayList<>();
        }
        Pair<Boolean,Pair<Pair<Graph,Graph>,ArrayList<Pair<Vertex,Vertex>>>> res = winningStrat;
        Graph T3 = res.getValue().getKey().getKey();
        Graph T4 = res.getValue().getKey().getValue();

        ArrayList<Pair<Vertex,Vertex>> aCut = res.getValue().getValue();
        if (!aCut.isEmpty()) {
            Graph T6 = new Graph(aCut);
            for (Vertex v : this.getVertices()) {
                if (!T6.getVertices().contains(v)) {
                    T6.addVertex(v);
                }
            }
            ret.add(T6);
            ret.add(new Graph());
            return ret;
        }

        //######################################
        //########## SECTION DE TEST ###########
        //######################################
        ret.add(T3);
        ret.add(T4.getSpanningTree());
        return ret;
    }




}



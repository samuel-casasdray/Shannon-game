package fr.projet.game;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import fr.projet.IA.BasicAI;
import fr.projet.IA.InterfaceIA;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.application.Application;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Game {
    private Graph graph;
    private Turn turn = Turn.CUT;
    @Setter
    private boolean cutWon = false;
    private boolean shortWon = false;
    private boolean againstAI = false;
    private InterfaceIA ia;
    private ArrayList<Pair<Vertex, Vertex>> secured = new ArrayList<>();
    public ArrayList<Pair<Vertex, Vertex>> cutted = new ArrayList<>();
    private long seed;

    public Game() {
        int nbVertices = 10;
        graph = new Graph(nbVertices);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices);
        }
        ia = new BasicAI(this, turn);
        if (againstAI) {
            Gui.setIa(ia);
            turn = turn.flip();
        }
        Gui.setGraph(graph);
        Gui.setGame(this);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }
    public Game(long seed) {
        this.seed = seed;
        int nbVertices = 10;
        graph = new Graph(nbVertices, seed);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices, seed);
        }
        ia = new BasicAI(this, turn);
        if (againstAI) {
            Gui.setIa(ia);
            turn = turn.flip();
        }
        Gui.setGraph(graph);
        Gui.setGame(this);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }

    private void play(Vertex key, Vertex value) {
        if (cutWon) return;
        if (againstAI && turn == Turn.CUT) {
            Pair<Vertex, Vertex> v = ia.playCUT();
            v.getKey().cut(v.getValue());
            cutted.add(new Pair<Vertex,Vertex>(v.getKey(), v.getValue()));
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(v)) {
                    element.getValue().getStrokeDashArray().addAll(25D, 10D);
                    break;
                }
            }
            turn = turn.flip();
        } else {
            for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                if (Vertex.isSameCouple(new Pair<>(key, value), neighbors.getKey())) {
                    if (key.isCutOrPanted(value)) {
                        return;
                    }
                    if (turn == Turn.CUT) {
                        key.cut(value);
                        cutted.add(new Pair<>(key, value));
                        neighbors.getValue().getStrokeDashArray().addAll(25D, 10D);

                    } else {
                        key.paint(value);
                        secured.add(new Pair<>(key, value));
                        neighbors.getValue().setStroke(Color.RED);
                    }
                    turn = turn.flip();
                    if (againstAI) {
                        play(key, value);
                    }
                    cutWon(); 
                    shortWon();
                    return;
                }
            }
        }
        cutWon();
        shortWon();
    }

    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon)
            return;
        if (mouseEvent.getSource() instanceof Line line &&
                line.getProperties().get("pair") instanceof Pair<?, ?> pair1 &&
                pair1.getKey() instanceof Vertex key && pair1.getValue() instanceof Vertex value) {
            play(key, value);
        }
        if (cutWon) {
            System.out.println("CUT a gagné");
        }
        else if (shortWon) {
            System.out.println("SHORT a gagné");
        }

    }

    public boolean shortWon() {
        if (!cutWon && cutted.size()+secured.size() >= graph.getNeighbors().size()) {
            shortWon = true;
        }
        return shortWon;
    }

    public boolean cutWon() {
        List<Vertex> notCuttedVerticices = graph.getVertices();
        notCuttedVerticices.stream().forEach(
            x -> x.setListNeighbors(x.getListNeighbors().stream().filter(y -> !x.isCut(y) && !y.isCut(x)).collect(Collectors.toCollection(ArrayList::new)))
        );
        Graph notCuttedGraph = new Graph(notCuttedVerticices);
        cutWon = !notCuttedGraph.estConnexe();
        return cutWon;
    }
}

package fr.projet.game;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.websocket.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.projet.WebSocket.WebSocketClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    @Setter
    private Turn turn = Turn.CUT;
    @Setter
    private boolean cutWon = false;
    private boolean shortWon = false;
    private final boolean againstAI = false;
    private InterfaceIA ia;
    private ArrayList<Pair<Vertex, Vertex>> secured = new ArrayList<>();
    public ArrayList<Pair<Vertex, Vertex>> cutted = new ArrayList<>();
    private boolean pvpOnline = false;
    private long seed;
    @Getter
    private  boolean joinerIsHere;
    private final String serverUri = "ws://51.75.126.59:2999/ws";
    private final String CreateGameUri = "ws://51.75.126.59:2999/create_game";
    private final String JoinGameUri = "ws://51.75.126.59:2999/join_game/:";
    private final WebSocketClient client = new WebSocketClient();

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
        Random rngSeed = new Random();
        seed = rngSeed.nextLong();
        Gui.setGraph(graph);
        Gui.setGame(this);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }
    public Game(long id, boolean joiner) throws DeploymentException, URISyntaxException, IOException, InterruptedException {
        client.setGame(this);
        if (joiner) {
            client.connect(JoinGameUri+id);
            while (client.getResponse() == null) {
                Thread.sleep(100);
            }
            JsonElement jsonElement = JsonParser.parseString(client.getResponse());
            seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            turn = Turn.SHORT;
        }
        else {
            client.connect(CreateGameUri);
            while (client.getResponse() == null) {
                Thread.sleep(100);
            }
            JsonElement jsonElement = JsonParser.parseString(client.getResponse());
            seed = jsonElement.getAsJsonObject().get("seed").getAsLong();
            turn = Turn.CUT;
        }
        this.joinerIsHere = joiner;
        client.connect(serverUri);
        this.pvpOnline = true;
        int nbVertices = 10;
        graph = new Graph(nbVertices, seed);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices, seed);
        }
        Gui.setGraph(graph);
        Gui.setGame(this);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
        new Thread(() -> Application.launch(Gui.class)).start();
    }

    public void playFirst() {
        // var v = ia.playCUT();
        var v = graph.getNeighbors().getFirst();
        graph.removeNeighbor(v);
        int i = 0;
        while (cutWon() && i < graph.getNeighbors().size())  {
            graph.addNeighbor(v);
            v = graph.getNeighbors().get(i);
            graph.removeNeighbor(v);
            i++;
        }
        graph.addNeighbor(v);
        v.getKey().cut(v.getValue());
        cutted.add(new Pair<>(v.getKey(), v.getValue()));
        for (var element : Gui.getEdges()) {
            if (element.getKey().equals(v)) {
                element.getValue().getStrokeDashArray().addAll(25D, 10D);
                break;
            }
        }
            cutWon();
    }


    public Pair<Vertex, Vertex> play(Vertex key, Vertex value) {
        if (cutWon || shortWon) return null;
        Pair<Vertex, Vertex> played = null;
        if (againstAI && turn == Turn.CUT) {
            Pair<Vertex, Vertex> v = ia.playCUT();
            v.getKey().cut(v.getValue());
            played = new Pair<Vertex,Vertex>(v.getKey(), v.getValue());
            cutted.add(played);
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(v)) {
                    element.getValue().getStrokeDashArray().addAll(25D, 10D);
                    break;
                }
            }
            if (!pvpOnline)
                turn = turn.flip();
        } else {
            for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                if (Vertex.isSameCouple(new Pair<>(key, value), neighbors.getKey())) {
                    if (key.isCutOrPanted(value)) {
                        return null;
                    }
                    if (turn == Turn.CUT) {
                        key.cut(value);
                        played = new Pair<>(key, value);
                        cutted.add(played);
                        neighbors.getValue().getStrokeDashArray().addAll(25D, 10D);

                    } else {
                        key.paint(value);
                        played = new Pair<>(key, value);
                        secured.add(played);
                        neighbors.getValue().setStroke(Color.RED);
                    }
                    if (!pvpOnline)
                        turn = turn.flip();
                    if (againstAI) {
                        play(key, value);
                    }
                    cutWon(); 
                    shortWon();
                    return played;
                }
            }
        }
        cutWon();
        shortWon();
        return played;
    }

    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon || shortWon)
            return;
        if (mouseEvent.getSource() instanceof Line line &&
                line.getProperties().get("pair") instanceof Pair<?, ?> pair1 &&
                pair1.getKey() instanceof Vertex key && pair1.getValue() instanceof Vertex value) {
            if (pvpOnline) {
                    try {
                        sendMove(new Pair<>(key, value));
                    }
                catch (Exception e) {
                    System.out.println(e);
                }
            }
            else {
                play(key, value);
            }
        }

    }

    public boolean shortWon() {
        if (!cutWon && cutted.size()+secured.size() >= graph.getNeighbors().size()) {
            shortWon = true;
        }
        if (shortWon)
            System.out.println("SHORT a gagné");
        return shortWon;
    }

    public boolean cutWon() {
        List<Vertex> notCuttedVerticices = graph.getVertices();
        notCuttedVerticices.stream().forEach(
            x -> x.setListNeighbors(x.getListNeighbors().stream().filter(y -> !x.isCut(y) && !y.isCut(x)).collect(Collectors.toCollection(ArrayList::new)))
        );
        Graph notCuttedGraph = new Graph(notCuttedVerticices);
        cutWon = !notCuttedGraph.estConnexe();
        if (cutWon)
            System.out.println("CUT a gagné");
        return cutWon;
    }

    public void sendMove(Pair<Vertex, Vertex> move) {
        int turnValue = 0;
        if (joinerIsHere)
            turnValue = 1;
        String data =
        move.getKey().getCoords().getKey().toString() + " "+ move.getKey().getCoords().getValue().toString() + " "+
        move.getValue().getCoords().getKey().toString()+ " "+ move.getValue().getCoords().getValue().toString() + " " +
                turnValue;
        try {
            if (client.isClosed()) {
                client.connect(serverUri);
            }
            client.sendMessage(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getJoinerIsHere() {
        return joinerIsHere;
    }

    public void play1vs1(String message) {
        if (message.isEmpty()) return;
        if (message.chars().toArray()[0] == '[') {
            String[] items = message.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");

            int[] results = new int[items.length];

            for (int i = 0; i < items.length; i++) {
                try {
                    results[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException nfe) {
                    System.out.println("Erreur de parsing dans la réponse");
                }
            }
            Vertex key = null;
            Vertex val = null;
            for (Pair<Vertex, Vertex> element : getGraph().getNeighbors()) {
                if (element.getKey().getCoords().getKey() == results[0] && element.getKey().getCoords().getValue() == results[1]) {
                    key = element.getKey();
                }
                if (element.getValue().getCoords().getKey() == results[2] && element.getValue().getCoords().getValue() == results[3]) {
                    val = element.getValue();
                }
            }
            if (key != null && val != null) {
                Turn t = getTurn();
                if (results[4] == 0)
                    setTurn(Turn.CUT);
                else
                    setTurn(Turn.SHORT);
                play(key, val);
                setTurn(t);
            } else {
                System.out.println(key + " " + val);
            }
        }
    }
}

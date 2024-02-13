package fr.projet.game;
import java.io.IOException;
import fr.projet.WebSocket.WebSocketClient;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import fr.projet.IA.BasicAI;
import fr.projet.IA.InterfaceIA;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.DeploymentException;

@Getter
@Slf4j
public class Game {
    private Graph graph;
    @Setter
    private Turn turn = Turn.CUT;
    @Setter
    private boolean cutWon = false;
    private boolean shortWon = false;
    private boolean againstAI = false;
    private Turn typeIA;
    private InterfaceIA ia;
    private ArrayList<Pair<Vertex, Vertex>> secured = new ArrayList<>();
    public ArrayList<Pair<Vertex, Vertex>> cutted = new ArrayList<>();
    private boolean pvpOnline = false;
    private long seed;
    @Getter
    private boolean joiner;
    private WebSocketClient client;
    private String serverUri;
    private long id;
    private ArrayList<Pair<Line, Turn>> lastsLines = new ArrayList<>();

    public Game() { this(false, Turn.CUT); }
    public Game(boolean withIA, Turn typeIA) {
        int nbVertices = 10;
        graph = new Graph(nbVertices);
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices);
        }
        if (withIA) {
            ia = new BasicAI(this, turn);
            this.againstAI = true;
            this.typeIA = typeIA;
        }
        Random rngSeed = new Random();
        seed = rngSeed.nextLong();
        Gui.setGraph(graph);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
    }
    public Game(long id, boolean joiner, WebSocketClient client, String serverUri, Long seed) {
        if (joiner) turn = Turn.SHORT;
        this.id = id;
        this.joiner = joiner;
        this.client = client;
        this.serverUri = serverUri;
        this.pvpOnline = true;
        int nbVertices = 10;
        graph = new Graph(nbVertices, seed);
        int c = 1;
        while (!graph.estConnexe()) {
            graph = new Graph(nbVertices, seed+c); // On ne génère pas deux fois le même graphe, ce qui faisait crash le client
            c++;
        }
        Gui.setGraph(graph);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
    }

    public Pair<Vertex, Vertex> play(Vertex key, Vertex value) {
        if (cutWon || shortWon) return null;
        Pair<Vertex, Vertex> played = null;
        if (againstAI && turn == typeIA) {
            if (typeIA == Turn.CUT) {
                Pair<Vertex, Vertex> v = ia.playCUT();
                v.getKey().cut(v.getValue());
                played = new Pair<>(v.getKey(), v.getValue());
                cutted.add(played);
                for (var element : Gui.getEdges()) {
                    if (element.getKey().equals(v)) {
                        cutEdge(element.getValue());
                        break;
                    }
                }
            } else {
                // TODO : Implementer IA PlayShort
            }
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
                        cutEdge(neighbors.getValue());

                    } else {
                        key.paint(value);
                        played = new Pair<>(key, value);
                        secured.add(played);
                        paintEdge(neighbors.getValue());
                    }
                    if (!pvpOnline)
                        turn = turn.flip();
                    if (againstAI) {
                        play(key, value);
                        return null;
                    }
                    detectWinner();
                    return played;
                }
            }
        }
        detectWinner();
        return played;
    }

    public void showWinner() {
        if (cutWon()) {
            Gui.PopupMessage(Turn.CUT);
        }
        else if (shortWon()) {
            Gui.PopupMessage(Turn.SHORT);
        }
    }

    private void detectWinner() {
        if (client == null) {
            showWinner();
        }
        else {
            Platform.runLater(this::showWinner); // Cas d'une game online
        }
    }
    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon || shortWon)
            return;
        if (mouseEvent.getSource() instanceof Line line &&
                line.getProperties().get("pair") instanceof Pair<?, ?> pair1 &&
                pair1.getKey() instanceof Vertex key && pair1.getValue() instanceof Vertex value) {
            Pair<Vertex, Vertex> move = new Pair<>(key, value);
            if (cutted.contains(move) || secured.contains(move)) return;
            if (pvpOnline) {
                try {
                    sendMove(new Pair<>(key, value));
                }
                catch (Exception e) {
                    log.error(e.getMessage());
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
        return shortWon;
    }

    public boolean cutWon() {
        List<Vertex> notCuttedVerticices = graph.getVertices();
        notCuttedVerticices.forEach(
            x -> x.setListNeighbors(x.getListNeighbors().stream().filter(y -> !x.isCut(y) && !y.isCut(x)).collect(Collectors.toCollection(ArrayList::new)))
        );
        Graph notCuttedGraph = new Graph(notCuttedVerticices);
        cutWon = !notCuttedGraph.estConnexe();
        return cutWon;
    }

    public void sendMove(Pair<Vertex, Vertex> move) {
        int turnValue = joiner ? 1 : 0;
        String data = graph.getVertices().indexOf(move.getKey()) + " " + graph.getVertices().indexOf(move.getValue()) + " " + turnValue;
        try {
            client.reConnect(serverUri);
            if (!shortWon && !cutWon)
                client.sendMessage(data);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void play1vs1(String message) throws IOException, DeploymentException, URISyntaxException, InterruptedException {
        if (message.isEmpty()) return;
        if (message.equals("L'adversaire a quitté la partie")) {
            if (cutWon || shortWon) return;
            client.sendMessage(turn.toString());
            if (turn == Turn.CUT) {
                cutWon = true;
            }
            else {
                shortWon = true;
            }
            return;
        }
        if (message.chars().toArray()[0] == '[') {
            String[] items = message.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\s", "").split(",");

            int[] results = new int[items.length];

            for (int i = 0; i < items.length; i++) {
                try {
                    results[i] = Integer.parseInt(items[i]);
                } catch (NumberFormatException nfe) {
                    log.error("Erreur de parsing dans la réponse");
                    return;
                }
            }
            Vertex val;
            Vertex key;
            try {
                key = graph.getVertices().get(results[0]);
                val = graph.getVertices().get(results[1]);
            } catch (IndexOutOfBoundsException e) {
                key = null;
                val = null;
            }
            if (key != null && val != null) {
                Turn t = getTurn();
                if (results[2] == 0)
                    setTurn(Turn.CUT);
                else
                    setTurn(Turn.SHORT);
                play(key, val);
                if (cutWon) {
                    client.sendMessage("CUT!");
                    client.close();
                }
                else if (shortWon) {
                    client.sendMessage("SHORT!");
                    client.close();
                }
                setTurn(t);
            } else {
                log.info(key + " " + val);
            }
        }
    }

    public void cutEdge(Line line) {
        lastsLines.add(new Pair<>(line, turn));
        line.setStroke(Color.BLUE);
        line.getStrokeDashArray().addAll(25D, 10D);
        setColor();
    }

    public void paintEdge(Line line) {
        lastsLines.add(new Pair<>(line, turn));
        line.setStroke(Color.BLUE);
        setColor();
    }

    public void setColor() {
        if (lastsLines.size() < 2) return;
        if (lastsLines.get(lastsLines.size()-2).getValue() == Turn.CUT) {
            lastsLines.get(lastsLines.size()-2).getKey().setStroke(Color.BLACK);
            lastsLines.get(lastsLines.size()-2).getKey().getStrokeDashArray().addAll(25D, 10D);
        }
        else {
            lastsLines.get(lastsLines.size()-2).getKey().setStroke(Color.RED);
        }
    }

    public boolean getCutWon() {
        return cutWon;
    }
    public boolean getShortWon() { return shortWon; }
}

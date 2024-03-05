package fr.projet.game;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import fr.projet.ia.BasicAI;
import fr.projet.ia.InterfaceIA;
import fr.projet.ia.Minimax;
import fr.projet.server.WebSocketClient;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

@Getter
@Slf4j
public class Game {
    private Graph graph;
    @Setter
    private Turn turn = Turn.CUT;
    @Setter
    private boolean cutWon = false;
    @Setter
    private boolean shortWon = false;
    private boolean againstAI = false;
    private Turn typeIA;
    private InterfaceIA ia;
    private final List<Pair<Vertex, Vertex>> secured = new ArrayList<>();
    private final List<Pair<Vertex, Vertex>> cutted = new ArrayList<>();
    private boolean pvpOnline = false;
    private long seed;
    @Getter
    private boolean joiner;
    private WebSocketClient client;
    private String serverUri;
    private long id;
    private final ArrayList<Pair<Line, Turn>> lastsLines = new ArrayList<>();
    private Turn creatorTurn;
    private final int nbVertices;
    public Game() { this(false, Turn.CUT, Level.EASY); }
    public Game(boolean withIA, Turn typeIA, Level level) {
        nbVertices = 20;
        do {
            graph = new Graph(nbVertices);
        } while (graphIsNotOkay());
        if (withIA) {
            switch (level) {
                case EASY -> ia = new BasicAI(this, turn);
                case MEDIUM -> ia = new Minimax(this, turn, 1);
                case HARD -> ia = new Minimax(this, turn, 2);
            }
            this.againstAI = true;
            this.typeIA = typeIA;
        }
        Random rngSeed = new Random();
        seed = rngSeed.nextLong();
        Gui.setGraph(graph);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
    }
    public Game(long id, boolean joiner, WebSocketClient client, String serverUri, Long seed, Turn creatorTurn) {
        this.creatorTurn = creatorTurn;
        if (joiner) turn = creatorTurn.flip();
        else turn = creatorTurn;
        this.id = id;
        this.joiner = joiner;
        this.client = client;
        this.serverUri = serverUri;
        this.pvpOnline = true;
        nbVertices = 20;
        int c = 0;
        do {
            graph = new Graph(nbVertices, seed+c); // On ne génère pas deux fois le même graphe, ce qui faisait crash le client
            c++;
        } while (graphIsNotOkay());
        Gui.setGraph(graph);
        Gui.setSeed(seed);
        Gui.setHandler(this::handleEvent);
    }

    public void play(Vertex key, Vertex value) {
        if (cutWon || shortWon) return;
        Pair<Vertex, Vertex> played;
        if (againstAI && turn == typeIA) {
            if (typeIA == Turn.CUT) {
                Pair<Vertex, Vertex> v = ia.playCUT();
                played = new Pair<>(v.getKey(), v.getValue());
                cutEdge(played);
                for (var element : Gui.getEdges()) {
                    if (element.getKey().equals(v)) {
                        cutLine(element.getValue());
                        break;
                    }
                }
            } else {
                Pair<Vertex, Vertex> v = ia.playSHORT();
                played = new Pair<>(v.getKey(), v.getValue());
                secureEdge(played);
                for (var element : Gui.getEdges()) {
                    if (element.getKey().equals(v)) {
                        paintLine(element.getValue());
                        break;
                    }
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
                        played = new Pair<>(key, value);
                        cutEdge(played);
                        cutLine(neighbors.getValue());

                    } else {
                        played = new Pair<>(key, value);
                        secureEdge(played);
                        paintLine(neighbors.getValue());
                    }
                    if (!pvpOnline)
                        turn = turn.flip();
                    if (againstAI) {
                        detectWinner();
                        play(key, value);
                        return;
                    }
                    detectWinner();
                    return;
                }
            }
        }
        detectWinner();
    }

    public void showWinner() {
        if (cutWon()) {
            Platform.runLater(() -> Gui.popupMessage(Turn.CUT));
        }
        else if (shortWon()) {
            Platform.runLater(() -> Gui.popupMessage(Turn.SHORT));
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

    public void cutEdge(Pair<Vertex, Vertex> edge) {
        edge.getKey().cut(edge.getValue());
        getCutted().add(edge);
    }

    public void secureEdge(Pair<Vertex, Vertex> edge) {
        edge.getKey().paint(edge.getValue());
        getSecured().add(edge);
    }
    public boolean shortWon() {
        if (shortWon) return true;
        Graph redGraph = new Graph(getSecured());
        shortWon = redGraph.getNbVertices() == graph.getNbVertices() && redGraphIsConnexe(redGraph);
        return shortWon;
    }

    private boolean redGraphIsConnexe(Graph redGraph) {
        if (redGraph.getVertices().isEmpty()) {
            return true;
        }
        HashSet<Vertex> marked = new HashSet<>();
        Stack<Vertex> pile = new Stack<>();
        pile.push(redGraph.getVertices().getFirst());
        while (!pile.empty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                for (Vertex t: v.getListNeighbors().stream().filter(x -> x.isPainted(v) || v.isPainted(x)).toList()) {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                }
            }
        }
        return marked.size() == redGraph.getVertices().size();
    }

    public boolean cutWon() {
        if (cutWon) return true;
        cutWon = !graphWithoutCuttedIsConnexe();
        return cutWon;
    }

    private boolean graphWithoutCuttedIsConnexe() {
        if (graph.getVertices().isEmpty()) {
            return true;
        }
        HashSet<Vertex> marked = new HashSet<>();
        Stack<Vertex> pile = new Stack<>();
        pile.push(graph.getVertices().getFirst());
        while (!pile.empty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                for (Vertex t: v.getListNeighbors().stream().filter(x -> !x.isCut(v) && !v.isCut(x)).toList()) {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                }
            }
        }
        return marked.size() == graph.getVertices().size();
    }

    public void sendMove(Pair<Vertex, Vertex> move) throws IOException {
        int turnValue;
        if (joiner) turnValue = creatorTurn == Turn.CUT ? 1: 0;
        else turnValue = creatorTurn == Turn.CUT ? 0: 1;
        String data = graph.getVertices().indexOf(move.getKey()) + " " + graph.getVertices().indexOf(move.getValue()) + " " + turnValue;
        try {
            client.reConnect(serverUri);
            if (!shortWon && !cutWon)
                client.sendMessage(data);
        } catch (URISyntaxException | IOException e) {
            log.error("Can't reconnect to serveur : ", e);
            throw new IOException("Can't reconnect");
        }
    }

    public void play1vs1(String message) throws IOException {
        if (message.isEmpty()) return;
        if (message.equals("L'adversaire a quitté la partie")) {
            if (cutWon || shortWon) return;
            if (!getClient().isClosed())
                client.sendMessage(turn.toString());
            if (turn == Turn.CUT) {
                cutWon = true;
            }
            else {
                shortWon = true;
            }
            if (!getClient().isClosed())
                showWinner();
            return;
        }
        if (message.chars().toArray()[0] == '[') {
            String[] items = message.replace("[", "").replace("]", "").replaceAll("\\s", "").split(",");

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

    public void cutLine(Line line) {
        lastsLines.add(new Pair<>(line, turn));
        line.setStroke(Color.BLUE);
        line.getStrokeDashArray().addAll(25D, 15D);
        setColor();
    }

    public void paintLine(Line line) {
        lastsLines.add(new Pair<>(line, turn));
        line.setStroke(Color.BLUE);
        setColor();
    }

    public void setColor() {
        if (lastsLines.size() < 2) return;
        if (lastsLines.get(lastsLines.size()-2).getValue() == Turn.CUT) {
            lastsLines.get(lastsLines.size()-2).getKey().setStroke(Color.BLACK);
            lastsLines.get(lastsLines.size()-2).getKey().getStrokeDashArray().addAll(25D, 15D);
        }
        else {
            lastsLines.get(lastsLines.size()-2).getKey().setStroke(Color.RED);
        }
    }

    public boolean graphIsNotOkay() {
        return graph.getVertices().size() != nbVertices || graph.minDeg() < 3 || graph.maxDeg() >= 8 || !graph.estConnexe();
    }
    public boolean getCutWon() {
        return cutWon;
    }
    public boolean getShortWon() { return shortWon; }
}

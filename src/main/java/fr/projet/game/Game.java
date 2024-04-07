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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;

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
    private InterfaceIA ia2;
    private final HashSet<Pair<Vertex, Vertex>> secured = new HashSet<>();
    private final HashSet<Pair<Vertex, Vertex>> cutted = new HashSet<>();
    @Getter
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
    private final int minDeg = 3;
    private final int maxDeg = 8;
    private final int AIDelay = 100;
    public Game(int nbv) throws TimeoutException { this(nbv,false, Turn.CUT, Level.EASY); }
    public Game() throws TimeoutException { this(20,false, Turn.CUT, Level.EASY); }

    public Game(int nbv, boolean withIA, Turn typeIA, Level level) throws TimeoutException {
        nbVertices = nbv;
        seed = new Random().nextLong();
        LocalTime duration = LocalTime.now();
        int c = 0;
        do {
            graph = new Graph(nbVertices, maxDeg, minDeg, seed+c);
            c++;
            if (duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                throw new TimeoutException();
            }
        } while (graphIsNotOkay());
        if (withIA) {
            ia = getIAwithDifficulty(level);
            this.againstAI = true;
            this.typeIA = typeIA;
        }
        Gui.setGraph(graph);
        Gui.setHandler(this::handleEvent);
    }
    public Game(int nbVertices, long id, boolean joiner, WebSocketClient client, String serverUri, Long seed, Turn creatorTurn) throws TimeoutException {
        this.creatorTurn = creatorTurn;
        if (joiner) turn = creatorTurn.flip();
        else turn = creatorTurn;
        this.id = id;
        this.joiner = joiner;
        this.client = client;
        this.serverUri = serverUri;
        this.pvpOnline = true;
        this.nbVertices = nbVertices;
        this.seed = seed;
        LocalTime duration = LocalTime.now();
        int c = 0;
        do {
            graph = new Graph(nbVertices, maxDeg, minDeg, seed+c); // On ne génère pas deux fois le même graphe, ce qui faisait crash le client
            c++;
            if (!joiner && duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                throw new TimeoutException();
            }
        } while (graphIsNotOkay());
        Gui.setGraph(graph);
        Gui.setHandler(this::handleEvent);
    }

    public Game(int nbVertices, Level levelIACut, Level levelIAShort) throws TimeoutException {
        this.nbVertices = nbVertices;
        seed = new Random().nextLong();
        LocalTime duration = LocalTime.now();
        int c = 0;
        do {
            graph = new Graph(nbVertices, maxDeg, minDeg, seed+c); // On ne génère pas deux fois le même graphe, ce qui faisait crash le client
            c++;
            if (duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                throw new TimeoutException();
            }
        } while (graphIsNotOkay());
        ia = getIAwithDifficulty(levelIACut);
        ia2 = getIAwithDifficulty(levelIAShort);
        Gui.setGraph(graph);
        Gui.setHandler(this::handleEvent);
    }

    public void aiVsAi() {
        LocalTime time = LocalTime.now();
        while (!cutWon && !shortWon) {
            AIPlay(ia, ia2, turn);
            long delay = Math.toIntExact(time.until(LocalTime.now(), ChronoUnit.MILLIS));
            if (delay < AIDelay) {
                try {
                    Thread.sleep(AIDelay-delay);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            time = LocalTime.now();
        }
    }
    public void play(Vertex key, Vertex value) {
        if (againstAI && turn == typeIA) {
            AIPlay(ia, ia, typeIA);
        } else {
            Pair<Vertex, Vertex> played;
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
                    if (!pvpOnline) {
                        turn = turn.flip();
                        detectWinner();
                        if (cutWon || shortWon) return;
                    }
                    if (againstAI) new Thread(() -> AIPlay(ia, ia, typeIA)).start();
                }
            }
        }
        detectWinner();
    }

    public void AIPlay(InterfaceIA ia1, InterfaceIA ia2, Turn turn) {
        if (ia2.getDepth() == 5 && graph.getNeighbors().size() - (cutted.size() + secured.size()) <= 20)
            ia2.setDepth(ia2.getDepth()+1);
        if (ia1.getDepth() == 5 && graph.getNeighbors().size() - (cutted.size() + secured.size()) <= 20)
            ia1.setDepth(ia1.getDepth()+1);
        Pair<Vertex, Vertex> played;
        if (turn == Turn.CUT) {
            Pair<Vertex, Vertex> v = ia1.playCUT();
            played = new Pair<>(v.getKey(), v.getValue());
            cutEdge(played);
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(v)) {
                    cutLine(element.getValue());
                    break;
                }
            }
        } else {
            Pair<Vertex, Vertex> v = ia2.playSHORT();
            played = new Pair<>(v.getKey(), v.getValue());
            secureEdge(played);
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(v)) {
                    paintLine(element.getValue());
                    break;
                }
            }
        }
        this.turn = this.turn.flip();
        detectWinner();
    }
    public void showWinner() {
        int typeGame;
        boolean thereAreAWinner = false;
        if (pvpOnline)
            typeGame = 2; // Deux joueurs en ligne
        else if (againstAI)
            typeGame = 1; // Contre l'IA
        else if (ia2 == null)
            typeGame = 0; // Deux joueurs local
        else
            typeGame = 3; // IA vs IA
        if (cutWon()) {
            if (ia2 == null)
                Platform.runLater(() -> Gui.popupMessage(Turn.CUT));
            if (!pvpOnline || !client.isClosed())
                isolateComponent();
            thereAreAWinner = true;
        }
        else if (shortWon()) {
            if (ia2 == null)
                Platform.runLater(() -> Gui.popupMessage(Turn.SHORT));
            if (!pvpOnline || !client.isClosed())
                deleteCuttedEdge();
            thereAreAWinner = true;
        }
        if (thereAreAWinner) {
            if (pvpOnline) {
                if (!joiner)
                    WebSocketClient.sendStatistics(typeGame, cutWon ? 0 : 1, seed); // Permet d'envoyer qu'une fois la game
            }
            else
                WebSocketClient.sendStatistics(typeGame, cutWon ? 0 : 1, seed);
        }
    }

    public void isolateComponent() {
        Set<Vertex> component = graph.getComponent(graph.getVertices().getFirst(), (x,v) -> !x.isCut(v) && !v.isCut(x));
        Optional<Vertex> u = Optional.empty();
        for (Vertex x : getGraph().getVertices()) {
            if (!component.contains(x)) {
                u = Optional.of(x);
                break;
            }
        }
        if (u.isEmpty()) return;
        Set<Vertex> secondComponent = graph.getComponent(u.get(), (x,v) -> !x.isCut(v) && !v.isCut(x));
        Set<Vertex> smallestComponent;
        if (component.size() > secondComponent.size()) {
            smallestComponent = secondComponent;
        }
        else {
            smallestComponent = component;
        }
        Set<Vertex> finalSmallestComponent = smallestComponent;
        List<Pair<Pair<Vertex, Vertex>, Line>> edgesGreen = Gui.getEdges().stream().filter(pair -> finalSmallestComponent.contains(pair.getKey().getKey())
                && finalSmallestComponent.contains(pair.getKey().getValue())
                && !pair.getKey().getKey().isCut(pair.getKey().getValue())).toList();
        createTimer(edgesGreen, false, 250);
        List<Pair<Pair<Vertex, Vertex>, Line>> cuttedLines = Gui.getEdges().stream().filter(x ->
                cutted.contains(x.getKey())).toList();
        createTimer(cuttedLines, true, 100);
    }

    public void deleteCuttedEdge() {
        List<Pair<Pair<Vertex, Vertex>, Line>> securedEdges = Gui.getEdges().stream().filter(x ->
                cutted.contains(x.getKey()) || !secured.contains(x.getKey())).toList();
        createTimer(securedEdges, true, 100);
    }

    public void createTimer(List<Pair<Pair<Vertex, Vertex>, Line>> edges, boolean opacity, int period) {
        Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            private int i = 0;
            @Override
            public void run() {
                if (i < edges.size())
                {
                    Pair<Pair<Vertex, Vertex>, Line> pair = edges.get(i);
                    if (opacity) {
                        pair.getValue().setVisible(false);
                    }
                    else {
                        pair.getValue().setStroke(Color.LIGHTGREEN);
                    }
                    i++;
                }
                else {
                    t.cancel();
                    t.purge();
                }
            }
        };
        t.scheduleAtFixedRate(tt, 100, period);
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
        if (cutWon || shortWon || ia2 != null)
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
        shortWon = redGraph.getNbVertices() == graph.getNbVertices() &&
                graphWithoutSomeNeighborsIsConnected(redGraph, (x,v) -> x.isPainted(v) || v.isPainted(x));
        return shortWon;
    }

    public boolean cutWon() {
        if (cutWon) return true;
        cutWon = !graphWithoutSomeNeighborsIsConnected(graph, (x,v) -> !x.isCut(v));
        return cutWon;
    }

    private boolean graphWithoutSomeNeighborsIsConnected(Graph graph, BiPredicate<Vertex, Vertex> predicate) {
        if (graph.getVertices().isEmpty()) {
            return true;
        }
        HashSet<Vertex> marked = new HashSet<>();
        Deque<Vertex> pile = new ArrayDeque<>();
        pile.push(graph.getVertices().getFirst());
        while (!pile.isEmpty()) {
            Vertex v = pile.pop();
            if (!marked.contains(v)) {
                marked.add(v);
                graph.getAdjVertices().get(v).stream().filter(x -> predicate.test(x,v)).forEach(t -> {
                    if (!marked.contains(t)) {
                        pile.push(t);
                    }
                });
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
            Turn turnValue;
            if (joiner) turnValue = creatorTurn == Turn.CUT ? Turn.SHORT: Turn.CUT;
            else turnValue = creatorTurn == Turn.CUT ? Turn.CUT: Turn.SHORT;
            if (cutWon || shortWon) return;
            if (!getClient().isClosed())
                client.sendMessage(turn.toString());
            if (turnValue == Turn.CUT) {
                cutWon = true;
            }
            else {
                shortWon = true;
            }
            if (!getClient().isClosed())
            {
                getClient().close();
                showWinner();
            }
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
        if (!lastsLines.isEmpty())
            lastsLines.getLast().getKey().setVisible(false);
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
        return graph.getVertices().size() != nbVertices || graph.minDeg() < minDeg || graph.maxDeg() >= maxDeg || !graph.estConnexe();
    }
    public boolean getCutWon() {
        return cutWon;
    }
    public boolean getShortWon() { return shortWon; }

    private InterfaceIA getIAwithDifficulty(Level level) {
        return switch (level) {
            case EASY -> new BasicAI(this, turn);
            case MEDIUM -> new Minimax(this, turn, 3);
            case HARD -> new Minimax(this, turn, 5);
        };
    }

    public boolean getJoiner() {
        return joiner;
    }
}

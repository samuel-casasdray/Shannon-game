package fr.projet.game;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import fr.projet.gui.UtilsGui;
import fr.projet.ia.BasicAI;
import fr.projet.ia.InterfaceIA;
import fr.projet.ia.Minimax;
import fr.projet.server.HttpsClient;
import fr.projet.server.WebSocketClient;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeoutException;
import javafx.scene.media.*;

@Getter
@Slf4j
public class Game {
    private final HashSet<Pair<Vertex, Vertex>> secured = new HashSet<>();
    private final HashSet<Pair<Vertex, Vertex>> cutted = new HashSet<>();
    private final ArrayList<Pair<Line, Turn>> lastsLinesCut = new ArrayList<>();
    private final ArrayList<Pair<Line, Turn>> lastsLinesPaint = new ArrayList<>();
    private final int nbVertices;
    private final int minDeg = 3;
    private final int maxDeg = 7;
    private final int AIDelay = 700;
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
    private boolean pvpOnline = false;
    private long seed;
    private boolean joiner;
    private WebSocketClient client;
    private String serverUri;
    private long id;
    private Turn creatorTurn;
    private boolean pending = false;
    @Setter
    @Getter
    private boolean interrupted = false;

    public Game(int nbv) throws TimeoutException { 
      this(nbv,false, Turn.CUT, Level.EASY); 
    }
  
    public Game() throws TimeoutException { 
      this(20,false, Turn.CUT, Level.EASY); 
    }

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
            graph = new Graph(nbVertices, maxDeg, minDeg, seed + c); // On ne génère pas deux fois le même graphe, ce qui faisait crash le client
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
            if (interrupted) return;
            AIPlay(ia, ia2, turn);
            long delay = Math.toIntExact(time.until(LocalTime.now(), ChronoUnit.MILLIS));
            if (delay < AIDelay) {
                try {
                    Thread.sleep(AIDelay - delay);
                } catch (InterruptedException e) {
                    log.error(e.getMessage() + "Interrupted");
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
                    if (key.isCutOrPanted(value) || isInterrupted()) {
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
                        showWinner();
                        if (cutWon || shortWon) return;
                    }
                    Platform.runLater(() -> {
                        if (againstAI) new Thread(() -> AIPlay(ia, ia, typeIA)).start();
                    });
                }
            }
        }
        showWinner();
    }

    public void AIPlay(InterfaceIA ia1, InterfaceIA ia2, Turn turn) {
        pending = true;
        //if (ia2.getDepth() == 5 && graph.getNeighbors().size() - (cutted.size() + secured.size()) <= 20)
            //ia2.setDepth(ia2.getDepth()+1);
        //if (ia1.getDepth() == 5 && graph.getNeighbors().size() - (cutted.size() + secured.size()) <= 20)
          //  ia1.setDepth(ia1.getDepth()+1);
        Pair<Vertex, Vertex> played;
        if (interrupted) return;
        if (turn == Turn.CUT) {
            played = ia1.playCUT();
            cutEdge(played);
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(played)) {
                    cutLine(element.getValue());
                    break;
                }
            }
        } else {
            played = ia2.playSHORT();
            secureEdge(played);
            for (var element : Gui.getEdges()) {
                if (element.getKey().equals(played)) {
                    paintLine(element.getValue());
                    break;
                }
            }
        }
        this.turn = this.turn.flip();
        showWinner();
        pending = false;
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
            Platform.runLater(() -> Gui.popupMessage(Turn.CUT));
            if (!pvpOnline || !client.isClosed())
                isolateComponent();
            thereAreAWinner = true;
        }
        else if (shortWon()) {
            Platform.runLater(() -> Gui.popupMessage(Turn.SHORT));
            if (!pvpOnline || !client.isClosed())
                deleteCuttedEdge();
            thereAreAWinner = true;
        }
        if (thereAreAWinner) {
            if (pvpOnline) {
                if (!joiner)
                    HttpsClient.sendStatistics(typeGame, cutWon ? 0 : 1, seed); // Permet d'envoyer qu'une fois la game
            }
            else
                HttpsClient.sendStatistics(typeGame, cutWon ? 0 : 1, seed);
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
        } else {
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
                if (i < edges.size()) {
                    if (interrupted) {
                        t.cancel();
                        t.purge();
                        return;
                    }
                    Pair<Pair<Vertex, Vertex>, Line> pair = edges.get(i);
                    if (opacity) {
                        Platform.runLater(() -> pair.getValue().setVisible(false));
                    } else {
                        Platform.runLater(() -> pair.getValue().setStroke(Color.LIGHTGREEN));
                    }
                    i++;
                } else {
                    t.cancel();
                    t.purge();
                }
            }
        };
        t.scheduleAtFixedRate(tt, 100, period);
    }

    private void handleEvent(MouseEvent mouseEvent) {
        if (cutWon || shortWon || ia2 != null || pending)
            return;
        Line line = null;
        if (mouseEvent.getTarget() instanceof Line line2) {
            line = line2;
        } else if (mouseEvent.getTarget() instanceof Pane) {
            List<Pair<Double, Line>> good = new ArrayList<>();
            for (Pair<Pair<Vertex, Vertex>, Line> pair : Gui.getEdges()) {
                double d;
                if (
                    (d = Graph.distancePointSegment(
                        mouseEvent.getX(),
                        mouseEvent.getY(),
                        pair.getKey().getKey().getX() + UtilsGui.CIRCLE_SIZE,
                        pair.getKey().getKey().getY() + UtilsGui.CIRCLE_SIZE,
                        pair.getKey().getValue().getX() + UtilsGui.CIRCLE_SIZE,
                        pair.getKey().getValue().getY() + UtilsGui.CIRCLE_SIZE)
                    ) < 10
                ) {
                    good.add(new Pair<>(d, pair.getValue()));
                }
            }
            if (!good.isEmpty())
                line = good.stream().min(Comparator.comparing(Pair::getKey)).get().getValue();
        }
        if (line == null) return;
        Pair<Vertex, Vertex> move = (Pair<Vertex, Vertex>) line.getProperties().get("pair");
        if (move == null || move.getKey().isCut(move.getValue()) || move.getKey().isPainted(move.getValue())) return;
        if (pvpOnline) {
            try {
                sendMove(move);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            play(move.getKey(), move.getValue());
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
        shortWon = redGraph.getNbVertices() == graph.getNbVertices() && redGraph.estConnexe();
        return shortWon;
    }

    public boolean cutWon() {
        if (cutWon) return true;
        Graph notCuttedGraph = new Graph(getGraph().getNeighbors());
        for (Pair<Vertex, Vertex> edge : getCutted()) {
            notCuttedGraph.removeNeighbor(edge);
        }
        cutWon = !notCuttedGraph.estConnexe();
        return cutWon;
    }

    public void sendMove(Pair<Vertex, Vertex> move) throws IOException {
        int turnValue;
        if (joiner) turnValue = creatorTurn == Turn.CUT ? 1 : 0;
        else turnValue = creatorTurn == Turn.CUT ? 0 : 1;
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
            if (turnValue == Turn.CUT) {
                cutWon = true;
            } else {
                shortWon = true;
            }
            if (!getClient().isClosed()) {
                if (cutWon) {
                    Platform.runLater(() -> Gui.popupMessage(Turn.CUT));
                    HttpsClient.sendStatistics(2, 0, seed);
                }
                else if (shortWon) {
                    Platform.runLater(() -> Gui.popupMessage(Turn.SHORT));
                    HttpsClient.sendStatistics(2, 1, seed);
                }
                client.sendMessage(turnValue.toString()); // On envoie le gagnant au serveur pour qu'il puisse update les elo
                getClient().close();
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
                } else if (shortWon) {
                    client.sendMessage("SHORT!");
                    client.close();
                }
            } else {
                log.info(key + " " + val);
            }
        }
    }

    public void cutLine(Line line) {
        lastsLinesCut.add(new Pair<>(line, turn));
        Platform.runLater(() -> {
            line.setStroke(Color.BLUE);
            line.getStrokeDashArray().addAll(25D, 15D);
        });
        setColor();
        playSoundCut();
    }

    public void paintLine(Line line) {
        if (!lastsLinesCut.isEmpty())
            Platform.runLater(() -> lastsLinesCut.getLast().getKey().setVisible(false));
        lastsLinesPaint.add(new Pair<>(line, turn));
        Platform.runLater(() -> line.setStroke(Color.RED));
        setColor();
        playSoundShort();
    }

    public void setColor() {
        if (lastsLinesCut.size() < 2) return;
        if (lastsLinesCut.get(lastsLinesCut.size()-2).getValue() == Turn.CUT) {
            Platform.runLater(() -> lastsLinesCut.get(lastsLinesCut.size()-2).getKey().setStroke(Color.BLACK));
            Platform.runLater(() -> lastsLinesCut.get(lastsLinesCut.size()-2).getKey().getStrokeDashArray().addAll(25D, 15D));
        }
        else {
            Platform.runLater(() -> lastsLinesCut.get(lastsLinesCut.size()-2).getKey().setStroke(Color.ORANGERED));
        }
        if (lastsLinesPaint.size() < 2) return;
        if (lastsLinesPaint.get(lastsLinesPaint.size()-2).getValue() == Turn.CUT) {
            Platform.runLater(() -> lastsLinesPaint.get(lastsLinesPaint.size()-2).getKey().setStroke(Color.BLACK));
            Platform.runLater(() -> lastsLinesPaint.get(lastsLinesPaint.size()-2).getKey().getStrokeDashArray().addAll(25D, 15D));
        }
        else {
            Platform.runLater(() -> lastsLinesPaint.get(lastsLinesPaint.size()-2).getKey().setStroke(Color.ORANGERED));
        }
    }

    public boolean graphIsNotOkay() {
        return graph.getVertices().size() != nbVertices || graph.minDeg() < minDeg || graph.maxDeg() > maxDeg || !graph.estConnexe();
    }

    public boolean getCutWon() {
        return cutWon;
    }

    public boolean getShortWon() {
        return shortWon;
    }

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

    public void playSound (String name) {
        String audioS = "Sounds/"+name+".mp3";
        URL audioUrl = this.getClass().getClassLoader().getResource(audioS);
        assert audioUrl != null;
        String audioFile = audioUrl.toExternalForm();
        Media sound = new Media(audioFile);
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(Gui.getVOLUME());
        mediaPlayer.play();
    }

    public void playSoundShort () {
        String audioS = "Sounds/short"+1+".mp3";
        URL audioUrl = this.getClass().getClassLoader().getResource(audioS);
        assert audioUrl != null;
        String audioFile = audioUrl.toExternalForm();
        Media sound = new Media(audioFile);
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(Gui.getVOLUME());
        Gui.getStage().setOnCloseRequest(event -> stopMediaPlayer2(mediaPlayer));
        mediaPlayer.play();
    }


    public void playSoundCut () {
        Random random = new Random();
        int i = random.nextInt(2)+1;
        String audioS = "Sounds/cut"+i+".mp3";
        URL audioUrl = this.getClass().getClassLoader().getResource(audioS);
        assert audioUrl != null;
        String audioFile = audioUrl.toExternalForm();
        Media sound = new Media(audioFile);
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(Gui.getVOLUME());
        Gui.getStage().setOnCloseRequest(event -> stopMediaPlayer2(mediaPlayer));
        mediaPlayer.play();
    }


    private void stopMediaPlayer2(MediaPlayer mp) {
        if (mp != null) {
            mp.stop();
        }
    }
}

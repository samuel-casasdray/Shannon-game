package fr.projet.game;

import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.gui.Gui;
import fr.projet.gui.GuiScene;
import fr.projet.gui.UtilsGui;
import fr.projet.ia.BasicAI;
import fr.projet.ia.InterfaceIA;
import fr.projet.ia.Minimax;
import fr.projet.ia.WinnerStrat;
import fr.projet.server.HttpsClient;
import fr.projet.server.WebSocketClient;
import javafx.animation.PauseTransition;
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
import javafx.scene.media.*;

@Getter
@Slf4j
public class Game {
    private final HashSet<Pair<Vertex, Vertex>> secured = new HashSet<>();
    private final HashSet<Pair<Vertex, Vertex>> cutted = new HashSet<>();
    private final ArrayList<Line> lastsLinesCut = new ArrayList<>();
    private final ArrayList<Line> lastsLinesPaint = new ArrayList<>();
    private final int nbVertices;
    private final int minDeg = 3;
    private final int maxDeg = 7;
    private final int AIDelay = 1000;
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
    Random random = new Random();
    private Media soundShort = new Media(getClass().getClassLoader().getResource("Sounds/short"+1+".mp3").toExternalForm());
    private Media soundCut1 = new Media(getClass().getClassLoader().getResource("Sounds/cut"+1+".mp3").toExternalForm());
    private Media soundCut2 = new Media(getClass().getClassLoader().getResource("Sounds/cut"+2+".mp3").toExternalForm());
    private Media soundFight = new Media(this.getClass().getClassLoader().getResource("Sounds/fight.mp3").toExternalForm());
    private List<Graph> stratGagnante;
    @Getter
    private Level levelIACut;
    @Getter
    private Level levelIAShort;

    public Game(int nbv) throws TimeoutException {
      this(nbv,false, Turn.CUT, Level.EASY); 
    }
  
    public Game() throws TimeoutException { 
      this(20,false, Turn.CUT, Level.EASY); 
    }

    public Game(List<Vertex> vertices, Map<Vertex, HashSet<Vertex>> adjVertices) {
        nbVertices = vertices.size();
        graph = new Graph(vertices, adjVertices);
        Gui.setHandler(this::handleEvent);
    }

    public Game(int nbv, boolean withIA, Turn typeIA, Level level) throws TimeoutException {
        nbVertices = nbv;
        seed = new Random().nextLong();
        LocalTime duration = LocalTime.now();
        int c = 0;
        if (level == Level.STRAT_WIN)
        {
            if (typeIA == Turn.CUT) {
                do {
                    do {
                        graph = new Graph(nbVertices, maxDeg, minDeg, seed+c);
                        c++;
                        if (duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                            throw new TimeoutException();
                        }
                    } while (graphIsNotOkay());
                    stratGagnante = graph.appelStratGagnante();
                } while (stratGagnante.isEmpty() || stratGagnante.get(1).getNbVertices() > 0);
            }
            else {
                do {
                    do {
                        graph = new Graph(nbVertices, maxDeg, minDeg, seed+c);
                        c++;
                        if (duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                            throw new TimeoutException();
                        }
                    } while (graphIsNotOkay());
                    stratGagnante = graph.appelStratGagnante();
                } while (stratGagnante.isEmpty() || stratGagnante.get(1).getNbVertices() == 0);
            }
        }
        else {
            do {
                graph = new Graph(nbVertices, maxDeg, minDeg, seed+c);
                c++;
                if (duration.until(LocalTime.now(), ChronoUnit.MILLIS) >= 2000) {
                    throw new TimeoutException();
                }
            } while (graphIsNotOkay());
        }
        if (withIA) {
            ia = getIAwithDifficulty(level);
            this.againstAI = true;
            this.typeIA = typeIA;
        }
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
        Gui.setHandler(this::handleEvent);
    }

    public Game(int nbVertices, Level levelIACut, Level levelIAShort) throws TimeoutException {
        this.nbVertices = nbVertices;
        this.levelIACut = levelIACut;
        this.levelIAShort = levelIAShort;
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
        Gui.setHandler(this::handleEvent);
    }

    public void aiVsAi() {
        LocalTime time = LocalTime.now();
        if (interrupted) return;
        AIPlay(ia, ia2, turn);
        while (!cutWon && !shortWon) {
            long delay = time.until(LocalTime.now(), ChronoUnit.MILLIS);
            if (delay < AIDelay) {
                try {
                    Thread.sleep(AIDelay - delay);
                    if (interrupted) return;
                    AIPlay(ia, ia2, turn);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            time = LocalTime.now();
        }
    }

    public void play(Pair<Vertex, Vertex> move) {
        if (againstAI && turn == typeIA) {
            AIPlay(ia, ia, typeIA);
        } else {
            Pair<Vertex, Vertex> played;
            for (Pair<Pair<Vertex, Vertex>, Line> neighbors : Gui.getEdges()) {
                if (Vertex.isSameCouple(move, neighbors.getKey())) {
                    if (isCutted(move) || isSecured(move) || isInterrupted()) {
                        return;
                    }
                    if (turn == Turn.CUT) {
                        played = move;
                        cutEdge(played);
                        cutLine(neighbors.getValue());
                    } else {
                        played = move;
                        secureEdge(played);
                        paintLine(neighbors.getValue());
                    }
                    if (!pvpOnline) {
                        turn = turn.flip();
                        showWinner();
                        if (cutWon || shortWon) return;
                    }
                    if (againstAI) new Thread(() -> AIPlay(ia, ia, typeIA)).start();
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
            Gui.popupMessage(Turn.CUT);
            if (!pvpOnline || !client.isClosed())
                isolateComponent();
            thereAreAWinner = true;
        }
        else if (shortWon()) {
            Gui.popupMessage(Turn.SHORT);
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
        Set<Vertex> component = graph.getComponent(graph.getVertices().getFirst(), (x,v) -> !isCutted(x, v));
        Optional<Vertex> u = Optional.empty();
        for (Vertex x : getGraph().getVertices()) {
            if (!component.contains(x)) {
                u = Optional.of(x);
                break;
            }
        }
        if (u.isEmpty()) return;
        Set<Vertex> secondComponent = graph.getComponent(u.get(), (x,v) -> !isCutted(x, v));
        Set<Vertex> smallestComponent;
        if (component.size() > secondComponent.size()) {
            smallestComponent = secondComponent;
        } else {
            smallestComponent = component;
        }
        Set<Vertex> finalSmallestComponent = smallestComponent;
        List<Pair<Pair<Vertex, Vertex>, Line>> edgesGreen = Gui.getEdges().stream().filter(pair -> finalSmallestComponent.contains(pair.getKey().getKey())
            && finalSmallestComponent.contains(pair.getKey().getValue())
            && !isCutted(pair.getKey())).toList();
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
        } else {
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
                    ) < 20
                ) {
                    good.add(new Pair<>(d, pair.getValue()));
                }
            }
            if (!good.isEmpty())
                line = good.stream().min(Comparator.comparing(Pair::getKey)).get().getValue();
        }
        if (line == null) return;
        Pair<Vertex, Vertex> move = (Pair<Vertex, Vertex>) line.getProperties().get("pair");
        if (move == null || isCutted(move) || isSecured(move)) return;
        if (pvpOnline) {
            try {
                sendMove(move);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            play(move);
        }
    }

    public void cutEdge(Pair<Vertex, Vertex> edge) {
        getCutted().add(edge);
    }

    public void secureEdge(Pair<Vertex, Vertex> edge) {
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
        Graph notCuttedGraph = new Graph(getGraph().getEdges());
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
                    Gui.popupMessage(Turn.CUT);
                    HttpsClient.sendStatistics(2, 0, seed);
                }
                else if (shortWon) {
                    Gui.popupMessage(Turn.SHORT);
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
                play(new Pair<>(key, val));
                if (cutWon) {
                    client.sendMessage("CUT!");
                    client.close();
                } else if (shortWon) {
                    client.sendMessage("SHORT!");
                    client.close();
                }
            }
        }
    }

    public void cutLine(Line line) {
        Platform.runLater(() -> {
            lastsLinesCut.add(line);
            line.setStroke(Color.BLUE);
            line.getStrokeDashArray().addAll(25D, 15D);
        });
        playSoundCut();
    }

    public void paintLine(Line line) {
        Platform.runLater(() -> {
            if (!lastsLinesCut.isEmpty())
                lastsLinesCut.getLast().setVisible(false);
            lastsLinesPaint.add(line);
            line.setStroke(Color.RED);
            if (lastsLinesPaint.size() < 2) return;
            lastsLinesPaint.get(lastsLinesPaint.size()-2).setStroke(Color.ORANGERED);
        });
        playSoundShort();
    }


    public boolean graphIsNotOkay() {
        return graph.getNbVertices() != nbVertices || graph.minDeg() < minDeg || graph.maxDeg() > maxDeg || !graph.estConnexe();
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
            case STRAT_WIN -> new WinnerStrat(this, turn, stratGagnante);
        };
    }

    public boolean getJoiner() {
        return joiner;
    }

    public void playSound () {
        MediaPlayer mediaPlayer = new MediaPlayer(soundFight);
        mediaPlayer.setVolume(GuiScene.getVOLUME());
        mediaPlayer.play();
    }

    public void playSoundShort () {
        MediaPlayer mediaPlayer = new MediaPlayer(soundShort);
        mediaPlayer.setVolume(GuiScene.getVOLUME()/2);
        mediaPlayer.play();
    }


    public void playSoundCut () {
        int i = random.nextInt(2)+1;
        Media sound;
        if (i == 1) {
            sound = soundCut1;
        } else {
            sound = soundCut2;
        }
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(GuiScene.getVOLUME()/2);
        mediaPlayer.play();
    }

    public boolean isCutted(Vertex key, Vertex value) {
        return cutted.contains(new Pair<>(key, value)) || cutted.contains(new Pair<>(value, key));
    }

    public boolean isCutted(Pair<Vertex, Vertex> edge) {
        return cutted.contains(edge) || cutted.contains(new Pair<>(edge.getValue(), edge.getKey()));
    }

    public boolean isSecured(Pair<Vertex, Vertex> edge) {
        return secured.contains(edge) || secured.contains(new Pair<>(edge.getValue(), edge.getKey()));
    }
}

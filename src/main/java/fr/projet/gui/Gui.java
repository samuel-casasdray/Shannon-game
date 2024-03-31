package fr.projet.gui;

import fr.projet.server.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@Slf4j
public class Gui extends Application {

    @Getter
    @Setter
    private static EventHandler<MouseEvent> handler;
    @Getter
    @Setter
    private static Graph graph;
    @Getter
    @Setter
    private Game game;
    @Getter
    @Setter
    private static List<Pair<Pair<Vertex, Vertex>, Line>> edges = new ArrayList<>();
    private Stage stage;
    private Level level;
    private Turn turn;
    private Boolean withIA;
    @Setter
    private int nbVertices = 20;
    @Getter
    private Optional<Long> gameCode = Optional.empty();
    private Thread gameThread;
    @Getter
    private Pane pane;

    @Override
    public void start(Stage stage) {
        // On initialise un handshake pour éviter de devoir attendre 1 seconde lorsqu'on appuie sur create
        new Thread(WebSocketClient::getHandshake).start();
        this.stage = stage;
        stage.setScene(GuiScene.home(this::handleButtonClick));
        stage.setTitle("Shannon Game");
        URL url = getClass().getResource("/icon-appli.png");
        if(url == null) {
            log.error("No icon");
        } else {
            Image icon = new Image(url.toExternalForm());
            stage.getIcons().add(icon);
        }
        stage.show();
    }
    public void handleButtonClick(ButtonClickType buttonClickType) {
        switch (buttonClickType) {
            case HOME -> stage.setScene(GuiScene.home(this::handleButtonClick));
            case HOME_PVIA-> {
                withIA=true;
                stage.setScene(GuiScene.pvia(this::handleButtonClick));
            }
            case JOUEUR -> stage.setScene(GuiScene.joueur(this::handleButtonClick));
            case HOME_PVPL -> {
                withIA=false;
                stage.setScene(GuiScene.nbVertices(this::handleButtonClick,withIA));
            }
            case HOME_PVPO -> stage.setScene(
                GuiScene.pvp(
                    this::handleButtonClick,
                    (textField, turn, nbVertices) -> join((TextField) textField),
                    (textField, turn, nbVertices) -> create((Text) textField, turn, nbVertices)
                )
            );
            case HOME_IAVIA -> {
                stage.setScene(GuiScene.aivsai(this::handleButtonClick));
            }
            case JOUEUR_SHORT -> {
                turn=Turn.CUT;
                stage.setScene(GuiScene.nbVertices(this::handleButtonClick,withIA));
            }
            case JOUEUR_CUT -> {
                turn=Turn.SHORT;
                stage.setScene(GuiScene.nbVertices(this::handleButtonClick, withIA));
            }
            case PVIA_EASY -> {
                this.level = Level.EASY;
                stage.setScene(GuiScene.joueur(this::handleButtonClick));
            }
            case PVIA_MEDIUM -> {
                this.level = Level.MEDIUM;
                stage.setScene(GuiScene.joueur(this::handleButtonClick));
            }
            case PVIA_HARD -> {
                this.level = Level.HARD;
                stage.setScene(GuiScene.joueur(this::handleButtonClick));
            }
            case JEU -> popupMessage();
            case VERTICES -> {
                this.nbVertices=GuiScene.getNbVertices();
                this.game = new Game(nbVertices, withIA, turn, level);
                stage.setScene(run());
                if (withIA && turn==Turn.CUT) {
                    gameThread = new Thread(() -> game.play(null, null));
                    gameThread.setDaemon(true);
                    gameThread.start();
                }
            }
            case AIvsAI -> {
                this.nbVertices = GuiScene.getNbVertices();
                Level levelAI1 = GuiScene.getLevel1();
                Level levelAI2 = GuiScene.getLevel2();
                this.game = new Game(nbVertices, levelAI1, levelAI2);
                stage.setScene(run());
                gameThread = new Thread(game::aiVsAi);
                gameThread.setDaemon(true);
                gameThread.start();
            }
        }
    }

    private void leaveGame() {
        stage.setScene(GuiScene.home(this::handleButtonClick));
        if (game.isPvpOnline()) {
            try {
                game.getClient().close();
            } catch (Exception ignored) {}
        }
        if (gameThread != null)
            gameThread.interrupt();
    }
    public void popupMessage(){
        if (game.getCutWon() || game.getShortWon()) {
            leaveGame();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Vous allez quitter le jeu !");
        alert.setContentText("Êtes vous sûr de vouloir quitter ?");
        ButtonType buttonAccept = new ButtonType("Accepter");
        ButtonType buttonCancel = new ButtonType("Annuler");
        alert.getButtonTypes().setAll(buttonAccept,buttonCancel);

        alert.showAndWait().ifPresent(response ->{
            if (response==buttonAccept){
                leaveGame();
            }
        });
    }

    public static void popupMessage(Turn turn){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fin de la partie");
        if (turn==Turn.CUT){
            alert.setHeaderText("CUT a gagné !");
        }
        else alert.setHeaderText("SHORT a gagné !");
        alert.show();
    }

    public static void popupMessage(String title, String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.show();
    }

    public Scene run() {
        // Création d'un BorderPane pour centrer le contenu
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);

        // Création du Pane pour afficher le graphique
        pane = new Pane();
        pane.setPrefSize(UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
        //Code pour afficher les deux arbres couvrants disjoints s'ils existent
//        List<Graph> result = graph.getTwoDistinctSpanningTrees();
//        if (!result.isEmpty()) {
//            for (Pair<Vertex, Vertex> pair : result.getFirst().getNeighbors()) {
//                Line line = new Line(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                        pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE,
//                        pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                        pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE);
//                line.setStroke(Color.LIGHTGREEN);
//                line.setStrokeWidth(10);
//                pane.getChildren().add(line);
//            }
//
//            for (Pair<Vertex, Vertex> pair : result.getLast().getNeighbors()) {
//                Line line = new Line(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                        pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE,
//                        pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                        pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE);
//                line.setStroke(Color.RED);
//                line.setStrokeWidth(10);
//                pane.getChildren().add(line);
//            }
//        }
        edges.clear();
        showGraph();
        pane.getChildren().add(UtilsGui.getReturnButton(ButtonClickType.JEU, this::handleButtonClick));
        borderPane.setCenter(pane);
        Scene scene = new Scene(borderPane, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);

        // Ajout d'un écouteur pour détecter les changements de taille de la fenêtre
        scene.widthProperty().addListener((observableValue, oldWidth, newWidth) -> {
            Platform.runLater(() -> {
                pane.setPrefWidth((double) newWidth);
                updateGraphLayout(pane);
            });
        });

        scene.heightProperty().addListener((observableValue, oldHeight, newHeight) -> {
            Platform.runLater(() -> {
                pane.setPrefHeight((double) newHeight);
                updateGraphLayout(pane);
            });
        });

        return scene;
}

//fonction qui recalcule les position des aretes et sommets lors d'un redimensionnement
    private void updateGraphLayout(Pane pane) {
        double xOffset = (pane.getWidth() - UtilsGui.WINDOW_SIZE) / 2;
        double yOffset = (pane.getHeight() - UtilsGui.WINDOW_SIZE) / 2;

        // Mise à jour des positions des arêtes
        for (Pair<Pair<Vertex, Vertex>, Line> edge : edges) {
            //System.out.println("aretes");
            Line line = edge.getValue();
            Pair<Vertex, Vertex> pair = edge.getKey();
            line.setStartX(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE + xOffset);
            line.setStartY(pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE + yOffset);
            line.setEndX(pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE + xOffset);
            line.setEndY(pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE + yOffset);
        }

        int nodeIndex=0;
        // Mise à jour des positions des sommets et des textes
        for (Node node : pane.getChildren()) {
            if (node instanceof Circle vertex) {
                //System.out.println("cercle");
                Pair<Integer, Integer> coord = this.game.getGraph().getVertices().get(nodeIndex).getCoords();
                vertex.relocate(coord.getKey()+ xOffset,coord.getValue() + yOffset);
                nodeIndex++;
            } else if (node instanceof Text text) {
                //System.out.println("texte");
                int i = Integer.parseInt(text.getText());
                Pair<Integer, Integer> coord = this.game.getGraph().getVertices().get(i-1).getCoords();
                // Centrage du texte
                if (i >= 9) {
                    text.relocate(coord.getKey() + 13.50 + xOffset, coord.getValue() + 15.50 + yOffset);
                } else {
                    text.relocate(coord.getKey() + 16.50 + xOffset, coord.getValue() + 15.50 + yOffset);
                }
            }
        }
    }
    private static List<String> colors = List.of("#00ccff", "#ff0000", "#00ff99", "#ffff66", "#9933ff", "#ff6600");
    private void colorPlanarGraph(Graph graph) {
        if (graph.getNbVertices() <= 6) {
            for (int i = 0; i < graph.getVertices().size(); i++) {
                graph.getVertices().get(i).setColor(i);
            }
        }
        Vertex v = new Vertex(0, 0);
        for (Vertex vertex : graph.getVertices()) {
            if (graph.getAdjVertices().get(vertex) != null)
                if (graph.getAdjVertices().get(vertex).size() <= 5) {
                    v = vertex;
                    break;
                }
        }
        if (!graph.getAdjVertices().containsKey(v)) return;

        Graph g2 = new Graph(graph.getVertices(), graph.getAdjVertices());
        g2.removeVertex(v);
        if (g2.getNbVertices() >= 1)
            colorPlanarGraph(g2);
        List<Boolean> boolColors = new ArrayList<>(List.of(true, true, true, true, true, true));
        for (Vertex u : graph.getAdjVertices().get(v)) {
            boolColors.set(u.getColor(), false);
        }
        for (int i = 0; i < 5; i++) {
            if (boolColors.get(i)) {
                v.setColor(i);
                break;
            }
        }
    }

    public void showGraph() {
        // Ajout des aretes sur l'affichage
        if (game == null) return; // Cas qui peut survenir si le serveur est off
        for (Pair<Vertex, Vertex> pair : this.game.getGraph().getNeighbors()) {
            Line line = new Line(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
                    pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE,
                    pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
                    pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE);
            line.setStrokeWidth(5);
            line.setOnMouseClicked(handler);
            line.getProperties().put("pair", pair);
            // Ajout de la ligne sur sur l'affichage
            pane.getChildren().add(line);
            edges.add(new Pair<>(pair, line));
        }
        // Ajout des sommets sur l'affichage
        for (int i = 0; i < this.game.getGraph().getNbVertices(); i++) {
            Pair<Integer, Integer> coord = this.game.getGraph().getVertices().get(i).getCoords();
            // On crée un texte pour le numéro du sommet
            Text text = new Text(String.valueOf(i + 1));
            text.setBoundsType(TextBoundsType.VISUAL);
            text.setFont(Font.font(UtilsGui.FONT, FontWeight.BOLD,15));
            // Centrage du texte
            if (i >= 9) {
                text.relocate(coord.getKey() + 13.50, coord.getValue() + 15.50);
            }
            else {
                text.relocate(coord.getKey() + 16.50, coord.getValue() + 15.50);
            }
            // Un cercle pour représenter le sommet
            colorPlanarGraph(game.getGraph());
            Circle vertex = new Circle(UtilsGui.CIRCLE_SIZE, Color.web(colors.get(game.getGraph().getVertices().get(i).getColor())));
            vertex.relocate(coord.getKey(), coord.getValue());
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, text);
        }
    }

    public void create(Text textField, Turn turn, int nbVertices) {
        try {
            if (game != null)
                game.getClient().close();
        }
        catch (IOException | NullPointerException e) {
            log.info(e.getMessage());
        }
        try {
            WebSocketClient client = new WebSocketClient(nbVertices, 0L, turn);
            gameCode = Optional.of(client.getId());
            textField.setText("Code de la partie: " + StringUtils.rightPad(String.valueOf(gameCode.get()), 4));
            this.game = client.connect(() -> Platform.runLater(() -> stage.setScene(run())));
            if (game == null) {
                textField.setText("");
                Platform.runLater(() -> popupMessage("La génération du graphe a pris trop de temps", "Veuillez essayer" +
                        " de réduire le nombre de sommets"));
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public void join(TextField textField) {
        try {
            long code = Long.parseLong(textField.getText());
            // On ne rentre pas le code que l'on vient de générer
            if (getGameCode().isPresent() && getGameCode().get() == code) return;
            WebSocketClient client = new WebSocketClient(code);
            game = client.connect(() -> {});
            stage.setScene(run());
            if (client.getWaiting() != null)
                game.play1vs1(client.getWaiting());
        } catch (IOException | URISyntaxException | InterruptedException | NumberFormatException e) {
            log.error(e.getMessage());
        }
    }
}


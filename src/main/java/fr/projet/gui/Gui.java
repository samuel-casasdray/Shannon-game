package fr.projet.gui;

import fr.projet.server.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    @Getter
    @Setter
    private static long seed;
    private Random random = new Random();
    private Stage stage;
    private Level level;
    @Getter
    private Optional<Long> gameCode = Optional.empty();

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
            case HOME_PVIA, JOUEUR -> stage.setScene(GuiScene.pvia(this::handleButtonClick));
            case HOME_PVPL -> {
                this.game = new Game();
                stage.setScene(run());
            }
            case HOME_PVPO -> stage.setScene(
                GuiScene.pvp(
                    this::handleButtonClick,
                    (textField, turn) -> join((TextField) textField, turn),
                    (textField, turn) -> create((Text) textField, turn)
                )
            );
            case HOME_IAVIA -> stage.setScene(run()); //TODO : Changer quand IA_VS_IA existe
            case JOUEUR_SHORT -> {
                this.game = new Game(true, Turn.CUT, level);
                stage.setScene(run());
                game.play(null, null);
            }
            case JOUEUR_CUT -> {
                this.game = new Game(true, Turn.SHORT, level);
                stage.setScene(run());
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
        }
    }


    public void popupMessage(){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Vous allez quiter le jeu !");
        alert.setContentText("Etes vous sûr de vouloir quitter ?");
        ButtonType buttonAccept = new ButtonType("Accepter");
        ButtonType buttonCancel = new ButtonType("Annuler");
        alert.getButtonTypes().setAll(buttonAccept,buttonCancel);

        alert.showAndWait().ifPresent(response ->{
            if (response==buttonAccept){
                stage.setScene(GuiScene.home(this::handleButtonClick));
                if (game.isPvpOnline()) {
                    try {
                        game.getClient().close();
                    } catch (Exception ignored) {}
                }
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
        Pane pane = new Pane();
        pane.setPrefSize(UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
        random = new Random(seed);
        showGraph(pane);
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
            if (node instanceof Circle) {
                //System.out.println("cercle");
                Circle vertex = (Circle) node;
                Pair<Integer, Integer> coord = this.game.getGraph().getVertices().get(nodeIndex).getCoords();
                vertex.relocate(coord.getKey()+ xOffset,coord.getValue() + yOffset);
                nodeIndex++;
            } else if (node instanceof Text) {
                //System.out.println("texte");
                Text text = (Text) node;
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


    public void showGraph(Pane pane) {
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
            Circle vertex = new Circle(UtilsGui.CIRCLE_SIZE, new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1));
            vertex.relocate(coord.getKey(), coord.getValue());
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, text);
        }
    }

    public void create(Text textField, Turn turn) {
        try {
            game.getClient().close();
        }
        catch (IOException | NullPointerException e) {
            log.info(e.getMessage());
        }
        try {
            WebSocketClient client = new WebSocketClient(0L, false, turn);
            gameCode = Optional.of(client.getId());
            textField.setText("Code de la partie: " + StringUtils.rightPad(String.valueOf(gameCode.get()), 4));
            this.game = client.connect(() -> Platform.runLater(() -> stage.setScene(run())));
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public void join(TextField textField, Turn turn) {
        try {
            long code = Long.parseLong(textField.getText());
            // On ne rentre pas le code que l'on vient de générer
            if (getGameCode().isPresent() && getGameCode().get() == code) return;
            WebSocketClient client = new WebSocketClient(code, true, turn);
            game = client.connect(() -> {});
            stage.setScene(run());
        } catch (IOException | URISyntaxException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}


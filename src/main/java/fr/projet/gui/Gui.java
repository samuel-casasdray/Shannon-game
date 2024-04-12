package fr.projet.gui;

import fr.projet.Main;
import fr.projet.server.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import jdk.jshell.execution.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javafx.scene.media.*;

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
    private static Stage stage;
    private Level level;
    @Setter
    private Turn turn;
    private Boolean withIA;
    @Setter
    private int nbVertices = 20;
    @Getter
    private Optional<Long> gameCode = Optional.empty();
    private Thread gameThread;
    @Getter
    private static Pane pane;
    private Random random = new Random();
    @Setter
    private static IntegerProperty victoryAchievedProperty= new SimpleIntegerProperty();

    private List<Line> posTransport = new ArrayList<>();



    Timeline timer = new Timeline(new KeyFrame(Duration.millis(20), event -> {
            for(Line line: posTransport) {
                ObservableMap<Object, Object> properties = line.getProperties();
                int i = (int) properties.get("i");
                double Ux = (double) properties.get("Ux");
                double Uy = (double) properties.get("Uy");
                int Ax = (int) properties.get("Ax");
                int Ay = (int) properties.get("Ay");
                int Bx = (int) properties.get("Bx");
                int By = (int) properties.get("By");
                i++;
                properties.put("i", i);
                double posX = i * Ux + Ax;
                double posY = i * Uy + Ay;
                if ((Ax < Bx && posX > Bx) || (Ax > Bx && posX < Bx) || (Ay < By && posY > By) || (Ay > By && posY < By)) {
                    i = 0;
                    properties.put("i", i);
                }
                line.setTranslateX(i*Ux);
                line.setTranslateY(i*Uy);
            }
    }));


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
        mainTheme ();
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
        if (turn==Turn.CUT){
            victoryAchievedProperty.set(0);
            victoryAchievedProperty.set(1);
        }
        else {
            victoryAchievedProperty.set(0);
            victoryAchievedProperty.set(2);
        }
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
        borderPane.setPrefSize(UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

        // Création du Pane pour afficher le graphique
        pane = new Pane();
        pane.setBackground(GuiScene.getBackground());
        pane.setPrefSize(UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
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
        Scene scene = new Scene(borderPane, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);


        //Ecouteur pour afficher message de victoire
        victoryAchievedProperty.addListener((observableValue, oldValue, newValue) -> {
            Platform.runLater(() -> {
               if(newValue.equals(1)){
                   //text1.setVisible(true);
                   Text text1 = UtilsGui.createText("CUT a gagné !",true);
                   text1.setFont(UtilsGui.FONT4);
                   text1.setX((scene.getWidth() - text1.getLayoutBounds().getWidth()) / 2);
                   text1.setY((scene.getHeight() - text1.getLayoutBounds().getHeight()) / 2);
                   text1.setTextAlignment(TextAlignment.CENTER);
                   animationTexte(text1);
                   pane.getChildren().add(text1);
               } else if (newValue.equals(2)) {
                   Text text2 = UtilsGui.createText("SHORT a gagné !",true);
                   text2.setFont(UtilsGui.FONT4);
                   text2.setX((scene.getWidth() - text2.getLayoutBounds().getWidth()) / 2);
                   text2.setY((scene.getHeight() - text2.getLayoutBounds().getHeight()) / 2);
                   text2.setTextAlignment(TextAlignment.CENTER);
                   animationTexte(text2);
                   pane.getChildren().add(text2);
               }
            });
        });


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

private void animationTexte (Text text){
    TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(2), text);
    translateTransition.setToY(40); // Déplacement de 50 pixels vers le bas
    translateTransition.setCycleCount(Animation.INDEFINITE); // Répéter indéfiniment
    translateTransition.setAutoReverse(true); // Revenir en arrière après chaque itération

    // Démarrer la translation
    translateTransition.play();
}

//fonction qui recalcule les position des aretes et sommets lors d'un redimensionnement
    private void updateGraphLayout(Pane pane) {
        double xOffset = (pane.getWidth() - UtilsGui.WINDOW_WIDTH) / 2;
        double yOffset = (pane.getHeight() - UtilsGui.WINDOW_HEIGHT) / 2;

        // Mise à jour des positions des arêtes
        for (Pair<Pair<Vertex, Vertex>, Line> edge : edges) {
            Line line = edge.getValue();
            Pair<Vertex, Vertex> pair = edge.getKey();
            line.setStartX(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE + xOffset);
            line.setStartY(pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE + yOffset);
            line.setEndX(pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE + xOffset);
            line.setEndY(pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE + yOffset);
        }

//        int nodeIndex=0;
        // Mise à jour des positions des sommets et des textes
//        List<Node> nodes = pane.getChildren();
//        for (int i = 0; i < nodes.size(); i++) {
//            Node node = nodes.get(i);
//        }
        for (Node node : pane.getChildren()) {
            if (node instanceof Circle || node instanceof ImageView) {
                node.setTranslateX(xOffset + UtilsGui.CIRCLE_SIZE);
                node.setTranslateY(yOffset + UtilsGui.CIRCLE_SIZE);
            } else if (node instanceof Text text) {
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
    private static List<String> colors = List.of("1", "2", "3", "4", "5", "6");
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
            int Ax = pair.getKey().getCoords().getKey();
            int Ay = pair.getKey().getCoords().getValue();
            int Bx = pair.getValue().getCoords().getKey();
            int By = pair.getValue().getCoords().getValue();
            double pas = random.nextDouble() / 200 + 0.0025;
            double Ux = (Bx-Ax) * pas;
            double Uy = (By-Ay) * pas;
            Line line = new Line(Ax, Ay, Bx, By);
            line.setStroke(Paint.valueOf("#a2d2ff"));
            Line line2 = new Line(Ax, Ay, Ax+Ux, Ay+Uy);
            line.setStrokeWidth(5);
            line2.setStrokeWidth(7);
            line2.setStroke(Paint.valueOf("#a2d2ff"));
            line.setOnMouseClicked(handler);
            line2.setOnMouseClicked(handler);
            line.getProperties().put("pair", pair);
            line2.getProperties().put("Ux", Ux);
            line2.getProperties().put("Uy", Uy);
            line2.getProperties().put("Ax", Ax);
            line2.getProperties().put("Ay", Ay);
            line2.getProperties().put("Bx", Bx);
            line2.getProperties().put("By", By);
            line2.getProperties().put("i", 0);
            // Ajout de la ligne sur sur l'affichage
            pane.getChildren().addAll(line, line2);
            edges.add(new Pair<>(pair, line));
            posTransport.add(line2);

        }
        // Ajout des sommets sur l'affichage
        for (int i = 0; i < this.game.getGraph().getNbVertices(); i++) {
            Pair<Integer, Integer> coord = this.game.getGraph().getVertices().get(i).getCoords();
            // Un cercle pour représenter le sommet
            colorPlanarGraph(game.getGraph());
            Circle vertex = new Circle(UtilsGui.CIRCLE_SIZE, Color.rgb(0, 0, 0, 0));

            String name = "planet" + i % 14 + ".gif";
            URL ressource = this.getClass().getClassLoader().getResource(name);
            if(Objects.isNull(ressource)) {
                log.error("Impossible de recupérer la ressource : " + name);
                vertex.relocate(coord.getKey(), coord.getValue());
                pane.getChildren().addAll(vertex);
                continue;
            }
            Image image = new Image(ressource.toExternalForm());
            ImageView imageView = new ImageView(image);
            double width = image.getWidth();
            double height = image.getHeight();
            vertex.relocate(coord.getKey() - width / 2, coord.getValue() - height / 2);
            imageView.relocate(coord.getKey() - width / 2, coord.getValue() - height / 2);
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, imageView);
        }
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        game.playSound("fight",1);
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


//    public static void destroy (Vertex v) {
//        System.out.println("odjdoajdz");
//        String name = "boom.gif";
//        URL ressource = Gui.stage.getClass().getClassLoader().getResource(name);
//        Image image = new Image(ressource.toExternalForm());
//        ImageView imageView = new ImageView(image);
//        double initialWidth = image.getWidth();
//        double initialHeight = image.getHeight();
//        imageView.setFitWidth(initialWidth / 2);
//        imageView.setFitHeight(initialHeight / 2);
//        double newX = v.getX() - (imageView.getFitWidth() / 2);
//        double newY = v.getY() - (imageView.getFitHeight() / 2);
//        imageView.setLayoutX(newX);
//        imageView.setLayoutY(newY);
//        double width = image.getWidth();
//        double height = image.getHeight();
//        Gui.pane.getChildren().addAll(imageView);
//    }


    public void mainTheme () {
        String audioS = "Sounds/testMusic.mp3";
        System.out.println("Audio "+audioS);
        URL audioUrl = this.getClass().getClassLoader().getResource(audioS);
        System.out.println("Lool "+audioUrl);
        assert audioUrl != null;
        String audioFile = audioUrl.toExternalForm();
        Media sound = new Media(audioFile);
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(1.7);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        stage.setOnCloseRequest(event -> stopMediaPlayer(mediaPlayer));
        mediaPlayer.play();
//        new Thread(() -> {
//            mediaPlayer.play();
//        }).start();
        //mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.stop());
        //long time = (long) (sound.getDuration().toMillis() + 1000);
        //mediaPlayer.play();
    }

    private void stopMediaPlayer(MediaPlayer mp) {
        if (mp != null) {
            mp.stop();
        }
    }


}


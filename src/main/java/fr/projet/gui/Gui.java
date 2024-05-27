package fr.projet.gui;

import fr.projet.server.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.media.*;

import java.io.*;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Gui extends Application {
    @Getter
    @Setter
    private static EventHandler<MouseEvent> handler;
    @Getter
    @Setter
    private static Game game;
    @Getter
    @Setter
    private static List<Pair<Pair<Vertex, Vertex>, Line>> edges = new ArrayList<>();
    @Getter
    private static Stage stage;
    private static Level level;
    @Setter
    private static Turn turn;
    private static Boolean withIA;
    @Setter
    private static int nbVertices = 20;
    @Getter
    @Setter
    private static Optional<Long> gameCode = Optional.empty();
    private static Thread gameThread;
    @Getter
    private static Pane pane;
    private static final Random random = new Random();
    @Setter
    private static IntegerProperty victoryAchievedProperty= new SimpleIntegerProperty();
    @Getter
    @Setter
    private static List<Etoile> etoiles = new ArrayList<>();
    private static final int planZ = 10;
    @Getter
    @Setter
    private static Timeline stars;
    @Getter
    @Setter
    private static Timeline timer = new Timeline();
    @Getter
    @Setter
    private static Timeline timerText = new Timeline();
    private static final List<ImageView> images = new ArrayList<>();
    private static final CheckBox planetes = new CheckBox("Afficher planètes");
    @Getter
    private static final Slider slider = new Slider(0, 1, GuiScene.getSlider().getValue());
    @Getter
    @Setter
    private static int NB_STARS = GuiScene.getNB_STARS();
    private static int MIN_STARS = GuiScene.getMIN_STARS();
    private static int MAX_STARS = GuiScene.getMAX_STARS();
    @Getter
    private static final Slider slider2 = new Slider(0, 1.5, GuiScene.getSlider2().getValue());
    @Getter
    @Setter
    private static double VOLUME = GuiScene.getVOLUME();
    private static double MIN_VOLUME = GuiScene.getMIN_VOLUME();
    private static double MAX_VOLUME = GuiScene.getMAX_VOLUME();
    private static MediaPlayer mainSound;
    public static void createAnim() {
        new Thread(() -> {
            Gui.setTimer(new Timeline(new KeyFrame(Duration.millis(20), event -> {
                draw(pane);
                if (etoiles.size() < NB_STARS) {
                    etoiles.addAll(generer(100));
                }
            })));
            timer.setCycleCount(Animation.INDEFINITE);
            timer.play();
        }).start();
    }

    public static List<Etoile> generer(int nb) {
        List<Etoile> lst = new ArrayList<>();
        for (int i = 0; i < nb; i++)
        {
            Etoile e = new Etoile();
            e.randomize(random);
            lst.add(e);
        }
        return lst;
    }

    public static void draw(Pane root) {
        float width = (float) UtilsGui.WINDOW_WIDTH;
        float height = (float) UtilsGui.WINDOW_HEIGHT;
//        for (Etoile e : etoiles) {
//            e.setZ(e.getZ() - 1);
//            if (e.getZ() <= 0) {
//                e.setZ(e.getZ()+1000);
//            }
//        }
        root.getChildren().removeIf(Rectangle.class::isInstance);
        //etoiles.sort(Comparator.comparingDouble(Etoile::getZ).reversed()); // des tests sont à faire mais
        // laissons commenté pour le moment
        for (Etoile etoile : etoiles) {
            etoile.setZ(etoile.getZ() - 1);
            if (etoile.getZ() <= 0) {
                etoile.setZ(etoile.getZ()+1000);
            }
            if (etoile.getZ() >= planZ) {
                float x = planZ * etoile.getX() / etoile.getZ() + width/2;
                float y = planZ * etoile.getY() / etoile.getZ() + height/2;
                if (x >= 0 && x <= width && y >= 0 && y <= height) {
                    Rectangle pixel = new Rectangle(x, y, 2,2);
                    pixel.setFill(etoile.pixelColor());
                    pixel.setViewOrder(100);
                    root.getChildren().add(pixel);
                }
            }
        }
    }

    @Override
    public void start(Stage stage) {
        // On initialise un handshake pour éviter de devoir attendre 1 seconde lorsqu'on appuie sur create
        new Thread(WebSocketClient::getHandshake).start();
        Gui.stage = stage;
        try {
            File file = new File("config.txt");
            FileReader fileReader;
            try {
                new FileReader(file);
            }
            catch (FileNotFoundException e) {
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("0.5");
                bufferedWriter.newLine();
                bufferedWriter.write("0.5");
                bufferedWriter.close();
                fileWriter.close();
            }
            fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            GuiScene.setVolumeSliderValue(Double.parseDouble(bufferedReader.readLine()));
            GuiScene.setStarsSliderValue(Double.parseDouble(bufferedReader.readLine()));
            bufferedReader.close();
            fileReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage.setScene(GuiScene.home(Gui::handleButtonClick));
        stage.setTitle("Shannon Game");
        URL url = getClass().getResource("/icon-appli.png");
        if(url == null) {
            log.error("No icon");
        } else {
            Image icon = new Image(url.toExternalForm());
            stage.getIcons().add(icon);
        }
        stage.show();
        mainTheme();
    }
    public static void handleButtonClick(ButtonClickType buttonClickType) {
        switch (buttonClickType) {
            case HOME -> stage.setScene(GuiScene.home(Gui::handleButtonClick));
            case HOME_PVIA-> {
                withIA=true;
                stage.setScene(GuiScene.pvia(Gui::handleButtonClick));
            }
            case JOUEUR -> stage.setScene(GuiScene.joueur(Gui::handleButtonClick));
            case HOME_PVPL -> {
                withIA=false;
                stage.setScene(GuiScene.nbVertices(Gui::handleButtonClick, withIA, level));
            }
            case HOME_PVPO -> stage.setScene(
                GuiScene.pvp(
                        Gui::handleButtonClick,
                    textField -> join((TextField) textField),
                    client -> create((WebSocketClient) client)
                )
            );
            case HOME_IAVIA -> stage.setScene(GuiScene.aivsai(Gui::handleButtonClick));
            case JOUEUR_SHORT -> {
                turn=Turn.CUT;
                stage.setScene(GuiScene.nbVertices(Gui::handleButtonClick,withIA, level));
            }
            case JOUEUR_CUT -> {
                turn=Turn.SHORT;
                stage.setScene(GuiScene.nbVertices(Gui::handleButtonClick, withIA, level));
            }
            case PVIA_EASY -> {
                Gui.level = Level.EASY;
                stage.setScene(GuiScene.joueur(Gui::handleButtonClick));
            }
            case PVIA_MEDIUM -> {
                Gui.level = Level.MEDIUM;
                stage.setScene(GuiScene.joueur(Gui::handleButtonClick));
            }
            case PVIA_HARD -> {
                Gui.level = Level.HARD;
                stage.setScene(GuiScene.joueur(Gui::handleButtonClick));
            }
            case STRAT_WIN -> {
                Gui.level = Level.STRAT_WIN;
                stage.setScene(GuiScene.joueur(Gui::handleButtonClick));
            }
            case JEU -> Platform.runLater(Gui::popupMessage);
            case VERTICES -> {
                Gui.nbVertices=GuiScene.getNbVertices();
                try {
                    Gui.game = new Game(nbVertices, withIA, turn, level);
                }
                catch (TimeoutException e) {
                    Platform.runLater(() -> popupMessage("La génération du graphe a pris trop de temps", "Veuillez essayer" +
                            " de réduire le nombre de sommets"));
                    return;
                }
                stage.setScene(run());
                if (withIA && turn==Turn.CUT) {
                    gameThread = new Thread(() -> game.play(null));
                    gameThread.setDaemon(true);
                    gameThread.start();
                }
            }
            case AIvsAI -> {
                Gui.nbVertices = GuiScene.getNbVertices();
                Level levelAI1 = GuiScene.getLevel1();
                Level levelAI2 = GuiScene.getLevel2();
                try {
                    Gui.game = new Game(nbVertices, levelAI1, levelAI2);
                }
                catch (TimeoutException e) {
                    Platform.runLater(() -> popupMessage("La génération du graphe a pris trop de temps", "Veuillez essayer" +
                            " de réduire le nombre de sommets"));
                    return;
                }
                stage.setScene(run());
                gameThread = new Thread(game::aiVsAi);
                gameThread.setDaemon(true);
                gameThread.start();
            }
            case STATS -> stage.setScene(GuiScene.stats(Gui::handleButtonClick));
            case RANKED -> stage.setScene(GuiScene.ranked(Gui::handleButtonClick));
            case LOGIN -> stage.setScene(GuiScene.login(Gui::handleButtonClick));
            case REGISTER -> stage.setScene(GuiScene.register(Gui::handleButtonClick));
            case HISTOIRE -> stage.setScene(GuiScene.histoire(Gui::handleButtonClick));
            case LEVEL1 -> {
                Gui.nbVertices = 4;
                List<Vertex> vertices = new ArrayList<>() {
                    {
                        add(new Vertex(UtilsGui.WINDOW_WIDTH / 2 - 200, UtilsGui.WINDOW_HEIGHT / 2 - 200));
                        add(new Vertex(UtilsGui.WINDOW_WIDTH / 2 + 200, UtilsGui.WINDOW_HEIGHT / 2 - 200));
                        add(new Vertex(UtilsGui.WINDOW_WIDTH / 2 - 200, UtilsGui.WINDOW_HEIGHT / 2 + 200));
                        add(new Vertex(UtilsGui.WINDOW_WIDTH / 2 + 200, UtilsGui.WINDOW_HEIGHT / 2 + 200));
                    }
                };
                Map<Vertex, HashSet<Vertex>> adjVertices = new HashMap<>() {{
                    put(vertices.get(0), new HashSet<>() {{
                        add(vertices.get(1));
                        add(vertices.get(2));
                        add(vertices.get(3));
                    }});
                    put(vertices.get(1), new HashSet<>() {{
                        add(vertices.get(0));
                        add(vertices.get(2));
                        add(vertices.get(3));
                    }});
                    put(vertices.get(2), new HashSet<>() {{
                        add(vertices.get(0));
                        add(vertices.get(1));
                        add(vertices.get(3));
                    }});
                    put(vertices.get(3), new HashSet<>() {{
                        add(vertices.get(0));
                        add(vertices.get(1));
                        add(vertices.get(2));
                    }});
                }};
                Gui.game = new Game(vertices, adjVertices);
                stage.setScene(run());
                GuiScene.histoire1(Gui::handleButtonClick);
            }
        }
    }

    private static void leaveGame() {
        withIA = false;
        level = null;
        timer.stop();
        stage.setScene(GuiScene.home(Gui::handleButtonClick));
        if (game.isPvpOnline()) {
            try {
                game.getClient().close();
            } catch (IOException ignored) {}
        }
        if (gameThread != null)
        {
            game.setInterrupted(true);
            if (gameThread.isAlive() && !game.cutWon() && !game.shortWon())
                gameThread.interrupt();
        }
    }
    public static void popupMessage(){
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

    public static Scene run() {
        slider.setValue(GuiScene.getSlider().getValue());
        slider.setMinorTickCount(0);
        slider.setMajorTickUnit(1);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double n) {
                if (n < 0.5) return "Less stars";
                return "More stars";
            }

            @Override
            public Double fromString(String s) {
                if (s.equals("Less stars")) {
                    return 0d;
                }
                return 1d;
            }
        });
        slider2.setMinorTickCount(0);
        slider2.setMajorTickUnit(1.5);
        slider2.setShowTickMarks(true);
        slider2.setShowTickLabels(true);
        slider2.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double n) {
                if (n < 0.5) return "-";
                return "+";
            }

            @Override
            public Double fromString(String s) {
                if (s.equals("-")) {
                    return 0d;
                }
                return 1.5d;
            }
        });
        slider2.setValue(GuiScene.getSlider2().getValue());
        NB_STARS = GuiScene.getNB_STARS();
        // Création d'un BorderPane pour centrer le contenu
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefSize(UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

        // Création du Pane pour afficher le graphique
        pane = new Pane();
        stars.stop();
        pane.setPrefSize(UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
        planetes.setSelected(true);
        pane.getChildren().removeAll(images);
        images.clear();
        planetes.setOnAction(event -> {
            if (planetes.isSelected()) {
                pane.getChildren().addAll(images);
            } else {
                pane.getChildren().removeAll(images);
            }
        });



        //Code pour afficher les deux arbres couvrants disjoints s'ils existent
//        Graph graph = game.getGraph();
//        Thread arbres = new Thread(() -> {
//            List<Graph> result = graph.appelStratGagnante();
//            if (!result.isEmpty()) {
//                for (Pair<Vertex, Vertex> pair : result.getFirst().getNeighbors()) {
//                    Line line = new Line(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                            pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE,
//                            pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                            pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE);
//                    Platform.runLater(() -> {
//                        line.setStroke(Color.LIGHTGREEN);
//                        line.setStrokeWidth(10);
//                        pane.getChildren().add(line);
//                    });
//                }
//
//                for (Pair<Vertex, Vertex> pair : result.getLast().getNeighbors()) {
//                    Line line = new Line(pair.getKey().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                            pair.getKey().getCoords().getValue() + UtilsGui.CIRCLE_SIZE,
//                            pair.getValue().getCoords().getKey() + UtilsGui.CIRCLE_SIZE,
//                            pair.getValue().getCoords().getValue() + UtilsGui.CIRCLE_SIZE);
//                    Platform.runLater(() -> {
//                        line.setStroke(Color.RED);
//                        line.setStrokeWidth(5);
//                        pane.getChildren().add(line);
//                    });
//                }
//            }
//            else {
//                System.out.println("Il n'y a pas deux arbres couvrants disjoints");
//            }
//        });
//        arbres.setDaemon(true);
//        arbres.start();

        Button returnButton = UtilsGui.getReturnButton(ButtonClickType.JEU, Gui::handleButtonClick);
        edges.clear();
        planetes.setLayoutX(500);
        planetes.setLayoutY(0);
        planetes.setTextFill(Color.WHITE);
        slider.setLayoutX(700);
        slider.setLayoutY(0);
        slider2.setLayoutX(900);
        slider2.setLayoutY(0);
        showGraph();
        if (game.isPvpOnline()) {
            GridPane root = new GridPane();
            root.setAlignment(Pos.CENTER);
            root.setHgap(80);
            root.setVgap(10);
            Turn turn;
            if ((game.getJoiner() && game.getCreatorTurn() == Turn.CUT) || (!game.getJoiner() && game.getCreatorTurn() == Turn.SHORT))
                turn = Turn.SHORT;
            else
                turn = Turn.CUT;
            Text text = UtilsGui.createText("Vous jouez : " + turn);
            root.add(text, 1, 1);
            pane.getChildren().addAll(root, returnButton, planetes, slider, slider2);
            game.playSound();
        }
        else if (game.getLevelIACut() != null && game.getLevelIAShort() != null) {
            GridPane root = new GridPane();
            root.setAlignment(Pos.CENTER);
            root.setHgap(80);
            root.setVgap(0);
            Text versus = UtilsGui.createText(game.getLevelIACut() + " vs " + game.getLevelIAShort());
            root.add(versus, 1, 1);
            pane.getChildren().addAll(root, returnButton, planetes, slider, slider2);
        }
        else
            pane.getChildren().addAll(returnButton, planetes, slider, slider2);

        slider.valueProperty().addListener(event -> {
            double t = slider.getValue();
            NB_STARS = (int) ((1-t)*MIN_STARS+MAX_STARS*t);
            GuiScene.getSlider().setValue(t);
            GuiScene.setNB_STARS(NB_STARS);
            GuiScene.setStarsSliderValue(t);
            createRemoveStars(NB_STARS);
        });

        slider2.valueProperty().addListener(event -> {
            double t = slider2.getValue();
            GuiScene.getSlider2().setValue(t);
            VOLUME = (1-t)*MIN_VOLUME+MAX_VOLUME*t;
            GuiScene.setVolumeSliderValue(t);
            changeVolume(VOLUME);
        });

        borderPane.setCenter(pane);
        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(borderPane, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT, Color.BLACK);


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
                   UtilsGui.animationTexte(text1);
                   pane.getChildren().add(text1);
               } else if (newValue.equals(2)) {
                   Text text2 = UtilsGui.createText("SHORT a gagné !",true);
                   text2.setFont(UtilsGui.FONT4);
                   text2.setX((scene.getWidth() - text2.getLayoutBounds().getWidth()) / 2);
                   text2.setY((scene.getHeight() - text2.getLayoutBounds().getHeight()) / 2);
                   text2.setTextAlignment(TextAlignment.CENTER);
                   UtilsGui.animationTexte(text2);
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

    static void createRemoveStars(int nbStars) {
        if (Gui.getEtoiles().size() < nbStars) {
            while (Gui.getEtoiles().size() < nbStars) {
                Gui.getEtoiles().addAll(Gui.generer(100));
            }
        }
        else {
            while (Gui.getEtoiles().size() > nbStars) {
                Gui.getEtoiles().remove(Gui.getEtoiles().getFirst());
            }
        }
    }

    private static void animationTexte(Text text){
    TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(2), text);
    translateTransition.setToY(40); // Déplacement de 50 pixels vers le bas
    translateTransition.setCycleCount(Animation.INDEFINITE); // Répéter indéfiniment
    translateTransition.setAutoReverse(true); // Revenir en arrière après chaque itération

    // Démarrer la translation
    translateTransition.play();
}

//fonction qui recalcule les position des aretes et sommets lors d'un redimensionnement
    private static void updateGraphLayout(Pane pane) {
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
        for (Node node : pane.getChildren()) {
            if (node instanceof Circle || node instanceof ImageView) {
                node.setTranslateX(xOffset + UtilsGui.CIRCLE_SIZE);
                node.setTranslateY(yOffset + UtilsGui.CIRCLE_SIZE);
            }
        }
    }
    private static final List<String> colors = List.of("#00ccff", "#ff0000", "#00ff99", "#ffff66", "#9933ff", "#ff6600");
    public static void colorPlanarGraph(Graph graph) {
        if (graph.getNbVertices() <= 6) {
            for (int i = 0; i < graph.getVertices().size(); i++) {
                graph.getVertices().get(i).setColor(i);
            }
            return;
        }
        Vertex v = null;
        for (Vertex vertex : graph.getVertices()) {
            if (graph.degree(vertex) <= 5) {
                v = vertex;
                break;
            }
        }
        Graph g2 = new Graph(graph.getVertices(), graph.getAdjVertices());
        g2.removeVertex(v);
        colorPlanarGraph(g2);
        Set<Integer> colorsNeighbor = new HashSet<>();
        for (Vertex v2 : graph.getAdjVertices().get(v)) {
            colorsNeighbor.add(v2.getColor());
        }
        for (int i = 0; i < 6; i++) {
            if (!colorsNeighbor.contains(i)) {
                v.setColor(i);
                return;
            }
        }
    }

    public static void showGraph() {
        // Ajout des aretes sur l'affichage
        if (game == null) return; // Cas qui peut survenir si le serveur est off
        for (Pair<Vertex, Vertex> pair : Gui.game.getGraph().getEdges()) {
            int Ax = pair.getKey().getCoords().getKey();
            int Ay = pair.getKey().getCoords().getValue();
            int Bx = pair.getValue().getCoords().getKey();
            int By = pair.getValue().getCoords().getValue();
            double pas = random.nextDouble() / 200 + 0.0025;
            Line line = new Line(Ax, Ay, Bx, By);
            LinearGradient gradient = new LinearGradient(
                Ax,
                Bx,
                Ay,
                By,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.valueOf("ff99c8")),
                new Stop(1, Color.valueOf("fff"))
            );
            line.setStroke(gradient);
            line.setStrokeWidth(5);
            line.setOnMouseClicked(handler);
            line.getProperties().put("pair", pair);
            // Ajout de la ligne sur sur l'affichage
            pane.getChildren().add(line);
            edges.add(new Pair<>(pair, line));
        }
        colorPlanarGraph(game.getGraph());
        for (int i = 0; i < Gui.game.getGraph().getNbVertices(); i++) {
            Pair<Integer, Integer> coord = Gui.game.getGraph().getVertices().get(i).getCoords();
            // Un cercle pour représenter le sommet
           Circle vertex = new Circle(UtilsGui.CIRCLE_SIZE, Color.web(colors.get(Gui.game.getGraph().getVertices().get(i).getColor())));

            String name = "planet" + i % 14 + ".gif";
            URL ressource = Gui.class.getClassLoader().getResource(name);
            if(Objects.isNull(ressource)) {
                log.error("Impossible de recupérer la ressource : " + name);
                vertex.relocate(coord.getKey()-UtilsGui.CIRCLE_SIZE, coord.getValue()-UtilsGui.CIRCLE_SIZE);
                continue;
            }
            Image image = new Image(ressource.toExternalForm());
            ImageView imageView = new ImageView(image);
            double width = image.getWidth();
            double height = image.getHeight();
            vertex.relocate(coord.getKey()-UtilsGui.CIRCLE_SIZE, coord.getValue()-UtilsGui.CIRCLE_SIZE);
            imageView.relocate(coord.getKey() - width / 2, coord.getValue() - height / 2);
            // On ajoute les 2 élements sur l'affichage
            images.add(imageView);
            pane.getChildren().addAll(vertex, imageView);
        }
        pane.setOnMouseClicked(handler);
        createAnim();
    }

    public static void create(WebSocketClient client) {
        try {
            if (game != null && game.getClient() != null)
                game.getClient().close();
        }
        catch (IOException | NullPointerException e) {
            log.info(e.getMessage());
            return;
        }
        try {
            Gui.game = client.connect(() -> Platform.runLater(() -> stage.setScene(run())));
        }
        catch (TimeoutException e) {
            Platform.runLater(() -> popupMessage("La génération du graphe a pris trop de temps", "Veuillez essayer" +
                    " de réduire le nombre de sommets"));
        }
        catch (SocketException se) {
            Platform.runLater(() -> popupMessage("Le serveur n'est pas joignable", "Vérifiez votre connexion internet"));
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void join(TextField textField) {
        try {
            long code = Long.parseLong(textField.getText());
            // On ne rentre pas le code que l'on vient de générer
            if (getGameCode().isPresent() && getGameCode().get() == code) return;
            WebSocketClient client = new WebSocketClient(code);
            game = client.connect(() -> {});
            if (game == null) return;
            stage.setScene(run());
            if (client.getWaiting() != null)
                game.play1vs1(client.getWaiting()); // ne surtout pas enlever
        } catch (SocketException se) {
            Platform.runLater(() -> popupMessage("Le serveur n'est pas joignable", "Vérifiez votre connexion internet"));
        }
        catch (IOException | URISyntaxException | NumberFormatException | TimeoutException e) {
            log.error(e.getMessage());
        }
        catch (InterruptedException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    public static void changeVolume (double v) {
        mainSound.setVolume(v);
        VOLUME = v;
    }


    public static void mainTheme () {
        Media sound = new Media(Gui.class.getClassLoader().getResource("Sounds/testMusic.mp3").toExternalForm());
        mainSound = new MediaPlayer(sound);
        mainSound.setCycleCount(MediaPlayer.INDEFINITE);
        mainSound.setVolume(GuiScene.getVOLUME());
        mainSound.play();
    }
}


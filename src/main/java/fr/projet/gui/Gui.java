package fr.projet.gui;

import fr.projet.game.Game;
import fr.projet.game.Turn;
import fr.projet.game.TypeJeu;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import fr.projet.serverClient.Client;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
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
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gui extends Application {

    public static final double CIRCLE_SIZE = 20D;
    public static final int WINDOW_SIZE = 600;
    public static final int WINDOW_MARGE = 100;
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


    @Override
    public void start(Stage stage) {
        // On initialise un handshake pour éviter de devoir attendre 1 seconde lorsqu'on appuie sur create
        new Thread(Client::getHandshake).start();
        this.stage = stage;
        stage.setScene(home());
        stage.setTitle("Shannon Game");
        Image icon = new Image(getClass().getResource("/icon-appli.png").toExternalForm());
        stage.getIcons().add(icon);
        stage.show();
    }

    public VBox getBasicScene() {
        VBox root = new VBox(50); // Espacement vertical entre les éléments
        root.setPadding(new Insets(40));

        BackgroundFill backgroundFill = new BackgroundFill(Color.LIGHTGREY, null, null);
        Background background = new Background(backgroundFill);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    public Text createText(String content) { return createText(content, false); }
    public Text createText(String content, boolean withShadow) {
        Text text = new Text(content);
        if (withShadow) {
            text.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
            DropShadow dropShadow = new DropShadow();
            dropShadow.setOffsetX(3.0);
            dropShadow.setOffsetY(3.0);
            dropShadow.setColor(Color.GRAY);
            text.setEffect(dropShadow);
        } else {
            text.setFont(Font.font("Consolas", 20));
        }
        return text;
    }
    private Button createButton(String text, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        button.setFont(Font.font("System", FontWeight.BOLD, 14));

        //effet d'ombre des boutons
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(5.0); // Décalage horizontal pour l'effet 3D
        shadow.setOffsetY(5.0);
        button.setEffect(shadow);

        //effet de grossissement lors du survol
        addHoverEffect(button);

        if (action != null)
            button.setOnAction(action);

        return button;
    }


    private Scene home() {
        VBox root = getBasicScene();

        Text text1 = createText("SHANNON GAME", true);
        Text text2 = createText("Choisissez votre mode de jeu :");

        //création des boutons d'option de jeu
        Button button1 = createButton("Jouer SHORT vs IA", event -> handleButtonClick(TypeJeu.SHORT_VS_IA));
        Button button2 = createButton("Jouer CUT vs IA", event -> handleButtonClick(TypeJeu.CUT_VS_IA));
        Button button3 = createButton("Joueur vs Joueur Online", event -> handleButtonClick(TypeJeu.PLAYER_VS_PLAYER_DISTANT));
        Button button4 = createButton("Joueur vs Joueur Local", event -> handleButtonClick(TypeJeu.PLAYER_VS_PLAYER_LOCAL));
        Button button5 = createButton("IA vs IA", event -> handleButtonClick(TypeJeu.IA_VS_IA));

        root.getChildren().addAll(text1,text2,button1,button2,button3,button4,button5);

        return new Scene(root, WINDOW_SIZE, WINDOW_SIZE);
    }

    private Scene PvP() {
        VBox root = getBasicScene();

        Text text1 = createText("Player vs Player", true);

        HBox type = new HBox(60);

        VBox join = new VBox(30);
        TextField textJoin = new TextField();
        Button button1 = createButton("Join", event -> {
            try {
                Client client = new Client(Long.parseLong(textJoin.getText()), true);
                this.game = client.connect(() -> {});
                stage.setScene(run());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        join.getChildren().addAll(button1, textJoin);

        VBox create = new VBox(30);
        Text textCreate = createText("");
        Button button2 = createButton("Create", event -> {
            try {
                Client client = new Client(0L, false);
                textCreate.setText(String.valueOf(client.getId()));
                this.game = client.connect(() -> Platform.runLater(() -> stage.setScene(run())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        create.getChildren().addAll(button2, textCreate);

        type.getChildren().addAll(join, create);
        type.setAlignment(Pos.CENTER);

        root.getChildren().addAll(text1,type);

        return new Scene(root, WINDOW_SIZE, WINDOW_SIZE);
    }

    //Fonction qui change la taille des boutons lorsqu'on les survole
   private void addHoverEffect(Button button) {

       // Effet de grossissement lorsqu'on passe sur le bouton
       ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), button);
       scaleTransition.setFromX(1.0);
       scaleTransition.setFromY(1.0);
       scaleTransition.setToX(1.2);
       scaleTransition.setToY(1.2);

       // Effet de réduction lorsqu'on quitte le bouton
       ScaleTransition scaleBackTransition = new ScaleTransition(Duration.millis(100), button);
       scaleBackTransition.setFromX(1.2);
       scaleBackTransition.setFromY(1.2);
       scaleBackTransition.setToX(1.0);
       scaleBackTransition.setToY(1.0);

       button.setOnMouseEntered(event -> scaleTransition.play());
       button.setOnMouseExited(event -> scaleBackTransition.play());
   }

    public void handleButtonClick(TypeJeu type){ //fonction qui change de scene lorsqu'on clique les differents boutons
        if (type == TypeJeu.SHORT_VS_IA){
            this.game = new Game(true, Turn.CUT);
            stage.setScene(run());
            game.play(null, null);
        }
        else if (type == TypeJeu.CUT_VS_IA){
            this.game = new Game(true, Turn.SHORT);
            stage.setScene(run());
        }
        else if (type == TypeJeu.PLAYER_VS_PLAYER_DISTANT){
            stage.setScene(PvP());
        }
        else if (type == TypeJeu.PLAYER_VS_PLAYER_LOCAL){
            this.game = new Game();
            stage.setScene(run());
        }
        else if (type == TypeJeu.IA_VS_IA){
            stage.setScene(run());
        }
    }

    public Scene run() {
        Pane pane = new Pane();
        // On définit la taille de notre affichage
        pane.setPrefSize(WINDOW_SIZE, WINDOW_SIZE);
        random = new Random(seed);
        showGraph(pane);
        return new Scene(pane, WINDOW_SIZE,WINDOW_SIZE);
}


    public void showGraph(Pane pane) {
        // Ajout des aretes sur l'affichage
        if (game == null) return; // Cas qui peut survenir si le serveur est off
        for (Pair<Vertex, Vertex> pair : this.game.getGraph().getNeighbors()) {
            Line line = new Line(pair.getKey().getCoords().getKey() + CIRCLE_SIZE,
                    pair.getKey().getCoords().getValue() + CIRCLE_SIZE,
                    pair.getValue().getCoords().getKey() + CIRCLE_SIZE,
                    pair.getValue().getCoords().getValue() + CIRCLE_SIZE);
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
            text.relocate(coord.getKey() + 18D, coord.getValue() + 18D);
            // Un cercle pour représenter le sommet
            Circle vertex = new Circle(CIRCLE_SIZE,
                    new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1));
            vertex.relocate(coord.getKey(), coord.getValue());
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, text);
        }
    }
}


package fr.projet.gui;

import fr.projet.IA.InterfaceIA;
import fr.projet.game.Game;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
    private static InterfaceIA ia;
    @Getter
    @Setter
    private static Game game;
    @Getter
    @Setter
    private static List<Pair<Pair<Vertex, Vertex>, Line>> edges = new ArrayList<>();
    @Getter
    @Setter
    private static long seed;
    private Random random = new Random();


    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(run()));
        VBox root = new VBox(60); // Espacement vertical entre les éléments
        root.setPadding(new Insets(40));
        stage.setScene(new Scene(root, WINDOW_SIZE, WINDOW_SIZE));

        stage.setTitle("Shannon Game");
        Image icon = new Image(getClass().getResource("/icon-appli.png").toExternalForm());
        stage.getIcons().add(icon);

        BackgroundFill backgroundFill = new BackgroundFill(Color.LIGHTGREY, null, null);
        Background background = new Background(backgroundFill);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);

        Text text1= new Text("SHANNON GAME");
        text1.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
        Text text2 = new Text("Choisissez votre mode de jeu :");
        text2.setFont(Font.font("Consolas", 20));
        DropShadow dropShadow = new DropShadow();
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.GRAY);
        text1.setEffect(dropShadow);

        //création des boutons d'option de jeu
        Button button1 = createButton("Jouer SHORT vs IA", stage);
        Button button2 = createButton("Jouer CUT vs IA", stage);
        Button button3 = createButton("Joueur vs Joueur", stage);

    }

    private Button createButton(String text, Stage stage){
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

        //effet lorsqu'on clique les boutons
        button.setOnAction(event -> handleButtonClick(text,stage));

        return button;
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

       button.setOnMouseEntered(event -> {
           scaleTransition.play();
       });
   }

    public void handleButtonClick(String text, Stage stage){ //fonction qui change de scene lorsqu'on clique les differents boutons
       if (text.equals("Jouer SHORT vs IA")){
            Scene SHORTvsIA = new Scene(run(),WINDOW_SIZE,WINDOW_SIZE);
            stage.setScene(SHORTvsIA);
       }
       else if (text.equals("Jouer CUT vs IA")){
            Scene CUTvsIA = new Scene(run(),WINDOW_SIZE,WINDOW_SIZE);
            stage.setScene(CUTvsIA);
       }
       else if (text.equals("Joueur vs Joueur")){
            Scene CUTvsSHORT = new Scene(run(),WINDOW_SIZE,WINDOW_SIZE);
            stage.setScene(CUTvsSHORT);
       }
       else if (text.equals("IA vs IA")){
            Scene IAvsIA = new Scene(run(),WINDOW_SIZE,WINDOW_SIZE);
            stage.setScene(IAvsIA);
       }
    }



    public Pane run() {
        Pane pane = new Pane();
        // On définit la taille de notre affichage
        pane.setPrefSize(WINDOW_SIZE, WINDOW_SIZE);
        random = new Random(seed);
        showGraph(pane);
        if (ia != null)
            game.playFirst();
        return pane;
}


    public void showGraph(Pane pane) {
        // Ajout des aretes sur l'affichage
        for (Pair<Vertex, Vertex> pair : graph.getNeighbors()) {
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
        for (int i = 0; i < graph.getNbVertices(); i++) {
            Pair<Integer, Integer> coord = graph.getVertices().get(i).getCoords();
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

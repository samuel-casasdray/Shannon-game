package fr.projet.gui;

import fr.projet.WebSocket.WebSocketClient;
import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.game.TypeJeu;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class Gui extends Application {

    public static final double CIRCLE_SIZE = 20D;
    public static final int WINDOW_SIZE = 600;
    public static final int WINDOW_MARGE = 50;
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
        new Thread(WebSocketClient::getHandshake).start();
        this.stage = stage;
        stage.setScene(home());
        stage.setTitle("Shannon Game");
        Image icon = new Image(getClass().getResource("/icon-appli.png").toExternalForm());
        stage.getIcons().add(icon);
        stage.show();
    }

    public VBox getBasicScene() {
        VBox root = new VBox(50); // Espacement vertical entre les éléments
        root.setPadding(new Insets(-40,0,10,0));

        BackgroundFill backgroundFill = new BackgroundFill(Color.LIGHTGREY, null, null);
        Background background = new Background(backgroundFill);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    //fonction pour ajouter fleche de retour dans la scene
    //TODO : faire en sorte que la fleche reste en haut à gauche et ne bouge pas avec les changements de scene
    public Pane getSceneWithReturn(String nomscene){
        Pane pane = new Pane();

        //creation de la fleche de retour
        Image imageReturn = new Image(getClass().getResource("/fleche-retour.png").toExternalForm());
        ImageView imageView = new ImageView(imageReturn);
        imageView.setFitWidth(40);
        imageView.setPreserveRatio(true);
        imageView.setLayoutX(10); imageView.setLayoutY(-4);

        Button returnButton = createButton("",event -> handleButtonClick(nomscene));
        returnButton.setStyle("-fx-background-color: transparent;");
        returnButton.setLayoutX(10); returnButton.setLayoutY(-4);

        pane.getChildren().addAll(imageView, returnButton);

        return pane;
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
        button.setMinSize(150,35);

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
        Button button1 = createButton("Jouer vs IA", event -> handleButtonClick(TypeJeu.PLAYER_VS_IA));
        Button button2 = createButton("Joueur vs Joueur Online", event -> handleButtonClick(TypeJeu.PLAYER_VS_PLAYER_DISTANT));
        Button button3 = createButton("Joueur vs Joueur Local", event -> handleButtonClick(TypeJeu.PLAYER_VS_PLAYER_LOCAL));
        Button button4 = createButton("IA vs IA", event -> handleButtonClick(TypeJeu.IA_VS_IA));

        root.getChildren().addAll(text1,text2,button1,button2,button3,button4);

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
                WebSocketClient client = new WebSocketClient(Long.parseLong(textJoin.getText()), true);
                this.game = client.connect(() -> {});
                stage.setScene(run());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        join.getChildren().addAll(button1, textJoin);

        VBox create = new VBox(30);
        Text textCreate = createText("");
        Button button2 = createButton("Create", event -> {
            try {
                WebSocketClient client = new WebSocketClient(0L, false);
                textCreate.setText(String.valueOf(client.getId()));
                this.game = client.connect(() -> Platform.runLater(() -> stage.setScene(run())));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        create.getChildren().addAll(button2, textCreate);

        type.getChildren().addAll(join, create);
        type.setAlignment(Pos.CENTER);

        root.getChildren().addAll(getSceneWithReturn("Choix 1"),text1,type);

        return new Scene(root, WINDOW_SIZE, WINDOW_SIZE);
    }

    private Scene Joueur(Level level){
        VBox root = getBasicScene();
        Text title = createText("JOUEUR VS IA", true);
        Text text1 = createText("Quel joueur voulez vous jouer ?");
        Button shortbut = createButton("SHORT",event -> handleButtonClick(TypeJeu.SHORT_VS_IA, level));
        Button cutbut = createButton("CUT",event -> handleButtonClick(TypeJeu.CUT_VS_IA, level));

        root.getChildren().addAll(getSceneWithReturn("Choix 2"),title,text1,shortbut,cutbut);
        return new Scene(root, WINDOW_SIZE, WINDOW_SIZE);
    }

    private Scene PvIA(){
        VBox root = getBasicScene();
        Text title = createText("JOUEUR VS IA", true);
        Text text1 = createText("Choisissez la dificulté");
        Button facile = createButton("facile",event -> handleButtonClick(Level.EASY));
        Button normal = createButton("normal",event -> handleButtonClick(Level.MEDIUM));
        Button difficile = createButton("difficile",event -> handleButtonClick(Level.HARD));

        root.getChildren().addAll(getSceneWithReturn("Choix 1"),title,text1,facile,normal,difficile);
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
        if (type == TypeJeu.PLAYER_VS_PLAYER_DISTANT){
            stage.setScene(PvP());
        }
        else if (type == TypeJeu.PLAYER_VS_PLAYER_LOCAL){
            this.game = new Game();
            stage.setScene(run());
        }
        else if (type == TypeJeu.PLAYER_VS_IA){
            stage.setScene(PvIA());
        }
    }

    public void handleButtonClick(Level level){ //fonction qui change de scene lorsqu'on clique les differents boutons
        stage.setScene(Joueur((level)));
    }

    // TODO : Rajouter level lorsqu'il sera mis en place
    public void handleButtonClick(TypeJeu type, Level level){
        if (type == TypeJeu.SHORT_VS_IA){
            this.game = new Game(true, Turn.CUT);
            stage.setScene(run());
            game.play(null, null);
        }
        else if (type == TypeJeu.CUT_VS_IA){
            this.game = new Game(true, Turn.SHORT);
            stage.setScene(run());
        }
        else if (type == TypeJeu.IA_VS_IA){
            stage.setScene(run());
            //TODO : Changer quand IA_VS_IA existe
        }
    }

    public void handleButtonClick(String nomscene){
        if (nomscene.equals("Jeu")){
            PopupMessage();
        }
        else if (nomscene.equals("Choix 1")){
            stage.setScene(home());
        }
        else if (nomscene.equals("Choix 2")) {
            stage.setScene(PvIA());
        }
    }

    public void PopupMessage(){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Vous allez quiter le jeu !");
        alert.setContentText("Etes vous sûr de vouloir quitter ?");


        ButtonType buttonAcept = new ButtonType("Accepter");
        ButtonType buttonCancel = new ButtonType("Annuler");
        alert.getButtonTypes().setAll(buttonAcept,buttonCancel);

        alert.showAndWait().ifPresent(response ->{
            if (response==buttonAcept){
                stage.setScene(home());
            }
        });
    }

    public static void PopupMessage(Turn turn){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fin de la partie");
        if (turn==Turn.CUT){
            alert.setHeaderText("CUT a gagné !");
        }
        else alert.setHeaderText("SHORT a gagné !");
        alert.show();

    }

    public Scene run() {
        Pane pane = new Pane();
        // On définit la taille de notre affichage
        pane.setPrefSize(WINDOW_SIZE, WINDOW_SIZE);
        random = new Random(seed);
        showGraph(pane);
        pane.getChildren().add(getSceneWithReturn("Jeu"));
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
            text.setFont(Font.font("Consolas", FontWeight.BOLD,15));
            text.relocate(coord.getKey() + 16D, coord.getValue() + 16D);
            // Un cercle pour représenter le sommet
            Circle vertex = new Circle(CIRCLE_SIZE,
                    new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1));
            vertex.relocate(coord.getKey(), coord.getValue());
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, text);
        }
    }
}


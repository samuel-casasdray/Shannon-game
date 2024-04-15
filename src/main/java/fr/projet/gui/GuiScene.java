package fr.projet.gui;

import fr.projet.game.Game;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;
import java.util.List;

@Slf4j
@UtilityClass
public class GuiScene {
    @Getter
    private int nbVertices;
    @Getter
    @Setter
    private Level level1;
    @Getter
    @Setter
    private Level level2;
    private static final Random random = new Random();
    @Getter
    @Setter
    private List<Etoile> etoiles = generer(1000);
    private int planZ = 10;
    @Getter
    private static Timeline stars;
    public static List<Etoile> generer(int nb) {
        List<Etoile> lst = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < nb; i++)
        {
            Etoile e = new Etoile();
            e.randomize(rnd);
            lst.add(e);
        }
        return lst;
    }

    public static void draw(Pane root, List<Node> nodes) {
        float width = (float) UtilsGui.WINDOW_WIDTH;
        float height = (float) UtilsGui.WINDOW_HEIGHT;
        for (Etoile e : etoiles) {
            e.setZ(e.getZ() - 1);
            if (e.getZ() < 0) {
                e.setZ(e.getZ()+1000);
            }
        }
        root.getChildren().removeIf(node -> node instanceof Rectangle);
        etoiles.sort(Comparator.comparingDouble(Etoile::getZ).reversed());
        for (Etoile etoile : etoiles.stream().filter(e -> e.getZ() >= planZ).toList()) {
            float x = planZ * etoile.getX() / etoile.getZ() + width/2;
            float y = planZ * etoile.getY() / etoile.getZ() + height/2;
            if (x >= 0 && x <= width && y >= 0 && y <= height) {
                Rectangle pixel;
                Color color = etoile.pixelColor();
                pixel = new Rectangle(x, y, 2,2);
                pixel.setFill(color);
                if (nodes.stream().noneMatch(node -> pixel.intersects(node.getBoundsInParent()))) {
                    root.getChildren().add(pixel);
                }
            }
        }
    }

    public Scene home(HandleClick handleButtonClick) {
        Pane root = getBasicScene();

        Text text1 = UtilsGui.createText("SHANNON GAME",true);
        Text text2 = UtilsGui.createText("Choisissez votre mode de jeu :");

        //création des boutons d'option de jeu
        Button button1 = UtilsGui.createButton("Jouer vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_PVIA));
        Button button2 = UtilsGui.createButton("Joueur vs Joueur Online", event -> handleButtonClick.call(ButtonClickType.HOME_PVPO));
        Button button3 = UtilsGui.createButton("Joueur vs Joueur Local", event -> handleButtonClick.call(ButtonClickType.HOME_PVPL));
        Button button4 = UtilsGui.createButton("IA vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_IAVIA));
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(100);
        text2.setX(UtilsGui.WINDOW_WIDTH/2 - text2.getLayoutBounds().getWidth()/2);
        text2.setY(200);
        button1.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button1.getPrefWidth()/2);
        button1.setLayoutY(300);
        button2.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button2.getPrefWidth()/2);
        button2.setLayoutY(400);
        button3.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button3.getPrefWidth()/2);
        button3.setLayoutY(500);
        button4.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button4.getPrefWidth()/2);
        button4.setLayoutY(600);
        root.getChildren().clear();
        if (stars != null)
            stars.stop();
        GuiScene.setEtoiles(generer(3000));
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(root, List.of(button1, button2, button3, button4, text1, text2));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();
        root.getChildren().addAll(text1, text2, button1, button2, button3, button4);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Pane getBasicScene() {
        //VBox root = new VBox(50); // Espacement vertical entre les éléments
        Pane root = new Pane();
        root.setPadding(new Insets(-40, 0, 10, 0));

        //root.setBackground(getBackground());
        root.setBackground(Background.fill(Color.BLACK));
        //root.setAlignment(Pos.CENTER);
        return root;
    }

    public Background getBackground() {
        String name = "bg.jpg";
        URL ressource = GuiScene.class.getClassLoader().getResource(name);
        if(Objects.isNull(ressource)) {
            log.error("Impossible de recupérer la ressource : " + name);
            return null;
        }
        Image image = new Image(ressource.toExternalForm());
        return new Background(new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(1.0, 1.0, true, true, false, false)));
    }

    public Scene pvp(HandleClick handleButtonClick, JoinCreateField joinField, JoinCreateField createField) {

        Pane scene = getBasicScene();

        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(300, 5, 5, 500));

        Text text1 = UtilsGui.createText("Joueur vs Joueur", true);

        Turn creatorTurn = Turn.CUT;

        TextField textJoin = new TextField();
        textJoin.setPromptText("code de partie");
        Button buttonJoin = UtilsGui.createButton("Rejoindre", event -> joinField.call(textJoin, creatorTurn, 0));

        UtilsGui.addEnterOnText(textJoin, event -> joinField.call(textJoin, creatorTurn, 0));
        Spinner<Integer> nbVertices = new Spinner<>(5,20,20);
        Text TextNbVertices = UtilsGui.createText("Nombre de sommets");
        Text textCreate = UtilsGui.createText(" ");
        //textCreate.setFont(Font.loadFont(UtilsGui.class.getResourceAsStream("/Fonts/Font3.ttf"),30));

        Text textTurn = UtilsGui.createText("Choisir son joueur");
        ComboBox<String> choixTurn = new ComboBox<>();
        choixTurn.getItems().addAll("aléatoire", "CUT", "SHORT");
        choixTurn.getSelectionModel().selectFirst();
        Button buttonCreate = UtilsGui.createButton(
                "Créer",
                event -> {
                    String selectedOption = choixTurn.getSelectionModel().getSelectedItem();
                    Turn turn;
                    if (selectedOption.equals("SHORT")) {
                        turn = Turn.SHORT;
                    } else if (selectedOption.equals("CUT")) {
                        turn = Turn.CUT;
                    } else {
                        turn = random.nextInt(2) == 0 ? Turn.CUT : Turn.SHORT;
                    }
                    int nbSommets;
                    try {
                        nbSommets = nbVertices.getValue();
                    }
                    catch (NumberFormatException e) {
                        //nbVertices.setText("20");
                        nbSommets = 20;
                    }
                    createField.call(textCreate, turn, nbSommets);
                });
        root.add(textJoin, 0, 1);
        root.add(buttonJoin, 0, 0);
        root.add(textCreate, 1, 3);
        root.add(buttonCreate, 1, 0);
        root.add(textTurn, 1, 1);
        root.add(choixTurn, 1, 2);
        root.add(TextNbVertices, 0, 2);
        root.add(nbVertices, 0, 3);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(100);
        stars.stop();
        scene.getChildren().clear();
        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME_PVPO, handleButtonClick), text1, root);
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(scene, List.of(text1, buttonJoin, textJoin, textCreate, buttonCreate, textTurn, choixTurn, TextNbVertices, nbVertices));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();
        return new Scene(scene, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Scene aivsai(HandleClick handleButtonClick) {
        Pane scene = getBasicScene();

        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(300, 5, 5, 500));

        Text text1 = UtilsGui.createText("IA vs IA", true);
        Text textIA1 = UtilsGui.createText("IA CUT");
        ComboBox<String> choixIA1 = new ComboBox<>();
        choixIA1.getItems().addAll("EASY", "MEDIUM", "HARD");
        Text textIA2 = UtilsGui.createText("IA SHORT");
        ComboBox<String> choixIA2 = new ComboBox<>();
        choixIA2.getItems().addAll("EASY", "MEDIUM", "HARD");
        choixIA2.getSelectionModel().select(1);
        choixIA1.getSelectionModel().select(1);
        Spinner<Integer> spinner = new Spinner<>(5, 20, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Text TextNbVertices = UtilsGui.createText("Nombre de sommets");
        Button buttonCreate = UtilsGui.createButton(
                "Lancer",
                event -> {
                    String selectedOptionAI1 = choixIA1.getSelectionModel().getSelectedItem();
                    String selectedOptionAI2 = choixIA2.getSelectionModel().getSelectedItem();
                    level1 = switch (selectedOptionAI1) {
                        case "EASY" -> Level.EASY;
                        case "MEDIUM" -> Level.MEDIUM;
                        case "HARD" -> Level.HARD;
                        default -> throw new IllegalStateException("Unexpected value: " + selectedOptionAI1);
                    };
                    level2 = switch (selectedOptionAI2) {
                        case "EASY" -> Level.EASY;
                        case "MEDIUM" -> Level.MEDIUM;
                        case "HARD" -> Level.HARD;
                        default -> throw new IllegalStateException("Unexpected value: " + selectedOptionAI2);
                    };
                    int nbSommets;
                    try {
                        nbSommets = spinner.getValue();
                    }
                    catch (NumberFormatException e) {
                        nbSommets = 20;
                    }
                    nbVertices = nbSommets;
                    handleButtonClick.call(ButtonClickType.AIvsAI);
                });

        root.add(textIA1, 1, 0);
        root.add(textIA2, 2, 0);
        root.add(choixIA1, 1, 1);
        root.add(choixIA2, 2, 1);
        root.add(buttonCreate, 1, 3);
        root.add(TextNbVertices, 2, 2);
        root.add(spinner, 2, 3);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(100);
        stars.stop();
        scene.getChildren().clear();
        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), text1, root);
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(scene, List.of(text1, textIA1, textIA2, choixIA1, choixIA2, buttonCreate, TextNbVertices, spinner));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();


        return new Scene(scene, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene joueur(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Quel joueur voulez vous jouer ?");
        Button shortbut = UtilsGui.createButton("SHORT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_SHORT));
        Button cutbut = UtilsGui.createButton("CUT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_CUT));
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(200);
        shortbut.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - shortbut.getPrefWidth()/2);
        shortbut.setLayoutY(300);
        cutbut.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - cutbut.getPrefWidth()/2);
        cutbut.setLayoutY(400);
        stars.stop();
        root.getChildren().clear();
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME_PVIA, handleButtonClick), title, text1, shortbut, cutbut);
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(root, List.of(title, text1, shortbut, cutbut));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene pvia(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Choisissez la difficulte");
        Button facile = UtilsGui.createButton("facile", event -> handleButtonClick.call(ButtonClickType.PVIA_EASY));
        Button normal = UtilsGui.createButton("normale", event -> handleButtonClick.call(ButtonClickType.PVIA_MEDIUM));
        Button difficile = UtilsGui.createButton("difficile", event -> handleButtonClick.call(ButtonClickType.PVIA_HARD));
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(200);
        facile.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - facile.getPrefWidth()/2);
        facile.setLayoutY(300);
        normal.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - normal.getPrefWidth()/2);
        normal.setLayoutY(400);
        difficile.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - difficile.getPrefWidth()/2);
        difficile.setLayoutY(500);
        stars.stop();
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), title, text1, facile, normal, difficile);
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(root, List.of(title, text1, facile, normal, difficile));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene nbVertices(HandleClick handleButtonClick, boolean IA) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("Choisissez le nombre de \n sommets de votre graphe");
        Spinner<Integer> spinner = new Spinner<>(5, 20, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Button enter = UtilsGui.createButton("Confirmer",e -> {
            nbVertices = spinner.getValue();
            handleButtonClick.call(ButtonClickType.VERTICES);

        });
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        spinner.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - spinner.getPrefWidth()/2);
        spinner.setLayoutY(200);
        enter.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - enter.getPrefWidth()/2);
        enter.setLayoutY(300);
        stars.stop();
        stars = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            draw(root, List.of(title, spinner, enter));
        }));
        stars.setCycleCount(Timeline.INDEFINITE);
        stars.play();
        if (IA) root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.JOUEUR,handleButtonClick),title, spinner,enter);
        else root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME,handleButtonClick),title, spinner,enter);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

}

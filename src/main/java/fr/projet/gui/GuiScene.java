package fr.projet.gui;

import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.ia.BasicAI;
import fr.projet.ia.Minimax;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Objects;
import java.util.Random;

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

    public VBox getBasicScene() {
        VBox root = new VBox(50); // Espacement vertical entre les éléments
        root.setPadding(new Insets(-40, 0, 10, 0));

        root.setBackground(getBackground());
        root.setAlignment(Pos.CENTER);

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

    public Scene home(HandleClick handleButtonClick) {
        VBox root = getBasicScene();

        Text text1 = UtilsGui.createText("SHANNON GAME", true);
        Text text2 = UtilsGui.createText("Choisissez votre mode de jeu :");

        //création des boutons d'option de jeu
        Button button1 = UtilsGui.createButton("Jouer vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_PVIA));
        Button button2 = UtilsGui.createButton("Joueur vs Joueur Online", event -> handleButtonClick.call(ButtonClickType.HOME_PVPO));
        Button button3 = UtilsGui.createButton("Joueur vs Joueur Local", event -> handleButtonClick.call(ButtonClickType.HOME_PVPL));
        Button button4 = UtilsGui.createButton("IA vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_IAVIA));

        root.getChildren().addAll(text1, text2, button1, button2, button3, button4);

        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene pvp(HandleClick handleButtonClick, JoinCreateField joinField, JoinCreateField createField) {

        VBox scene = getBasicScene();

        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));

        Text text1 = UtilsGui.createText("Joueur vs Joueur", true);

        Turn creatorTurn = Turn.CUT;

        TextField textJoin = new TextField();
        textJoin.setPromptText("code de partie");
        Button buttonJoin = UtilsGui.createButton("Rejoindre", event -> joinField.call(textJoin, creatorTurn, 0));

        UtilsGui.addEnterOnText(textJoin, event -> joinField.call(textJoin, creatorTurn, 0));
        TextField nbVertices = new TextField();
        Text TextNbVertices = UtilsGui.createText("Nombre de sommets");
        nbVertices.setText("20");
        Text textCreate = UtilsGui.createText(" ");

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
                        nbSommets = Integer.parseInt(nbVertices.getText());
                    }
                    catch (NumberFormatException e) {
                        nbVertices.setText("20");
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

        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), text1, root);


        return new Scene(scene, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene aivsai(HandleClick handleButtonClick) {
        VBox scene = getBasicScene();

        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));

        Text text1 = UtilsGui.createText("IA vs IA", true);
        Text textIA1 = UtilsGui.createText("IA CUT");
        ComboBox<String> choixIA1 = new ComboBox<>();
        choixIA1.getItems().addAll("EASY", "MEDIUM", "HARD");
        Text textIA2 = UtilsGui.createText("IA SHORT");
        ComboBox<String> choixIA2 = new ComboBox<>();
        choixIA2.getItems().addAll("EASY", "MEDIUM", "HARD");
        choixIA2.getSelectionModel().select(1);
        choixIA1.getSelectionModel().select(1);
        TextField nbVerticesField = new TextField();
        Text TextNbVertices = UtilsGui.createText("Nombre de sommets");
        nbVerticesField.setText("20");
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
                        nbSommets = Integer.parseInt(nbVerticesField.getText());
                    }
                    catch (NumberFormatException e) {
                        nbVerticesField.setText("20");
                        nbSommets = 20;
                    }
                    nbVertices = nbSommets;
                    handleButtonClick.call(ButtonClickType.AIvsAI);
                });

        root.add(text1, 0, 1);
        root.add(textIA1, 1, 0);
        root.add(textIA2, 2, 0);
        root.add(choixIA1, 1, 1);
        root.add(choixIA2, 2, 1);
        root.add(buttonCreate, 1, 3);
        root.add(TextNbVertices, 2, 2);
        root.add(nbVerticesField, 2, 3);

        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), text1, root);


        return new Scene(scene, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene joueur(HandleClick handleButtonClick) {
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Quel joueur voulez vous jouer ?");
        Button shortbut = UtilsGui.createButton("SHORT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_SHORT));
        Button cutbut = UtilsGui.createButton("CUT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_CUT));

        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME_PVIA, handleButtonClick), title, text1, shortbut, cutbut);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene pvia(HandleClick handleButtonClick) {
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Choisissez la difficulté");
        Button facile = UtilsGui.createButton("facile", event -> handleButtonClick.call(ButtonClickType.PVIA_EASY));
        Button normal = UtilsGui.createButton("normal", event -> handleButtonClick.call(ButtonClickType.PVIA_MEDIUM));
        Button difficile = UtilsGui.createButton("difficile", event -> handleButtonClick.call(ButtonClickType.PVIA_HARD));

        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), title, text1, facile, normal, difficile);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene nbVertices(HandleClick handleButtonClick, boolean IA) {
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("Choisissez le nombre de \n sommets de votre graphe",true);
        Spinner<Integer> spinner = new Spinner<>(5, 20, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Button enter = UtilsGui.createButton("Confirmer",e -> {
            nbVertices = spinner.getValue();
            handleButtonClick.call(ButtonClickType.VERTICES);

        });
        if (IA) root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.JOUEUR,handleButtonClick),title, spinner,enter);
        else root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME,handleButtonClick),title, spinner,enter);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

}

package fr.projet.gui;

import com.google.gson.JsonElement;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.server.HttpsClient;
import fr.projet.server.WebSocketClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
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

        BackgroundFill backgroundFill = new BackgroundFill(Color.LIGHTGREY, null, null);
        Background background = new Background(backgroundFill);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    public Scene home(HandleClick handleButtonClick) {
        VBox root = getBasicScene();
        String pseudo = WebSocketClient.getPseudoCUT();
        Text pseudoText = UtilsGui.createText("Pseudo : " + pseudo);
        Text elo = UtilsGui.createText("Elo : " + HttpsClient.getElo(WebSocketClient.getPseudoCUT()));
        Text text1 = UtilsGui.createText("SHANNON GAME", true);
        Text text2 = UtilsGui.createText("Choisissez votre mode de jeu :");

        //création des boutons d'option de jeu
        Button button1 = UtilsGui.createButton("Jouer vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_PVIA));
        Button button2 = UtilsGui.createButton("Joueur vs Joueur Online", event -> handleButtonClick.call(ButtonClickType.HOME_PVPO));
        Button button3 = UtilsGui.createButton("Joueur vs Joueur Local", event -> handleButtonClick.call(ButtonClickType.HOME_PVPL));
        Button button4 = UtilsGui.createButton("IA vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_IAVIA));
        Button button5 = UtilsGui.createButton("Mode compétitif", event -> handleButtonClick.call(ButtonClickType.RANKED));
        Button statsButton = new Button("?");
        statsButton.setOnAction(event -> handleButtonClick.call(ButtonClickType.STATS));
        statsButton.setStyle("-fx-background-color: LIGHTGREY;");
        statsButton.setAlignment(Pos.TOP_RIGHT);
        root.setSpacing(root.getSpacing()/1.5);
        if (pseudo.length() >= 3)
            root.getChildren().addAll(statsButton, text1, text2, button1, button2, button3, button4, button5, pseudoText, elo);
        else
            root.getChildren().addAll(statsButton, text1, text2, button1, button2, button3, button4, button5);

        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }
    public Scene pvp(HandleClick handleButtonClick, JoinCreateField joinField, JoinCreateField createField) {

        VBox scene = getBasicScene();

        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));
        String pseudo = WebSocketClient.getPseudoCUT();
        Text pseudoText = UtilsGui.createText("Pseudo : " + pseudo);
        Text elo = UtilsGui.createText("Elo : " + HttpsClient.getElo(WebSocketClient.getPseudoCUT()));
        Text text1 = UtilsGui.createText("Joueur vs Joueur", true);

        Turn creatorTurn = Turn.CUT;

        TextField textJoin = new TextField();
        textJoin.setPromptText("code de partie");
        Button buttonJoin = UtilsGui.createButton("Rejoindre", event -> joinField.call(textJoin));

        UtilsGui.addEnterOnText(textJoin, event -> joinField.call(textJoin));
        TextField nbVertices = new TextField();
        Text code = UtilsGui.createText("");
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
                    WebSocketClient client = null;
                    try {
                        client = new WebSocketClient(nbSommets, turn);
                    } catch (IOException | URISyntaxException e) {
                        log.error("Erreur lors de la création de la partie", e);
                        return;
                    }
                    catch (InterruptedException e) {
                        log.error("Erreur lors de la création de la partie", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                    Optional<Long> gameCode = Optional.of(client.getId());
                    code.setText("Code de la partie: " + StringUtils.rightPad(String.valueOf(gameCode.get()), 4));
                    createField.call(client);
                });
        if (pseudo.length() >= 3)
        {
            root.add(pseudoText, 0, 2);
            root.add(elo, 0, 3);
        }
        root.add(textJoin, 0, 1);
        root.add(buttonJoin, 0, 0);
        root.add(textCreate, 1, 3);
        root.add(buttonCreate, 1, 0);
        root.add(textTurn, 1, 3);
        root.add(choixTurn, 1, 4);
        root.add(TextNbVertices, 1, 1);
        root.add(nbVertices, 1, 2);
        root.add(code, 1, 5);
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
        scene.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                buttonCreate.fire();
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
        Spinner<Integer> spinner = new Spinner<>(5, 50, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Button enter = UtilsGui.createButton("Confirmer",e -> {
            nbVertices = spinner.getValue();
            handleButtonClick.call(ButtonClickType.VERTICES);

        });
        root.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                enter.fire();
        });
        if (IA) root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.JOUEUR,handleButtonClick),title, spinner,enter);
        else root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME,handleButtonClick),title, spinner,enter);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene stats(HandleClick handleButtonClick) {
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("Nombre de parties faites :",false);
        Text cutText = UtilsGui.createText("Nombre de parties gagnées par cut :",false);
        Text shortText = UtilsGui.createText("Nombre de parties gagnées par short :",false);
        Text onlineText = UtilsGui.createText("Nombre de parties en ligne :", false);
        WebSocketClient ws = new WebSocketClient();
        Text response = new Text();
        Text cut = new Text();
        Text shorts = new Text();
        Text online = new Text();
        List<JsonElement> statsList = ws.getStats().asList();
        if (statsList.isEmpty()) {
            Text erreurText = UtilsGui.createText("Vérifiez votre connexion internet");
            root.getChildren().addAll(erreurText, UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick));
            return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
        }
        response.setText(statsList.getFirst().getAsString());
        cut.setText(statsList.get(1).getAsString());
        shorts.setText(statsList.get(2).getAsString());
        online.setText(statsList.get(3).getAsString());
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), title,
                response, cutText, cut, shortText, shorts, onlineText, online);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene ranked(HandleClick handleButtonClick) {
        VBox root = getBasicScene();
        Button button1 = UtilsGui.createButton("Se connecter", event -> handleButtonClick.call(ButtonClickType.LOGIN));
        Button button2 = UtilsGui.createButton("S'inscrire", event -> handleButtonClick.call(ButtonClickType.REGISTER));
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), button1, button2);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene login(HandleClick handleButtonClick) {
        VBox scene = getBasicScene();
        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));
        Text title = UtilsGui.createText("Login",true);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        Button login = UtilsGui.createButton("Se connecter", event -> {
            if (HttpsClient.login(username.getText(), password.getText()).getKey()) {
                WebSocketClient.setPseudoCUT(username.getText());
                WebSocketClient.setPseudoSHORT(username.getText());
                handleButtonClick.call(ButtonClickType.HOME_PVPO);
            }
            else {
                Text error = UtilsGui.createText("Pseudo ou mot de passe incorrect",false);
                error.setFill(Color.RED);
                root.add(error, 0, 4);
                password.clear();
            }
        });
        root.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                login.fire();
        });
        username.setPromptText("Pseudo");
        password.setPromptText("Mot de passe");
        root.add(title, 0, 0);
        root.add(username, 0, 1);
        root.add(password, 0, 2);
        root.add(login, 0, 3);
        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.RANKED, handleButtonClick), root);
        return new Scene(scene, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene register(HandleClick handleButtonClick) {
        VBox scene = getBasicScene();
        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));
        Text title = UtilsGui.createText("Inscription",true);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        PasswordField passwordRepeat = new PasswordField();
        Button register = UtilsGui.createButton("S'inscrire", event -> {
            var response = HttpsClient.register(username.getText(), password.getText(), passwordRepeat.getText());
            if (response.getKey()) {
                WebSocketClient.setPseudoCUT(username.getText());
                WebSocketClient.setPseudoSHORT(username.getText());
                handleButtonClick.call(ButtonClickType.HOME_PVPO);
            }
            else {
                Text error = UtilsGui.createText(response.getValue(),false);
                error.setFill(Color.RED);
                root.add(error, 0, 5);
                password.clear();
                passwordRepeat.clear();
            }
        });
        root.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                register.fire();
        });
        username.setPromptText("Pseudo");
        password.setPromptText("Mot de passe");
        passwordRepeat.setPromptText("Répétez le mot de passe");
        root.add(title, 0, 0);
        root.add(username, 0, 1);
        root.add(password, 0, 2);
        root.add(passwordRepeat, 0, 3);
        root.add(register, 0, 4);
        scene.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.RANKED, handleButtonClick), root);
        return new Scene(scene, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }
}

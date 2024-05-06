package fr.projet.gui;

import com.google.gson.JsonElement;
import fr.projet.game.Level;
import fr.projet.game.Turn;
import fr.projet.server.HttpsClient;
import fr.projet.server.WebSocketClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;
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
import java.util.concurrent.atomic.AtomicReference;
import java.net.URL;
import java.util.*;

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
    private int NB_STARS = 4000;
    @Getter
    private int MIN_STARS = 1000;
    @Getter
    private int MAX_STARS = 8000;
    @Getter
    @Setter
    private Slider slider = new Slider(0, 1, 0.5);
    @Getter
    private double VOLUME = 0.5;
    @Getter
    private double MIN_VOLUME = 0.0;
    @Getter
    private double MAX_VOLUME = 1.5;
    @Getter
    @Setter
    private Slider slider2 = new Slider(0, 1.5, 0.5);

    public Scene home(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        String pseudo = WebSocketClient.getPseudoCUT();
        Text pseudoText = UtilsGui.createText("Pseudo : " + pseudo);
        int eloPlayer = -1;
        if (pseudo.length() >= 3) {
            eloPlayer = HttpsClient.getElo(WebSocketClient.getPseudoCUT());
            if (eloPlayer < 0) {
                pseudo = WebSocketClient.getPseudoCUT();
            }
        }
        Text elo = UtilsGui.createText("Elo : " + eloPlayer);
        Text text1 = UtilsGui.createText("SHANNON GAME", true);
        Text text2 = UtilsGui.createText("Choisissez votre mode de jeu :");
        slider.setLayoutX(100);
        slider.setLayoutY(0);
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
        slider2.setLayoutX(300);
        slider2.setLayoutY(0);
        //création des boutons d'option de jeu
        Button button1 = UtilsGui.createButton("Jouer vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_PVIA), false);
        Button button2 = UtilsGui.createButton("Joueur vs Joueur Online", event -> handleButtonClick.call(ButtonClickType.HOME_PVPO), false);
        Button button3 = UtilsGui.createButton("Joueur vs Joueur Local", event -> handleButtonClick.call(ButtonClickType.HOME_PVPL), false);
        Button button4 = UtilsGui.createButton("IA vs IA", event -> handleButtonClick.call(ButtonClickType.HOME_IAVIA), false);
        Button button5 = UtilsGui.createButton("Mode compétitif", event -> handleButtonClick.call(ButtonClickType.RANKED), false);
        Button statsButton = new Button("?");
        statsButton.setTextFill(Color.RED);
        Button deconnexion = new Button("Déconnexion");
        deconnexion.setOnAction(event -> {
            WebSocketClient.setPseudoCUT("A");
            WebSocketClient.setPseudoSHORT("B");
            Platform.runLater(() -> {
                root.getChildren().removeAll(deconnexion, pseudoText, elo);
                root.getChildren().add(button5);
            });
        });
        statsButton.setOnAction(event -> handleButtonClick.call(ButtonClickType.STATS));
        statsButton.setStyle("-fx-background-color: transparent;");
        statsButton.setAlignment(Pos.TOP_RIGHT);
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
        button5.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button4.getPrefWidth()/2);
        button5.setLayoutY(700);
        pseudoText.setX(UtilsGui.WINDOW_WIDTH/2 - pseudoText.getLayoutBounds().getWidth()/2);
        pseudoText.setY(700);
        elo.setX(UtilsGui.WINDOW_WIDTH/2 - elo.getLayoutBounds().getWidth()/2);
        elo.setY(750);
        deconnexion.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - deconnexion.getPrefWidth()/2);
        deconnexion.setLayoutY(800);
        if (pseudo.length() >= 3)
            root.getChildren().addAll(statsButton, text1, text2, button1, button2, button3, button4, pseudoText, elo, deconnexion, slider, slider2);
        else
            root.getChildren().addAll(statsButton, text1, text2, button1, button2, button3, button4, button5, slider, slider2);
        slider.valueProperty().addListener(event -> {
            double sliderValue = slider.getValue();
            NB_STARS = (int) ((1-sliderValue)*MIN_STARS+MAX_STARS*sliderValue);
            Gui.createRemoveStars(NB_STARS);
        });
        slider2.valueProperty().addListener(event -> {
            double sliderValue = slider2.getValue();
            VOLUME = (1-sliderValue)*MIN_VOLUME+MAX_VOLUME*sliderValue;
            Gui.changeVolume(VOLUME);
        });
        if (Gui.getStars() != null)
            Gui.getStars().stop();
        if (Gui.getEtoiles().isEmpty())
            Gui.setEtoiles(Gui.generer(200));
        new Thread(() -> {
            Gui.setStars(new Timeline(new KeyFrame(Duration.millis(20), e ->
            {
                Gui.draw(root);
                if (Gui.getEtoiles().size() < NB_STARS) {
                    Gui.getEtoiles().addAll(Gui.generer(100));
                }
            })));
            Gui.getStars().setCycleCount(Animation.INDEFINITE);
            Gui.getStars().play();
        }).start();
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public void createTimeLineThread(Pane root) {
        new Thread(() -> {
            Gui.setStars(new Timeline(new KeyFrame(Duration.millis(20), e -> {
                Gui.draw(root);
            })));
            Gui.getStars().setCycleCount(Animation.INDEFINITE);
            Gui.getStars().play();
        }).start();
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
        String pseudo = WebSocketClient.getPseudoCUT();
        Text pseudoText = UtilsGui.createText("Pseudo : " + pseudo);
        int eloPlayer = -1;
        if (pseudo.length() >= 3) {
            eloPlayer = HttpsClient.getElo(WebSocketClient.getPseudoCUT());
            if (eloPlayer < 0) {
                return new Scene(new Pane(), UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
            }
        }
        Text elo = UtilsGui.createText("Elo : " + eloPlayer);
        Text text1 = UtilsGui.createText("Joueur vs Joueur", true);

        TextField textJoin = new TextField();
        textJoin.setPromptText("code de partie");
        Button buttonJoin = UtilsGui.createButton("Rejoindre", event -> joinField.call(textJoin), false);

        UtilsGui.addEnterOnText(textJoin, event -> joinField.call(textJoin));
        UtilsGui.addEnterOnText(textJoin, event -> joinField.call(textJoin));
        Spinner<Integer> nbVertices = new Spinner<>(5,40,20);
        Text code = UtilsGui.createText("");
        Text textNbVertices = UtilsGui.createText("Nombre de sommets");
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
                        nbSommets = 20;
                    }
                    WebSocketClient client = null;
                    AtomicReference<WebSocketClient> finalClient = new AtomicReference<>(client);
                    try {
                        int finalNbSommets = nbSommets;
                        new Thread(() -> {
                            try {
                                finalClient.set(new WebSocketClient(finalNbSommets, turn));
                                Optional<Long> gameCode = Optional.of(finalClient.get().getId());
                                Gui.setGameCode(gameCode);
                                code.setText("Code de la partie: " + StringUtils.rightPad(String.valueOf(gameCode.get()), 4));
                                code.setFill(Color.WHITE);
                                createField.call(finalClient.get());
                            } catch (IOException e) {
                                code.setText("Vérifiez votre connexion internet");
                                code.setFill(Color.RED);
                            }
                            catch (URISyntaxException e) {
                                log.error("Erreur lors de la création de la partie", e);
                            }
                            catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();

                    } catch (Exception ignored) {}
                }, false);
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
        root.add(textNbVertices, 1, 1);
        root.add(nbVertices, 1, 2);
        root.add(code, 1, 5);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(100);
        scene.getChildren().addAll(text1, root, UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick));
        Gui.getStars().stop();
        createTimeLineThread(scene);
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
        Spinner<Integer> spinner = new Spinner<>(5, 40, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Text textNbVertices = UtilsGui.createText("Nombre de sommets");
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
                }, false);
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
        root.add(textNbVertices, 2, 2);
        root.add(spinner, 2, 3);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(100);
        scene.getChildren().addAll(text1, root, UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick));
        Gui.getStars().stop();
        createTimeLineThread(scene);
        return new Scene(scene, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Scene joueur(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Quel joueur voulez vous jouer ?");
        Button shortbut = UtilsGui.createButton("SHORT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_SHORT), false);
        Button cutbut = UtilsGui.createButton("CUT", event -> handleButtonClick.call(ButtonClickType.JOUEUR_CUT), false);
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        text1.setX(UtilsGui.WINDOW_WIDTH/2 - text1.getLayoutBounds().getWidth()/2);
        text1.setY(200);
        shortbut.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - shortbut.getPrefWidth()/2);
        shortbut.setLayoutY(300);
        cutbut.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - cutbut.getPrefWidth()/2);
        cutbut.setLayoutY(400);
        Gui.getStars().stop();
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME_PVIA, handleButtonClick), title, text1, shortbut, cutbut);
        Gui.setStars(new Timeline(new KeyFrame(Duration.millis(20), e ->
                Gui.draw(root))));
        Gui.getStars().setCycleCount(Animation.INDEFINITE);
        Gui.getStars().play();
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene pvia(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Choisissez la difficulte");
        Button facile = UtilsGui.createButton("facile", event -> handleButtonClick.call(ButtonClickType.PVIA_EASY), false);
        Button normal = UtilsGui.createButton("normale", event -> handleButtonClick.call(ButtonClickType.PVIA_MEDIUM), false);
        Button difficile = UtilsGui.createButton("difficile", event -> handleButtonClick.call(ButtonClickType.PVIA_HARD), false);
        Button strat = UtilsGui.createButton("Stratégie Gagnante", event -> handleButtonClick.call(ButtonClickType.STRAT_WIN), false);
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
        strat.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - strat.getPrefWidth()/2);
        strat.setLayoutY(600);
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), title, text1, facile, normal, difficile, strat);
        Gui.getStars().stop();
        createTimeLineThread(root);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene nbVertices(HandleClick handleButtonClick, boolean IA) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("Choisissez le nombre de \n sommets de votre graphe",true);
        Spinner<Integer> spinner = new Spinner<>(5, 40, 20);
        spinner.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        spinner.setEditable(true);
        Button enter = UtilsGui.createButton("Confirmer",e -> {
            nbVertices = spinner.getValue();
            handleButtonClick.call(ButtonClickType.VERTICES);

        }, false);
        root.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                enter.fire();
        });
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        spinner.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - spinner.getPrefWidth()/2);
        spinner.setLayoutY(200);
        enter.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - enter.getPrefWidth()/2);
        enter.setLayoutY(300);
        if (IA) root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.JOUEUR,handleButtonClick),title, spinner,enter);
        else root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME,handleButtonClick),title, spinner,enter);
        Gui.getStars().stop();
        createTimeLineThread(root);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);

    }

    public Scene stats(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Text title = UtilsGui.createText("Nombre de parties faites :",false);
        Text cutText = UtilsGui.createText("Nombre de parties gagnées par cut :",false);
        Text shortText = UtilsGui.createText("Nombre de parties gagnées par short :",false);
        Text onlineText = UtilsGui.createText("Nombre de parties en ligne :", false);
        Text response = new Text();
        Text cut = new Text();
        Text shorts = new Text();
        Text online = new Text();
        List<JsonElement> statsList = HttpsClient.getStats().asList();
        if (statsList.isEmpty()) {
            Text erreurText = UtilsGui.createText("Vérifiez votre connexion internet");
            erreurText.setFill(Color.RED);
            erreurText.setX(UtilsGui.WINDOW_WIDTH/2 - erreurText.getLayoutBounds().getWidth()/2);
            erreurText.setY(150);
            root.getChildren().addAll(erreurText, UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick));
            Gui.getStars().stop();
            createTimeLineThread(root);
            return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
        }
        response.setText(statsList.getFirst().getAsString());
        cut.setText(statsList.get(1).getAsString());
        shorts.setText(statsList.get(2).getAsString());
        shorts.setFill(Color.RED);
        online.setText(statsList.get(3).getAsString());
        response.setX(UtilsGui.WINDOW_WIDTH/2 - response.getLayoutBounds().getWidth()/2);
        response.setY(150);
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        cutText.setX(UtilsGui.WINDOW_WIDTH/2 - cutText.getLayoutBounds().getWidth()/2);
        cutText.setY(200);
        cut.setFill(Color.RED);
        cut.setX(UtilsGui.WINDOW_WIDTH/2 - cut.getLayoutBounds().getWidth()/2);
        cut.setY(250);
        shortText.setX(UtilsGui.WINDOW_WIDTH/2 - shortText.getLayoutBounds().getWidth()/2);
        shortText.setY(300);
        shorts.setX(UtilsGui.WINDOW_WIDTH/2 - shorts.getLayoutBounds().getWidth()/2);
        shorts.setY(350);
        onlineText.setX(UtilsGui.WINDOW_WIDTH/2 - onlineText.getLayoutBounds().getWidth()/2);
        onlineText.setY(400);
        online.setX(UtilsGui.WINDOW_WIDTH/2 - online.getLayoutBounds().getWidth()/2);
        online.setY(450);
        online.setFill(Color.RED);
        response.setFill(Color.RED);
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), title,
                response, cutText, cut, shortText, shorts, onlineText, online);
        Gui.getStars().stop();
        createTimeLineThread(root);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Scene ranked(HandleClick handleButtonClick) {
        Pane root = getBasicScene();
        Button button1 = UtilsGui.createButton("Se connecter", event -> handleButtonClick.call(ButtonClickType.LOGIN), false);
        Button button2 = UtilsGui.createButton("S'inscrire", event -> handleButtonClick.call(ButtonClickType.REGISTER), false);
        button1.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button1.getPrefWidth()/2);
        button1.setLayoutY(300);
        button2.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - button2.getPrefWidth()/2);
        button2.setLayoutY(400);
        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick), button1, button2);
        Gui.getStars().stop();
        createTimeLineThread(root);
        return new Scene(root, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Scene login(HandleClick handleButtonClick) {
        Pane scene = getBasicScene();
        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));
        Text title = UtilsGui.createText("Login",true);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        AtomicReference<Text> error = new AtomicReference<>(UtilsGui.createText("", false));
        Button login = UtilsGui.createButton("Se connecter", event -> {
            new Thread(() -> {
                var response = HttpsClient.login(username.getText(), password.getText());
                if (response.getKey()) {
                    WebSocketClient.setPseudoCUT(username.getText());
                    WebSocketClient.setPseudoSHORT(username.getText());
                    Platform.runLater(() -> handleButtonClick.call(ButtonClickType.HOME));
                }
                else {
                    Platform.runLater(() -> {
                        error.get().setText("");
                        error.set(UtilsGui.createText(response.getValue(), false));
                        error.get().setFill(Color.RED);
                        root.add(error.get(), 6, 16);
                        password.clear();
                    });
                }
            }).start();
        }, false);
        scene.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                login.fire();
        });
        username.setPromptText("Pseudo");
        password.setPromptText("Mot de passe");
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        username.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - username.getPrefWidth()/2);
        username.setLayoutY(200);
        password.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - password.getPrefWidth()/2);
        password.setLayoutY(250);
        login.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - login.getPrefWidth()/2);
        login.setLayoutY(300);
        scene.getChildren().addAll(root, title, username, password, login, UtilsGui.getReturnButton(ButtonClickType.RANKED, handleButtonClick));
        Gui.getStars().stop();
        createTimeLineThread(scene);
        return new Scene(scene, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }

    public Scene register(HandleClick handleButtonClick) {
        Pane scene = getBasicScene();
        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(90);
        root.setVgap(25);
        root.setPadding(new Insets(30, 5, 5, 5));
        Text title = UtilsGui.createText("Inscription",true);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        PasswordField passwordRepeat = new PasswordField();
        AtomicReference<Text> error = new AtomicReference<>(UtilsGui.createText("", false));
        Button register = UtilsGui.createButton("S'inscrire", event -> {
            new Thread(() -> {
                if (username.getText().contains(" ") || password.getText().contains(" ") || passwordRepeat.getText().contains(" ")) {
                    Platform.runLater(() -> {
                        error.get().setText("");
                        error.set(UtilsGui.createText("Les espaces ne sont pas autorisés", false));
                        error.get().setFill(Color.RED);
                        root.add(error.get(), 6, 16);
                    });
                    return;
                }
                var response = HttpsClient.register(username.getText(), password.getText(), passwordRepeat.getText());
                if (response.getKey()) {
                    WebSocketClient.setPseudoCUT(username.getText());
                    WebSocketClient.setPseudoSHORT(username.getText());
                    Platform.runLater(() -> handleButtonClick.call(ButtonClickType.HOME));
                }
                else {
                    Platform.runLater(() -> {
                        error.get().setText("");
                        error.set(UtilsGui.createText(response.getValue(), false));
                        error.get().setFill(Color.RED);
                        root.add(error.get(), 6, 16);
                        password.clear();
                        passwordRepeat.clear();
                    });
                }
            }).start();
        }, false);
        scene.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                register.fire();
        });
        username.setPromptText("Pseudo");
        password.setPromptText("Mot de passe");
        passwordRepeat.setPromptText("Répétez le mot de passe");
        title.setX(UtilsGui.WINDOW_WIDTH/2 - title.getLayoutBounds().getWidth()/2);
        title.setY(100);
        username.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - username.getPrefWidth()/2);
        username.setLayoutY(200);
        password.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - password.getPrefWidth()/2);
        password.setLayoutY(250);
        passwordRepeat.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - passwordRepeat.getPrefWidth()/2);
        passwordRepeat.setLayoutY(300);
        register.setLayoutX(UtilsGui.WINDOW_WIDTH/2 - register.getPrefWidth()/2);
        register.setLayoutY(350);
        scene.getChildren().addAll(root, title, username, password, passwordRepeat, register, UtilsGui.getReturnButton(ButtonClickType.RANKED, handleButtonClick));
        Gui.getStars().stop();
        createTimeLineThread(scene);
        return new Scene(scene, UtilsGui.WINDOW_WIDTH, UtilsGui.WINDOW_HEIGHT);
    }
}

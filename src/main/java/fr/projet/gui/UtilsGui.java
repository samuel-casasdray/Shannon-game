package fr.projet.gui;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
@Slf4j
public class UtilsGui {

    public static final Font FONT1 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font1.ttf"),50);
    public static final Font FONT2 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font.otf"),35);
    public static final Font FONT3 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font.otf"),25);
    public static final Font FONT4= Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font1.ttf"),100);


    public static final double CIRCLE_SIZE = 20D;
    @Setter
    public static double WINDOW_WIDTH = Screen.getPrimary().getBounds().getWidth();
    @Setter
    public static double WINDOW_HEIGHT = Screen.getPrimary().getBounds().getHeight()-75;
    public static final int WINDOW_MARGE = (int) (0.1*WINDOW_HEIGHT);


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

    public Text createText(String content) { return createText(content, false); }
    public Text createText(String content, boolean withShadow) {
        Text text = new Text(content);
        text.setFill(Color.WHITE);
        if (withShadow) {
            text.setFont(FONT1);
            DropShadow dropShadow = new DropShadow();
            dropShadow.setOffsetX(3.0);
            dropShadow.setOffsetY(3.0);
            dropShadow.setColor(Color.GREY);
            text.setEffect(dropShadow);
        } else {
            text.setFont(FONT2);
        }
        return text;
    }
    public Label createLabel(String content) { return createLabel(content, false); }
    public Label createLabel(String content, boolean withShadow) {
        Label text = new Label(content);
        text.setTextFill(Color.WHITE);
        if (withShadow) {
            text.setFont(FONT1);
            DropShadow dropShadow = new DropShadow();
            dropShadow.setOffsetX(3.0);
            dropShadow.setOffsetY(3.0);
            dropShadow.setColor(Color.GREY);
            text.setEffect(dropShadow);
        } else {
            text.setFont(FONT2);
        }
        return text;
    }
    public void addEnterOnText(TextField text, EventHandler<KeyEvent> action) {
        text.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) action.handle(event);
        });
    }
    public Button createButton(String text, EventHandler<ActionEvent> action, boolean arrow) {
        Button button = new Button(text);
        //button.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        button.setFont(FONT3);
        if (!arrow)
            button.setPrefSize(350,35);
        else
            button.setPrefSize(50,35);
        //effet d'ombre des boutons
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(3.0); // Décalage horizontal pour l'effet 3D
        shadow.setOffsetY(3.0);
        shadow.setColor(Color.DARKGREY);
        button.setEffect(shadow);

        //effet de grossissement lors du survol
        addHoverEffect(button);

        if (action != null)
            button.setOnAction(action);

        return button;
    }

    public Button getReturnButton(ButtonClickType nomscene, HandleClick handleButtonClick){
        URL url = UtilsGui.class.getResource("/fleche-retour-blanc.png");
        ImageView imageView = null;
        if(url == null) {
            log.error("Pas d'icon fleche retour");
        } else {
            Image imageReturn = new Image(url.toExternalForm());
            imageView = new ImageView(imageReturn);
            imageView.setFitWidth(40);
            imageView.setPreserveRatio(true);
        }

        Button returnButton = UtilsGui.createButton(imageView == null ? "<-" : "", event ->
                new Thread(() -> Platform.runLater(() -> handleButtonClick.call(nomscene))).start(), true);
        returnButton.setStyle("-fx-background-color: transparent;");
        returnButton.setMinSize(40, 20);
        returnButton.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        returnButton.setLayoutY(0);
        if(imageView != null)
            returnButton.setGraphic(imageView);
        return returnButton;
    }

    public static void animationTexte (Node node) {
        animationTexte(node, 40, 2);
    }
    public static void animationTexte (Node node, int n, double time) {
        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(time), node);
        translateTransition.setToY(n); // Déplacement de 50 pixels vers le bas
        translateTransition.setCycleCount(Animation.INDEFINITE); // Répéter indéfiniment
        translateTransition.setAutoReverse(true); // Revenir en arrière après chaque itération

        // Démarrer la translation
        translateTransition.play();
    }

    public static void updateOnResize(Pane pane, List<Pair<Node, Number>> childs) {
        pane.heightProperty().addListener((obs, oldVal, newVal) -> setWINDOW_HEIGHT(newVal.floatValue()));

        pane.widthProperty().addListener((obs, oldVal, newVal) -> {
            setWINDOW_WIDTH(newVal.floatValue());
            childs.forEach(child -> {
                if(child.getKey() instanceof Text childT) {
                    childT.setX(WINDOW_WIDTH / 2 + child.getValue().doubleValue());
                } else
                    child.getKey().setLayoutX(WINDOW_WIDTH / 2 + child.getValue().doubleValue());
            });
        });
    }
    public static void updateOnResize(Pane pane) {
        updateOnResize(pane, List.of());
    }
    @SafeVarargs
    public static void updateOnResize(Pane pane, Pair<Node, Number>... childs) {
        updateOnResize(pane, List.of(childs));
    }
    public static void updateOnResize(Pane pane, Pair<Node, Number> child) {
        updateOnResize(pane, List.of(child));
    }
    public static void updateOnResize(Pane pane, Node... childs) {
        updateOnResize(pane, Arrays.stream(childs).map(child -> {
            if (child instanceof Text || child instanceof Label) {
                return new Pair<>(child, (Number)(-child.getLayoutBounds().getWidth() / 2));
            } else if (child instanceof Button childB) {
                return new Pair<>(child, (Number)(-childB.getPrefWidth() / 2));
            } else {
                return new Pair<>(child, (Number)0);
            }
        }).toList());
    }
    public static void updateOnResize(Pane pane, Node child) {
        if (child instanceof Text || child instanceof Label) {
            updateOnResize(pane, new Pair<>(child, (Number) (-child.getLayoutBounds().getWidth() / 2)));
        } else if (child instanceof Button childB) {
            updateOnResize(pane, new Pair<>(child, (Number) (-childB.getPrefWidth() / 2)));
        } else {
            updateOnResize(pane, new Pair<>(child, (Number)0));
        }
    }

}

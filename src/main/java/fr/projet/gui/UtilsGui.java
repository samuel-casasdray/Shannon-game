package fr.projet.gui;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

@UtilityClass
@Slf4j
public class UtilsGui {

    public static final Font FONT1 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font1.ttf"),50);
    public static final Font FONT2 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font.otf"),35);
    public static final Font FONT3 = Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font.otf"),25);
    public static final Font FONT4= Font.loadFont(UtilsGui.class.getResourceAsStream("/fonts/Font1.ttf"),100);


    public static final double CIRCLE_SIZE = 20D;
    public static final double WINDOW_WIDTH = Screen.getPrimary().getBounds().getWidth();
    public static final double WINDOW_HEIGHT = Screen.getPrimary().getBounds().getHeight()-100;
    public static final int WINDOW_MARGE = 100;


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
    public void addEnterOnText(TextField text, EventHandler<KeyEvent> action) {
        text.setOnKeyPressed(event -> {
            if (event.getCode().getName().equals("Enter")) action.handle(event);
        });
    }
    public Button createButton(String text, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        //button.setStyle("-fx-background-color: #00A4B4; -fx-text-fill: white;");
        button.setFont(FONT3);
        button.setPrefSize(350,35);

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

        Button returnButton = UtilsGui.createButton(imageView == null ? "<-" : "", event -> handleButtonClick.call(nomscene));
        returnButton.setStyle("-fx-background-color: transparent;");
        returnButton.setLayoutX(10);
        returnButton.setLayoutY(0);
        returnButton.setMinSize(40, 20);
        if(imageView != null)
            returnButton.setGraphic(imageView);

        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(5.0); // Décalage horizontal pour l'effet 3D
        shadow.setOffsetY(5.0);
        shadow.setColor(Color.GRAY);
        returnButton.setEffect(shadow);
        GuiScene.getStars().stop();
        return returnButton;
    }

}

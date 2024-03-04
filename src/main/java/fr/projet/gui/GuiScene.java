package fr.projet.gui;

import fr.projet.game.Turn;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class GuiScene {
    private VBox getBasicScene() {
        VBox root = new VBox(50); // Espacement vertical entre les éléments
        root.setPadding(new Insets(-40,0,10,0));

        BackgroundFill backgroundFill = new BackgroundFill(Color.LIGHTGREY, null, null);
        Background background = new Background(backgroundFill);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);

        return root;
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

        root.getChildren().addAll(text1,text2,button1,button2,button3,button4);

        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene pvp(HandleClick handleButtonClick, JoinCreateField joinField, JoinCreateField createField) {
        VBox root = getBasicScene();

        Text text1 = UtilsGui.createText("Player vs Player", true);

        HBox type = new HBox(60);

        VBox join = new VBox(30);
        TextField textJoin = new TextField();
        Turn creatorTurn = Turn.CUT;
        Button button1 = UtilsGui.createButton("Join", event -> joinField.call(textJoin, creatorTurn));
        join.getChildren().addAll(button1, textJoin);

        VBox create = new VBox(30);
        Text textCreate = UtilsGui.createText("");
        Button button2 = UtilsGui.createButton("Create", event -> createField.call(textCreate, null));
        create.getChildren().addAll(button2, textCreate);

        type.getChildren().addAll(join, create);
        type.setAlignment(Pos.CENTER);

        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick),text1,type);

        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene joueur(HandleClick handleButtonClick){
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Quel joueur voulez vous jouer ?");
        Button shortbut = UtilsGui.createButton("SHORT",event -> handleButtonClick.call(ButtonClickType.JOUEUR_SHORT));
        Button cutbut = UtilsGui.createButton("CUT",event -> handleButtonClick.call(ButtonClickType.JOUEUR_CUT));

        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME_PVIA, handleButtonClick),title,text1,shortbut,cutbut);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }

    public Scene pvia(HandleClick handleButtonClick){
        VBox root = getBasicScene();
        Text title = UtilsGui.createText("JOUEUR VS IA", true);
        Text text1 = UtilsGui.createText("Choisissez la dificulté");
        Button facile = UtilsGui.createButton("facile",event -> handleButtonClick.call(ButtonClickType.PVIA_EASY));
        Button normal = UtilsGui.createButton("normal",event -> handleButtonClick.call(ButtonClickType.PVIA_MEDIUM));
        Button difficile = UtilsGui.createButton("difficile",event -> handleButtonClick.call(ButtonClickType.PVIA_HARD));

        root.getChildren().addAll(UtilsGui.getReturnButton(ButtonClickType.HOME, handleButtonClick),title,text1,facile,normal,difficile);
        return new Scene(root, UtilsGui.WINDOW_SIZE, UtilsGui.WINDOW_SIZE);
    }
}

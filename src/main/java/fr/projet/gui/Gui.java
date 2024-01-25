package fr.projet.gui;

import fr.projet.IA.InterfaceIA;
import fr.projet.game.Game;
import fr.projet.graph.Graph;
import fr.projet.graph.Vertex;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gui extends Application {

    public static final double CIRCLE_SIZE = 20D;
    public static final int WINDOW_SIZE = 800;
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
    private final Random random = new Random();

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(run()));
        stage.show();
    }

    public Pane run() {
        Pane pane = new Pane();
        // On définit la taille de notre affichage
        pane.setPrefSize(WINDOW_SIZE, WINDOW_SIZE);
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

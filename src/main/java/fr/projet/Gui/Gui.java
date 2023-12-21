package fr.projet.Gui;

import fr.projet.Graph;
import fr.projet.Vertex;
import javafx.application.Application;
import javafx.scene.Scene;
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

    @Getter
    @Setter
    private static Graph graph = null;

    private final Random random = new Random();


    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(run()));
        stage.show();
    }

    public Pane run() {
        Pane pane = new Pane();
        // On définit la taille de notre affichage
        pane.setPrefSize(800, 800);
        if (graph == null) graph = new Graph();
        showGraph(pane);
        return pane;
    }

    public void showGraph(Pane pane) {

        // Ajout des aretes sur l'affichage
        List<Pair<Vertex, Vertex>> neighbors = new ArrayList<>();
        for (Vertex vertex : graph.getVertices()) {
            for (Vertex vertex1 : vertex.getListNeighbors()) {
                Pair<Vertex, Vertex> pair = new Pair<>(vertex, vertex1);
                if (neighbors.stream().noneMatch(neighbor -> Vertex.isSameCouple(neighbor, pair))) {
                    Line line = new Line(vertex.getCoords().getKey() + 20,
                            vertex.getCoords().getValue() + 20,
                            vertex1.getCoords().getKey() + 20,
                            vertex1.getCoords().getValue() + 20);
                    // Ajout de la ligne sur sur l'affichage
                    pane.getChildren().add(line);
                    neighbors.add(pair);
                }
            }
        }

        // Ajout des sommets sur l'affichage
        for (int i = 0; i < graph.getNbVertices(); i++) {
            Pair<Integer, Integer> coord = graph.getVertices().get(i).getCoords();
            // On crée un texte pour le numéro du sommet
            Text text = new Text(String.valueOf(i + 1));
            text.setBoundsType(TextBoundsType.VISUAL);
            text.relocate(coord.getKey() + 18, coord.getValue() + 18);
            // Un cercle pour représenter le sommet
            Circle vertex = new Circle(20, new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1));
            vertex.relocate(coord.getKey(), coord.getValue());
            // On ajoute les 2 élements sur l'affichage
            pane.getChildren().addAll(vertex, text);
        }
    }
}

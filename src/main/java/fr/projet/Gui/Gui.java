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

import java.util.Random;

public class Gui extends Application {

    @Getter
    @Setter
    private static int nbVertex = 5; // nombre de vertex

    @Getter
    @Setter
    private static boolean aroundCircle = true;

    @Getter
    @Setter
    private static double proba = 0.5;

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
        showGraph(nbVertex, pane);
        return pane;
    }

    public void showGraph(int vertices, Pane pane) {
        Graph g = new Graph();
        // Instantiation des N (nbVextex) sommets et de leur coordonnées.
        //List<Pair<Integer, Integer>> coords = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            Pair<Integer, Integer> coord;
            if (aroundCircle) { // Coord autour d'un cercle
                double toRad = (i * 360f / vertices) * (Math.PI / 180);
                coord = new Pair<>((int) (Math.cos(toRad) * 300 + 400), (int) (Math.sin(toRad) * 300 + 400));
            } else // Coord aléatoire
                coord = new Pair<>(random.nextInt(0, 780), random.nextInt(0, 780));
            // On ajoute le sommet au graphe
            g.addVertex(new Vertex(false, false, coord.getKey(), coord.getValue()));
        }

        // Instantiation des aretes selon une probabilité (proba) entre 2 sommets
        //List<Pair<Integer, Integer>> edges = new ArrayList<>();
        for (int i = 0; i < nbVertex; i++) {
            for (int j = i + 1; j < nbVertex; j++) {
                float p = random.nextFloat();
                if (p > proba) {
                    g.getVertices().get(i).addNeighborVertex(g.getVertices().get(j));
                    g.getVertices().get(j).addNeighborVertex(g.getVertices().get(i));

                    // Création de l'arete graphiquement
                    Line line = new Line(g.getVertices().get(i).getCoords().getKey()+20,
                            g.getVertices().get(i).getCoords().getValue()+20,
                            g.getVertices().get(j).getCoords().getKey()+20,
                            g.getVertices().get(j).getCoords().getValue()+20);
                    // Ajout de la ligne sur sur l'affichage
                    pane.getChildren().add(line);
                }
            }
        }

        // Ajout des sommets sur le graph
        for (int i = 0; i < nbVertex; i++) {
            Pair<Integer, Integer> coord = g.getVertices().get(i).getCoords();
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

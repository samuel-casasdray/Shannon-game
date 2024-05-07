package fr.projet;

import fr.projet.gui.Gui;
import fr.projet.gui.GuiScene;
import javafx.application.Application;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Application.launch(Gui.class);
        try {
            File file = new File("config.txt");
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(String.valueOf(GuiScene.getSlider2().getValue()));
            bufferedWriter.newLine();
            bufferedWriter.write(String.valueOf(GuiScene.getSlider().getValue()));
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}

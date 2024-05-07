package fr.projet.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record AnimationType(String text, Perso[] perso, int actif) {
    String getNameActif() {
        if (perso == null || perso.length - 1 < actif) {
            return "";
        }
        return perso[actif].name();
    }

    static AnimationType[] loadAnimationTypes(String file) {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is;
        try {
            is = AnimationType.class.getResource("/" + file).openStream();
        } catch (IOException e) {
            log.error("Impossible de charger le fichier : " + file);
            return new AnimationType[0];
        }
        try {
            return mapper.readValue(is, AnimationType[].class);
        } catch (IOException e) {
            log.error("Impossible de mapper le fichier animation");
            return new AnimationType[0];
        }
    }
}

@Slf4j
record Perso(String name, String img, boolean same) {
    public Image getImage() throws IOException {
        URL url = getClass().getResource("/" + img);
        if(url == null) {
            log.error("Impossible de trouver la ressource : " + img);
            throw new IOException();
        } 
        return new Image(url.toExternalForm());
    }
}

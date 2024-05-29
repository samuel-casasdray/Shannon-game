package fr.projet.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Animations(AnimationType[] histoire, Perso[] perso, String[] text) {
    static Animations loadAnimations() {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is;
        try {
            is = Objects.requireNonNull(Animations.class.getResource("/animations.json")).openStream();
        } catch (IOException e) {
            log.error("Impossible de charger le fichier : animations.json");
            return new Animations(new AnimationType[]{}, new Perso[]{}, new String[]{});
        }
        try {
            return mapper.readValue(is, Animations.class);
        } catch (IOException e) {
            log.error("Impossible de mapper le fichier animation");
            return new Animations(new AnimationType[]{}, new Perso[]{}, new String[]{});
        }
    }
}

@Slf4j
record AnimationType(String text, Perso[] perso, int actif, int size) {
    String getNameActif() {
        if (perso == null || perso.length - 1 < actif) {
            return "";
        }
        return perso[actif].name();
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

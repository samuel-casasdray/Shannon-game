package fr.projet.gui;

import javafx.scene.paint.Color;
import lombok.Data;

import java.util.Random;

@Data
public class Etoile {
    private float X;
    private float Y;
    private float Z;
    public void randomize(Random random) {
        X = (float) (random.nextDouble() * 100000.0 - 50000.0);
        Y = (float) (random.nextDouble() * 100000.0 - 50000.0);
        Z = (float) (random.nextDouble() * 1000);
    }

    public Color pixelColor() {
        var d2 = Z * Z;
        var invd2 = 20000000 / d2;
        var b = Math.round(invd2);
        if (b > 255)
            b = 255;
        return Color.rgb(b, b, b);
    }
}

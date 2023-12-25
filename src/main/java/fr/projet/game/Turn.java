package fr.projet.game;

public enum Turn {
    CUT, SHORT;

    public Turn flip() {
        return this == CUT ? SHORT : CUT;
    }
}

package fr.projet;

public enum Turn {
    CUT, SHORT;

    public Turn flip() {
        return this == CUT ? SHORT : CUT;
    }
}

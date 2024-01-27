package fr.projet;

import fr.projet.serverClient.Client;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        Client client = new Client();
        client.CreateGame(); // On créé une game
        //client.JoinGame(884); // On récupère l'id de la game créée (affichée dans un print)
        //new Game();
    }
}

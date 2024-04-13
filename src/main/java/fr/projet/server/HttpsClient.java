package fr.projet.server;

import javafx.util.Pair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpsClient {
    static HttpClient client = HttpClient.newHttpClient();
    public static Pair<Boolean, String> register(String username, String password, String passwordRepeat) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://cryp.tf/add_player"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"pseudo\":\""+username+"\", \"password\":\""+password+"\", " +
                                "\"password_repeat\":\""+passwordRepeat+"\"}"))
                .build();
        var response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
        return new Pair<>(response.statusCode() == 200, response.body());
    }

    public static Pair<Boolean, String> login(String username, String password) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://cryp.tf/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"pseudo\":\""+username+"\", \"password\":\""+password+"\"}"))
                .build();
        CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var response = futureResponse.join();
        return new Pair<>(response.statusCode() == 200, response.body());
    }

    public static int getElo(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://cryp.tf/get_elo/"+username))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var response = futureResponse.join();
        System.out.println(response.statusCode());
        if (response.statusCode() == 200)
            return Integer.parseInt(response.body());
        return -1;
    }
}

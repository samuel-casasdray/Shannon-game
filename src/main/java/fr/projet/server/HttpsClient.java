package fr.projet.server;

import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.util.Pair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpsClient {
    static HttpClient client = HttpClient.newHttpClient();
    public static Pair<Boolean, String> register(String username, String password, String passwordRepeat) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cryp.tf/add_player"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"pseudo\":\""+username+"\", \"password\":\""+password+"\", " +
                                    "\"password_repeat\":\""+passwordRepeat+"\"}"))
                    .build();
            var response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            return new Pair<>(response.statusCode() == 200, response.body());
        }
        catch (Exception e) {
            WebSocketClient.setPseudoCUT("A");
            WebSocketClient.setPseudoSHORT("B");
            return new Pair<>(false, "Vérifiez votre connexion à internet");
        }
    }

    public static Pair<Boolean, String> login(String username, String password) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cryp.tf/login"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"pseudo\":\""+username+"\", \"password\":\""+password+"\"}"))
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var response = futureResponse.join();
            return new Pair<>(response.statusCode() == 200, response.body());
        }
        catch (Exception e) {
            WebSocketClient.setPseudoCUT("A");
            WebSocketClient.setPseudoSHORT("B");
            return new Pair<>(false, "Vérifiez votre connexion à internet");
        }
    }

    public static int getElo(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cryp.tf/get_elo/"+username))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var response = futureResponse.join();
            if (response.statusCode() != 200) throw new Exception();
            return Integer.parseInt(response.body());
        }
        catch (Exception e) {
            WebSocketClient.setPseudoCUT("A");
            WebSocketClient.setPseudoSHORT("B");
            return -1;
        }
    }

    public static JsonArray getStats() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cryp.tf/games"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var response = futureResponse.join();
            if (response.statusCode() != 200) throw new Exception();
            return JsonParser.parseString(response.body()).getAsJsonObject().get("stats").getAsJsonArray();
        }
        catch (Exception e) {
            return new JsonArray();
        }
    }

    public static void sendStatistics(int typeGame, int winner, long seed) {
        try {
            new Thread(() -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://cryp.tf/game_stat/"+typeGame+"/"+winner+"/"+seed))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                futureResponse.join();
            }).start();
        }
        catch (Exception e) {}
    }
}

use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    extract::{Path, State},
    response::IntoResponse,
    routing::get,
    Router,
};
use futures::{lock::MutexGuard, SinkExt, StreamExt};
use rand::Rng;
use serde::Serialize;
use std::sync::Arc;

use serde_json::json;
use tokio::sync::broadcast;

use mongodb::{
    bson::{doc, Document},
    options::ClientOptions,
    Client, Collection,
};

#[tokio::main]
async fn main() {
    let games = Arc::new(futures::lock::Mutex::new(Games { games: vec![] }));
    let app = Router::new()
        .route(
            "/create_game/:creator_turn/:nb_vertices",
            get(create_game_handler),
        ) // Ici, c'est pour créer une game
        .route("/join_game/:id", get(join_game_handler)) // Pour rejoindre une game par son identifiant, il s'agit d'un code d'invitation
        .route("/ws/:id", get(ws_handler)) // La route permettetant de transmettre les données (CUT, SHORT, etc)
        .route(
            "/game_stat/:type_game/:winner/:seed",
            get(game_stat_handler),
        )
        .route("/games", get(get_games_handler))
        .with_state(games);
    // 51.75.126.59:2999 (ip de mon serveur)
    let listener = tokio::net::TcpListener::bind("0.0.0.0:2999") // Port random mais probablement pas déjà pris
        .await
        .unwrap();
    axum::serve(listener, app.into_make_service())
        .await
        .unwrap();
}

async fn create_game_handler(
    ws: WebSocketUpgrade,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    Path(params): Path<(u8, u32)>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| create_game(socket, State(games), params))
}

async fn create_game(
    socket: WebSocket,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    (creator_turn, nb_vertices): (u8, u32),
) {
    let (mut sender, _receiver) = socket.split();
    let mut games = games.lock().await;
    let mut rng = rand::thread_rng();
    let mut n = rng.gen_range(0..=1000); // Pour la démo avec Bessy
    while games.games.iter().any(|x| x.id == n) {
        // On empêche de créer deux fois une game avec le même id
        n = rng.gen_range(0..=1000);
    }
    let seed = rng.gen::<i64>();
    let creator_turn = match creator_turn {
        0 => Turn::Cut,
        _ => Turn::Short,
    };
    games.games.push(Game {
        id: n,
        seed,
        tx: Some(broadcast::channel(100).0), // On ajoute une Game avec id et seed random, et un canal de communication
        previous_move: Turn::Short, // C'est à cut de commencer, donc le previous move est SHORT
        joined: false,              // Personne n'a rejoint jusque là
        ended: false,               // La game n'est pas encore finie
        creator_turn,
        nb_vertices,
    });
    games.games.retain(|game| !game.ended); // On ne garde que les games non finies.
    while games.games.len() > 10 {
        games.games.remove(0); // On ne garde que les 10 dernières games (potentiellement à ajuster/enlever)
    }
    println!("{:?}", games.games);
    let partial = PartialGame {
        id: n,
        seed,
        creator_turn,
        nb_vertices,
    }; // On créé une "Game Partielle" pour pouvoir sérializer certaines informations
    let tx = games.games.last().unwrap().tx.clone().unwrap();
    // Partie compliquée qui consiste à envoyer la Game au Client
    let mut rx = tx.subscribe();
    let game = json!(partial).to_string(); // La Game au format json mais String
    let _ = tx.send(game);
    let _send_task = tokio::spawn(async move {
        loop {
            let response = rx.recv().await;
            match response {
                Ok(game) => {
                    if let Err(e) = sender.send(Message::Text(game)).await {
                        println!("Fatal error {}", e);
                        break;
                    }
                }
                Err(e) => {
                    println!("Erreur create_game {:?}", e);
                    break;
                }
            }
        }
    });
}

async fn join_game_handler(
    ws: WebSocketUpgrade,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    Path(game_id): Path<i64>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| join_game(socket, State(games), game_id))
}

async fn join_game(
    socket: WebSocket,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    payload: i64,
) {
    let (mut sender, _receiver) = socket.split();
    let mut games = games.lock().await;
    games.games.retain(|game| !game.ended); // On ne garde que les games non finies.
    let (mut games, current_game_indice) = get_current_indice(games, payload);
    if current_game_indice == -1 {
        println!("No game has been found to join");
        let _ = sender.send(Message::Text("Not found".to_string())).await;
        return;
    }
    let current_game_indice = current_game_indice as usize;
    let partial = PartialGame {
        id: games.games[current_game_indice].id,
        seed: games.games[current_game_indice].seed,
        creator_turn: games.games[current_game_indice].creator_turn,
        nb_vertices: games.games[current_game_indice].nb_vertices,
    };
    games.games[current_game_indice].joined = true;
    let msg = json!(partial).to_string();
    let tx = games.games[current_game_indice].tx.clone().unwrap();
    // Partie compliquée qui sert à envoyer la game à la personne qui veut rejoindre la game
    let mut rx = tx.subscribe();
    let _ = tx.send(msg);
    let _send_task = tokio::spawn(async move {
        loop {
            let response = rx.recv().await;
            match response {
                Ok(msg) => {
                    if let Err(e) = sender.send(Message::Text(msg)).await {
                        println!("Fatal error 2 {}", e);
                        break;
                    }
                }
                Err(e) => {
                    println!("Erreur join_game {:?}", e);
                    break;
                }
            }
        }
    });
}

async fn ws_handler(
    websocket: WebSocketUpgrade,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    Path(game_id): Path<i64>,
) -> impl IntoResponse {
    websocket.on_upgrade(move |socket| handle_socket(socket, State(games), game_id))
}

async fn handle_socket(
    socket: WebSocket,
    game: State<Arc<futures::lock::Mutex<Games>>>,
    game_id: i64,
) {
    let (_sender, mut receiver) = socket.split();
    let _recv_task = tokio::spawn(async move {
        while let Some(Ok(Message::Text(vertices))) = receiver.next().await {
            let games = game.lock().await;
            let (mut games, current_game_indice) = get_current_indice(games, game_id);
            if current_game_indice == -1 {
                println!("No game has been found to join");
                return;
            }
            let current_game_indice = current_game_indice as usize;
            let tx = games.games[current_game_indice].tx.clone().unwrap();
            if vertices == *"Ping" {
                let _ = tx.send("Pong".to_string());
                continue;
            }
            if vertices == *"CUT!" || vertices == *"SHORT!" {
                // Fin de la game
                games.games[current_game_indice].ended = true;
                return;
            }
            if vertices == *"CUT" || vertices == *"SHORT" {
                // Un des deux joueurs s'est déconnecté, et celui qui est resté à répondu
                let _ = tx.send(vertices + " a gagné"); // "je suis SHORT" ou "je suis CUT", on lui attribue la victoire
                games.games[current_game_indice].ended = true;
                return;
            }
            // On récupère les deux vertices depuis le client sous la forme "id1 id2 move" par exemple "3 4 1"
            // 3 et 4 représentent l'id des vertices et 1 représente le type de move (CUT ou SHORT, 0 pour CUT, 1 pour SHORT)
            let vertices: Vec<i64> = vertices
                .split(' ') // On transforme "x y z" en [x, y, z]
                .map(|x| x.parse().unwrap())
                .collect();
            if vertices.len() != 3 {
                continue; // Il faut obligatoirement 3 éléments : les deux premiers vertices et le type de move
            }
            if !games.games[current_game_indice].joined {
                println!("Nobody joined the game"); // Si personne n'a rejoint la game, le créateur ne peut pas encore jouer
                continue;
            }
            let turn;
            if vertices[2] == 0 {
                // 0 = CUT
                turn = Turn::Cut;
            } else if vertices[2] == 1 {
                // 1 = SHORT
                turn = Turn::Short;
            } else {
                println!("Turn should be 0 or 1");
                continue;
            }
            if turn == games.games[current_game_indice].previous_move {
                println!("Wait the next turn");
                continue; // On empêche le joueur de jouer deux fois d'affilé
            }
            games.games[current_game_indice].previous_move.flip(); // On change le previous move (0 -> 1 et 1 -> 0)
            let _ = tx.send(json!(vertices).to_string()); // On envoie les vertices aux clients
        }
        // Si on quitte le while, l'un des deux joueurs s'est déconnecté
        let games = game.lock().await;
        let (mut games, current_game_indice) = get_current_indice(games, game_id);
        if current_game_indice != -1 {
            games.games[current_game_indice as usize].ended = true;
            let tx = games.games[current_game_indice as usize]
                .tx
                .clone()
                .unwrap();
            if games.games[current_game_indice as usize].joined {
                let _ = tx.send("L'adversaire a quitté la partie".to_string()); // On envoie au joueur restant l'information
            }
        }
    });
    println!("WebSocket context destroyed"); // On ferme la connexion websocket
}

fn get_current_indice(games: MutexGuard<'_, Games>, game_id: i64) -> (MutexGuard<'_, Games>, i32) {
    let mut current_game_indice = -1;
    for i in 0..games.games.len() {
        if games.games[i].id == game_id {
            // On récupère l'indice de la game
            current_game_indice = i as i32;
            break;
        }
    }
    (games, current_game_indice)
}

async fn game_stat_handler(Path(game): Path<(u32, u8, i64)>) -> impl IntoResponse {
    game_stat(game).await;
}

async fn game_stat((type_game, winner, seed): (u32, u8, i64)) {
    match dotenv::from_path("/home/julien/backend_server/.env").ok() {
        None => {
            println!("Returned");
            return;
        }
        Some(_) => {}
    }
    let client_options = ClientOptions::parse(
        "mongodb+srv://shannon:".to_owned()
            + &std::env::var("PASSWORD_SHANNON").unwrap()
            + "@cluster0.lbnu03x.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
    )
    .await
    .unwrap();
    let client = Client::with_options(client_options).unwrap();
    let my_coll = client
        .database("shannon_switching_game")
        .collection("games");
    let doc = json!({"type_game": type_game, "winner": winner, "seed": seed});
    my_coll.insert_one(doc, None).await.unwrap();
}

async fn get_games_handler(ws: WebSocketUpgrade) -> impl IntoResponse {
    ws.on_upgrade(get_games)
}

async fn get_games(socket: WebSocket) {
    let (mut sender, _receiver) = socket.split();
    match dotenv::from_path("/home/julien/backend_server/.env").ok() {
        None => {
            println!("Returned");
            return;
        }
        Some(_) => {}
    }
    let client_options = ClientOptions::parse(
        "mongodb+srv://shannon:".to_owned()
            + &std::env::var("PASSWORD_SHANNON").unwrap()
            + "@cluster0.lbnu03x.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
    )
    .await
    .unwrap();
    let client = Client::with_options(client_options).unwrap();
    let my_coll: Collection<Document> = client
        .database("shannon_switching_game")
        .collection("games");
    let number_total_games = my_coll.count_documents(None, None).await.unwrap();
    let number_cut_games = my_coll
        .count_documents(doc! { "winner": 0}, None)
        .await
        .unwrap();
    let number_short_games = my_coll
        .count_documents(doc! { "winner": 1}, None)
        .await
        .unwrap();
    let number_online_games = my_coll
        .count_documents(doc! {"type_game": 2}, None)
        .await
        .unwrap();
    let tx: broadcast::Sender<serde_json::Value> = broadcast::channel(1).0;
    let mut rx = tx.subscribe();
    if let Err(e) = tx.send(json!({
        "stats": vec![number_total_games, number_cut_games, number_short_games, number_online_games]
    })) {
        println!("{:?}", e);
    }
    let _send_task = tokio::spawn(async move {
        let response = rx.recv().await;
        match response {
            Ok(msg) => {
                if let Err(e) = sender.send(Message::Text(msg.to_string())).await {
                    println!("Erreur : {}", e);
                }
            }
            Err(e) => {
                println!("Erreur get_games {:?}", e);
            }
        }
    });
}

#[derive(Debug)]
pub struct Game {
    id: i64,
    seed: i64,
    tx: Option<broadcast::Sender<String>>,
    previous_move: Turn,
    joined: bool,
    ended: bool,
    creator_turn: Turn,
    nb_vertices: u32,
}

#[derive(Debug)]
pub struct Games {
    pub games: Vec<Game>,
}

#[derive(Debug, Serialize)]
pub struct PartialGame {
    // ça permet de pouvoir sérialiser une Game, en gardant seulement l'id, la seed, le tour du créateur et le nombre de sommets du graphe
    id: i64,
    seed: i64,
    creator_turn: Turn,
    nb_vertices: u32,
}

#[derive(Debug, PartialEq, Eq, Clone, Serialize, Copy)]
enum Turn {
    Cut,
    Short,
}

impl Turn {
    fn flip(&mut self) {
        *self = match *self {
            Turn::Cut => Turn::Short,
            _ => Turn::Cut,
        }
    }
}

use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    extract::{ConnectInfo, Path, State},
    response::IntoResponse,
    routing::get,
    Json, Router,
};
use axum_extra::{headers, TypedHeader};
use futures::{SinkExt, StreamExt};
use rand::Rng;
use serde::Serialize;
use std::{net::SocketAddr, sync::Arc};

use serde_json::json;
use tokio::sync::broadcast;

#[tokio::main]
async fn main() {
    // On créé une Game "default"
    let game = Game {
        id: 0,
        seed: 0,
        tx: None,
        previous_move: Turn::SHORT,
        joined: false,
    };
    let games = Arc::new(futures::lock::Mutex::new(Games { games: vec![game] }));
    let app = Router::new()
        .route("/create_game", get(create_game_handler)) // Ici, c'est pour créer une game
        .route("/join_game/:id", get(join_game_handler)) // Pour rejoindre une game par son identifiant, il s'agit d'un code d'invitation
        .route("/ws/:id", get(ws_handler)) // La route permettetant de transmettre les données (CUT, SHORT, etc)
        .with_state(games.clone());
    // 51.75.126.59:2999
    let listener = tokio::net::TcpListener::bind("0.0.0.0:2999") // Ip de mon serveur avec un port random mais probablement pas déjà pris
        .await
        .unwrap();
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await
    .unwrap();
}

async fn create_game_handler(
    ws: WebSocketUpgrade,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| create_game(socket, State(games)))
}

async fn create_game(socket: WebSocket, State(games): State<Arc<futures::lock::Mutex<Games>>>) {
    let (mut sender, _receiver) = socket.split();
    let mut games = games.lock().await;
    let mut rng = rand::thread_rng();
    let mut n = rng.gen_range(0..=1000); // Pour la démo avec Bessy
    while games.games.iter().any(|x| x.id == n) {
        n = rng.gen_range(0..=1000);
    }
    let seed = rng.gen::<i64>();
    match games.games.first() {
        Some(x) => {
            if x.tx.is_none() {
                games.games.remove(0); // On vire la game "default"
            }
        }
        None => {}
    }
    games.games.push(Game {
        id: n,
        seed,
        tx: Some(broadcast::channel(100).0), // On ajoute une Game avec id et seed random, et un canal de communication
        previous_move: Turn::SHORT, // C'est à cut de commencer, donc le previous move est SHORT
        joined: false,    // Personne n'a rejoint jusque là
    });
    while games.games.len() > 10 {
        games.games.remove(0); // On ne garde que les 10 dernières games
    }
    println!("{:?}", games.games);
    let partial = PartialGame { id: n, seed }; // On créé une "Game Partielle" pour pouvoir sérializer certaines informations
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
    Path(game_id): Path<String>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| join_game(socket, State(games), Json(game_id)))
}

async fn join_game(
    socket: WebSocket,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    Json(payload): Json<String>,
) {
    let (mut sender, _receiver) = socket.split();
    let payload = (&payload[1..]).to_string(); // On enlève le premier caractère ":123456" -> "123456"
    let mut games = games.lock().await;
    while games.games.len() > 10 {
        games.games.remove(0); // On ne garde que les 10 dernières games
    }
    let mut current_game_indice = -1; // Indice de la game qui nous intéresse, initialisé à -1
    for i in 0..games.games.len() {
        if games.games[i].id.to_string() == payload {
            current_game_indice = i as i32;
            break;
        }
    }
    if current_game_indice == -1 {
        println!("No game has been found to join");
        return;
    }
    let partial = PartialGame {
        id: games.games[current_game_indice as usize].id,
        seed: games.games[current_game_indice as usize].seed,
    };
    games.games[current_game_indice as usize].joined = true;
    let msg = json!(partial).to_string();
    let tx = games.games[current_game_indice as usize]
        .tx
        .clone()
        .unwrap();
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
    user_agent: Option<TypedHeader<headers::UserAgent>>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    Path(game_id): Path<String>
) -> impl IntoResponse {
    let user_agent = if let Some(TypedHeader(user_agent)) = user_agent {
        user_agent.to_string()
    } else {
        String::from("Unknown browser")
    };
    println!("`{user_agent}` at {addr} connected.");
    websocket.on_upgrade(move |socket| handle_socket(socket, addr, State(games), Json(game_id)))
}

async fn handle_socket(
    socket: WebSocket,
    who: SocketAddr,
    game: State<Arc<futures::lock::Mutex<Games>>>,
    Json(game_id): Json<String>
) {
    let (_sender, mut receiver) = socket.split();
    let game_id = (&game_id[1..]).to_string(); // On enlève le premier caractère ":123456" -> "123456"
    let _recv_task = tokio::spawn(async move {
        while let Some(Ok(Message::Text(vertices))) = receiver.next().await {
            let mut games = game.lock().await;
            let mut current_game_indice = -1;
            for i in 0..games.games.len() {
                if games.games[i].id == game_id.parse::<i64>().unwrap() { // On récupère l'indice de la game
                    current_game_indice = i as i32;
                    break;
                }
            }
            let tx = games.games[current_game_indice as usize].tx.clone().unwrap();
            if vertices == "CUT!".to_string() || vertices == "SHORT!".to_string() { // Fin de la game 
                return;
            }
            if vertices == "CUT".to_string() || vertices == "SHORT".to_string() { // Un des deux joueurs a déconnecté, et celui qui est resté à répondu 
                let _ = tx.send(vertices + " a gagné"); // "je suis SHORT" ou "je suis CUT", on lui attribue la victoire
                return;
            }
            // On récupère les deux vertices depuis le client sous la forme "id1 id2 move" par exemple "3 4 1"
            // 3 et 4 représentent l'id des vertices et 1 représente le type de move (CUT ou SHORT, 0 pour CUT, 1 pour SHORT)
            let vertices: Vec<i64> = vertices
                .split(' ')
                .into_iter()  // On transforme "x y z" en [x, y, z]
                .map(|x| x.parse().unwrap())
                .collect();
            if vertices.len() != 3 {
                continue; // Il faut obligatoirement 4 éléments : les deux premiers vertices et le type de move
            }
            if current_game_indice == -1 {
                println!("No game found");
                continue; // Si l'indice est -1, la game n'est pas présente, c'est cassé
            }
            if !games.games[current_game_indice as usize].joined {
                println!("Nobody joined the game"); // Si personne n'a rejoint la game, le créateur ne peut pas encore jouer
                continue;
            }
            let turn;
            if vertices[2] == 0 {
                turn = Turn::CUT;
            }
            else if vertices[2] == 1 {
                turn = Turn::SHORT;
            }
            else {
                println!("Turn should be 0 or 1");
                continue;
            }
            if turn == games.games[current_game_indice as usize].previous_move {
                println!("previous move crash");
                continue; // On empêche le joueur de jouer deux fois d'affilé
            }
            games.games[current_game_indice as usize].previous_move = flip(&games.games[current_game_indice as usize].previous_move); // On change le previous move (0 -> 1 et 1 -> 0)
            let _ = tx.send(json!(vertices).to_string()); // On envoie les vertices aux clients
        }
        // Si on quitte le while, l'un des deux joueurs a déconnecté
        let games = game.lock().await;
        let mut current_game_indice = -1;
            for i in 0..games.games.len() {
                if games.games[i].id == game_id.parse::<i64>().unwrap() { // On récupère l'indice de la game
                    current_game_indice = i as i32;
                    break;
            }
        }
        if current_game_indice != -1 {
            let tx = games.games[current_game_indice as usize].tx.clone().unwrap();
            let _ = tx.send("L'adversaire a quitté la partie".to_string()); // On envoie au joueur restant l'information
        }
    });
    println!("WebSocket context {} destroyed", who); // On ferme la connexion websocket
}

#[derive(Clone, Debug)]
pub struct Game {
    id: i64,
    seed: i64,
    tx: Option<broadcast::Sender<String>>,
    previous_move: Turn,
    joined: bool,
}

#[derive(Clone, Debug)]
pub struct Games {
    pub games: Vec<Game>,
}

#[derive(Clone, Debug, Serialize)]
pub struct PartialGame {
    // ça permet de pouvoir sérialiser une Game, en enlevant juste la tx et le previous move (utiles juste pour le serveur)
    id: i64,
    seed: i64,
}

#[derive(Clone, Debug, PartialEq)]
enum Turn {
    CUT,
    SHORT
}

fn flip(turn: &Turn) -> Turn {
    return match turn {
        Turn::CUT => Turn::SHORT,
        _ => Turn::CUT
    }
}
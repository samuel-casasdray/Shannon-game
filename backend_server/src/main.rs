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
        previous_move: 0,
    };
    let games = Arc::new(futures::lock::Mutex::new(Games { games: vec![game] }));
    let app = Router::new()
        .route("/create_game", get(create_game_handler)) // Ici, c'est pour créer une game
        .route("/join_game/:id", get(join_game_handler)) // Pour rejoindre une game par son identifiant, il s'agit d'un code d'invitation
        .route("/ws", get(ws_handler)) // La route permettetant de transmettre les données (CUT, SHORT, etc)
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
    let n = rng.gen_range(0..=1000); // Pour la démo avec Bessy
    let seed = rng.gen::<i64>();
    games.games = match games.games.first() {
        Some(x) => {
            if x.tx.is_none() {
                let mut g = games.games.clone();
                g.remove(0);
                g
            } else {
                games.games.clone()
            }
        }
        None => games.games.clone(),
    }; // On vire la game "default"
    games.games.push(Game {
        id: n,
        seed,
        tx: Some(broadcast::channel(100).0), // On ajoute une Game avec id et seed random, et un canal de communication
        previous_move: 1,
    });

    println!("{:?}", games);

    let partial = PartialGame { id: n, seed }; // On créé une "Game Partielle" pour pouvoir sérializer certaines informations
    let tx = games.games.last().unwrap().tx.clone().unwrap(); // Ligne à optim (match)
    games.games.last_mut().unwrap().tx = Some(tx.clone()); // Ligne à optim, améliorer, match les erreurs etc

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
    if games.games.len() > 10 {
        games.games = games.games.clone().into_iter().skip(1).collect(); // On garde que les 10 premières games
    }
    let mut current_game_indice = -1;
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
    // On trouve, parmi les games, celle dont l'id est égal au payload et on créé une game partielle pour pouvoir l'envoyer en JSON
    // for i in 0..games.games.len() {
    //     if games.games[i].id.to_string() == payload {
    //         let partial = PartialGame {
    //             id: games.games[i].id,
    //             seed: games.games[i].seed,
    //         };
    //         msg = json!(partial).to_string();
    //         break;
    //     }
    // }
    let partial = PartialGame {
        id: games.games[current_game_indice as usize].id,
        seed: games.games[current_game_indice as usize].seed,
    };
    let msg = json!(partial).to_string();
    let tx = games.games[current_game_indice as usize].tx.clone().unwrap();
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
) -> impl IntoResponse {
    let user_agent = if let Some(TypedHeader(user_agent)) = user_agent {
        user_agent.to_string()
    } else {
        String::from("Unknown browser")
    };
    println!("`{user_agent}` at {addr} connected.");
    websocket.on_upgrade(move |socket| handle_socket(socket, addr, State(games)))
}

async fn handle_socket(
    socket: WebSocket,
    who: SocketAddr,
    game: State<Arc<futures::lock::Mutex<Games>>>,
) {
    let (mut sender, mut receiver) = socket.split();
    // On récupère les deux vertices depuis le client sous la forme "id1 id2 move" par exemple "3 4 1"
    // 3 et 4 représentent l'id des vertices et 1 représente le type de move (CUT ou SHORT, 0 pour CUT, 1 pour SHORT)
    let vertices = match receiver.next().await {
        Some(Ok(msg)) => match msg {
            Message::Text(text) => text,
            _ => "".to_string(),
        },
        Some(Err(_)) => {
            println!("Error receiving message from {}", who);
            return;
        }
        None => {
            println!("Client {} disconnected", who);
            return;
        }
    };

    let vertices: Vec<i64> = vertices
        .split(' ')
        .into_iter()
        .map(|x| x.parse().unwrap())
        .collect(); // on transforme "x y m id" en [x, y, m, id]
    if vertices.len() != 4 {
        return; // Il faut obligatoirement 3 éléments : les deux premiers vertices, le type de move, et l'id de la game
    }
    let mut games = game.lock().await;
    let mut current_game_indice = -1;
    for i in 0..games.games.len() {
        if games.games[i].id == vertices[3] {
            current_game_indice = i as i32;
            break;
        }
    }
    if current_game_indice == -1 {
        println!("No game found");
        return;
    }
    if vertices[2] == games.games[current_game_indice as usize].previous_move as i64 {
        println!("previous move crash");
        return; // On empêche le joueur de jouer deux fois d'affilé
    }
    games.games[current_game_indice as usize].previous_move =
        1 - games.games[current_game_indice as usize].previous_move; // On change le coup précédent (1 -> 0 et 0 -> 1)
    let tx = match games.games[current_game_indice as usize].tx.clone() {
        // Ligne à optim (gérer les erreurs etc)
        Some(tx) => tx,
        None => {
            println!("Error: No message channel found for game");
            return;
        }
    };
    let mut rx = tx.subscribe();
    match tx.send(json!(vertices).to_string()) {
        // On envoie les vertices
        Ok(_) => {}
        Err(e) => {
            println!("Error sending message to broadcast channel: {:?}", e);
            return;
        }
    }
    // Partie compliquée qui sert à ne jamais déconnecter les clients afin de pouvoir envoyer tous les moves sans avoir à se reconnecter
    let send_task = tokio::spawn(async move {
        loop {
            let response = rx.recv().await;
            match response {
                Ok(msg) => {
                    if let Err(e) = sender.send(Message::Text(msg.clone())).await {
                        println!("Error sending message to {}: {}", who, e);
                        break;
                    } else {
                        println!("Message : {}", msg);
                        break;
                    }
                }
                Err(e) => {
                    println!("Error : {:?}", e);
                    break;
                }
            }
        }
    });
    send_task.await.unwrap();
    println!("WebSocket context {} destroyed", who); // On ferme la connexion websocket
}

#[derive(Clone, Debug)]
pub struct Game {
    id: i64,
    seed: i64,
    tx: Option<broadcast::Sender<String>>,
    previous_move: u8,
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

use axum::{
    extract::{ws::{Message, WebSocket, WebSocketUpgrade}, Path, State}, http::StatusCode, response::{IntoResponse, Result}, routing::{get, post}, Json, Router
};
use futures::{lock::MutexGuard, SinkExt, StreamExt};
use rand::Rng;
use serde::{Serialize, Deserialize};
use std::sync::Arc;

use serde_json::json;
use tokio::sync::broadcast;

use mongodb::{
    bson::doc,
    options::ClientOptions,
    Client, Collection,
};

#[tokio::main]
async fn main() {
    let games = Arc::new(futures::lock::Mutex::new(Games { games: vec![] }));
    match dotenv::from_path("/home/julien/backend_server/.env").ok() {
        None => {
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
	let my_coll: Collection<Player> = client
		.database("shannon_switching_game")
		.collection("players");
	let stats: Collection<serde_json::Value> = client
		.database("shannon_switching_game")
		.collection("games");
    let app = Router::new()
        .route(
            "/create_game/:creator_turn/:nb_vertices",
            get(create_game_handler),
        ) // Ici, c'est pour créer une game
        .route("/join_game/:id", get(join_game_handler)) // Pour rejoindre une game par son identifiant, il s'agit d'un code d'invitation
        .with_state(games.clone())
        .route("/ws/:id", get(ws_handler)) // La route permettetant de transmettre les données (CUT, SHORT, etc)
        .with_state((games, my_coll.clone()))
        .route(
            "/game_stat/:type_game/:winner/:seed",
            get(add_stat_handler),
        )
        .route("/games", get(get_games_handler))
        .with_state(stats)
        .route("/add_player", post(insert_player_handler))
        .route("/login", post(log_in))
        .with_state(my_coll);
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
    let mut n = rng.gen_range(0..=1000); // 1000 arbitraire, on pourrait mettre beaucoup plus
    while games.games.iter().any(|x| x.id == n) {
        // On empêche de créer deux fois une game avec le même id
        n = rng.gen_range(0..=1000);
    }
    let seed = rng.gen::<i64>(); // Seed random pour la game
    let creator_turn = match creator_turn {
        0 => Turn::Cut,
        _ => Turn::Short,
    };
    games.games.push(Game {
        id: n,
        seed,
        tx: Some(broadcast::channel(1).0), // On ajoute une Game avec id et seed random, et un canal de communication
        previous_move: Turn::Short, // C'est à cut de commencer, donc le previous move est SHORT
        joined: false,              // Personne n'a rejoint jusque là
        ended: false,               // La game n'est pas encore finie
        creator_turn,
        nb_vertices,
        cut_player: "Wronsk",
        short_player: "",
    });
    games.games.retain(|game| !game.ended); // On ne garde que les games non finies.
    println!("{:?}", games.games);
    let partial = PartialGame {
        id: n,
        seed,
        creator_turn,
        nb_vertices,
    }; // On créé une "Game Partielle" pour pouvoir sérializer certaines informations
    let tx = games.games.last().unwrap().tx.clone().unwrap(); // last existe forcément, on vient de l'ajouter
    // Partie compliquée qui consiste à envoyer la Game au Client
    let mut rx = tx.subscribe();
    let game = json!(partial).to_string(); // La Game au format json mais String
    let _ = tx.send(game); // On envoie la Game au client
    let _send_task = tokio::spawn(async move {
        loop {
            let response = rx.recv().await; // On attend une réponse
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
    let current_game_indice = get_current_indice(&games, payload);
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
    games.games[current_game_indice].short_player = "Carlsen";
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
                        println!("Erreur send {:?}", e);
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
    State((games, my_coll)): State<(Arc<futures::lock::Mutex<Games>>, Collection<Player>)>,
    Path(game_id): Path<i64>,
) -> impl IntoResponse {
    websocket.on_upgrade(move |socket| handle_socket(socket, State(games), my_coll, game_id))
}

async fn handle_socket(
    socket: WebSocket,
    game: State<Arc<futures::lock::Mutex<Games>>>,
    my_coll: Collection<Player>,
    game_id: i64,
) {
    let (_sender, mut receiver) = socket.split();
    let _recv_task = tokio::spawn(async move {
        while let Some(Ok(Message::Text(move_message))) = receiver.next().await {
            let mut games = game.lock().await;
            let current_game_indice = get_current_indice(&games, game_id);
            if current_game_indice == -1 {
                println!("No game has been found to join");
                return;
            }
            let current_game_indice = current_game_indice as usize;
            let cut_player = games.games[current_game_indice].cut_player;
            let short_player = games.games[current_game_indice].short_player;
            let tx = games.games[current_game_indice].tx.clone().unwrap();
            if move_message == *"Ping" {
                let _ = tx.send("Pong".to_string());
                continue;
            }
            if (move_message == *"CUT!" || move_message == *"SHORT!") && !games.games[current_game_indice].ended {
                // Fin de la game
                if move_message == *"CUT!" {
					update_players(&my_coll, cut_player, short_player, Turn::Cut).await;
				} else {
					update_players(&my_coll, short_player, cut_player, Turn::Short).await;
				}
                games.games[current_game_indice].ended = true;
                return;
            }
            if (move_message == *"CUT" || move_message == *"SHORT") && !games.games[current_game_indice].ended {
                // Un des deux joueurs s'est déconnecté, et celui qui est resté a répondu
                if move_message == *"CUT" {
					update_players(&my_coll, cut_player, short_player, Turn::Cut).await;
				} else {
					update_players(&my_coll, short_player, cut_player, Turn::Short).await;
				}
				games.games[current_game_indice].ended = true;
				let _ = tx.send(move_message + " a gagné"); // "je suis SHORT" ou "je suis CUT", on lui attribue la victoire
                return;
            }
            // On récupère les deux vertices depuis le client sous la forme "id1 id2 move" par exemple "3 4 1"
            // 3 et 4 représentent l'id des vertices et 1 représente le type de move (CUT ou SHORT, 0 pour CUT, 1 pour SHORT)
            let vertices: Vec<i64> = move_message
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
        let mut games = game.lock().await;
        let current_game_indice = get_current_indice(&games, game_id);
        if current_game_indice != -1 {
        	let current_game_indice = current_game_indice as usize;
        	games.games[current_game_indice].ended = true;
            let tx = games.games[current_game_indice]
                .tx
                .clone()
                .unwrap();
            // let cut_player = games.games[current_game_indice].cut_player;
            // let short_player = games.games[current_game_indice].short_player;
            if games.games[current_game_indice].joined {
                let _ = tx.send("L'adversaire a quitté la partie".to_string()); // On envoie au joueur restant l'information
                //update_players(&my_coll, cut_player, short_player, Turn::Cut).await;
            }
        }
    });
    println!("WebSocket context destroyed"); // On ferme la connexion websocket
}

fn get_current_indice(games: &MutexGuard<'_, Games>, game_id: i64) -> i32 {
    let mut current_game_indice = -1;
    for i in 0..games.games.len() {
        if games.games[i].id == game_id {
            // On récupère l'indice de la game
            current_game_indice = i as i32;
            break;
        }
    }
    current_game_indice // On renvoie -1 si aucune game n'est trouvée
}

async fn add_stat_handler(State(my_coll): State<Collection<serde_json::Value>>, Path(game): Path<(u32, u8, i64)>) -> impl IntoResponse {
    add_stat(my_coll, game).await;
}

async fn add_stat(my_coll: Collection<serde_json::Value>, (type_game, winner, seed): (u32, u8, i64)) {
    let doc = json!({"type_game": type_game, "winner": winner, "seed": seed});
    my_coll.insert_one(doc, None).await.unwrap();
}

async fn get_games_handler(State(my_coll): State<Collection<serde_json::Value>>, ws: WebSocketUpgrade) -> impl IntoResponse {
    ws.on_upgrade(move |socket| get_games(socket, my_coll))
}

async fn get_games(socket: WebSocket, my_coll: Collection<serde_json::Value>) {
    let (mut sender, _receiver) = socket.split();
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

async fn insert_player_handler(State(my_coll): State<Collection<Player>>, Json(player): Json<CreatePlayer>) -> Result<String, (StatusCode, String)> {
	let pseudo = player.pseudo;
	let password = player.password;
	let password_repeat = player.password_repeat;
	if password != password_repeat
	{
		return Err((StatusCode::UNAUTHORIZED, "Passwords do not match".to_string()));
	}
	let sha256_password_with_salt = bcrypt::hash(password, 12).unwrap(); // Hash avec bcrypt pour simplifier le salage
    insert_player(&pseudo.to_string(), sha256_password_with_salt, my_coll).await?;
    Ok("Player inserted".to_string())
}

async fn insert_player(pseudo: &str, password_hash: String, my_coll: Collection<Player>) -> Result<(), (StatusCode, String)> {
    if pseudo.len() < 3 {
		return Err((StatusCode::BAD_REQUEST, "Pseudo too short".to_string()));
	}
    else if pseudo.len() > 20 {
    	return Err((StatusCode::BAD_REQUEST, "Pseudo too long".to_string()));
    }
    else if my_coll.find_one(doc! {"pseudo": pseudo}, None).await.unwrap().is_some() {
    	return Err((StatusCode::CONFLICT, "Pseudo already taken".to_string()));
    }
    let player = Player {
		pseudo: pseudo.to_string(),
		password_hash,
		elo: 1200,
		nb_games: 0,
	};
    my_coll.insert_one(player, None).await.unwrap();
    Ok(())
}

async fn log_in(State(my_coll): State<Collection<Player>>, Json(player): Json<LogInPlayer>) -> Result<String, (StatusCode, String)> {
	let user = my_coll.find_one(doc! {"pseudo": player.pseudo}, None).await.unwrap();
	match user {
		None => {
			Err((StatusCode::UNAUTHORIZED, "Pseudo or password incorrect".to_string()))
		}
		Some(user) => {
			if bcrypt::verify(player.password, &user.password_hash).unwrap() {
				Ok("Logged in".to_string())
			}
			else {
				Err((StatusCode::UNAUTHORIZED, "Pseudo or password incorrect".to_string()))
			}
		}
	}
}

async fn set_elo(my_coll: &Collection<Player>, player: &Player, elo: u32) {
	my_coll.update_one(doc! {"pseudo": &player.pseudo}, doc! {"$set": {"elo": elo}}, None).await.unwrap();
}

async fn increase_nb_games(my_coll: &Collection<Player>, player: &Player) {
	my_coll.update_one(doc! {"pseudo": &player.pseudo}, doc! {"$set": {"nb_games": player.nb_games+1}}, None).await.unwrap();
}

async fn update_players(my_coll: &Collection<Player>, player_cut: &str, player_short: &str, winner: Turn) {
	let cut_player = my_coll.find_one(doc! {"pseudo": player_cut}, None).await.unwrap();
	let short_player = my_coll.find_one(doc! {"pseudo": player_short}, None).await.unwrap();
	let cut_elo = if let Some(player) = &cut_player {
		player.elo as f64
	}
	else {
		0.0
	};
	match &cut_player {
		None => {}
		Some(player) => {
			if short_player.is_some() {
				let k_cut = match player.nb_games {
					0..=29 => 40.0,
					_ => 20.0,
				};
				let w_cut = match winner {
					Turn::Cut => 1.0,
					_ => 0.0,
				};
				let elo = player.elo as f64;
				let short_elo = short_player.as_ref().unwrap().elo as f64;
				let new_elo = elo+k_cut*(w_cut-p(elo-short_elo));
				set_elo(my_coll, player, new_elo as u32).await;
				increase_nb_games(my_coll, player).await;
			}
		}
	}
	match &short_player {
		None => {}
		Some(player) => {
			if cut_player.is_some() {
				let k_short = match player.nb_games {
					0..=29 => 40.0,
					_ => 20.0,
				};
				let w_short = match winner {
					Turn::Short => 1.0,
					_ => 0.0,
				};
				let elo = player.elo as f64;
				let new_elo = elo+k_short*(w_short-p(elo-cut_elo));
				set_elo(my_coll, player, new_elo as u32).await;
				increase_nb_games(my_coll, player).await;
			}
		}
	}
}

fn p(d: f64) -> f64 {
	1.0 / (1.0 + 10.0_f64.powf(-d / 400.0))
}

#[derive(Deserialize)]
struct CreatePlayer {
    pseudo: String,
    password: String,
    password_repeat: String,
}

#[derive(Deserialize)]
struct LogInPlayer {
    pseudo: String,
    password: String,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
struct Player {
    pseudo: String,
    password_hash: String,
    elo: u32,
    nb_games: u32,
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
    cut_player: &'static str,
    short_player: &'static str,
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

#[derive(Debug, PartialEq, Eq, Clone, Serialize, Copy)] // On peut comparer deux Turn, les cloner et les sérialiser
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

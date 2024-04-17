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
            "/create_game/:creator_turn/:nb_vertices/:pseudo",
            get(create_game_handler),
        ) // Ici, c'est pour créer une game
        .route("/join_game/:id/:pseudo", get(join_game_handler)) // Pour rejoindre une game par son identifiant, il s'agit d'un code d'invitation
        .with_state(games.clone())
        .route("/ws/:id", get(ws_handler)) // La route permettetant de transmettre les données (CUT, SHORT, etc)
        .with_state((games, my_coll.clone())) // On peut clone car il s'agit d'un Arc
        .route(
            "/game_stat/:type_game/:winner/:seed",
            get(add_stat_handler), // Route permettant d'ajouter une stat à la base de données
        )
        .route("/games", get(get_games_handler)) // Route permettant de récupérer les statistiques des games
        .with_state(stats)
        .route("/add_player", post(insert_player_handler)) // Route permettant d'ajouter un joueur à la base de données (inscription)
        .route("/login", post(log_in)) // Route permettant de se connecter
        .route("/get_elo/:pseudo", get(get_elo)) // Route permettant de récupérer l'elo d'un joueur
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
    Path(params): Path<(u8, u32, String)>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| create_game(socket, State(games), params.0, params.1, params.2))
}

async fn create_game(
    socket: WebSocket,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    creator_turn: u8,
    nb_vertices: u32,
    pseudo: String,
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
    let (cut_player, short_player) = if creator_turn == Turn::Cut {
		(pseudo, "".to_string())
	} else {
		("".to_string(), pseudo)
	};
    games.games.push(Game {
        id: n,
        seed,
        tx: Some(broadcast::channel(1).0), // On ajoute une Game avec id et seed random, et un canal de communication
        previous_move: Turn::Short, // C'est à cut de commencer, donc le previous move est SHORT
        joined: false,              // Personne n'a rejoint jusque là
        ended: false,               // La game n'est pas encore finie
        creator_turn,				// Le joueur qui a créé la game
        nb_vertices,				// Le nombre de sommets
        cut_player,					// Le pseudo du joueur qui a choisi CUT
        short_player,				// Le pseudo du joueur qui a choisi SHORT
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
    Path((game_id, pseudo)): Path<(i64, String)>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| join_game(socket, State(games), game_id, pseudo))
}

async fn join_game(
    socket: WebSocket,
    State(games): State<Arc<futures::lock::Mutex<Games>>>,
    payload: i64,
    pseudo: String
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
    if games.games[current_game_indice].creator_turn == Turn::Cut {
		games.games[current_game_indice].short_player = pseudo; // Si le créateur a choisi CUT, on ajoute le pseudo 
		// du joueur qui a rejoint en SHORT
	} else {
		games.games[current_game_indice].cut_player = pseudo; // Sinon en CUT
	}
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
            let cut_player = &games.games[current_game_indice].cut_player;
            let short_player = &games.games[current_game_indice].short_player;
            let tx = games.games[current_game_indice].tx.clone().unwrap();
            if move_message == *"Ping" {
                let _ = tx.send("Pong".to_string());
                continue;
            }
            if move_message == *"CUT!" || move_message == *"SHORT!" {
                // Fin de la game
                if !games.games[current_game_indice].ended {
	                if move_message == *"CUT!" {
						update_players(&my_coll, cut_player, short_player, Turn::Cut).await;
					} else {
						update_players(&my_coll, short_player, cut_player, Turn::Short).await;
					}
                }
                games.games[current_game_indice].ended = true;
                return;
            }
            if move_message == *"CUT" || move_message == *"SHORT"  {
                // Un des deux joueurs s'est déconnecté, et celui qui est resté a répondu
                // "je suis SHORT" ou "je suis CUT", on lui attribue la victoire
                if !games.games[current_game_indice].ended {
	               	if move_message == *"CUT" {
						update_players(&my_coll, cut_player, short_player, Turn::Cut).await;
					} else {
						update_players(&my_coll, short_player, cut_player, Turn::Short).await;
					}
                }
				games.games[current_game_indice].ended = true;
				let _ = tx.send(move_message + " a gagné"); 
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

async fn add_stat_handler(State(my_coll): State<Collection<serde_json::Value>>, Path((type_game, winner, seed)): Path<(u32, u8, i64)>) {
	let doc = json!({"type_game": type_game, "winner": winner, "seed": seed});
    my_coll.insert_one(doc, None).await.unwrap();
}

async fn get_games_handler(State(my_coll): State<Collection<serde_json::Value>>) -> Result<String, (StatusCode, String)> {
	match get_games(my_coll).await {
		Ok(games) => Ok(games),
		Err(e) => Err((StatusCode::BAD_REQUEST, e.to_string()))
	}
}

async fn get_games(my_coll: Collection<serde_json::Value>) -> Result<String, mongodb::error::Error> {
    let number_total_games = my_coll.count_documents(None, None).await.unwrap();
    let number_cut_games = my_coll
        .count_documents(doc! { "winner": 0}, None)
        .await?;
    let number_short_games = my_coll
        .count_documents(doc! { "winner": 1}, None)
        .await?;
    let number_online_games = my_coll
        .count_documents(doc! {"type_game": 2}, None)
        .await?;
    let games = json!({
        "stats": vec![number_total_games, number_cut_games, number_short_games, number_online_games]
    });
    Ok(games.to_string())
}

async fn insert_player_handler(State(my_coll): State<Collection<Player>>, Json(player): Json<CreatePlayer>) -> Result<String, (StatusCode, String)> {
	let pseudo = player.pseudo;
	let password = player.password;
	let password_repeat = player.password_repeat;
	if password != password_repeat
	{
		return Err((StatusCode::UNAUTHORIZED, "Les mots de passe ne correspondent pas".to_string()));
	}
	if password.is_empty() {
		return Err((StatusCode::BAD_REQUEST, "Le mot de passe est vide".to_string()));
	}
	let bcrypt_password_with_salt = bcrypt::hash(password, 12).unwrap(); // Hash avec bcrypt pour simplifier le salage
    insert_player(&pseudo.to_string(), bcrypt_password_with_salt, my_coll).await?;
    Ok("Player inserted".to_string())
}

async fn insert_player(pseudo: &str, password_hash: String, my_coll: Collection<Player>) -> Result<(), (StatusCode, String)> {
    if pseudo.len() < 3 {
		return Err((StatusCode::BAD_REQUEST, "Pseudo trop court".to_string()));
	}
    else if pseudo.len() > 20 {
    	return Err((StatusCode::BAD_REQUEST, "Pseudo trop long".to_string()));
    }
    else if my_coll.find_one(doc! {"pseudo": pseudo}, None).await.unwrap().is_some() {
    	return Err((StatusCode::CONFLICT, "Pseudo déjà pris".to_string()));
    }
    let player = Player {
		pseudo: pseudo.to_string(),
		password_hash,
		elo: 1200, // Elo de base
		nb_games: 0,
	};
    my_coll.insert_one(player, None).await.unwrap();
    Ok(())
}

async fn log_in(State(my_coll): State<Collection<Player>>, Json(player): Json<LogInPlayer>) -> Result<String, (StatusCode, String)> {
	let user = my_coll.find_one(doc! {"pseudo": player.pseudo}, None).await.unwrap();
	match user {
		None => {
			Err((StatusCode::UNAUTHORIZED, "Pseudo ou mot de passe incorrect".to_string()))
		}
		Some(user) => {
			if bcrypt::verify(player.password, &user.password_hash).unwrap() { 
				// On vérifie le mot de passe avec bcrypt pour éviter les attaques par dictionnaire,
				// les attaques par rainbow tables ainsi que les attaques par brute force
				Ok("Logged in".to_string())
			}
			else {
				Err((StatusCode::UNAUTHORIZED, "Pseudo ou mot de passe incorrect".to_string()))
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
	if player_cut == player_short {
		return;
	}
	let cut_player = my_coll.find_one(doc! {"pseudo": player_cut}, None).await.unwrap();
	let short_player = my_coll.find_one(doc! {"pseudo": player_short}, None).await.unwrap();
	let cut_elo = if let Some(player) = &cut_player {
		player.elo as f64
	}
	else {
		0.0 // Si le joueur n'existe pas, on met son elo à 0, ce n'est pas grave car il ne sera pas utilisé
	};
	match &cut_player {
		None => {}
		Some(player) => {
			if short_player.is_some() {
				let mut k_cut = match player.nb_games {
					0..=29 => 40.0,
					_ => 20.0,
				};
				let w_cut = match winner {
					Turn::Cut => 1.0,
					_ => 0.0,
				};
				let elo = player.elo as f64;
				if elo > 2400.0 && player.nb_games >= 30 {
				    k_cut = 10.0;
				}
				// Calcul du nouvel elo
				let short_elo = short_player.as_ref().unwrap().elo as f64;
				let new_elo = elo+k_cut*(w_cut-p(elo-short_elo));
				set_elo(my_coll, player, new_elo as u32).await;
				increase_nb_games(my_coll, player).await;
			}
		}
	}
	// On fait la même chose pour le joueur short
	match &short_player {
		None => {}
		Some(player) => {
			if cut_player.is_some() {
				let mut k_short = match player.nb_games {
					0..=29 => 40.0,
					_ => 20.0,
				};
				let w_short = match winner {
					Turn::Short => 1.0,
					_ => 0.0,
				};
				let elo = player.elo as f64;
				if elo > 2400.0 && player.nb_games >= 30 {
				    k_short = 10.0;
				}
				let new_elo = elo+k_short*(w_short-p(elo-cut_elo));
				set_elo(my_coll, player, new_elo as u32).await;
				increase_nb_games(my_coll, player).await;
			}
		}
	}
}

// Fonction de calcul de probabilité de victoire (wikipedia)
fn p(d: f64) -> f64 {
	1.0 / (1.0 + 10.0_f64.powf(-d / 400.0))
}

async fn get_elo(State(my_coll): State<Collection<Player>>, Path(pseudo): Path<String>) -> Result<String, (StatusCode, String)> {
	let player = my_coll.find_one(doc! {"pseudo": pseudo}, None).await.unwrap();
	match player {
		None => {
			Err((StatusCode::NOT_FOUND, "Joueur non trouvé".to_string()))
		}
		Some(player) => {
			Ok(player.elo.to_string())
		}
	}
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
    cut_player: String,
    short_player: String,
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

use std::net::{IpAddr, Ipv4Addr, TcpListener};
use std::io::{Read, Write};
use std::sync::Arc;
use std::thread;
use serde::{Deserialize, Serialize};
use reqwest::blocking::Client;
use std::time::SystemTime;
use std::env;

use bot::handler;
// use bot::handler;
use handler::{NAME, AVATAR, PORT, handle_request};

#[derive(Serialize, Deserialize)]
struct JoinData {
    name: &'static str,
    url: String,
    avatar: &'static str,
    model: &'static str,
}

fn get_local_ip() -> String {
    let listener = TcpListener::bind((Ipv4Addr::UNSPECIFIED, 0)).expect("Failed to bind socket");
    let local_addr = listener.local_addr().expect("Failed to get local address");
    local_addr.ip().to_string()
}

fn send_initial_post(remote_server: &str, ip: &str) {
    let client = Client::new();
    let data = JoinData {
        name: NAME,
        url: format!("http://{}:{}", ip, PORT),
        avatar: AVATAR,
        model: "llama3.1",
    };

    if let Err(e) = client.post(&format!("http://{}/join", remote_server))
        .json(&data)
        .send()
    {
        eprintln!("Error sending initial POST request: {}", e);
    }
}

fn handle_client(mut stream: std::net::TcpStream) {
    let mut buffer = [0; 1024];
    if let Ok(size) = stream.read(&mut buffer) {
        let request = String::from_utf8_lossy(&buffer[..size]);

        if request.starts_with("POST ") {
            let body_start = request.find("\r\n\r\n").unwrap_or(request.len()) + 4;
            if let Ok(json_data) = serde_json::from_str::<serde_json::Value>(&request[body_start..]) {
                let response_data = handle_request(json_data);
                let response_json = serde_json::to_string(&response_data).unwrap();

                let response = format!(
                    "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: {}\r\n\r\n{}",
                    response_json.len(), response_json
                );

                stream.write_all(response.as_bytes()).unwrap();
            }
        }
    }
}

fn run_server(remote_server: &str) {
    let ip = get_local_ip();
    send_initial_post(remote_server, &ip);

    let listener = TcpListener::bind(format!("0.0.0.0:{}", PORT)).expect("Failed to bind to port");
    println!("Starting server on {}:{}...", ip, PORT);

    for stream in listener.incoming() {
        if let Ok(stream) = stream {
            thread::spawn(|| handle_client(stream));
        }
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        eprintln!("Usage: cargo run <remote-http-server>");
        return;
    }
    run_server(&args[1]);
}

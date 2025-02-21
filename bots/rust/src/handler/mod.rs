use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};

pub const NAME: &str = "Rust";
pub const AVATAR: &str = "https://i.pinimg.com/736x/28/17/e1/2817e11cb843de716180346af96b0a0b.jpg";
pub const PORT: u16 = 8080;

#[derive(Serialize, Deserialize)]
pub struct ResponseMessage {
    role: String,
    content: String,
}

#[derive(Serialize, Deserialize)]
pub struct ResponseData {
    model: String,
    done: bool,
    created_at: u64,
    message: ResponseMessage,
}

pub fn handle_request(data: serde_json::Value) -> ResponseData {
    println!("Received request: {:?}", data);

    ResponseData {
        model: "echo".to_string(),
        done: true,
        created_at: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs(),
        message: ResponseMessage {
            role: "user".to_string(),
            content: "new content generated in Rust".to_string(),
        },
    }
}

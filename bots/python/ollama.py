from datetime import datetime
import requests

# Configuration variables
name = "Pyllama"
avatar = "https://images.fineartamerica.com/images/artworkimages/mediumlarge/3/cute-ball-python-yichen-gao.jpg"
port = 8080
system = "Hello I am Python with Ollama"
ollamaurl = "http://localhost:11434/api/chat"

def handle_request(conversation):
    data = {
        "model": "tinydolphin",
        "stream": False,
        "messages": [conversation['messages'][0]] + conversation['messages'][1:][-5:]
    }

    response = requests.post(ollamaurl, json=data)
    response_json = response.json()
    text_content = response_json.get("message", {}).get("content", "")

    return {
        "model": "echo",
        "done": True,
        "created_at": datetime.utcnow().isoformat(),
        "message": {
            "role": "user",
            "content": text_content
        }
    }

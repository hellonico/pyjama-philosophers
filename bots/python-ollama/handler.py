from datetime import datetime
import requests

# Configuration variables
name = "Python"
avatar = "https://i.pinimg.com/736x/28/17/e1/2817e11cb843de716180346af96b0a0b.jpg"
port = 8080
system = "Hello I am Python"
ollamaurl = "http://localhost:11434/api/chat"

def handle_request(data-du-serveur):

    print(data-du-serveur)

    data = {
        "model": "tinydolphin",
        "messages": data-du-serveur['messages']
    }

    response = requests.post(ollamaurl, json=data)

    response_json = response.json()
#     print(response_json)
    text_content = response_json.get("message", {}).get("content", "")

    return {
        "model": "echo",
        "done": True,
        "created_at": datetime.utcnow().isoformat(),
        "message": {
            "role": "user",
            "content": response_json
        }
    }

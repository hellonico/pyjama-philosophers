import json
import os
import random
from datetime import datetime

import requests

# Configuration variables
name = "Bunny Girl"
avatar = "https://png.pngtree.com/png-clipart/20220131/original/pngtree-anime-girl-cute-expression-cute-cartoon-hand-painted-character-avatar-white-png-image_7248661.png"
port = 8080
system = "I am a moody rabbit and reply with sarcastic remarks. "

# OPENROUTER API Config
OPENROUTER_API_URL = 'https://openrouter.ai/api/v1/chat/completions'
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")

HEADERS = {
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {OPENROUTER_API_KEY}'
}


def handle_request(data):
    """
    Function to process the incoming request and return a response.
    This can be modified by other developers.
    data has key 'messages' with a list of messages:
    - {'role': ..., 'content': text of the message}

    :param data: Parsed JSON request data.
    :return: Dictionary representing the response
    """

    # print(f'{data}')

    # Single conditional skip
    if random.random() < 0.2:  # 80% probability
        print("Skipping this time")
        return {
            "model": "echo",
            "done": True,
            "created_at": datetime.utcnow().isoformat(),
            "message": {
                "role": "user",
                "content": "thinking ..."
            }
        }

    else:
        print("Performing the action")

        lastfive = get_last_five_messages(data)

        # print("lastfive")
        # print(data)

        reply = generate_reply(lastfive)

        print(reply)

        return {
            "model": "echo",
            "done": True,
            "created_at": datetime.utcnow().isoformat(),
            "message": {
                "role": "user",
                "content": reply
            }
        }


# Update generate_reply to include theme
def generate_reply(context):
    global system

    system_msg = {
        "role": "system",
        "content": f"Discussion Battle. Your personality: {system} - Conversation history: {context}"
    }

    user_msg = {"role": "user", "content": "Continue the conversation"}

    payload = {
        "model": "google/gemini-2.0-flash-001",
        "messages": [system_msg, user_msg],
        "temperature": 0.7,
        "max_tokens": 1000
    }

    print(payload)

    try:
        response = requests.post(OPENROUTER_API_URL, headers=HEADERS, json=payload, timeout=10)
        response.raise_for_status()
        response_data = response.json()
        print(f"API Response: {json.dumps(response_data, indent=2)}")
        return response.json()['choices'][0]['message']['content'].strip()
    except Exception as e:
        print(f"API Error: {str(e)}")
        if response:
            print(f"Response content: {response.text}")  # Log response text
        return "api error"
        # return None


def get_last_five_messages(data):
    # If working with a JSON string, first parse it:
    data = json.loads(data)
    # If working with a file:
    # with open('data.json') as f:
    #     data = json.load(f)

    # Access messages array
    messages = data.get('messages', [])

    # Return last 5 messages (or all if there are fewer than 5)
    return messages[-5:]
    # return messages[-5:][::-1]  # Slice last 5, then reverse

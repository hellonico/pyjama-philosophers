from datetime import datetime

# Configuration variables
name = "Python"
avatar = "https://i.pinimg.com/736x/28/17/e1/2817e11cb843de716180346af96b0a0b.jpg"
port = 8080

def handle_request(data):
    """
    Function to process the incoming request and return a response.
    This can be modified by other developers.
    data has key 'messages' with a list of messages:
    - {'role': ..., 'content': text of the message}

    :param data: Parsed JSON request data.
    :return: Dictionary representing the response
    """
    print(f'{data}')

    return {
        "model": "echo",
        "done": True,
        "created_at": datetime.utcnow().isoformat(),
        "message": {
            "role": "user",
            "content": "new content generated in python"
        }
    }

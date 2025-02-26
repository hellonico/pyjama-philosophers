import requests

url = "http://localhost:11434/api/chat"
data = {
    "model": "tinydolphin",
    "stream": False,
    "messages": [
        {"role": "system", "content": "You are a helpful AI assistant."},
        {"role": "user", "content": "Hello!"},
        {"role": "assistant", "content": "Hi there! How can I assist you today?"},
        {"role": "user", "content": "Tell me a fun fact."}
    ]
}

response = requests.post(url, json=data)
# print(response.text)
response_json = response.json()
# print(response_json)
# Extract just the content of the assistant's message
text_content = response_json.get("message", {}).get("content", "")

print(text_content)
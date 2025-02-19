import http.client
import json
import os
import sys
from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer

os.environ['NO_PROXY'] = '127.0.0.1'

# def get_local_ip():
#     """Retrieve the local machine's IP address"""
#     s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#     s.connect(('8.8.8.8', 80))  # Connect to a remote server (Google DNS)
#     ip = s.getsockname()[0]  # Get the local machine's IP address
#     s.close()
#     return ip

import socket

def get_local_ip():
    """Retrieve the local network IP address (LAN IP)"""
    try:
        hostname = socket.gethostname()
        local_ip = socket.gethostbyname(hostname)

        # Check if the obtained IP is a loopback address, if so, find a better one
        if local_ip.startswith("127.") or local_ip == "0.0.0.0":
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(("192.168.1.1", 80))  # Connect to a common local gateway
                local_ip = s.getsockname()[0]  # Get the assigned local IP

        return local_ip
    except Exception as e:
        print(f"Error getting local IP: {e}")
        return "127.0.0.1"  # Fallback to localhost if detection fails

def send_initial_post(remote_server, ip, port, name, endpoint, avatar):
    try:
        # Prepare the data to send
        data = {
            "name": name,
            "url": f"http://{ip}:{port}",
            "avatar": avatar,
            "model": "llama3.1"
        }

        # Log the URL and data for debugging
        print(f"Sending initial POST to {remote_server}/join with data: {json.dumps(data)}")

        # Establish a connection to the remote server
        conn = http.client.HTTPConnection(remote_server)

        # Send the POST request
        conn.request("POST", "/join", body=json.dumps(data), headers={"Content-Type": "application/json"})

        # Get the response
        response = conn.getresponse()
        print(f"Initial POST response: {response.status} {response.reason}")
        conn.close()
    except Exception as e:
        print(f"Error sending initial POST request: {e}")

class RequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        # Read the body of the request
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)

        # Parse the JSON data
        try:
            data = json.loads(post_data)

            # Create the response
            response = {
                "model": "echo",
                "done": True,
                "created_at": datetime.utcnow().isoformat(),
                "message": {
                    "role": "user",
                    "content": "new content generated in python"
                }
            }

            # Send response headers
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()

            print(f"{json.dumps(response).encode()}")

            # Write response body
            self.wfile.write(json.dumps(response).encode())

        except json.JSONDecodeError:
            # If the request body is not valid JSON
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b'Invalid JSON')

def run(server_class=HTTPServer, handler_class=RequestHandler, port=8080, remote_server="localhost", name="MyName", endpoint="http://localhost", avatar="http://example.com/avatar.png"):
    # Get the local IP address
    ip = get_local_ip()

    # Send the initial HTTP POST request
    send_initial_post(remote_server, ip, port, name, endpoint, avatar)

    # Start the local HTTP server
    server_address = ('0.0.0.0', port)
    httpd = server_class(server_address, handler_class)
    print(f"Starting server on {ip}:{port}...")
    httpd.serve_forever()

if __name__ == '__main__':
    # Get the remote server address from command-line arguments
    if len(sys.argv) < 2:
        print("Usage: python script.py <remote-http-server>")
        sys.exit(1)

    remote_server = sys.argv[1]
    name = "Python"  # You can also pass this as a command-line argument if needed
#     endpoint = "http://localhost"
    avatar = "images/python.webp"
    port = 8080  # You can also pass this as a command-line argument if needed

    run(remote_server=remote_server, name=name, avatar=avatar, port=port)
import json
import sys
import http.client
import importlib
from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer
import socket

# Import handler module dynamically
# handler = importlib.import_module("handler")
module_name = sys.argv[2] if len(sys.argv) > 1 else "handler"
handler = importlib.import_module(module_name)

# Retrieve configuration from handler.py
NAME = getattr(handler, "name", "DefaultName")
AVATAR = getattr(handler, "avatar", "http://example.com/avatar.png")
PORT = getattr(handler, "port", 8080)
SYSTEM = getattr(handler, "system", "I am the scary python")
MODEL = getattr(handler, "model", "llama3.1")

def get_local_ip():
    """Retrieve the local network IP address (LAN IP)"""
    try:
        hostname = socket.gethostname()
        local_ip = socket.gethostbyname(hostname)

        if local_ip.startswith("127.") or local_ip == "0.0.0.0":
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(("192.168.1.1", 80))
                local_ip = s.getsockname()[0]

        return local_ip
    except Exception as e:
        print(f"Error getting local IP: {e}")
        return "127.0.0.1"


def send_initial_post(remote_server, ip):
    try:
        data = {
            "name": NAME,
            "url": f"http://{ip}:{PORT}",
            "avatar": AVATAR,
            "model": MODEL, # this is just for showing
            "system": SYSTEM,
        }

        print(f"Sending initial POST to {remote_server}/join with data: {json.dumps(data)}")

        conn = http.client.HTTPConnection(remote_server)
        conn.request("POST", "/join", body=json.dumps(data), headers={"Content-Type": "application/json"})

        response = conn.getresponse()
        print(f"Initial POST response: {response.status} {response.reason}")
        conn.close()
    except Exception as e:
        print(f"Error sending initial POST request: {e}")


class RequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)

        try:
            data = json.loads(post_data)
            response = handler.handle_request(data)  # Call the function from handler.py

            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())

        except json.JSONDecodeError:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b'Invalid JSON')


def run(server_class=HTTPServer, handler_class=RequestHandler, remote_server="localhost"):
    ip = get_local_ip()
    send_initial_post(remote_server, ip)

    server_address = ('0.0.0.0', PORT)
    httpd = server_class(server_address, handler_class)
    print(f"Starting server on {ip}:{PORT}...")
    httpd.serve_forever()


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python main.py <remote-http-server> <handler>")
        sys.exit(1)

    remote_server = sys.argv[1]
    run(remote_server=remote_server)

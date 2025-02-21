require 'json'
require 'socket'
require 'net/http'
require 'webrick'
require_relative 'handler'

NAME = Handler::NAME || "DefaultName"
AVATAR = Handler::AVATAR || "http://example.com/avatar.png"
PORT = Handler::PORT || 8080
SYSTEM = Handler::SYSTEM || "Hello, I am ruby"

# Retrieve the local network IP address
def get_local_ip
  begin
    ip = Socket.ip_address_list.detect { |addr| addr.ipv4_private? }&.ip_address
    ip || "127.0.0.1"
  rescue => e
    puts "Error getting local IP: #{e}"
    "127.0.0.1"
  end
end

# Send an initial POST request to the remote server
def send_initial_post(remote_server, ip)
  uri = URI("http://#{remote_server}/join")
  data = {
    name: NAME,
    url: "http://#{ip}:#{PORT}",
    system: SYSTEM,
    avatar: AVATAR,
    model: "llama3.1"
  }

  begin
    puts "Sending initial POST to #{uri} with data: #{data.to_json}"
    response = Net::HTTP.post(uri, data.to_json, {"Content-Type" => "application/json"})
    puts "Initial POST response: #{response.code} #{response.message}"
  rescue => e
    puts "Error sending initial POST request: #{e}"
  end
end

# Define the HTTP request handler
class RequestHandler < WEBrick::HTTPServlet::AbstractServlet
  def do_POST(request, response)
    begin
      data = JSON.parse(request.body)
      response_data = Handler.handle_request(data)
      response.status = 200
      response['Content-Type'] = 'application/json'
      response.body = response_data.to_json
    rescue JSON::ParserError
      response.status = 400
      response.body = 'Invalid JSON'
    end
  end
end

# Start the server
def run(remote_server)
  ip = get_local_ip
  send_initial_post(remote_server, ip)

  server = WEBrick::HTTPServer.new(:Port => PORT, :BindAddress => '0.0.0.0')
  server.mount '/', RequestHandler

  trap('INT') { server.shutdown }
  puts "Starting server on #{ip}:#{PORT}..."
  server.start
end

if __FILE__ == $0
  if ARGV.empty?
    puts "Usage: ruby script.rb <remote-http-server>"
    exit 1
  end

  remote_server = ARGV[0]
  run(remote_server)
end

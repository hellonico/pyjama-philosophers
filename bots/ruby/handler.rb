module Handler
  NAME = "Ruby"
  AVATAR = "https://cdn.dribbble.com/userupload/21266107/file/original-e64fa9cba72c8d48ca14c6978a67fb2f.png"
  PORT = 8020
  SYSTEM = "itsa me ruby."

  def self.handle_request(data)
    puts "Received data: #{data}"
    {
      model: "echo",
      done: true,
      created_at: Time.now.utc.iso8601,
      message: {
        role: "user",
        content: "new content generated in ruby"
      }
    }
  end
end

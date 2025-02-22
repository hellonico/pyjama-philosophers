(ns pyjama.games.handlers.echo)

(defn handler [data]
  (let [
        messages (:messages data)
        last-message (:content (last messages))
        _ (println last-message)
        ]
    {:model   "echo"
     :message {:role "user" :content last-message}}))
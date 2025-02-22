(ns pyjama.games.handlers.piporag
  (:require [pyjama.core]
            [pyjama.io.readers]))

(def knowledge
  (pyjama.io.readers/extract-text "knowledge/Yuval.epub"))

(def ollama-url
  "http://localhost:11434")

(defn handler [data]
  (let [
        messages (:messages data)
        last-message (:content (last messages))
        first-message (:content (first messages))
        _ (println "First message: " first-message)
        _ (println "Last message: " last-message)
        model (or (:model data) "llama3.1")
        response-text
        (pyjama.core/ollama
          ollama-url
          :generate
          {:model  model
           :prompt [knowledge first-message last-message]
           :stream false
           :pre    "This is your knowledge: %s\n.
           This is the first question in the conversation: %s\n.
           This is the last comment in the conversation: %s.
           Make a short witty statement/answer on the last message.."}
          :response
          )
        _ (println "Response: " response-text)
        ]
    {:message {:role "user" :content response-text}}))
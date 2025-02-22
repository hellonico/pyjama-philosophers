(ns pyjama.games.handlers.human)

(defn- get-user-input []
  (print "Your response: ")
  (flush)
  (read-line))

(defn handler [_]
  {:model   "human"
   :message {:role "user" :content (get-user-input)}})
(ns pyjama.games.philosophersv3
  (:gen-class)
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.pprint]
    [clojure.tools.cli]
    [pyjama.state]))

(defn- read-personalities [file]
  (with-open [reader (io/reader file)]
    (doall (rest (csv/read-csv reader)))))

(defn- random-personalities [file]
  (let [all-personalities (read-personalities file)]
    (shuffle all-personalities)))

(defn- load-states [personalities]
  (mapv (fn [[name system url model]]
          (atom {:name     name
                 :url      (if (empty? url)
                             (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
                             url)
                 :model    (or model "tinyllama")
                 :system   system
                 :stream   false
                 :messages []}))
        personalities))

(defn chat-simulation [app-state personalities-file turns original-question broadcast-fn]
  (let [states (load-states (random-personalities personalities-file))]
    (doseq [state states]
      (swap! state assoc
             :messages [{:role :system :content
                         (str "This is a conversation battle. Everyone should chat, with simple, witty answers.
                              May the most intelligent win. " original-question)}]))

    (loop [remaining-turns turns
           last-speaker-idx nil]

      (when (and (pos? remaining-turns) (:chatting @app-state))
        (let [available-indices (remove #(= % last-speaker-idx) (range (count states)))
              speaker-idx (nth available-indices (rand-int (count available-indices)))
              speaker-atom (nth states speaker-idx)]
          (pyjama.state/handle-chat speaker-atom)

          (while (:processing @speaker-atom)
            (Thread/sleep 500))

          (let [last-response (last (:messages @speaker-atom))
                formatted-response (assoc last-response
                                     :role :user
                                     :content (str (:name @speaker-atom) "> " (:content last-response)))]

            (if (:chatting @app-state)
              (do
                (broadcast-fn {:image (str "/images/" (:name @speaker-atom) ".png") :name (:name @speaker-atom) :text (:content last-response)})

                ;(Thread/sleep 2000)

                (doseq [state states]
                  (when (not= state speaker-atom)
                    (swap! state update :messages conj formatted-response)))))

            (recur (dec remaining-turns) speaker-idx)))))

    (broadcast-fn {:image "/images/end.png"
                   :name  ""
                   :text  "This battle has finished"})

    ))
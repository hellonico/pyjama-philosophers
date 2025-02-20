(ns pyjama.games.philosophersv4
  (:gen-class)
  (:require
    [clojure.pprint]
    [clojure.tools.cli]
    [pyjama.state]))

(defn random-robin [app-state last-speaker-name]
  (let [
        all-people (:people @app-state)
        ;_ (clojure.pprint/pprint all-people)
        available (remove #(or
                             (and (contains? (deref %) :alive) (= false (:alive (deref %))))
                             (= last-speaker-name (:name (deref %)))) all-people)
        _ (println "Available to speak:" (map #(:name (deref %)) available))
        ]
    (rand-nth available)))

(defn tell-everybody-else [states current-speaker new-message]
  (doseq [state states]
    (when (not= state current-speaker)
      (swap! state update :messages conj new-message))))

(defn chat-simulation [app-state turns original-question broadcast-fn]
  (let [states (:people @app-state)]

    (doseq [state states]
      (swap! state assoc
             :messages [{:role :system :content
                         (str (:battle-message @app-state)
                              original-question)}]))

    (loop [remaining-turns turns
           last-speaker-name nil]

      (when (and (pos? remaining-turns) (:chatting @app-state))
        (let [
              speaker-atom (random-robin app-state last-speaker-name)
              speaker (:name @speaker-atom)
              position (rand-nth [:left :right])
              ]

          (println speaker " is talking...")
          ; who is speaking ...
          (broadcast-fn {:image (:avatar @speaker-atom) :name speaker :position position :text "..."})

          (pyjama.state/handle-chat speaker-atom)

          ; wait for input
          (while (:processing @speaker-atom)
            (Thread/sleep 500))

          (if (not (nil? (:error @speaker-atom)))
            (do
              (println (:error @speaker-atom))
              (broadcast-fn {:image (:avatar @speaker-atom) :name speaker :position position :text "<has left the chat>>"})
              (tell-everybody-else states speaker-atom {:role :system :content (str speaker " has left the chat")}))
            (assoc @speaker-atom :alive false))
          (let [last-response (last (:messages @speaker-atom))
                formatted-response (assoc last-response
                                     :role :user
                                     :content (str (:name @speaker-atom) "> " (:content last-response)))]

            (swap! app-state update :messages conj
                   {:name speaker :role :assistant :content (:content last-response)})

            (if (:chatting @app-state)
              (do
                (broadcast-fn {:image (:avatar @speaker-atom) :position position :name speaker :text (:content last-response)})
                (Thread/sleep ^long (:lag @app-state))
                (tell-everybody-else states speaker-atom formatted-response))))

          (recur (dec remaining-turns) speaker))))

    (broadcast-fn {:image "/images/end.png"
                   :name  ""
                   :text  "This battle has finished"})

    ))
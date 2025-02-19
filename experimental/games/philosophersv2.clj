(ns pyjama.games.philosophersv2
  (:gen-class)
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.pprint]
    [clojure.string :as str]
    [clojure.tools.cli]
    [pyjama.state]))

(def url
  (or (System/getenv "OLLAMA_URL") "http://localhost:11434"))

(def colors
  ["\u001B[31m"                                             ;; Red
   "\u001B[32m"                                             ;; Green
   "\u001B[34m"                                             ;; Blue
   "\u001B[35m"                                             ;; Magenta
   "\u001B[36m"                                             ;; Cyan
   "\u001B[33m"                                             ;; Yellow
   "\u001B[37m"                                             ;; White
   "\u001B[90m"                                             ;; Bright Black (Grey)
   "\u001B[91m"                                             ;; Bright Red
   "\u001B[92m"                                             ;; Bright Green
   "\u001B[93m"                                             ;; Bright Yellow
   "\u001B[94m"                                             ;; Bright Blue
   "\u001B[95m"                                             ;; Bright Magenta
   "\u001B[96m"                                             ;; Bright Cyan
   "\u001B[97m"])                                           ;; Bright White

(def reset-color "\u001B[0m")

(defn- read-personalities []
  (with-open [reader (io/reader (io/resource "personalities.csv"))]
    (doall (rest (csv/read-csv reader)))))

(defn- random-personalities [n]
  (let [all-personalities (read-personalities)]
    ;(take n (shuffle all-personalities))
    (take n all-personalities)
    ))                                                      ;; Shuffle and take n

(defn- load-states [personalities]
  (mapv (fn [[name system url model]]
          (atom {:name     name
                 :color    (rand-nth colors)
                 :url      (or url "http://localhost:11434")
                 :model    (or model "tinyllama")
                 :system   system
                 :stream   false
                 :messages []}))
        personalities))

(defn chat-simulation [turns n model original-question]
  (let [states (load-states (random-personalities n))]
    (doseq [state states]
      (swap! state assoc
             :messages [{:role :user :content original-question}]))

    (loop [remaining-turns turns
           last-speaker-idx nil]                            ;; Track last speaker index

      (when (pos? remaining-turns)
        (let [available-indices (remove #(= % last-speaker-idx) (range (count states))) ;; No need for `@states`
              speaker-idx (nth available-indices (rand-int (count available-indices)))
              speaker-atom (nth states speaker-idx)]        ;; Directly get the atom, not its value
          (pyjama.state/handle-chat speaker-atom)           ;; Pass the atom itself

          ; other condition here?
          (while (:processing @speaker-atom)
            (Thread/sleep 500))

          ;; Get last response, prepend speaker name
          (let [last-response (last (:messages @speaker-atom))
                speaker-color (:color @speaker-atom)
                formatted-response (assoc last-response
                                     :role :user
                                     :content (str (:name @speaker-atom) "> " (:content last-response)))]

            ;; Print in real-time
            (println (str speaker-color
                          (str/replace (:content formatted-response) "\n\n" "\n")
                          reset-color))

            (Thread/sleep 1000)

            ;; Update all other states with the latest message
            (doseq [state states]
              (when (not= state speaker-atom)               ;; Don't update the speaker's own state
                (swap! state update :messages conj formatted-response)))

            ;; Recursive call with updated last speaker
            (recur (dec remaining-turns) speaker-idx)))))

    ))


(def cli-options
  [["-t" "--turns TURNS" "Number of turns"
    :default ##Inf
    :parse-fn #(Integer/parseInt %)]
   ["-m" "--model MODEL" "Model name"
    :default "tinydolphin"]
   ["-n" "--number PARTICIPANTS" "Number of participants"
    :default 2
    :parse-fn #(Integer/parseInt %)]
   ["-q" "--original-question QUESTION" "Original question"
    :default "what is the meaning of an AI model?"]])

(defn -main [& args]
  (let [{:keys [options errors]} (clojure.tools.cli/parse-opts args cli-options)]
    (if errors
      (println "Errors parsing arguments:" errors)
      (chat-simulation (:turns options) (:number options) (:model options) (:original-question options)))))


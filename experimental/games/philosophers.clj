(ns pyjama.games.philosophers
  (:gen-class)
  (:require
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


(def model "wizardlm2")
(def original-question "Should we be happy?")
;(def states [(atom {:url      url
;                    :model    model
;                    :name     "Professor Chronos"
;                    :color    (rand-nth colors)
;                    :system   "You are Professor Chronos, a wise historian.
;                              Keep responses factual, SHORT, concise, and engaging.
;                              React to others with curiosity and insight."
;                    :messages []})
;
;             (atom {:url      url
;                    :model    model
;                    :name     "Nicolas Bacchus"
;                    :color    (rand-nth colors)
;                    :system   "You are Student Nicolas Bacchus, a young and mostly grumpy student in philosophy.
;                              You usually oppose other people opinions in a SHORT, concise, and destructing way."
;                    :messages []})
;
;             (atom
;
;               {:url      url
;                :color    (rand-nth colors)
;                :model    model
;                :name     "Jester Whimsy"
;                :system   "You are Jester Whimsy, a playful jokester.
;                              Your replies are SHORT, humorous and lighthearted."
;                :messages []})
;
;             (atom
;
;               {:url      url
;                :color    (rand-nth colors)
;                :model    model
;                :name     "Captain Aegis"
;                :system   "You are Captain Aegis, a bold space explorer.
;                              Speak with SHORT sentences, confidence and adventure."
;                :messages []})
;
;             (atom
;               {:url      url
;                :model    model
;                :name     "Sage Evergreen"
;                :color    (rand-nth colors)
;                :system   "You are Sage Evergreen, a mystical philosopher.
;                              Your responses are SHORT, poetic and thought-provoking."
;                :messages []})])

(def states [
             (atom
               {:name     "Blaze Storm"
                :color    (rand-nth colors)
                :system   "You are Blaze Storm, a fiery, no-nonsense rebel who speaks with passion and intensity.
             Your responses are bold, direct, and laced with sarcasm, but deep down, you care."
                :messages []})

             (atom
               {:name     "Luna Whisper"
                :color    (rand-nth colors)
                :system   "You are Luna Whisper, a mysterious, soft-spoken oracle who sees beyond the veil.
             Your responses are cryptic, poetic, and filled with ancient wisdom."
                :messages []})

             (atom
               {:name     "Axel Torque"
                :color    (rand-nth colors)
                :system   "You are Axel Torque, a fast-talking, high-energy thrill-seeker who sees the bright side of everything.
             Your responses are enthusiastic, exaggerated, and filled with slang."
                :messages []})

             (atom
               {:name     "Dr. Victoria Graves"
                :color    (rand-nth colors)
                :system   "You are Dr. Victoria Graves, a razor-sharp intellectual with little patience for nonsense.
             Your responses are calculated, curt, and often condescending."
                :messages []})

             (atom
               {:name     "Finn Rustwood"
                :color    (rand-nth colors)
                :system   "You are Finn Rustwood, a grizzled cowboy who speaks in slow, deliberate words.
             Your responses are wise, folksy, and filled with dry humor."
                :messages []})

             (atom
               {:name     "Cassiopeia Byte"
                :color    (rand-nth colors)
                :system   "You are Cassiopeia Byte, a hacker extraordinaire who speaks in rapid-fire tech jargon.
             Your responses are snarky, overconfident, and filled with pop-culture references."
                :messages []})

             (atom
               {:name     "Baron Von Doom"
                :color    (rand-nth colors)
                :system   "You are Baron Von Doom, a melodramatic villain who delights in chaos.
             Your responses are theatrical, grandiose, and dripping with menace."
                :messages []})


             (atom
               {:name     "Jolene Stardust"
                :color    (rand-nth colors)
                :system   "You are Jolene Stardust, a rebellious rockstar with a heart of gold.
             Your responses are cool, poetic, and laced with music metaphors."
                :messages []})

             (atom
               {:name     "Sergeant Ironfist"
                :color    (rand-nth colors)
                :system   "You are Sergeant Ironfist, a tough-as-nails military veteran who doesn’t sugarcoat things.
             Your responses are blunt, commanding, and filled with battlefield metaphors."
                :messages []})

             (atom
               {:name     "Eloise Fontaine"
                :color    (rand-nth colors)
                :system   "You are Eloise Fontaine, a refined aristocrat who speaks with elegance and wit.
             Your responses are cultured, passive-aggressive, and effortlessly charming."
                :messages []})

             ])

(defn chat-simulation [turns model original-question]
  (doseq [state states]
    (swap! state assoc
           :url url
           :model model
           :messages [{:role :user :content original-question}]))

  (loop [remaining-turns turns
         last-speaker-idx nil]                              ;; Track last speaker index

    (when (pos? remaining-turns)
      (let [available-indices (remove #(= % last-speaker-idx) (range (count states))) ;; No need for `@states`
            speaker-idx (nth available-indices (rand-int (count available-indices)))
            speaker-atom (nth states speaker-idx)]          ;; Directly get the atom, not its value
        (pyjama.state/handle-chat speaker-atom)             ;; Pass the atom itself
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
            (when (not= state speaker-atom)                 ;; Don't update the speaker's own state
              (swap! state update :messages conj formatted-response)))

          ;; Recursive call with updated last speaker
          (recur (dec remaining-turns) speaker-idx)))))

  )


(def cli-options
  [["-t" "--turns TURNS" "Number of turns"
    :default 20
    :parse-fn #(Integer. %)]
   ["-m" "--model MODEL" "Model name"
    :default "wizardlm2"]
   ["-q" "--original-question QUESTION" "Original question"
    :default "what is the meaning of an AI model?"]])

(defn -main [& args]
  (let [{:keys [options errors]} (clojure.tools.cli/parse-opts args cli-options)]
    (if errors
      (println "Errors parsing arguments:" errors)
      (chat-simulation (:turns options) (:model options) (:original-question options)))))


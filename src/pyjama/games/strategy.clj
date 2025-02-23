(ns pyjama.games.strategy
  (:require [clojure.string :as str]))

(defn- filter-available-speakers [app-state last-speaker-name]
  (let [all-people (:people @app-state)]
    (remove #(or
               (and (contains? (deref %) :alive) (= false (:alive (deref %))))
               (= last-speaker-name (:name (deref %))))
            all-people)))

(defn round-robin [app-state last-speaker-name]
  "Selects the next speaker in a circular order based on last-speaker-name."
  (let [available (filter-available-speakers app-state last-speaker-name)
        names (map #(-> % deref :name) available)
        next-index (mod (inc (.indexOf names last-speaker-name)) (count names))]
    (nth available next-index)))

(defn weighted-random [app-state last-speaker-name]
  "Selects next speaker with weighted probability based on recency."
  (let [available (filter-available-speakers app-state last-speaker-name)
        weights (map (fn [speaker] {:speaker speaker :weight (rand)}) available)]
    (:speaker (apply max-key :weight weights))))

(defn least-recently-spoken [app-state _]
  "Selects the speaker who has spoken least recently."
  (let [messages (:messages @app-state)
        last-spoken (group-by :speaker-name messages)
        available (filter-available-speakers app-state nil)]
    (->> available
         (sort-by #(count (get last-spoken (:name (deref %)) [])))
         first)))

(defn queue-based [app-state _]
  "Picks the first available person in the queue."
  (let [queue (:queue @app-state)
        available (filter-available-speakers app-state nil)]
    (first (filter #(some (fn [q] (= (:name (deref %)) q)) queue) available))))

(defn priority-based [app-state _]
  "Picks a speaker from a priority list first, falling back to random."
  (let [priority (:priority @app-state)
        available (filter-available-speakers app-state nil)
        priority-speakers (filter #(some (fn [p] (= (:name (deref %)) p)) priority) available)]
    (if (seq priority-speakers)
      (rand-nth priority-speakers)
      (rand-nth available))))

(defn time-based-boost [app-state _]
  "Gives priority to speakers who have been idle the longest."
  (let [messages (:messages @app-state)
        last-spoken (group-by :speaker-name messages)
        available (filter-available-speakers app-state nil)]
    (->> available
         (sort-by #(or (some-> (last (get last-spoken (:name (deref %)))) :timestamp) Long/MAX_VALUE))
         first)))

(defn group-based-rotation [app-state last-speaker-name]
  "Rotates between groups before picking an individual."
  (let [groups (:groups @app-state)
        last-group (some #(when (some #{last-speaker-name} %) %) groups)
        next-group (or (nth groups (mod (inc (.indexOf groups last-group)) (count groups))) [])
        available (filter-available-speakers app-state last-speaker-name)]
    (rand-nth (filter #(some #{(:name (deref %))} next-group) available))))

(defn last-speaker-exclusion [app-state last-speaker-name]
  "Prevents the last speaker from speaking again immediately."
  (let [available (filter-available-speakers app-state last-speaker-name)]
    (rand-nth available)))

(defn contextual-randomness [app-state last-speaker-name]
  "Chooses someone relevant to the topic."
  (let [messages (:messages @app-state)
        last-topic (:topic (last messages))
        available (filter-available-speakers app-state last-speaker-name)]
    (rand-nth (filter #(some #{last-topic} (:topics (deref %))) available))))

(defn response-probability-weighting [app-state _]
  "Picks someone who was mentioned or hasn't responded yet."
  (let [messages (:messages @app-state)
        last-message (last messages)
        mentioned (set (:mentions last-message))
        available (filter-available-speakers app-state nil)]
    (if-let [candidates (seq (filter #(mentioned (:name (deref %))) available))]
      (rand-nth candidates)
      (rand-nth available))))

(defn entropy-selection [app-state _]
  "Selects someone who hasn't repeated the same topics too much."
  (let [messages (:messages @app-state)
        topic-counts (frequencies (map :topic messages))
        available (filter-available-speakers app-state nil)]
    (->> available
         (sort-by #(or (get topic-counts (:topic (last messages))) 0))
         first)))

(defn engagement-randomness [app-state _]
  "Selects a less engaged speaker more often."
  (let [messages (:messages @app-state)
        engagement (frequencies (map :speaker-name messages))
        available (filter-available-speakers app-state nil)]
    (->> available
         (sort-by #(get engagement (:name (deref %)) 0))
         first)))

(defn chain-reaction [app-state _]
  "Picks a speaker based on word association with the last message."
  (let [messages (:messages @app-state)
        last-message (:content (last messages))
        words (set (str/split last-message #"\s+"))
        available (filter-available-speakers app-state nil)]
    (rand-nth (filter #(some words (set (str/split (:last-message (deref %)) #"\s+"))) available))))

(defn echo-picking [app-state _]
  "Selects someone who echoed key phrases in the conversation."
  (let [messages (:messages @app-state)
        recent-phrases (set (take-last 5 (mapcat #(str/split (:content %) #"\s+") messages)))
        available (filter-available-speakers app-state nil)]
    (rand-nth (filter #(some recent-phrases (set (str/split (:last-message (deref %)) #"\s+"))) available))))

(defn length-weighted-random [app-state _]
  "Reduces the probability of selecting speakers who tend to write long messages."
  (let [messages (:messages @app-state)
        available (filter-available-speakers app-state nil)
        avg-lengths (reduce
                      (fn [acc {:keys [speaker-name content]}]
                        (update acc speaker-name (fnil #(conj % (count content)) [])))
                      {} messages)
        avg-lengths (into {} (map (fn [[k v]] [k (/ (reduce + v) (count v))]) avg-lengths))
        max-length (apply max (vals avg-lengths))
        weights (map (fn [speaker]
                       (let [name (:name (deref speaker))
                             length (get avg-lengths name 1) ;; Default 1 if no messages
                             weight (/ (- max-length length) (inc max-length))] ;; Inverse proportion
                         {:speaker speaker :weight weight}))
                     available)]
    (rand-nth (map :speaker (sort-by #(rand (* (:weight %) (:weight %))) weights)))))



(defn select-speaker [app-state last-speaker-name]
  "Determines the strategy to use based on app-state and picks a speaker."
  (let [strategy (:strategy @app-state)]
    (case strategy
      :round-robin (round-robin app-state last-speaker-name)
      :weighted-random (weighted-random app-state last-speaker-name)
      :least-recently-spoken (least-recently-spoken app-state last-speaker-name)
      :queue-based (queue-based app-state last-speaker-name)
      :length-weighted-random (length-weighted-random app-state last-speaker-name)
      :priority-based (priority-based app-state last-speaker-name)
      :time-based-boost (time-based-boost app-state last-speaker-name)
      :group-based-rotation (group-based-rotation app-state last-speaker-name)
      :last-speaker-exclusion (last-speaker-exclusion app-state last-speaker-name)
      :contextual-randomness (contextual-randomness app-state last-speaker-name)
      :response-probability-weighting (response-probability-weighting app-state last-speaker-name)
      :entropy-selection (entropy-selection app-state last-speaker-name)
      :engagement-randomness (engagement-randomness app-state last-speaker-name)
      :chain-reaction (chain-reaction app-state last-speaker-name)
      :echo-picking (echo-picking app-state last-speaker-name)
      ;; Default to random selection
      (rand-nth (filter-available-speakers app-state last-speaker-name)))))

(ns pyjama.games.joining
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn read-personalities [file]
  (with-open [reader (io/reader file)]
    (let [[headers & rows] (doall (csv/read-csv reader))]
      (map #(zipmap (map keyword headers) %) rows))))


(defn random-personalities [file]
  (let [all-personalities (read-personalities file)]
    (shuffle all-personalities)))

(defn load-one-philosopher [{:keys [name system url model temperature avatar]}]
  (atom {:name     name
         :url      (if (empty? url)
                     (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
                     url)
         :model    (or model "tinyllama")
         :avatar   (or avatar (str "/images/" name ".png"))
         ; TODO: fix the temperature
         :options  {:temperature (or (Float/parseFloat temperature) 0.9)}
         :system   system
         :stream   false
         :messages []}))

(defn load-people [app-state people-file]
  (let [personality-file (io/resource people-file )
        personalities (random-personalities personality-file)]
    (doseq [p personalities]
      (swap! app-state update :people conj (load-one-philosopher p)))))
(ns pyjama.games.human
  (:require [cheshire.core :as json]
            [compojure.core :refer [POST defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as response])
  (:import (java.time Instant)))

(defn get-user-input []
  (print "Your response: ")
  (flush)
  (read-line))

(defn handle-chat [request]
  (let [body (slurp (:body request))
        data (json/parse-string body true)
        messages (:messages data)
        _ (println "Last messages: " (last messages))
        response-text (get-user-input)
        response-map {:model "human"
                      :created_at (str (Instant/now))
                      :message {:role    "user"
                                :content response-text}
                      :done true}]
    (response/response (json/generate-string response-map))))

(defroutes app-routes
           (POST "/api/chat" request (handle-chat request))
           (route/not-found "Not Found"))

(defn -main []
  (run-jetty app-routes {:port 3000 :join? false}))
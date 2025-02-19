(ns htmls.v4
  (:require [cheshire.core :as json]
            [clojure.core.async :as async]
            [org.httpkit.server :as http]
            [pyjama.games.joining :as joining]
            [pyjama.games.philosophersv4 :as v4]
            [pyjama.utils]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.codec :as codec]
            [ring.util.response :refer [content-type response]]
            [ring.util.response :refer [response]]
            [utils.core]))

(defonce ws-clients (atom #{}))

(defn ws-handler [req]
  (http/with-channel req channel
                     (swap! ws-clients conj channel)
                     (http/on-close channel (fn [_] (swap! ws-clients disj channel)))))

(defn broadcast! [message]
  (let [json-msg (json/generate-string message)]
    (doseq [client @ws-clients]
      (http/send! client json-msg))))

(def app-state
  (atom {:lag      5000
         :battle-message
         "This is a conversation battle. Everyone should chat, with simple, very very short, witty answers.
                              May the most intelligent win. "
         :chatting false}))

(defn thread-with-speakers [question]
  (async/thread
    (swap! app-state assoc
           :messages []
           :current-question question
           :chatting true)
    (v4/chat-simulation
      app-state
      ##Inf
      question
      broadcast!)))

(defn handle-question [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        question (:question json)]
    (println "Received question:" question)
    (spit "questions.log" (str question "\n") :append true)
    (thread-with-speakers question)
    (response (json/generate-string {:answer question}))))

(defn handle-state [req]
  (utils.core/mark-alive app-state)
  (-> app-state
      utils.core/deep-deref
      json/generate-string
      response
      (content-type "application/json")))

(defn handle-summary [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        question (or (:question json) "Make a five point summary of the conversation.")
        summary
        {:url      (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
         :model    "tinydolphin"
         :options  {:temperature 0.9}
         :stream   false
         :messages (reverse (conj (:messages @app-state)
                                  {:role :user :content question}))}
        ]
    (->
      (pyjama.core/ollama (:url summary) :chat (dissoc summary :url) identity)
      (json/generate-string)
      (response)
      (content-type "application/json"))
    )
  )

(defn handle-questions [req]
  (->
    "questions.log"
    pyjama.utils/load-lines-of-file
    set
    reverse
    json/generate-string
    response
    (content-type "application/json")
    ))

(defn handle-join [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        ;_ (clojure.pprint/pprint json)
        new-atom (joining/load-one-philosopher json)
        ok? (try
              (swap! app-state update :people conj (joining/load-one-philosopher json))
              true
              (catch Exception e
                (do
                  (.printStackTrace e)
                  )
                false)
              )
        _ (if ok?
            (do (println (:name json) " has joined.")
                (v4/tell-everybody-else (:people @app-state) new-atom {:role :user :content (str (:name " has joined"))}))
            (println (:name json) " failed to joined."))
        http-res {:status  200
                  :headers {"Content-Type" "text/plain"}
                  :body    ok?}
        ]
    (-> (json/generate-string http-res)
        response
        (content-type "application/json"))))

(defn handle-leave [req]
  (let [
        params (codec/form-decode (:query-string req))
        name (get params "name")
        _ (println name)
        people (:people @app-state)
        ;_ (println people)
        find? (some #(= (:name (deref %)) name) people)
        _ (println find?)
        ]
    (if find?
      (do
        (swap! app-state update :people (fn [people] (remove #(= (:name (deref %)) name) people)))
        {:status 200 :body "true"})
      {:status 400 :body "false"})))


(defn app []
  (-> (fn [req]
        (cond
          (= (:uri req) "/ws") (ws-handler req)
          (= (:uri req) "/state") (handle-state req)
          (= (:uri req) "/join") (handle-join req)
          (= (:uri req) "/leave") (handle-leave req)
          (= (:uri req) "/ask") {:status  200
                                 :headers {"Content-Type" "text/html"}
                                 :body    (slurp "public/v4/ask.html")}
          (= (:uri req) "/human") {:status  200
                                   :headers {"Content-Type" "text/html"}
                                   :body    (slurp "public/v4/human.html")}
          (= (:uri req) "/people") {:status  200
                                    :headers {"Content-Type" "text/html"}
                                    :body    (slurp "public/v4/people.html")}
          (= (:uri req) "/summary") (handle-summary req)
          (= (:uri req) "/questions") (handle-questions req)
          (= (:uri req) "/question") (handle-question req)  ; New route to handle the POST request
          :else (do
                  ;(swap! app-state assoc :chatting false)   ; TODO remove this
                  {:status  200
                   :headers {"Content-Type" "text/html"}
                   :body    (slurp "public/v4/index.html")})))
      (wrap-file "public")))

(defn -main []
  (println "Starting server on http://localhost:3001")
  (joining/load-people app-state "personalitiesv5.csv")
  (http/run-server (app) {:host "0.0.0.0" :port 3001}))

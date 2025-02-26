(ns pyjama.games.web
  (:require [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [compojure.core :refer [DELETE GET POST defroutes]]
            [compojure.route :as route]
            [org.httpkit.server :as http]
            [pyjama.games.joining :as joining]
            [pyjama.games.philosophersv4 :as v4]
            [pyjama.utils]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.codec :as codec]
            [ring.util.response :refer [content-type response]]
            [utils.core :as utils]
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
  (atom {:lag
         {
          :last     0
          :strategy :fixed
          :params   [5000]
          }
         :timeout        10000
         :messages       []
         ;:strategy :length-weighted-random
         :battle-message "This is a conversation battle. Everyone should chat, with simple, very very short, witty answers. May the most intelligent win."
         :chatting       false}))

(defn start-chat-thread [question]
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


(defn- as-json [message]
  (->
    message
    (json/generate-string)
    (response)
    (content-type "application/json")))

(defn- page [page-file]
  (fn [_]
    (let [content (slurp (str "resources/html/" page-file))]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (str/replace content "localhost:3001" (:host-s @app-state))})))

(defn handle-question [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        question (:question json)]
    (println "Received question:" question)
    (if (:chatting @app-state)
      (do
        (println "but ... already chatting...")
        (as-json {:started false})
        )
      (do
        (spit "questions.log" (str question "\n") :append true)
        (start-chat-thread question)
        (as-json {:started true :answer question})))))

(defn handle-state [_]
  ; TODO: do a separate thread to mark alive people?
  ;(utils.core/mark-alive app-state)
  (clojure.pprint/pprint (map #(vector (:name @%) (:url @%) (:alive @%)) (:people @app-state)))
  (-> app-state
      utils.core/deep-deref
      as-json))

(defn handle-summary [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        question (or (:question json) "You are a moderator. Make a five point summary of the conversation.")
        model (or (:model json) "tinyllama")
        messages (conj (:messages @app-state)
                                {:role :system :content question})
        summary
        {:url      (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
         :model    model
         :options  {:temperature 0.9}
         :stream   false
         :messages messages
         }
        ]
    (clojure.pprint/pprint messages)
    (as-json (pyjama.core/ollama (:url summary) :chat (dissoc summary :url) identity))))

(defn handle-questions [_]
  (->
    "questions.log"
    pyjama.utils/load-lines-of-file
    set
    reverse
    json/generate-string
    response
    (content-type "application/json")))

(defn handle-intervention [req]
  (let [
        body (slurp (:body req))
        json (json/parse-string body true)

        image (or (:image json) "images/UFO.jpg")
        shout? (or (:shout json) false)
        human-name (or (:name json) "Intervention")
        human-message (or (:message json) "I think humans will win. AI is going to be forgotten.")
        content (str human-name " says " human-message)
        content (if shout? (str/upper-case content) content)
        broadcast-msg {:image image :position :left :name human-name :text content}
        new-msg {:name human-name :role :user :content content}
        ]

    (swap! app-state update :messages conj new-msg)
    (clojure.pprint/pprint new-msg)
    (v4/tell-everybody-else (:people @app-state) nil new-msg)
    (broadcast! broadcast-msg)))

(defn handle-join [req]
  (let [body (slurp (:body req))
        json (json/parse-string body true)
        new-atom (joining/load-one-philosopher json)
        ok? (try
              (swap! app-state update :people conj new-atom)
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
        http-res {:body ok?}
        ]
    (as-json http-res)))

(defn handle-leave [req]
  (let [
        params (codec/form-decode (:query-string req))
        name (get params "name")
        _ (println name)
        people (:people @app-state)
        find? (some #(= (:name (deref %)) name) people)
        _ (println find?)
        ]
    (if find?
      (do
        (swap! app-state update :people (fn [people] (remove #(= (:name (deref %)) name) people)))))
    (as-json {:body find?})))

(defn handle-stop [_]
  (swap! app-state assoc :chatting false)
  (as-json "stopped"))

(defroutes
  app-routes
  (GET "/ws" [] ws-handler)

  ; json
  (GET "/state" [] handle-state)
  (POST "/question" [] handle-question)

  (POST "/join" [] handle-join)
  (POST "/intervention" [] handle-intervention)
  (POST "/summary" [] handle-summary)
  (GET "/summary-json" [] handle-summary)
  (POST "/stop" [] handle-stop)
  (GET "/questions" [] handle-questions)

  (DELETE "/leave" [] handle-leave)

  ; html
  (GET "/current" [] (page "current.html"))
  (GET "/chat" [] (page "chat.html"))
  (GET "/ask" [] (page "ask.html"))
  (GET "/human" [] (page "human.html"))
  (GET "/history" [] (page "history.html"))
  (GET "/people" [] (page "people.html"))
  (GET "/" [] (page "welcome.html"))
  (GET "/3d" [] (page "3d.html"))

  (route/not-found (page "notfound.html"))

  )

(defn app []
  (-> app-routes
      (wrap-file "public")
      (wrap-cors
        :access-control-allow-origin [#".*"]
        :access-control-allow-methods [:get :post :put :delete :options]
        :access-control-allow-headers ["Content-Type" "Authorization"])))


(defn -main []
  (let [port 3001
        ip (utils/get-local-ip)
        host (str "http://" ip ":" port)
        ]
    (swap! app-state assoc
           :host-s (str ip ":" port)
           :host host)
    (println "Starting server on " host)
    (joining/load-people app-state "csv/personalitiesv6.csv")
    (http/run-server (app) {:host "0.0.0.0" :port port})))
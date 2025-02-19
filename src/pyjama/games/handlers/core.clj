(ns pyjama.games.handlers.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [compojure.core :refer [POST defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as response])
  (:import (java.net InetAddress NetworkInterface)
           (java.time Instant)))

(def cli-options
  [["-n" "--name NAME" "Friendly name for the handler"]
   ["-h" "--handler HANDLER" "Handler function name (namespace/function)"]
   ["-p" "--port PORT" "Port number (0 for dynamic)" :parse-fn #(Integer. %) :default 0]
   ["-s" "--server-url URL" "Remote server URL"]
   ["-a" "--avatar AVATAR" "Path to avatar image"]])

(defn get-handler-fn [handler-path]
  (let [[ns-name fn-name] (str/split handler-path #"/")]
    (let [handler-ns (symbol ns-name)]
      (require handler-ns)
      (ns-resolve handler-ns (symbol fn-name)))))

(defn handle-request [handle-messages request]
  (let [body (slurp (:body request))
        data (json/parse-string body true)]
    (-> data
        handle-messages
        (assoc :created_at (str (Instant/now)) :done true)
        json/generate-string
        response/response)))

(defn create-app [handle-messages]
  (defroutes app-routes
             (POST "/api/chat" request (handle-request handle-messages request))
             (route/not-found "Not Found"))
  app-routes)

;
;(defn get-local-ip []
;  (-> (InetAddress/getLocalHost) .getHostAddress))

(defn get-non-local-ip []
  (->> (NetworkInterface/getNetworkInterfaces)
       (enumeration-seq)
       (mapcat #(enumeration-seq (.getInetAddresses %)))
       (filter #(and (not (.isLoopbackAddress %))
                     (.isSiteLocalAddress %)))  ;; Filters non-local IPs
       (map str)
       first)) ;; Gets the first non-local IP
(def get-local-ip get-non-local-ip)

(defn register-handler [server-url name local-ip port avatar]
  (let [endpoint (str "http://" local-ip ":" port)
        payload {:name name :url endpoint :avatar avatar}]
    (println "Registering handler to" server-url "with data:" payload)
    (http/post (str server-url "/join")
               {:body (json/generate-string payload)
                :headers {"Content-Type" "application/json"}
                ;:throw-exceptions false
                })))

(defn -main [& args]
  (let [{:keys [options]} (parse-opts args cli-options)
        handle-messages (get-handler-fn (:handler options))
        port (:port options)
        handler-name (:name options)]
    (if (nil? handler-name)
      (do (println "Error: --name parameter is required")
          (System/exit 1))
      (let [server (run-jetty (create-app handle-messages) {:host "0.0.0.0" :port port :join? false})
            assigned-port (-> server .getConnectors first (#(.getLocalPort %)))
            ; local-ip (get-non-local-ip)
            local-ip (get-local-ip)
            ]
        (when (:server-url options)
          (register-handler (:server-url options) handler-name local-ip assigned-port (:avatar options)))))))

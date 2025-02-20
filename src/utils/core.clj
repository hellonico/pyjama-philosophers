(ns utils.core
  (:require [clj-http.client :as http])
  (:import
    (java.net Socket)
    (clojure.lang Atom)))

(defn deep-deref [x]
  (cond
    (instance? Atom x) (deep-deref @x)
    (map? x) (into {} (map (fn [[k v]] [k (deep-deref v)]) x))
    (coll? x) (into (empty x) (map deep-deref x))
    :else x))


(defn port-open? [host port]
  (try
    (with-open [_ (Socket. host port)]
      true)
    (catch Exception _ false)))

(defn closed-ports [state]
  (->> (:people state)
       (filter (fn [{:keys [url]}]
                 (let [[_ host port] (re-matches #"http://([^:]+):(\d+)" url)
                       port (Integer/parseInt port)]
                   (not (port-open? host port)))))
       (map :name)))


(defn url-alive? [url]
  (try
    (let [response (http/get url {:throw-exceptions false :timeout 2000})]
      ;(<= 200 (:status response) 399)
      true ; TODO: check response code later
      ) ;; Alive if status code is in 200-399 range
    (catch Exception _ false)))

;
;(defn mark-alive [app-state]
;  (doseq [person-atom (:people @app-state)]
;    (swap! person-atom
;           (fn [{:keys [url] :as person}]
;             (let [[_ host port] (re-matches #"http://([^:]+):(\d+)" url)
;                   port (Integer/parseInt port)]
;               (assoc person :alive (port-open? host port)))))))
;

(defn mark-alive [app-state]
  (doseq [person-atom (:people @app-state)]
    (swap! person-atom
           (fn [{:keys [url] :as person}]
             (assoc person :alive (url-alive? url))))))
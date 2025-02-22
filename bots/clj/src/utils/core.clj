(ns utils.core
  (:require [clj-http.client :as http])
  (:import
    (java.net NetworkInterface Socket)
    (clojure.lang Atom)))

(defn get-non-local-ip []
  (->> (NetworkInterface/getNetworkInterfaces)
       (enumeration-seq)
       (mapcat #(enumeration-seq (.getInetAddresses %)))
       (filter #(and (not (.isLoopbackAddress %))
                     (.isSiteLocalAddress %)))  ;; Filters non-local IPs
       (map str)
       first)) ;; Gets the first non-local IP
(def get-local-ip get-non-local-ip)
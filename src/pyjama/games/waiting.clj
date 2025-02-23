(ns pyjama.games.waiting)

(defn fixed-wait [params]
  (first params))                                           ;; Takes the first parameter as the fixed duration ;; Always returns the same wait time

(defn random-wait []
  (+ 5 (rand-int 16)))                                      ;; Random between 5 and 20

(defn oscillating-wait [prev]
  (if (nil? prev)
    10
    (+ 10 (* 10 (Math/sin (/ prev 10.0))))))                ;; Oscillating pattern

(defn wait-more-if-long [last-message]
  (let [length (count (:content last-message))]
    (cond
      (< length 20) 5
      (< length 100) 10
      :else 20)))                                           ;; Longer wait for longer messages

(defn biased-wait [bias]
  (if (= bias :long)
    (+ 10 (rand-int 11))                                    ;; More bias towards longer wait
    (+ 5 (rand-int 6))))                                    ;; More bias towards shorter wait

(defn wait-less-if-long [last-message]
  (let [length (count (:content last-message))]
    (cond
      (< length 20) 10                                      ;; Short messages wait longer
      (< length 100) 7
      :else 3)))                                            ;; Long messages wait shorter


(defn get-wait-time [{:keys [strategy params last]} last-message]
  (case strategy
    :fixed (fixed-wait params)                              ;; Extracts value from :params
    :random (random-wait)
    :oscillating (oscillating-wait last)
    :wait-more-if-long (wait-more-if-long last-message)
    :wait-less-if-long (wait-less-if-long last-message)
    :biased (biased-wait :long)                             ;; Change bias as needed
    10))                                                    ;; Default wait time
(ns stream2es.seque
  (:require [clj-http.client :as http]))

(defn producer
  "Returns a lazy-seq of thunks"
  []
  (map (fn [x] #(do (Thread/sleep (rand-int 10)) (inc x))) (range 1000)))

(defn unchunk
  "Takes a chunked sequence and turns it into an unchunked sequence"
  [s]
  (lazy-seq
   (when-let [[x] (seq s)]
     (cons x (unchunk (rest s))))))

(defn consume
  "Consume things without running out of memory."
  [thunks]
  (seque 16 (apply pcalls (unchunk thunks))))

(comment
  (def c (consume (producer)))
  )

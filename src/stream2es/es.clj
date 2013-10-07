(ns stream2es.es
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-http.conn-mgr :as conn-mgr]
            [stream2es.log :as log]))

(defonce conn-pool
  (conn-mgr/make-reusable-conn-manager
   {:threads 2
    :timeout 15
    :default-per-route 2}))

(defn index-url [url index]
  (format "%s/%s" url index))

(defn post
  ([url data]
     (log/trace "POSTing" (count (.getBytes data)) "bytes")
     (http/post url {:body data :connection-manager conn-pool}))
  ([url index data]
     (http/post (index-url url index)
                {:body data :connection-manager conn-pool})))

(defn delete [url index]
  (http/delete (index-url url index)
               {:throw-exceptions false :connection-manager conn-pool}))

(defn exists? [url index]
  (try
    (http/get (format "%s/_mapping" (index-url url index))
              {:connection-manager conn-pool})
    (catch Exception _)))

(defn error-capturing-bulk [url items serialize-bulk]
  (let [resp (json/decode (:body (post url (serialize-bulk items))) true)]
    (->> (:items resp)
         (map-indexed (fn [n obj]
                        (when (contains? (val (first obj)) :error)
                          (spit (str "error-"
                                     (:_id (val (first obj))))
                                (with-out-str
                                  (prn obj)
                                  (println)
                                  (prn (nth items n))))
                          obj)))
         (remove nil?)
         count)))

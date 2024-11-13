(ns demo.main
  (:gen-class)
  (:require
   [ring.adapter.jdk :as jdk]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "hello"})

(defn -main [& _]
  (jdk/server handler {:port 8082}))

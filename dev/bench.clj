(ns bench
  (:require
   [ring.adapter.jetty :as jetty]
   [ring.jdk :as jdk]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "hello"})

(comment

  (def -server-jetty
    (jetty/run-jetty handler
                     {:port 8081
                      :join? false}))

  (.stop -server-jetty)

  (def -server-jdk
    (jdk/server "127.0.0.1" 8082 handler))

  (.close -server-jdk)

  )

(ns bench
  (:require
   [ring.adapter.jetty :as jetty]
   [ring.adapter.jdk :as jdk]))

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
    (jdk/server handler {:port 8082}))

  (.close -server-jdk)

  )

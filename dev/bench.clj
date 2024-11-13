(ns bench
  (:import
   org.eclipse.jetty.util.thread.QueuedThreadPool
   java.util.concurrent.Executors)
  (:require
   [ring.adapter.jetty :as jetty]
   [ring.adapter.jdk :as jdk]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "hello"})

(comment

  (def -exe
    (Executors/newVirtualThreadPerTaskExecutor))

  (.close -exe)

  ;; default
  (def -server-jetty
    (jetty/run-jetty handler
                     {:port 8081
                      :join? false}))

  ;; executor
  (def -server-jetty
    (let [pool (doto (new QueuedThreadPool)
                 (.setVirtualThreadsExecutor
                  (Executors/newVirtualThreadPerTaskExecutor)))]
      (jetty/run-jetty handler
                       {:port 8081
                        :join? false
                        :thread-pool pool})))

  (.stop -server-jetty)

  ;; default
  (def -server-jdk
    (jdk/server handler {:port 8082}))

  ;; executor
  (def -server-jdk
    (jdk/server handler {:port 8082
                         :executor -exe}))

  (.close -server-jdk)

  )

;; mb m3 pro 32g
;; ab -l -n 1000 -c 50 http://127.0.0.1:8081/ (jetty)
;; ab -l -n 1000 -c 50 http://127.0.0.1:8082/ (jdk)

;; jetty (default)
;; Requests per second:    13505.30 [#/sec] (mean)

;; jdk (default)
;; Requests per second:    12615.75 [#/sec] (mean)


;; jetty (virtual threads)
;; Requests per second:    12797.71 [#/sec] (mean)

;; jdk (virtual threads)
;; Requests per second:    13640.15 [#/sec] (mean)

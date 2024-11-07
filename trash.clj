
(require '[ring.jdk :as jdk])

(def -s
  (jdk/start-server "127.0.0.1"
                    8080
                    (fn [request]
                      {:status (int 200)
                       :headers {}
                       :body "test"})))

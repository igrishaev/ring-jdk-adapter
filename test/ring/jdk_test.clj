(ns ring.jdk-test
  (:require
   [clj-http.client :as client]
   [clojure.test :refer [deftest is]]
   [ring.jdk :as jdk]))


(def PORT 8081)

(def URL
  (format "http://127.0.0.1:%s/" PORT))

(defn simplify [response]
  (-> response
      (update :headers dissoc "Date")
      (select-keys [:status
                    :protocol-version
                    :headers
                    :length
                    :headers
                    :body])))

(def RESP_HELLO
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "hello"})

(defn handler-ok [request]
  RESP_HELLO)

(deftest test-server-ok
  (jdk/with-server ["127.0.0.1" PORT handler-ok]
    (let [response
          (client/get URL)]
      (is (= {:status 200
              :protocol-version {:name "HTTP"
                                 :major 1
                                 :minor 1}
              :length 5
              :headers
              {"Content-type" "text/plain",
               "Content-length" "5"}
              :body "hello"}
             (simplify response))))))

(deftest test-capture-request
  (let [capture! (atom nil)]
    (jdk/with-server ["127.0.0.1" PORT
                      (fn [request]
                        (reset! capture! request)
                        RESP_HELLO)]
      (let [_ (client/get URL)
            request @capture!]
        (is (= {:protocol "HTTP/1.1",
                :headers
                {"Accept-encoding" "gzip, deflate",
                 "Connection" "close",
                 "Host" "127.0.0.1:8081"},
                :server-port 8081,
                :server-name "localhost"
                :uri "/",
                :request-method "GET"}
               (-> request
                   (update :headers dissoc "User-agent")
                   (dissoc :body
                           :remote-addr))))))))

;; capture request
;; input stream
;; file
;; string
;; lazy seq
;; multiple headers
;; status < 100 or > 600
;; exception in ring
;; broken response: status, headers, body
;; read body

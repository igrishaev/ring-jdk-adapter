(ns ring.adapter.jdk-test
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [ring.adapter.jdk :as jdk]))


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


(defn handler-capture [atom!]
  (fn [request]
    (reset! atom! request)
    RESP_HELLO))


(deftest test-server-ok
  (jdk/with-server [handler-ok {:port PORT}]
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
    (jdk/with-server [(handler-capture capture!)
                      {:port PORT}]
      (let [_ (client/get URL)
            request @capture!]
        (is (= {:protocol "HTTP/1.1",
                :headers
                {"Accept-encoding" "gzip, deflate",
                 "Connection" "close",
                 "Host" "127.0.0.1:8081"},
                :server-port 8081,
                :server-name "localhost"
                :query-string nil
                :uri "/",
                :request-method "GET"}
               (-> request
                   (update :headers dissoc "User-agent")
                   (dissoc :body
                           :remote-addr))))))))


(deftest test-server-query-string-presents
  (let [capture! (atom nil)]
    (jdk/with-server [(handler-capture capture!)
                      {:port PORT}]
      (let [response
            (client/get URL {:query-params {:foo 1 :bar 2 :baz [3 4 5]}})
            request @capture!]
        (is (= 200 (:status response)))
        (is (= "foo=1&bar=2&baz=3&baz=4&baz=5"
               (:query-string request)))))))


(deftest test-server-to-string
  (with-open [server (jdk/server handler-ok {:port PORT})]
    (is (str/includes?
         (str server)
         "<Server 127.0.0.1:8081, handler: ring.adapter.jdk_test$handler_ok"))))


;; input stream
;; file
;; string
;; lazy seq
;; multiple headers
;; status < 100 or > 600
;; exception in ring
;; broken response: status, headers, body
;; read body
;; fix method
;; form-params
;; some middleware

(ns ring.adapter.jdk-test
  (:import
   java.io.File)
  (:require
   [clj-http.client :as client]
   [clojure.java.io :as io]
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


(defn get-temp-file
  "
  Return an temporal file, an instance of java.io.File class.
  "
  (^File []
   (get-temp-file "tmp" ".tmp"))
  (^File [prefix suffix]
   (File/createTempFile prefix suffix)))


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
                :request-method :get}
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


(deftest test-server-return-input-stream
  (jdk/with-server [(constantly
                     {:status 200
                      :body (-> "hello abc"
                                (.getBytes)
                                (io/input-stream))
                      :headers {"content-type" "text/plain"}})
                    {:port PORT}]
    (let [response
          (client/get URL)]
      (is (= 200 (:status response)))
      (is (= "hello abc" (:body response))))))


(deftest test-server-return-file
  (let [file (get-temp-file)
        _ (spit file "some string")]
    (jdk/with-server [(constantly
                       {:status 200
                        :body file
                        :headers {"content-type" "text/plain"}})
                      {:port PORT}]
      (let [response
            (client/get URL)]
        (is (= 200 (:status response)))
        (is (= "some string" (:body response)))))))


(deftest test-server-return-missing-file
  (let [file (io/file "some-file.test")]
    (jdk/with-server [(constantly
                       {:status 200
                        :body file
                        :headers {"content-type" "text/plain"}})
                      {:port PORT}]
      (let [{:keys [status body]}
            (client/get URL {:throw-exceptions false})]
        (is (= 500 status))
        (is (str/includes? body "java.lang.RuntimeException: file not found: some-file.test"))))))


(deftest test-server-return-iterable
  (let [items (for [x ["aaa" "bbb" "ccc" 1 :foo {:test 3} nil [1 2 3]]]
                x)]
    (jdk/with-server [(constantly
                       {:status 200
                        :body items
                        :headers {"content-type" "text/plain"}})
                      {:port PORT}]
      (let [response
            (client/get URL)]
        (is (= 200 (:status response)))
        (is (= "aaabbbccc1:foo{:test 3}[1 2 3]"
               (:body response)))))))


(deftest test-server-header-multi-return
  (jdk/with-server [(constantly
                     {:status 200
                      :body "test"
                      :headers {"content-type" "text/plain"
                                "X-TEST" ["foo" "bar" "baz"]}})
                    {:port PORT}]
    (let [{:keys [status headers]}
          (client/get URL)]
      (is (= 200 status))
      (is (= ["foo" "bar" "baz"]
             (get headers "X-TEST"))))))


(deftest test-server-header-multi-pass
  (let [request! (atom nil)]
    (jdk/with-server [(handler-capture request!)
                      {:port PORT}]
      (let [{:keys [status]}
            (client/get URL {:headers {:X-TEST ["foo" "bar" "baz"]}})

            request
            @request!]

        (is (= 200 status))
        (is (= ["foo" "bar" "baz"]
               (get-in request [:headers "X-test"])))))))


(deftest test-server-header-multi-pass
  (jdk/with-server [(fn [_]
                      (/ 0 0))
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]

      (is (= 500 status))
      (is (= "text/plain" (get headers "Content-Type")))

      (is (str/includes? body "failed to execute ring handler"))
      (is (str/includes? body "java.lang.ArithmeticException: Divide by zero"))
      (is (str/includes? body "\tat clojure.lang.Numbers.divide")))))


(deftest test-server-status>500
  (jdk/with-server [(constantly
                     {:status 999
                      :body "test"
                      :headers {"content-type" "text/plain"}})
                    {:port PORT}]
    (let [{:keys [status]}
          (client/get URL {:throw-exceptions false})]
      (is (= 999 status)))))


(deftest test-server-status-only
  (jdk/with-server [(constantly
                     {:status 200})
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= {"Transfer-encoding" "chunked"}
             (dissoc headers "Date")))
      (is (= "" body)))))


(deftest test-server-status-nil
  (jdk/with-server [(constantly {:status nil})
                    {:port PORT}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 500 status))
      (is (str/includes? body "java.lang.RuntimeException: ring status is not integer: null")))))


(deftest test-server-ring-response-nil
  (jdk/with-server [(constantly nil)
                    {:port PORT}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 500 status))
      (is (str/includes? body "java.lang.RuntimeException: unsupported ring response: null")))))


(deftest test-server-body-nil
  (jdk/with-server [(constantly {:status 200
                                 :body nil})
                    {:port PORT}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= "" body)))))


(deftest test-server-headers-nil
  (jdk/with-server [(constantly {:status 200
                                 :headers nil})
                    {:port PORT}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= "" body)))))


#_
(deftest test-server-headers-nil
  (jdk/with-server [(constantly {:status 200
                                 :headers {"X-Some-Metric" "42"}
                                 :body "aa"})
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= 1 headers))
      (is (= "" body)))))



;; some middleware: wrap-params, kw-params
;; form-params

;; header keyword
;; header value integer

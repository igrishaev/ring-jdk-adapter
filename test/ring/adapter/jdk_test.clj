(ns ring.adapter.jdk-test
  (:import
   (java.io File IOException))
  (:require
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [ring.adapter.jdk :as jdk]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.request :as request]))


(def PORT 8081)

(def URL
  (format "http://127.0.0.1:%s" PORT))


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
      (let [_ (client/get (str URL "/hello?foo=1&bar=2"))
            request @capture!]
        (is (= {:protocol "HTTP/1.1",
                :headers
                {"accept-encoding" "gzip, deflate",
                 "connection" "close",
                 "host" "127.0.0.1:8081"},
                :server-port 8081,
                :server-name "localhost"
                :query-string "foo=1&bar=2"
                :uri "/hello",
                :scheme :http
                :request-method :get}
               (-> request
                   (update :headers dissoc "user-agent")
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


(deftest test-server-threads-ok
  (jdk/with-server [(constantly {:status 200})
                    {:port PORT
                     :threads 8}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= "" body)))))


(deftest test-server-root-path
  (let [request! (atom nil)]
    (jdk/with-server [(handler-capture request!)
                      {:port PORT
                       :root-path "/some/prefix"}]

      (let [{:keys [status body]}
            (client/get URL {:throw-exceptions false})]
        (is (= nil @request!))
        (is (= 404 status))
        (is (= "<h1>404 Not Found</h1>No context found for request" body)))

      (let [{:keys [status body]}
            (client/get (str URL "/some/prefix/hello") {:throw-exceptions false})]
        (is (= "/some/prefix/hello"
               (:uri @request!)))
        (is (= 200 status))
        (is (= "hello" body))))))

(deftest test-server-headers-nil
  (jdk/with-server [(constantly {:status 200
                                 :headers nil})
                    {:port PORT}]
    (let [{:keys [status body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= "" body)))))


(deftest test-server-params-middleware
  (let [request! (atom nil)
        handler (-> request!
                    handler-capture
                    wrap-keyword-params
                    wrap-params
                    wrap-multipart-params)]

    ;; check GET
    (jdk/with-server [handler {:port PORT}]
      (let [{:keys [status headers body]}
            (client/get URL {:query-params {:q1 "ABC" :q2 "XYZ"}
                             :throw-exceptions false})

            request
            @request!]

        (is (= 200 status))

        (is (= {"q1" "ABC" "q2" "XYZ"}
               (:query-params request)))

        (is (= {:q1 "ABC" :q2 "XYZ"}
               (:params request))))

      ;; check POST
      (let [{:keys [status headers body]}
            (client/post URL {:form-params {:f1 "AAA" :f2 "BBB"}
                              :throw-exceptions false})

            request
            @request!]

        (is (= 200 status))

        (is (= {}
               (:query-params request)))

        (is (= {"f1" "AAA" "f2" "BBB"}
               (:form-params request)))

        (is (= {:f1 "AAA" :f2 "BBB"}
               (:params request)))))))


(deftest test-server-header-value-non-string
  (jdk/with-server [(constantly
                     {:status 200
                      :headers
                      {"X-Metric-A" 42
                       "X-Metric-B" nil
                       "X-Metric-C" true
                       "X-Metric-D" ["A" "B" 123 nil :hello]}})
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= {"X-metric-d" ["A" "B" "123" ":hello"],
              "X-metric-a" "42",
              "X-metric-c" "true"}
             (dissoc headers "Date" "Transfer-encoding"))))))


(deftest test-server-header-key-keyword
  (jdk/with-server [(constantly
                     {:status 200
                      :headers {:hello/foo 42}})
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 200 status))
      (is (= "42" (get headers "Hello/foo"))))))


(deftest test-server-header-key-weird
  (jdk/with-server [(constantly
                     {:status 200
                      :headers {42 42}})
                    {:port PORT}]
    (let [{:keys [status headers body]}
          (client/get URL {:throw-exceptions false})]
      (is (= 500 status))
      (is (str/includes? body "unsupported header key: 42")))))


(deftest test-server-ring-util-functions
  (let [request! (atom nil)]
    (jdk/with-server [(handler-capture request!)
                      {:port PORT}]
      (let [{:keys [status]}
            (client/get (str URL "/foo/bar/baz?test=1")
                        {:throw-exceptions false
                         :headers {"content-type" "Text/Plain"
                                   "content-Length" "42"}
                         })

            request
            @request!]

        (is (= 200 status))

        (is (= :http
               (:scheme request)))

        (is (= "/foo/bar/baz"
               (:uri request)))

        (is (= "http://127.0.0.1:8081/foo/bar/baz?test=1"
               (request/request-url request)))

        (is (= "Text/Plain"
               (request/content-type request)))

        (is (= 42
               (request/content-length request)))

        (try
          (slurp (:body request))
          (is false)
          (catch IOException e
            (is (= "Stream is closed" (.getMessage e)))))

        (try
          (request/body-string request)
          (is false)
          (catch IOException e
            (is (= "Stream is closed" (.getMessage e)))))))))

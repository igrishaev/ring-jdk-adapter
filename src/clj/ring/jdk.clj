(ns ring.jdk
  (:import
   ring.jdk.Server))

(defn server ^Server [host port fn-handler]
  (Server/start host port fn-handler))

(defmacro with-server [[& args] & body]
  `(with-open [server# (Server/start ~@args)]
     ~@body))

(ns ring.adapter.jdk
  (:import
   (ring.adapter.jdk Server
                     Config)))

(defn ->Config ^Config [opt]
  (if (empty? opt)
    Config/DEFAULT
    (let [{:keys [host
                  port
                  stop-delay-sec
                  root-path
                  threads
                  executor
                  socket-backlog]}
          opt]

      (cond-> (Config/builder)

        host
        (.host host)

        port
        (.port port)

        stop-delay-sec
        (.stop_delay_sec stop-delay-sec)

        root-path
        (.root_path root-path)

        threads
        (.threads threads)

        executor
        (.executor executor)

        socket-backlog
        (.socket_backlog socket-backlog)

        :finally
        (.build)))))

(defn server ^Server [handler]
  (Server/start handler))

(defn server ^Server [handler opt]
  (Server/start handler (->Config opt)))

(defn close [^Server server]
  (.close server))

(defn stop [^Server server]
  (.close server))

(defmacro with-server [[handler opt] & body]
  `(with-open [server# (Server/start ~handler (->Config ~opt))]
     ~@body))

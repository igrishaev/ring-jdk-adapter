(ns ring.adapter.jdk
  (:import
   (ring.adapter.jdk Server
                     Config)))

(set! *warn-on-reflection* true)

(defn ->Config
  "
  Build a Config object out from a Clojure map.
  "
  ^Config [opt]
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

(defn server
  "
  Given a Clojure Ring handler (arity-1 function)
  and, optionally a Clojure map of options, run
  an HTTP server in a separate thread. Return an
  instance of the `ring.adapter.jdk.Server` class.
  Needs to be closed afterwards; can be used with
  the `with-open` macro.

  Optional parameters:
  - `:host` is the host name to bind (default is 127.0.0.1).
  - `:port` is the port number to listen (default is 8080).
  - `:threads` is the number of threads to use. When > 0,
     then a custom instance of FixedThreadPool is used.
  - `:executor` is a custom Executor object, if needed.
  - `:root-path` is a string, a global path prefix for
     the handier.
  - `:stop-delay-sec` is a number of seconds to wait when
    closing the server;
  - `:socket-backlog` is a number, an option for socket
    when binding a server.
  "

  (^Server [handler]
   (Server/start handler Config/DEFAULT))
  (^Server [handler opt]
   (Server/start handler (->Config opt))))

(defn close
  "
  Close the server (the same as stop).
  "
  [^Server server]
  (.close server))

(defn stop
  "
  Close the server (the same as close).
  "
  [^Server server]
  (.close server))

(defmacro with-server
  "
  A wrapper on top of the `with-open` macro. Takes a handler
  (mandatory) and an optional map of parameters. Runs the server
  while the body is being executed. Closes the server instance
  on exit.
  "
  [[handler opt] & body]
  `(with-open [server# (Server/start ~handler (->Config ~opt))]
     ~@body))

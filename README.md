# Ring JDK Adapter

Ring JDK Adapter is a small wrapper on top of a built-in HTTP server available
in Java. It's like Jetty but has no dependencies. It's almost as fast as Jetty,
too (see benchmars below).

## Table of Contents

<!-- toc -->

- [Why](#why)
- [Availability](#availability)
- [Installation](#installation)
- [Quick Demo](#quick-demo)
- [Parameters](#parameters)
- [Body Type](#body-type)
- [Middleware](#middleware)
- [Exception Handling](#exception-handling)
- [HTTPs & SSL](#https--ssl)
- [Benchmarks](#benchmarks)

<!-- tocstop -->

## Why

Sometimes you want a local HTTP server in Clojure, e.g. for testing or mocking
purposes. There is a number of adapters for Ring but all of them rely on third
party servers like Jetty, Undertow, etc. Running them means to fetch plenty of
dependencies. This is tolerable to some extent, yet sometimes you really want
something quick and simple.

Since version 9 or 11 (I don't remember for sure), Java ships its own HTTP
server. The package name is `com.sun.net.httpserver` and the module name is
`jdk.httpserver`. The library provides an adapter to serve Ring handlers. It's
completely free from any dependencies.

Ring JDK Adapter is a great choice for local HTTP stubs or mock services that
mimic HTTP services. Despite some people think it's for development purposes
only, the server is pretty fast! One can use it even in production.

## Availability

It's worth mentioning that some Java installations may miss the `jdk.httpserver`
module. Please ensure the JVM you're using in production supports it
first. Check out the following links:

- [StackOverflow: Is package com.sun.net.httpserver
  standard?](https://stackoverflow.com/questions/58764710/is-package-com-sun-net-httpserver-standard)
- [Java® Platform, Standard Edition & Java Development Kit Version 21 API Specification](https://docs.oracle.com/en/java/javase/21/docs/api/index.html)

## Installation

~~~clojure
;; lein
[com.github.igrishaev/ring-jdk-adapter "0.1.0"]

;; deps
com.github.igrishaev/ring-jdk-adapter {:mvn/version "0.1.0"}
~~~

Requires Java version at least 16, Clojure at least 1.8.0.

## Quick Demo

Import the namespace, declare a Ring handler as usual:

~~~clojure
(ns demo
  (:require
   [ring.adapter.jdk :as jdk]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello world!"})
~~~

Pass it into the `server` function and check the http://127.0.0.1:8082 page in
your browser:

~~~clojure
(def server
  (jdk/server handler {:port 8082}))
~~~

The `server` function returns an instance of the `Server` class. To stop it,
pass the result into the `jdk/stop` or `jdk/close` functions:

~~~clojure
(jdk/stop server)
~~~

Since the `Server` class implements `AutoCloseable` interface, it's compatible
with the `with-open` macro:

~~~clojure
(with-open [server (jdk/server handler opt?)]
  ...)
~~~

The server gets closed once you've exited the macro. Here is a similar
`with-server` macro which acts the same:

~~~clojure
(jdk/with-server [handler opt?]
  ...)
~~~

## Parameters

The `server` function and the `with-server` macro accept the second optional map
of the parameters:

| Name              | Default   | Description                                                                   |
|-------------------|-----------|-------------------------------------------------------------------------------|
| `:host`           | 127.0.0.1 | Host name to listen                                                           |
| `:port`           | 8080      | Port to listen                                                                |
| `:stop-delay-sec` | 0         | How many seconds to wait when stopping the server                             |
| `:root-path`      | /         | A path to mount the handler                                                   |
| `:threads`        | 0         | Amount of CPU threads. When > thn 0, a new `FixedThreadPool` executor is used |
| `:executor`       | null      | A custom instance of `Executor`. Might be a virtual executor as well          |
| `:socket-backlog` | 0         | A numeric value passed into the `HttpServer.create` method                    |

Example:

~~~clojure
(def server
  (jdk/server handler
              {:host "0.0.0.0" ;; listen all addresses
               :port 8800      ;; a custom port
               :threads 8      ;; use custom fixed trhead executor
               :root-path "/my/app"}))
~~~

When run, the handler above is be available by the address
http://127.0.0.1:8800/my/app in the browser.

## Body Type

JDK adapter supports the following response `:body` types:

- `java.lang.String`
- `java.io.InputStream`
- `java.io.File`
- `java.lang.Iterable<?>` (see below)
- `null` (nothing gets sent)

When the body is `Iterable` (might be a lazy seq as well), every item is sent as
a string in UTF-8 encoding. Null values are skipped.

## Middleware

To gain all the power of Ring (parsed parameters, JSON, sessions, etc), wrap
your handler with the standard middleware:

~~~clojure
(ns demo
  (:require
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(let [handler (-> handler
                  wrap-keyword-params
                  wrap-params
                  wrap-multipart-params)]
  (jdk/server handler {:port 8082}))
~~~

The wrapped handler will receive a `request` map with parsed `:query-params`,
`:form-params`, and `:params` fields. These middleware come from the `ring-core`
library which you need to add into your dependencies. The same applies to
handling JSON and the `ring-json` library.

## Exception Handling

If something gets wrong while handling a request, you'll get a plain text page
with a short message and a stack trace:

~~~clojure
(defn handler [request]
  (/ 0 0) ;; !
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "hello"})
~~~

This is what you'll get in the browser:

~~~text
failed to execute ring handler
java.lang.ArithmeticException: Divide by zero
	at clojure.lang.Numbers.divide(Numbers.java:190)
	at clojure.lang.Numbers.divide(Numbers.java:3911)
	at bench$handler.invokeStatic(form-init14855917186251843338.clj:8)
	at bench$handler.invoke(form-init14855917186251843338.clj:7)
	at ring.adapter.jdk.Handler.handle(Handler.java:112)
	at jdk.httpserver/com.sun.net.httpserver.Filter$Chain.doFilter(Filter.java:98)
	at jdk.httpserver/sun.net.httpserver.AuthFilter.doFilter(AuthFilter.java:82)
	at jdk.httpserver/com.sun.net.httpserver.Filter$Chain.doFilter(Filter.java:101)
	at jdk.httpserver/sun.net.httpserver.ServerImpl$Exchange$LinkHandler.handle(ServerImpl.java:873)
	at jdk.httpserver/com.sun.net.httpserver.Filter$Chain.doFilter(Filter.java:98)
	at jdk.httpserver/sun.net.httpserver.ServerImpl$Exchange.run(ServerImpl.java:849)
	at jdk.httpserver/sun.net.httpserver.ServerImpl$DefaultExecutor.execute(ServerImpl.java:204)
	at jdk.httpserver/sun.net.httpserver.ServerImpl$Dispatcher.handle(ServerImpl.java:567)
	at jdk.httpserver/sun.net.httpserver.ServerImpl$Dispatcher.run(ServerImpl.java:532)
	at java.base/java.lang.Thread.run(Thread.java:1575)
~~~

To prevent this data from being leaked to the client, use your own
`wrap-exception` middleware, something like this:

~~~clojure
(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/errorf e ...)
        {:status 500
         :headers {...}
         :body "No cigar! Roll again!"}))))
~~~

## HTTPs & SSL

At the moment, the adapter supports HTTP only. There is a pending TODO to make
it also work with HTTPs and a custom SSL context.

## Benchmarks

As mentioned above, the JDK server although though is for dev purposes only, is
not so bad! The chart below proves it's almost as fast as Jetty. There are five
attempts of `ab -l -n 1000 -c 50 ...` made against both Jetty and JDK servers
(1000 requests in total, 50 parallel). The levels of RPS are pretty equal: about
12-13K requests per second.

Measured on Macbook M3 Pro 32Gb, default settings, the same REPL.

<img src="media/chart_1.svg" width=75% height=auto>

Ivan Grishaev, 2024

(def MIN_JAVA_VERSION "16")
(def RING_VERSION "1.13.0")

(defproject com.github.igrishaev/ring-jdk-adapter "0.1.0"

  :description
  "Zero-deps Ring server on top of jdk.httpserver"

  :url
  "https://github.com/igrishaev/ring-jdk-adapter"

  :pom-addition
  [:properties
   ["maven.compiler.source" ~MIN_JAVA_VERSION]
   ["maven.compiler.target" ~MIN_JAVA_VERSION]]

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :deploy-repositories
  {"releases"
   {:url "https://repo.clojars.org"
    :creds :gpg}
   "snapshots"
   {:url "https://repo.clojars.org"
    :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["with-profile" "uberjar" "install"]
   ["with-profile" "uberjar" "deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"
                  "-Xlint:preview"
                  "--release" ~MIN_JAVA_VERSION]

  :dependencies
  [[org.clojure/clojure]]

  :managed-dependencies
  [[org.clojure/clojure "1.8.0"]
   [ring/ring-core ~RING_VERSION]
   [ring/ring-jetty-adapter ~RING_VERSION]
   [clj-http "3.13.0"]]

  :profiles
  {:dev
   {:source-paths ["dev"]
    :dependencies [[org.clojure/clojure]
                   [ring/ring-core]
                   [ring/ring-jetty-adapter]
                   [clj-http]]
    :global-vars
    {*warn-on-reflection* true
     *assert* true}}
   :test
   {:source-paths ["test"]}

   :demo
   {:dependencies [[org.clojure/clojure]]
    :source-paths ["dev"]
    :main ^:skip-aot demo.main
    :aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

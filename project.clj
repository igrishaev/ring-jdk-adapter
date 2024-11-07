(def MIN_JAVA_VERSION "16")

(defproject ring-jdk-adapter "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

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

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"
                  "-Xlint:preview"
                  "--release" ~MIN_JAVA_VERSION]

  :dependencies
  []

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]]

  :profiles
  {:dev
   {:source-paths ["dev"]
    :dependencies [[org.clojure/clojure]]
    :global-vars
    {*warn-on-reflection* true
     *assert* true}}
   :test
   {:source-paths ["test"]}})

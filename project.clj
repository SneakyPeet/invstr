(defproject invstr "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [feedparser-clj "0.2"]
                 [clj-http "3.7.0"]
                 [hickory "0.7.1"]
                 [hiccup-find "0.5.0"]]
  :main invstr.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "invstr.jar")

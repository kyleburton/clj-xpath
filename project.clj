(defproject
    org.clojars.kyleburton/clj-xpath
    "1.0.8"
  :description "Simplified XPath from Clojure."
  :url         "http://github.com/kyleburton/clj-xpath"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "Same as Clojure"}
  :warn-on-reflection true
  :jvm-opts ["-Xmx512M"]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [log4j/log4j "1.2.14"]
                 [xalan "2.7.1"]])

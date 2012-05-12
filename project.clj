(defproject org.clojars.kyleburton/clj-xpath "1.3.0"
  :description "Simplified XPath from Clojure."
  :url         "http://github.com/kyleburton/clj-xpath"
  :lein-release {:deploy-vai :clojars}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "Same as Clojure"}
  :local-repo-classpath true
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [midje "1.4.0"]
                 [log4j/log4j "1.2.14"]
                 [xalan "2.7.1"]
                 [org.clojars.kyleburton/clj-etl-utils "1.3.0"]])

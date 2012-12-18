(defproject org.clojars.kyleburton/clj-xpath "1.4.0"
  :description "Simplified XPath from Clojure."
  :url         "http://github.com/kyleburton/clj-xpath"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "Same as Clojure"}
  :local-repo-classpath true
  :dev-dependencies [[swank-clojure "1.4.3"]
                     [midje "1.4.0"]]
  :dependencies [[org.clojure/clojure "1.5.0-alpha6"]
                 [xalan "2.7.1"]])

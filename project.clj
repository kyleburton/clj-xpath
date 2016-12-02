(defproject com.github.kyleburton/clj-xpath "1.4.10-SNAPSHOT"
  :description          "Simplified XPath from Clojure."
  :url                  "http://github.com/kyleburton/clj-xpath"
  :license              {:name "Eclipse Public License - v 1.0"
                         :url "http://www.eclipse.org/legal/epl-v10.html"
                         :distribution :repo
                         :comments "Same as Clojure"}
  :repositories         {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories  [["releases" :clojars]]
  :local-repo-classpath true
  ;; :main ^:skip-aot clj-xpath.nrepl
  :plugins [[org.clojure/tools.nrepl   "0.2.11"]]
  :profiles             {:dev {:dependencies [[org.clojure/clojure       "1.8.0"]
                                              [cider/cider-nrepl         "0.10.2"]]}
                         ;; NB: the use of ex-info prevents 1.3 from being supported
                         :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
                         :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
                         :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
                         :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases              {"all" ["with-profile" "1.5:1.6:1.7"]}
  :global-vars          {*warn-on-reflection* true}
  :dependencies [
    [org.clojure/tools.logging  "0.3.1"]
    [xalan                      "2.7.2"]
    [prismatic/schema           "1.1.3"]
  ])

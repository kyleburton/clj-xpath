(defproject com.github.kyleburton/clj-xpath "1.4.13"
  :description          "Simplified XPath from Clojure."
  :url                  "http://github.com/kyleburton/clj-xpath"
  :license              {:name "Eclipse Public License - v 1.0"
                         :url "http://www.eclipse.org/legal/epl-v10.html"
                         :distribution :repo
                         :comments "Same as Clojure"}
  :repositories         {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories  [["releases" :clojars]]
  :local-repo-classpath true
  :plugins [[org.clojure/tools.nrepl   "0.2.11"]]
  :profiles             {:dev {:dependencies [[org.clojure/clojure       "1.11.1"]
                                              [cider/cider-nrepl         "0.28.7"]
                                              [org.clojure/data.json     "2.4.0"]]}
                         :1.7    {:dependencies [[org.clojure/clojure "1.7.0"]]}
                         :1.8    {:dependencies [[org.clojure/clojure "1.8.0"]]}
                         :1.9    {:dependencies [[org.clojure/clojure "1.9.0"]]}
                         :1.10   {:dependencies [[org.clojure/clojure "1.10.3"]]}
                         :1.11   {:dependencies [[org.clojure/clojure "1.11.1"]]}}
  :aliases              {"all" ["with-profile" "1.7:1.8:1.9:1.10:1.11"]}
  :global-vars          {*warn-on-reflection* true}
  :dependencies [[org.clojure/tools.logging  "1.2.4"]
                 [prismatic/schema           "1.4.1"]])

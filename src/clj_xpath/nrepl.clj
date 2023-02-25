(ns clj-xpath.nrepl
  (:require
   [cider.nrepl           :refer [cider-nrepl-handler]]
   [clojure.data.json     :as json]
   [clojure.java.io       :as io]
   [clojure.tools.logging :as log]
   [nrepl.server          :refer [start-server]]
   [schema.core           :as s]))

(defonce nrepl-server (atom nil))
(defonce config (atom {:nrepl {:port 4011}}))

;; to stop the running server you can eval: (nrepl.server/stop-server @nrepl-server)
(defn -main [& _args]
  (when (.exists (io/file ".config.json"))
    (reset! config (-> ".config.json" slurp (json/read-str :key-fn #'keyword))))
  (reset! nrepl-server (start-server
                        :port    (-> @config :nrepl :port)
                        :handler cider-nrepl-handler))
  (log/infof "nrepl is running %s" @config)
  (s/set-fn-validation! true))

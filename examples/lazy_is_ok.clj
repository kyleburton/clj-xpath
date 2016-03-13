(ns lazy-is-ok
  (:require
   [clj-xpath.core        :as xp]
   [clj-xpath.lib         :as lib]
   [clj-xpath.test.core   :refer [xml-fixtures]]
   [clojure.tools.logging :as log]))


(defn ex1-lazy-is-not-ok-for-core []
  (xp/with-namespace-context
    (xp/xmlnsmap-from-root-node (:namespaces xml-fixtures))
    (map
     #(xp/$x:text "./atom:title" %)
     (xp/$x "//atom:feed" (:namespaces xml-fixtures)))))

(comment

  (ex1-lazy-is-not-ok-for-core)
  
  ;; => BOOM! you should have gotten a
  ;;   "java.lang.RuntimeExceptionjava.lang.RuntimeException"
  )

(defn ex1-eager-is-ok-for-core []
  (xp/with-namespace-context
    (xp/xmlnsmap-from-root-node (:namespaces xml-fixtures))
    (mapv
     #(xp/$x:text "./atom:title" %)
     (xp/$x "//atom:feed" (:namespaces xml-fixtures)))))

(comment

  (ex1-eager-is-ok-for-core)
  ;; => ["BookingCollection"]

)

(defn ex1-lazy-is-ok-for-lib []
  (let [opts        {:namespace-aware  true
                     :default-encoding "UTF-8"}
        xp-compiler (lib/make-xpath-factory opts)
        xml         (:namespaces xml-fixtures)
        xmlnsmap    (lib/xmlnsmap-from-root-node xp-compiler xml opts)]
    (lib/set-ns-context! xp-compiler xmlnsmap)
    (map
     (fn extract-one-title [elt] (lib/$x:text xp-compiler "./atom:title" elt opts))
     (lib/$x xp-compiler "//atom:feed" xml opts))))

(comment

  (ex1-lazy-is-ok-for-lib)
  ;; => ("BookingCollection")

  )

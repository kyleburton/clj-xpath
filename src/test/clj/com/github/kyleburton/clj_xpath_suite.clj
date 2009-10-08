(ns com.github.kyleburton.clj-xpath-suite
  (:use
   clojure.contrib.test-is)
  (:require
   [com.github.kyleburton.clj-xpath-test])
  (:gen-class))


(def *all-tests*
     '(com.github.kyleburton.clj-xpath-test))

(defn -main [& args]
  (apply run-tests *all-tests*))
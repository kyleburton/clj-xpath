(ns extension-functions
  (:require
   [clj-xpath.core        :as xp]
   [clojure.tools.logging :as log])
  (:import
   [javax.xml.xpath
    XPathFunction
    XPathFunctionResolver]
   [javax.xml.namespace QName]
   [java.util.regex Pattern]))

(defn ex1-using-java-api []
  ;; This example sets a custom xpath function resolver on the
  ;; xpath-compiler.  The only handled match is for the regexp:match
  ;; function.
  (.reset xp/*xpath-compiler*)
  (.setXPathFunctionResolver
   xp/*xpath-compiler*
   (reify
     javax.xml.xpath.XPathFunctionResolver
     (resolveFunction [this fname arity]
       (cond
        (= (javax.xml.namespace.QName. "regexp" "match"))
        (reify javax.xml.xpath.XPathFunction
          (evaluate [this args]
            (let [[node-list pattern] args
                  node                (.item node-list 0)]
              (.matches
               (.matcher (java.util.regex.Pattern/compile pattern)
                         (.getData node))))))

        ;; no matched function
        :otherwise
        nil))))

  {:expect-1 (xp/$x:text
              "//h2[regexp:match(text(), '^big title$')]"
              (slurp "fixtures/extension-functions-001.xhtml"))
   :expect-2 (xp/$x:text+
              "//*[regexp:match(text(), '^big title$')]"
              (slurp "fixtures/extension-functions-001.xhtml"))})

(comment
  (ex1-using-java-api)
  ;; =>
  {:expect-1 "big title", :expect-2 ("big title" "big title")}

  )


(defn ex2-using-registry []
  ;; clj-xpath >= 1.4.6 supports registering xpath extension
  ;; functions.  Before executing xpath expressions, you can register
  ;; Clojure functions by supplying the qualified name
  ;; (javax.xml.namespace.QName) and an arity.  The Clojure function
  ;; will be passed a "this" (the instance of the XPathFunction
  ;; object) and the arguments passed to the function from the xpath
  ;; expression.  This example registers regexp:match and it gets passed
  ;; the DOM node's text() and a string (the regular expression).
  (.reset xp/*xpath-compiler*)
  (xp/register-xpath-function
   ["regexp" "match"] ; QName
   2                  ; arity
   (fn [this [node-list pattern]]
     (log/infof "lambda/evaluate: node-list=%s pattern=%s"
                (xp/dom-node-list->seq node-list)
                pattern)
     (.matches
      (.matcher
       (Pattern/compile pattern)
       (.getData (.item node-list 0))))))
  {:expect-1 (xp/$x:text
              "//h2[regexp:match(text(), '^big title$')]"
              (slurp "fixtures/extension-functions-001.xhtml"))
   :expect-2 (xp/$x:text+
              "//*[regexp:match(text(), '^big title$')]"
              (slurp "fixtures/extension-functions-001.xhtml"))})

(comment
  (ex2-using-registry)
  ;; =>
  {:expect-1 "big title", :expect-2 ("big title" "big title")}

)

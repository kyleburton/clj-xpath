(ns clj-xpath.util)

;; http://common-lisp.net/project/docudown/documentation/anaphora-package/macro-aprog1.html
(defmacro aprog1 [res & body]
  `(let [~'it ~res]
     ~@body
     ~'it))


(defn throwf
  "Helper for throwing exceptions using a format string.  Arguments are

     fmt-string
     args...

See: format"
  [& args]
  (throw (RuntimeException. ^String (apply format args))))




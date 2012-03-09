(ns clj-path.util)

;; http://common-lisp.net/project/docudown/documentation/anaphora-package/macro-aprog1.html
(defmacro aprog1 [res & body]
  `(let [~'it ~res]
     ~@body
     ~'it))
(add-to-list 'slime-lisp-implementations
             '(clj-xpath ("@bin.dir@/repl")
                        :init swank-clojure-init
                        :init-function krb-swank-clojure-init) t)
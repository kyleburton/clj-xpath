(ns com.github.kyleburton.clj-xpath-test
  (:use clojure.contrib.test-is
        [com.github.kyleburton.clj-xpath :as xp :only [$x $x:tag $x:text $x:attrs $x:node tag]]))

(def *xml* {:simple (tag :top-tag "this is a foo")
            :attrs  (tag [:top-tag :name "bobby tables"]
                      "drop tables")
            :nested (tag :top-tag
                      (tag :inner-tag
                        (tag :more-inner "inner tag body")))})

(deftest test-xml->doc
  (is (isa? (class (xp/xml->doc (:simple *xml*))) org.w3c.dom.Document))
  (is (isa? (class (xp/xml->doc (.getBytes (:simple *xml*))))
            org.w3c.dom.Document))
  (is (isa? (class (xp/xml->doc (xp/xml->doc (:simple *xml*))))
            org.w3c.dom.Document)))

(deftest test-$x-top-tag
  (is (= :top-tag
         ($x:tag "/*" (:simple *xml*)))))

(deftest test-$x-get-body
  (is (= "this is a foo"
         ($x:text "/*" (:simple *xml*)))))

(deftest test-$x-get-attrs
  (is (= "bobby tables"
         (:name
          ($x:attrs "/*" (:attrs *xml*))))))

(deftest test-$x-node
  (is (= "top-tag"
         (.getNodeName ($x:node "/*" (:simple *xml*))))))

(deftest test-$x-on-result
  (is (= :more-inner
         ($x:tag "./*"
                 ($x:node "/top-tag/*" (:nested *xml*))))))
(comment

  (test-$x-on-result)

  (test-$x-node)

  ;; pending:


  ($x:node "/top-tag/*" (:nested *xml*))
  ($x:node "/top-tag"   (:nested *xml*))

  ($x:tag "./*" ($x:node "/top-tag/*" (:nested *xml*)))
  ($x:tag "./*" ($x:node "/top-tag"   (:nested *xml*)))

  )
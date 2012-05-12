(ns org.clojars.kyleburton.clj-xpath-test
  (:use clojure.contrib.test-is
        [org.clojars.kyleburton.clj-xpath :as xp :only [$x $x:tag $x:text $x:attrs $x:node $x:tag? $x:text? $x:tag+ $x:text+ tag xp:compile xml->doc nscontext xmlnsmap-from-root-node *xpath-compiler* *namespace-aware*]]))

(def *xml* {:simple (tag :top-tag "this is a foo")
            :attrs  (tag [:top-tag :name "bobby tables"]
                      "drop tables")
            :nested (tag :top-tag
                      (tag :inner-tag
                        (tag :more-inner "inner tag body")))
            :namespaces (slurp "fixtures/namespace1.xml")})

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


(deftest compile-should-compile-strings
  (is (isa?
       (class (xp:compile "//*"))
       javax.xml.xpath.XPathExpression)))

(deftest compile-should-return-compiled-xpath-expr
  (is (isa?
       (class (xp:compile (xp:compile "//*")))
       javax.xml.xpath.XPathExpression)))

(deftest $x-should-support-precompiled-xpath-expressions
  (let [expr (xp:compile "/*")
        doc  (xml->doc (:simple *xml*))]
    (is (= :top-tag
           ($x:tag expr doc)))))

(deftest should-support-input-stream-as-xml-source
  (let [istr (java.io.ByteArrayInputStream. (.getBytes (:simple *xml*)))]
    (is (= :top-tag
           ($x:tag "/*" istr)))))

(deftest test-zero-or-one-results
  (is (not        ($x:tag? "/foo" (:simple *xml*))))
  (is (= :top-tag ($x:tag? "/*"   (:simple *xml*))))
  (is (not               ($x:text? "/foo" (:simple *xml*))))
  (is (= "this is a foo" ($x:text? "/*"   (:simple *xml*)))))

(deftest test-zero-or-more-results
  (is (thrown? Exception        ($x:tag+ "/foo" (:simple *xml*))))
  (is (= :top-tag        (first ($x:tag+ "/*"   (:simple *xml*)))))
  (is (thrown? Exception        ($x:text+ "/foo" (:simple *xml*))))
  (is (= "this is a foo" (first ($x:text+ "/*"   (:simple *xml*))))))


(deftest test-namespace
  (.setNamespaceContext
   *xpath-compiler*
   (nscontext (xmlnsmap-from-root-node (:namespaces *xml*))))
  (binding [*namespace-aware* (atom true)]
    (is (= "BookingCollection" ($x:text "//atom:title" (:namespaces *xml*))))))


(comment

  (.setNamespaceContext
   *xpath-compiler*
   (nscontext {"atom" "http://www.w3.org/2005/Atom"}))
  (binding [*namespace-aware* (atom true)]
    ($x:text "//atom:title" (:namespaces *xml*)))
  ;; => "BookingCollection"

  (test-namespace)

  (is (= "this is a foo"
         ($x:text "/*" (:simple *xml*))))

  ($x-should-support-precompiled-xpath-expressions)

  (test-$x-on-result)

  (test-$x-node)

  ;; pending:


  ($x:node "/top-tag/*" (:nested *xml*))
  ($x:node "/top-tag"   (:nested *xml*))

  ($x:tag "./*" ($x:node "/top-tag/*" (:nested *xml*)))
  ($x:tag "./*" ($x:node "/top-tag"   (:nested *xml*)))

  ($x:tag "./*" (:namespaces *xml*))


  (:attrs (first ($x "//*" (:namespaces *xml*))))



  )

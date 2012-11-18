(ns clj-xpath.test.core
  (:use [clojure.test]
        [clj-xpath.core :as xp
         :only [$x $x:tag $x:text $x:attrs $x:node $x:tag? $x:text? $x:tag+ $x:text+ xp:compile tag xml->doc *xpath-compiler* *namespace-aware* nscontext xmlnsmap-from-root-node with-namespace-context]]))

(def xml-fixtures {:simple (tag :top-tag "this is a foo")
                   :attrs  (tag [:top-tag :name "bobby tables"]
                             "drop tables")
                   :nested (tag :top-tag
                             (tag :inner-tag
                               (tag :more-inner "inner tag body")))
                   :namespaces (slurp "fixtures/namespace1.xml")})

(deftest test-xml->doc
  (is (isa? (class (xp/xml->doc (:simple xml-fixtures))) org.w3c.dom.Document))
  (is (isa? (class (xp/xml->doc (.getBytes (:simple xml-fixtures))))
            org.w3c.dom.Document))
  (is (isa? (class (xp/xml->doc (xp/xml->doc (:simple xml-fixtures))))
            org.w3c.dom.Document)))

(deftest test-$x-top-tag
  (is (= :top-tag
         ($x:tag "/*" (:simple xml-fixtures)))))

(deftest test-$x-get-body
  (is (= "this is a foo"
         ($x:text "/*" (:simple xml-fixtures)))))

(deftest test-$x-get-attrs
  (is (= "bobby tables"
         (:name
          ($x:attrs "/*" (:attrs xml-fixtures))))))

(deftest test-$x-node
  (is (= "top-tag"
         (.getNodeName ($x:node "/*" (:simple xml-fixtures))))))

(deftest test-$x-on-result
  (is (= :more-inner
         ($x:tag "./*"
                 ($x:node "/top-tag/*" (:nested xml-fixtures))))))


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
        doc  (xml->doc (:simple xml-fixtures))]
    (is (= :top-tag
           ($x:tag expr doc)))))

(deftest should-support-input-stream-as-xml-source
  (let [istr (java.io.ByteArrayInputStream. (.getBytes (:simple xml-fixtures)))]
    (is (= :top-tag
           ($x:tag "/*" istr)))))

(deftest test-zero-or-one-results
  (is (not        ($x:tag? "/foo" (:simple xml-fixtures))))
  (is (= :top-tag ($x:tag? "/*"   (:simple xml-fixtures))))
  (is (not               ($x:text? "/foo" (:simple xml-fixtures))))
  (is (= "this is a foo" ($x:text? "/*"   (:simple xml-fixtures)))))

(deftest test-zero-or-more-results
  (is (thrown? Exception        ($x:tag+ "/foo" (:simple xml-fixtures))))
  (is (= :top-tag        (first ($x:tag+ "/*"   (:simple xml-fixtures)))))
  (is (thrown? Exception        ($x:text+ "/foo" (:simple xml-fixtures))))
  (is (= "this is a foo" (first ($x:text+ "/*"   (:simple xml-fixtures))))))


(deftest test-namespace
  (.setNamespaceContext
   *xpath-compiler*
   (nscontext {"atom" "http://www.w3.org/2005/Atom"}))
  (binding [*namespace-aware* true]
    (is (= "BookingCollection" ($x:text "//atom:title" (:namespaces xml-fixtures))))))

(deftest test-with-ns-context-macro
  (with-namespace-context
      (xmlnsmap-from-root-node (:namespaces xml-fixtures))
    (is (= "BookingCollection" ($x:text "//atom:title" (:namespaces xml-fixtures))))))

(deftest test-lazy-children
  (is (nil? (:children ($x "/top-tag/*" (:simple xml-fixtures)))))
  (is (not  (realized? (:children (first ($x "/top-tag/*" (:nested xml-fixtures)))))))
  (is (=    "inner tag body" (:text (first (deref (:children (first ($x "/top-tag/*" (:nested xml-fixtures))))))))))

(comment

  (with-namespace-context {"atom" "http://www.w3.org/2005/Atom"}
    ($x:text "//atom:title" (:namespaces xml-fixtures)))

  (with-namespace-context (xmlnsmap-from-root-node (:namespaces xml-fixtures))
    ($x:text "//atom:title" (:namespaces xml-fixtures)))

  (run-tests)

  )

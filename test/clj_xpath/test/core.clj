(ns clj-xpath.test.core
  (:use [clojure.test]
        [clj-xpath.core :as xp
         :only [$x $x:tag $x:text $x:attrs $x:node $x:tag? $x:text? $x:tag+ $x:text+ xp:compile tag xml->doc *xpath-compiler* *namespace-aware* nscontext xmlnsmap-from-root-node with-namespace-context abs-path]]))

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

(def labels-xml "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>
<labels>
  <label added=\"2003-06-20\">
    <quote>
      <emph>Midwinter Spring</emph> is its own season&#8230;
    </quote>
    <name>Thomas Eliot</name>
    <address>
      <street>3 Prufrock Lane</street>
      <city>Stamford</city>
      <state>CT</state>
    </address>
  </label>
  <label added=\"2003-06-10\">
    <name>Ezra Pound</name>
    <address>
      <street>45 Usura Place</street>
      <city>Hailey</city>
      <state>ID</state>
    </address>
  </label>
</labels>")

(deftest test-absolute-paths
  (let [dom (xml->doc labels-xml)
        root (first ($x "/labels" dom))
        children @(:children root)]
    (is (= "/labels[1]"           (abs-path root)))
    (is (= "/labels[1]/text()[1]" (abs-path (first children))))
    (is (= "/labels[1]/label[1]"  (abs-path (second children))))
    (is (= "/labels[1]/text()[2]" (abs-path (nth children 2))))
    (is (= "/labels[1]/label[2]"  (abs-path (nth children 3))))
    (is (= "/labels[1]/text()[3]" (abs-path (nth children 4))))))

;; The document soap1.xml uses 3 namepsaces.  The 3rd is implicit / blank in the SOAP Body element.
(deftest test-with-blank-ns
  (let [xml (slurp "fixtures/soap1.xml")]
    (xp/with-namespace-awareness
      (let [doc (xp/xml->doc xml)]
        (xp/set-namespace-context! (xp/xmlnsmap-from-document doc))
        (is (= :OTA_HotelAvailRQ (xp/$x:tag "/soapenv:Envelope/soapenv:Body/:OTA_HotelAvailRQ" doc)))))))

#_(deftest test-abs-path-with-blank-namespace
 (let [xml (slurp "fixtures/soap1.xml")]
   (xp/with-namespace-awareness
     (let [doc (xp/xml->doc xml)]
       (xp/set-namespace-context! (xp/xmlnsmap-from-document doc))
       (is (= "/soapenv:Envelope[1]/soapenv:Body[1]/:OTA_HotelAvailRQ[1]"
              (xp/abs-path (first (xp/$x "/soapenv:Envelope[1]/soapenv:Body[1]/:OTA_HotelAvailRQ[1]" doc)))))))))


(comment

  (test-with-blank-ns)



  (with-namespace-context {"atom" "http://www.w3.org/2005/Atom"}
    ($x:text "//atom:title" (:namespaces xml-fixtures)))

  (with-namespace-context (xmlnsmap-from-root-node (:namespaces xml-fixtures))
    ($x:text "//atom:title" (:namespaces xml-fixtures)))

  (run-tests)

  (let [doc (slurp "sabre.xml")
        xp (str "/soapenv:Envelope/soapenv:Body/:OTA_HotelAvailRQ/"
                ":AvailRequestSegments/:AvailRequestSegment/"
                ":HotelSearchCriteria/:Criterion/:HotelRef")
        ns-map {"soapenv" "http://schemas.xmlsoap.org/soap/envelope/"
                "head"    "http://htng.org/1.1/Header/"
                ""        "http://www.opentravel.org/OTA/2003/05"}]
    (xp/with-namespace-context ns-map
      (xp/$x:attrs* xp doc :HotelCode)))
  ;; => ("11206")

  (let [doc (slurp "sabre.xml")
        ns-map {"soapenv" "http://schemas.xmlsoap.org/soap/envelope/"
                "head"    "http://htng.org/1.1/Header/"}]
    (xp/with-namespace-context ns-map
      (let [node (xp/$x:node "/soapenv:Envelope/soapenv:Body/*" doc)]
        #_(xp/set-namespace-context! {"" "http://www.opentravel.org/OTA/2003/05"})
        (xp/xmlnsmap-from-node node)
        #_(xp/$x:tag "./:OTA_HotelAvailRQ" node))))

  (defn get-soap-body [xml]
    (xp/with-namespace-awareness
      (let [doc (xp/xml->doc xml)]
        (xp/set-namespace-context! (xp/xmlnsmap-from-root-node doc))
        (xp/node->xml (xp/$x:node "/soapenv:Envelope/soapenv:Body/*" doc)))))

  (let [orig-xml (slurp "sabre.xml")
        body-xml (xp/xml->doc (get-soap-body orig-xml))]
    (xp/$x:attrs* "/OTA_HotelAvailRQ/AvailRequestSegments/AvailRequestSegment/HotelSearchCriteria/Criterion/HotelRef" body-xml :HotelCode))
  ("11206")






  )













(ns clj-xpath.core
  (:require
   [clojure.string :as str-utils])
  (:import
   [java.io                     InputStream InputStreamReader StringReader File IOException ByteArrayInputStream]
   [org.xml.sax                 InputSource SAXException]
   [javax.xml.transform         Source]
   [javax.xml.transform.stream  StreamSource]
   [javax.xml.validation        SchemaFactory]
   [org.w3c.dom                 Document Node]
   [javax.xml.parsers           DocumentBuilderFactory]
   [javax.xml.xpath             XPathFactory XPathConstants XPathExpression])
  (:gen-class))

(def ^:dynamic *namespace-aware*  false)
(def ^:dynamic *default-encoding* "UTF-8")

(defn throwf [& args]
  (throw (RuntimeException. (apply format args))))

(defn logf [fmt & args]
  (.println System/err (apply format fmt args)))

(defn node-list->seq [node-list]
  (loop [length (.getLength node-list)
         idx    0
         res    []]
                                        ;(logf "node-list: idx:%s node-list=%s length=%s" idx node-list length)
    (if (>= idx length)
      (reverse res)
      (recur length
             (inc idx)
             (cons (.item node-list idx) res)))))

(def ^:dynamic *validation* false)

(defn- input-stream->dom [istr & [opts]]
  (let [opts        (or opts {})
        dom-factory (let [it (DocumentBuilderFactory/newInstance)]
                      (.setNamespaceAware it *namespace-aware*)
                      (.setValidating it (opts :validation *validation*))
                      it)
        builder     (let [it (.newDocumentBuilder dom-factory)]
                      (if (contains? opts :error-handler)
                        (.setErrorHandler it (:error-handler opts)))
                      it)]
    (.parse builder istr)))

(defn- xml-bytes->dom [bytes & [opts]]
  (input-stream->dom (ByteArrayInputStream. bytes) opts))

(defmulti  xml->doc (fn [thing & [opts]] (class thing)))
(defmethod xml->doc String               [thing & [opts]] (xml-bytes->dom (.getBytes thing *default-encoding*)))
(defmethod xml->doc (Class/forName "[B") [thing & [opts]] (xml-bytes->dom thing))
(defmethod xml->doc InputStream          [thing & [opts]] (input-stream->dom thing))
(defmethod xml->doc org.w3c.dom.Document [thing & [opts]] thing)
(defmethod xml->doc :default             [thing & [opts]]
  (throwf "Error, don't know how to build a doc out of '%s' of class %s" thing (class thing)))

(comment

  ($x:text "/this" (xml-bytes->dom (.getBytes "<this>foo</this>") {:validation true}))

  ($x:text "/this" (xml-bytes->dom (.getBytes "<this>foo</this>") {:validation false}))

  (binding [*validation* false]
    ($x:text "/this" "<this>foo</this>"))

  ($x:text "/this"
           (xml->doc "<this>foo</this>" {:validating false}))

  )

(defn attrs
  "Extract the attributes from the node."
  [nodeattrs]
                                        ;(logf "attrs: nodeattrs=%s attrs=%s" nodeattrs (.getAttributes nodeattrs))
  (if-let [the-attrs (.getAttributes nodeattrs)]
    (loop [[node & nodes] (node-list->seq (.getAttributes nodeattrs))
           res {}]
      (if node
        (recur nodes (assoc res (keyword (.getNodeName node)) (.getTextContent node)))
        res))
    nil))

(defn text
  "Accessor for text content from the node."
  [#^Node node]
  (.getTextContent node))

(defn node-name
  "Accessor for the node's name."
  [#^Node node]
  (keyword (.getNodeName node)))

(defn- node->map [#^Node node]
  (let [lazy-children (fn [n] (delay
                               (map node->map
                                    (node-list->seq (.getChildNodes n)))))
        m  {:node node
            :tag   (node-name node)
            :attrs (attrs node)
            :text  (text node)}
        m   (if (.hasChildNodes node)
              (assoc m :children (lazy-children node))
              m)
        ]
    m))

(defmulti xp:compile class)

(def ^:dynamic *xpath-factory* (org.apache.xpath.jaxp.XPathFactoryImpl.))
(def ^:dynamic *xpath-compiler* (.newXPath *xpath-factory*))

(defmethod xp:compile String          [xpexpr] (.compile *xpath-compiler* xpexpr))
(defmethod xp:compile XPathExpression [xpexpr] xpexpr)
(defmethod xp:compile :default        [xpexpr]
  (throwf "xp:compile: don't know how to compile xpath expr of type:%s '%s'" (class xpexpr) xpexpr))

(defmulti $x
  "Perform an xpath query on the given XML document which may be a String, byte array, or InputStream."
  (fn [xp xml-thing] (class xml-thing)))

(defmethod $x String [xp xml]
  ($x xp (xml->doc (.getBytes xml *default-encoding*))))

(defmethod $x (Class/forName "[B") [xp bytes]
  ($x xp (xml->doc bytes)))

(defmethod $x InputStream [xp istr]
  ($x xp (xml->doc istr)))

                                        ;(defmethod $x clojure.lang.PersistentArrayMap [xp xml] ($x xp (:node xml)))
(defmethod $x java.util.Map                   [xp xml] ($x xp (:node xml)))

;; assume a Document (or api compatible)
(defmethod $x :default [xp-expression doc]
  (map node->map
       (node-list->seq
        (.evaluate (xp:compile xp-expression) doc XPathConstants/NODESET))))

;; ($x "//*" (tag :foo "body"))

(defn summarize
  "Summarize a string to a specific maximu length (truncating it and adding ... if it is longer than len)."
  [s len]
  (let [s (str s)]
    (if (>= len (.length s))
      s
      (str (.substring s 0 len) "..."))))

(defn $x:tag*
  "Perform an xpath search, resulting in zero or more nodes, return just the tag name."
  [xp xml]
  (map :tag ($x xp xml)))

(defn $x:tag? [xp xml]
  (let [res ($x:tag* xp xml)]
    (if (> (count res) 1)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:tag+ [xp xml]
  (let [res ($x:tag* xp xml)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:tag [xp xml]
  (let [res ($x:tag* xp xml)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:text* [xp xml]
  (map :text ($x xp xml)))

(defn $x:text? [xp xml]
  (let [res ($x:text* xp xml)]
    (if (> (count res) 1)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:text+ [xp xml]
  (let [res ($x:text* xp xml)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:text [xp xml]
  (let [res ($x:text* xp xml)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:attrs* [xp xml attr-name]
  (map (if (keyword? attr-name) attr-name (keyword attr-name)) (map attrs (map :node ($x xp xml)))))

(defn $x:attrs? [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (> (count res) 1)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:attrs+ [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:attrs [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:node* [xp xml]
  (map :node ($x xp xml)))

(defn $x:node? [xp xml]
  (let [res ($x:node* xp xml)]
    (if (> (count res) 1)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:node+ [xp xml]
  (let [res ($x:node* xp xml)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:node [xp xml]
  (let [res ($x:node* xp xml)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defmulti format-tag (fn [arg & [with-attrs]] (class arg)))

(defn format-tag-seq [tag-and-attrs & [with-attrs]]
  (if with-attrs
    (let [[tag & attrs] tag-and-attrs]
      (format "%s %s" (name tag)
              (str-utils/join " " (map (fn [[key val]]
                                         (format "%s=\"%s\"" (if (keyword? key) (name key) key) val))
                                       (partition 2 attrs)))))
    (name (first tag-and-attrs))))

(defmethod format-tag clojure.lang.PersistentVector [tag-and-attrs & [with-attrs]]
  (format-tag-seq tag-and-attrs with-attrs))

(defmethod format-tag clojure.lang.LazilyPersistentVector [tag-and-attrs & [with-attrs]]
  (format-tag-seq tag-and-attrs with-attrs))

(defmethod format-tag clojure.lang.Keyword [tag & [_]]
  (name tag))

(defmethod format-tag :default [tag & [_]]
  (str tag))


(defn tag [tag & body]
  (format "<%s>%s</%s>" (format-tag tag :with-attrs) (apply str body) (format-tag tag)))



(defn xmlnsmap-from-root-node [xml]
  (let [attrs (:attrs (first ($x "//*" xml)))]
    (reduce
     (fn xyz [m k]
       (if (.startsWith (name k) "xmlns:")
         (let [val (get attrs k)
               k    (.replace (name k) "xmlns:" "")]
           (-> m
               (assoc k   val)
               (assoc val k)))
         m))
     {}
     (keys attrs))))

(defn nscontext [prefix-map]
  (let [uri-map (reduce (fn unmap [m k]
                          (assoc m (get prefix-map k) k))
                        {}
                        (keys prefix-map))]
    (proxy [javax.xml.namespace.NamespaceContext]
        []
      (getNamespaceURI [prefix]
                       ;;(println (format "getNamespaceURI: %s => %s" prefix (get prefix-map prefix)))
                       (get prefix-map prefix))
      (getPrefixes [val]
                   ;;(println (format "getPrefixes: %s" val))
                   nil)
      (getPrefix [uri]
                 ;;(println (format "getPrefix: %s => %s" uri (get uri-map uri)))
                 (get uri-map uri)))))

;; (defn string-reader [s] (InputSource. (StringReader. s)))
;; ;; turn a Node into the same form the clojure.xml/parse builds
;; (defmulti  node-parse (fn [thing] (class thing)))
;; (defmethod node-parse String [thing] (node-parse (.getDocumentElement (xml->doc thing))))
;; (defmethod node-parse Node   [node]
;;   (logf "node-parse: node=%s; childNodes=%s" node (.getChildNodes node))
;;   ;; check if it's a Node/TEXT_NODE
;;   (if (= Node/TEXT_NODE (.getNodeType node))
;;     (text node)
;;     {:tag     (node-name node)
;;      :attrs   (attrs node)
;;      ;; Should we flatten the vector if it only has 1 elt?
;;      :content (apply vector (map node-parse (node-list->seq (.getChildNodes node))))}))

;; ;; TODO: render (from whatever node we're at)

(comment
  (node-parse (tag :foo "some body content"))
  (clojure.xml/parse (string-reader (tag :foo "body")))
  {:tag :foo, :attrs nil, :content ["body"]}

  (.getTagName (.getDocumentElement (xml->doc (tag :foo "body"))))

  (tag :foo "body")
  (tag [:foo :name "bobby tables"] "select me from where I come from")

  ($x "/*" (tag [:foo :name "bobby tables"] "select me from where I come from"))
  ($x:tag "/*" (tag [:foo :name "bobby tables"] "select me from where I come from"))
  ($x:attrs "/*" (tag [:foo :name "bobby tables"] "select me from where I come from"))
  ($x:text "/*" (tag [:foo :name "bobby tables"] "select me from where I come from"))

  ($x "//project" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml"))

  ($x:tag "/*" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml"))

  (binding [*namespace-aware*  false]
    ($x "//project" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml")))

  (binding [*namespace-aware*  true]
    ($x "//project" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml")))

  (map :tag ($x "//*" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml")))

  ($x "//goal" (slurp "/Users/kburton/personal/projects/sandbox/clj-xpath/pom.xml"))

  (with-xpath [$x "my-xml-document-please-do-the-right-thing"]
    ($x "/*")
    {:tag (tag)
     :body (text)})

  ;; Notes:
  ;;   have $x returns a map (see below) which is 1 level deep the map has: { :tag :text :node }
  ;;      where the node is an org.w3c.dom.Node
  ;;      we then extend tbe multimethod $x to support this map and just extract the node
  ;;   introduce a 'parse' binding which returns the same type of structure
  ;;      that xml.parse does (the nested map/vector thingy)

  (with-xpath "my-xml-document-please"
    (let [[{tag :tag body :text} node :node] ($x "/*")]
      (do-some-stuff)))

  (def *doc* nil)

  (defmacro with-xpath [the-doc & body]
    `(bindings [*doc* (xml->doc ~the-doc)]
               (let [$x (fn [xp] ($x xp *doc*))]
                 ~@body)))


  )


(defn with-namespace-context* [context-map f]
  (binding [*namespace-aware* true
            *xpath-compiler*  (.newXPath *xpath-factory*)]
    (.setNamespaceContext *xpath-compiler* (nscontext context-map))
    (f)))

(defmacro with-namespace-context [context-map & body]
  `(with-namespace-context* ~context-map (fn [] ~@body)))

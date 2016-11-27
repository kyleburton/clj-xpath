(ns clj-xpath.lib
  (:require
   [clojure.string :as str-utils]
   [clj-xpath.util :refer [throwf]]
   [schema.core    :as s])
  (:import
   [java.io                     InputStream InputStreamReader StringReader File IOException ByteArrayInputStream]
   [org.xml.sax                 InputSource SAXException]
   [javax.xml.transform         Source]
   [javax.xml.transform.stream  StreamSource]
   [javax.xml.validation        SchemaFactory]
   [org.w3c.dom                 Document Node]
   [javax.xml.parsers           DocumentBuilderFactory]
   [javax.xml.xpath             XPathFactory XPathConstants XPathExpression XPath]
   [javax.xml                   XMLConstants]
   [javax.xml.namespace QName]))

(def disallow-doctype-decl       "http://apache.org/xml/features/disallow-doctype-decl")
(def load-external-dtd           "http://apache.org/xml/features/nonvalidating/load-external-dtd")
(def external-general-entities   "http://xml.org/sax/features/external-general-entities")
(def external-parameter-entities "http://xml.org/sax/features/external-parameter-entities")

(def Options
  {(s/optional-key :validation)                   s/Bool
   (s/optional-key :default-encoding)             s/Str
   (s/optional-key :feature-secure-processing)    s/Bool
   (s/optional-key :disallow-doctype-decl)        s/Bool
   (s/optional-key :load-external-dtd)            s/Bool
   (s/optional-key :external-general-entities)    s/Bool
   (s/optional-key :external-parameter-entities)  s/Bool
   (s/optional-key :namespace-aware)              s/Bool})

(defn dom-node-map->seq
  "Convert a org.w3c.dom.NodeList into a clojure sequence."
  [^org.w3c.dom.NamedNodeMap node-list]
  (loop [length (.getLength node-list)
         idx    0
         res    []]
    (if (>= idx length)
      (reverse res)
      (recur length
             (inc idx)
             (cons (.item node-list idx) res)))))


(defn dom-node-list->seq
  "Convert a org.w3c.dom.NodeList into a clojure sequence."
  [^org.w3c.dom.NodeList node-list]
  (loop [length (.getLength node-list)
         idx    0
         res    []]
    (if (>= idx length)
      (reverse res)
      (recur length
             (inc idx)
             (cons (.item node-list idx) res)))))

(defn node-list->seq
  "Convert a org.w3c.dom.NodeList into a clojure sequence."
  [thing]
  (cond
    (isa? (class thing) org.w3c.dom.NodeList)
    (dom-node-list->seq thing)

    (isa? (class thing) org.w3c.dom.NamedNodeMap)
    (dom-node-map->seq thing)

    :unrecognized
    (throw (RuntimeException. "Unknown node list object type=%s" (class thing)))))


(defn make-dom-factory [opts]
  (doto (DocumentBuilderFactory/newInstance)
    (.setNamespaceAware (:namespace-aware opts false))
    (.setValidating (:validation opts false))
    (.setFeature XMLConstants/FEATURE_SECURE_PROCESSING (:feature-secure-processing   opts true))
    (.setFeature disallow-doctype-decl                  (:disallow-doctype-decl       opts true))
    (.setFeature load-external-dtd                      (:load-external-dtd           opts false))
    (.setFeature external-general-entities              (:external-general-entities   opts false))
    (.setFeature external-parameter-entities            (:external-parameter-entities opts false))))

(defn input-stream->dom
  "Convert an input stream into a DOM."
  [^java.io.InputStream istr & [opts]]
  (let [opts        (or opts {})
        dom-factory (make-dom-factory opts)
        builder     (.newDocumentBuilder ^DocumentBuilderFactory dom-factory)
        error-h     (:error-handler opts)]
    (when error-h
      (.setErrorHandler builder error-h))
    (.parse builder istr)))

(defn xml-bytes->dom
  "Convert a byte array into a DOM."
  [bytes & [opts]]
  (with-open [istr (ByteArrayInputStream. bytes)]
    (input-stream->dom istr opts)))

(defmulti  xml->doc
  "Convert various forms of XML into a Document.  Supported forms:

    String
    byte array
    Input Stream
    org.w3c.dom.Document
    org.w3c.dom.Node
  "
  (fn [thing & [opts]] (class thing)))

(defmethod xml->doc String               [thing & [opts]] (xml-bytes->dom (.getBytes ^String thing ^String (:default-encoding opts "UTF-8")) opts))
(defmethod xml->doc (Class/forName "[B") [thing & [opts]] (xml-bytes->dom thing opts))
(defmethod xml->doc InputStream          [thing & [opts]] (input-stream->dom thing opts))
(defmethod xml->doc org.w3c.dom.Document [thing & [opts]] thing)
(defmethod xml->doc Node                 [thing & [opts]] thing)
(defmethod xml->doc :default             [thing & [opts]]
  (throwf "Error, don't know how to build a doc out of '%s' of class %s" thing (class thing)))


(defn attrs
  "Extract the attributes from the node."
  [^Node nodeattrs]
  (if-let [the-attrs (.getAttributes nodeattrs)]
    (loop [[^Node node & nodes] (node-list->seq (.getAttributes nodeattrs))
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


(defn node->map
  "Create a logical map out of the Node's properties:
    :node      the-node
    :tag       tag name of node
    :attrs     map of the node's attributes
    :text      the text of the node
    :children  a lazy sequence of the node's children.
  "
  [#^Node node]
  (let [lazy-children (fn [#^Node n] (delay
                                      (map node->map
                                           (node-list->seq (.getChildNodes n)))))
        m  {:node node
            :tag   (node-name node)
            :attrs (attrs node)
            :text  (text node)}
        m   (if (.hasChildNodes node)
              (assoc m :children (lazy-children node))
              m)]
    m))


(defmulti xp:compile
  "Compile an XPath expression.  If the argument is already a compiled XPath expression, it is returned as-is."
  (fn [xp-compiler xpexpr] (class xpexpr)))

(defmethod xp:compile String          [xp-compiler xpexpr] (.compile ^XPath xp-compiler xpexpr))
(defmethod xp:compile XPathExpression [xp-compiler xpexpr] xpexpr)
(defmethod xp:compile :default        [xp-compiler xpexpr]
  (throwf "xp:compile: don't know how to compile xpath expr of type:%s '%s'" (class xpexpr) xpexpr))


(defmulti $x
  "Perform an xpath query on the given XML document which may be a String, byte array, or InputStream.
  See xml->doc, and xp:compile."
  (fn [xp-compiler xp xml-thing & [opts]] (class xml-thing)))

(defmethod $x String               [xp-compiler xp ^String xml & [opts]]
  ($x xp-compiler xp (xml->doc (.getBytes ^String xml ^String (:default-encoding opts "UTF-8")) opts) opts))

(defmethod $x (Class/forName "[B") [xp-compiler xp bytes & [opts]]
  ($x xp-compiler xp (xml->doc bytes opts) opts))

(defmethod $x InputStream          [xp-compiler xp istr & [opts]]
  ($x xp-compiler xp (xml->doc istr opts) opts))

(defmethod $x java.util.Map        [xp-compiler xp xml & [opts]]
  ($x xp-compiler xp (:node xml) opts))

;; assume a Document (or api compatible)
(defmethod $x :default [xp-compiler xp-expression ^Document doc & [opts]]
  (map node->map
       (node-list->seq
        (.evaluate ^javax.xml.xpath.XPathExpression (xp:compile xp-compiler xp-expression) doc XPathConstants/NODESET))))

(defn summarize
  "Summarize a string to a specific maximu length (truncating it and adding ... if it is longer than len)."
  [s len]
  (let [s (str s)]
    (if (>= len (.length s))
      s
      (str (.substring s 0 len) "..."))))


(defn $x:tag*
  "Perform an xpath search, resulting in zero or more nodes, return just the tag name."
  [xp-compiler xp xml & [opts]]
  (map :tag ($x xp-compiler xp xml opts)))

(defn $x:tag?
  "Perform an xpath search, resulting in zero or one node.  Return only the tag name."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:tag* xp-compiler xp xml opts)]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:tag+
  "Perform an xpath search, resulting in one or more nodes.  Return only the tag name."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:tag* xp-compiler xp xml opts)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:tag [xp-compiler xp xml & [opts]]
  "Perform an xpath search, resulting in one and only one node.  Return only the tag name."
  (let [res ($x:tag* xp-compiler xp xml opts)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defn $x:text*
  "Perform an xpath search, resulting in zero or more nodes.  Return only each the node's text."
  [xp-compiler xp xml & [opts]]
  (map :text ($x xp-compiler xp xml opts)))

(defn $x:text?
  "Perform an xpath search, resulting in zero or one node.  Return only the node's text."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:text* xp-compiler xp xml opts)]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:text+
  "Perform an xpath search, resulting in one or more nodes.  Return only each the node's text."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:text* xp-compiler xp xml opts)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:text
  "Perform an xpath search, resulting in one and only one node.  Return only the node's text."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:text* xp-compiler xp xml opts)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defn $x:attrs*
  "Perform an xpath search, resulting in zero or more nodes.  When an attr-name is passed, return only each the node's attrs."
  ([xp-compiler xp xml opts]
   (map attrs (map :node ($x xp-compiler xp xml opts))))
  ([xp-compiler xp xml attr-name opts]
   (map (if (keyword? attr-name) attr-name (keyword attr-name)) (map attrs (map :node ($x xp-compiler xp xml opts))))))


(defn $x:attrs?
  "Perform an xpath search, resulting in zero or one node.  Return only the node's attrs."
  [xp-compiler xp xml & [opts]]
  (let [res (map attrs (map :node ($x xp-compiler xp xml opts)))]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defn $x:attrs+
  "Perform an xpath search, resulting in one or more nodes.  Return only each the node's attrs."
  [xp-compiler xp xml & [opts]]
  (let [res (map attrs (map :node ($x xp-compiler xp xml opts)))]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))


(defn $x:attrs
  "Perform an xpath search, resulting in one and only one node.  Return only the node's attrs."
  [xp-compiler xp xml & [opts]]
  (let [res (map attrs (map :node ($x xp-compiler xp xml opts)))]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defn $x:node*
  "Perform an xpath search, resulting in zero or more nodes.  Returns the nodes."
  [xp-compiler xp xml & [opts]]
  (map :node ($x xp-compiler xp xml opts)))

(defn $x:node?
  "Perform an xpath search, resulting in zero or one node.  Returns the node."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:node* xp-compiler xp xml opts)]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:node+
  "Perform an xpath search, resulting in one or more nodes.  Returns the nodes."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:node* xp-compiler xp xml opts)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))


(defn $x:node
  "Perform an xpath search, resulting in one and only one node.  Returns the node."
  [xp-compiler xp xml & [opts]]
  (let [res ($x:node* xp-compiler xp xml opts)]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))


(defmulti format-tag
  "Helper for generating XML (mostly used by the test suite)."
  (fn [arg & [with-attrs]] (class arg)))

(defn format-tag-seq
  "Helper for generating XML (mostly used by the test suite)."
  [tag-and-attrs & [with-attrs]]
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


(defn tag
  "Helper for generating XML (mostly used by the test suite)."
  [tag & body]
  (format "<%s>%s</%s>" (format-tag tag :with-attrs) (apply str body) (format-tag tag)))

(defn xmlnsmap-from-node
  "Extract a map of XML Namespace prefix to URI (and URI to prefix) from the given node."
  [node]
  (let [attributes (attrs node)]
    (reduce
     (fn extract-namespaces [m k]
       (if (.startsWith (name k) "xmlns")
         (let [val (get attributes k)
               k    (.replaceAll (name k) "^xmlns:?" "")]
           (-> m
               (assoc k   val)
               (assoc val k)))
         m))
     {}
     (keys attributes))))

(defn xmlnsmap-from-root-node
  "Extract a map of XML Namespace prefix to URI (and URI to prefix) from the root node of the document."
  [xp-compiler xml & [opts]]
  (xmlnsmap-from-node (first ($x:node* xp-compiler "//*" xml opts))))


(defn xmlnsmap-from-document
  "Extract a map of XML Namespace prefix to URI (and URI to prefix) recursively from the entire document."
  [xml]
  (let [^Node node   (xml->doc xml)]
    (reduce
     (fn merge-nsmaps [m node]
       (merge
        m
        (xmlnsmap-from-document node)))
     (xmlnsmap-from-node node)
     (node-list->seq (.getChildNodes node)))))

(defn nscontext
  "Create a javax.xml.namespace.NamespaceContext from the given map."
  [prefix-map]
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

;; NB: these macros and helpers specifically deal with the dynamic
;; vars, I don't think they belong in the lib namespace

;; with-namespace-awareness*
;; with-namespace-awareness
;; set-namespace-context!
;; with-namespace-context*
;; with-namespace-context




(defmulti abs-path*
  "Determine the absolute path to node."
  (fn [^Node node] (.getNodeType node)))

(defn- walk-back
  "Walk up the document to the root node, tracing the path all the way back up."
  [^Node node tail]
  (if-let [anc (.getParentNode node)]
    (str (abs-path* anc) "/" tail)
    tail))

(defmethod abs-path* Node/ELEMENT_NODE [^org.w3c.dom.Element node]
  (let [name (.getTagName node)
        posn (count (->> node
                         (iterate #(.getPreviousSibling ^org.w3c.dom.Element %))
                         (take-while boolean)
                         ;; (filter #(and (= Node/ELEMENT_NODE (.getNodeType ^Node %)) (= name (.getTagName ^Node %))))
                         (filter (fn [^Node node]
                                   (and (= Node/ELEMENT_NODE (.getNodeType node))
                                        (= name (.getTagName ^org.w3c.dom.Element node)))))
                         ))
        step (str name "[" posn "]")]
    (walk-back node step)))

(defmethod abs-path* Node/ATTRIBUTE_NODE [node]
  (throw (RuntimeException. "Not implemented yet.")))

(defn- node-type->xpath-function [nt]
  ({Node/TEXT_NODE                   "text"
    Node/COMMENT_NODE                "comment"
    Node/PROCESSING_INSTRUCTION_NODE "processing-instruction"} nt))

(defmethod abs-path* :default [^Node node]
  (let [nt   (.getNodeType node)
        posn (count (->> node
                         (iterate #(.getPreviousSibling ^Node %))
                         (take-while boolean)
                         (filter #(and % (= nt (.getNodeType ^Node %))))))
        step (str (node-type->xpath-function nt) "()[" posn "]")]
    (walk-back node step)))

(defmethod abs-path* Node/DOCUMENT_NODE [node] "")

(defn abs-path
  "Determine an absolute xpath expression that locates this node inside the enclosing document.
   Based on code developed by Florian Bösch on XML-SIG (http://mail.python.org/pipermail/xml-sig/2004-August/010423.html)
   as enhanced and published by Uche Ogbuji (http://www.xml.com/pub/a/2004/11/24/py-xml.html)"
  [node]
  (when (:node node) (abs-path* (:node node))))

(defn node->xml
  "Convert a Node to a String of XML."
  [^Node node]
  (let [dw         (java.io.StringWriter.)
        serializer (..
                    (javax.xml.transform.TransformerFactory/newInstance)
                    newTransformer)]
    (.transform
     serializer
     (javax.xml.transform.dom.DOMSource. node)
     (javax.xml.transform.stream.StreamResult. dw))
    (str dw)))

(defn ->qualified-name [qname]
  (cond
    (isa? (class qname) QName)
    qname

    (string? qname)
    (QName. qname)

    (and (sequential? qname) (= 1 (count qname)))
    (QName. (first qname))

    (and (sequential? qname) (= 2 (count qname)))
    (QName. (first qname) (second qname))

    (and (sequential? qname) (= 3 (count qname)))
    (QName. (nth qname 0) (nth qname 1) (nth qname 2))

    :otherwise
    (throw (RuntimeException. (format "Error: don't know how to make a QName out of: %s" qname)))))


(defonce xpath-functions-by-xp-compiler (atom {}))

(defn register-xpath-function [^XPath xp-compiler qualified-name arity f]
  (let [qname     (->qualified-name qualified-name)
        xpfn      (reify javax.xml.xpath.XPathFunction
                    (evaluate [this args]
                      (f this args)))
        fmap-atom (if (-> xpath-functions-by-xp-compiler deref (contains? xp-compiler))
                    (-> xpath-functions-by-xp-compiler deref (get xp-compiler))
                    (do
                      (swap! xpath-functions-by-xp-compiler assoc xp-compiler (atom {}))
                      (-> xpath-functions-by-xp-compiler deref (get xp-compiler))))]
    (swap! fmap-atom
           assoc-in
           [qname arity]
           xpfn)
    (when (nil? (.getXPathFunctionResolver xp-compiler))
      (let [resolver (reify
                       javax.xml.xpath.XPathFunctionResolver
                       (resolveFunction [this fname arity]
                         (get
                          (->>
                           @fmap-atom
                           (filter
                            (fn [[qname arities]]
                              (= qname fname)))
                           first
                           second)
                          arity)))]
        (.setXPathFunctionResolver ^XPath xp-compiler resolver)))
    qname))

(s/defn make-xpath-compiler [opts :- Options]
  (let [fact (org.apache.xpath.jaxp.XPathFactoryImpl.)
        xp-compiler (.newXPath ^org.apache.xpath.jaxp.XPathFactoryImpl fact)]
    xp-compiler))

(defn set-ns-context! [xp-compiler context-map]
  (.setNamespaceContext ^XPath xp-compiler (nscontext context-map))
  xp-compiler)

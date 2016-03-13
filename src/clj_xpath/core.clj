(ns clj-xpath.core
  (:require
   [clojure.string :as str-utils]
   [clj-xpath.util :refer [throwf]]
   [clj-xpath.lib  :as lib]
   [schema.core    :as s])
  (:import
   [java.io                     InputStream InputStreamReader StringReader File IOException ByteArrayInputStream]
   [org.xml.sax                 InputSource SAXException]
   [javax.xml.transform         Source]
   [javax.xml.transform.stream  StreamSource]
   [javax.xml.validation        SchemaFactory]
   [org.w3c.dom                 Document Node]
   [javax.xml.parsers           DocumentBuilderFactory]
   [javax.xml.xpath             XPathFactory XPathConstants XPathExpression]
   [javax.xml                   XMLConstants]
   [javax.xml.namespace QName])
  (:gen-class))

(def ^{:dynamic true} *namespace-aware*  false)
(def ^{:dynamic true :tag String} *default-encoding* "UTF-8")
(def ^{:dynamic true} *validation* false)
(def ^{:dynamic true :tag javax.xml.xpath.XPathFactory} *xpath-factory* (org.apache.xpath.jaxp.XPathFactoryImpl.))
(def ^{:dynamic true :tag javax.xml.xpath.XPath} *xpath-compiler* (.newXPath *xpath-factory*))

(s/defn merge-dynvars-with-opts :- lib/Options [opts :- lib/Options]
  (merge 
   {:namespace-aware  *namespace-aware*
    :default-encoding *default-encoding*
    :validation       *validation*}
   opts))

(s/defn make-dom-factory :- DocumentBuilderFactory [opts :- lib/Options]
  (lib/make-dom-factory (merge-dynvars-with-opts opts)))

(defn input-stream->dom [^java.io.InputStream istr & [opts]]
  (lib/input-stream->dom istr (merge-dynvars-with-opts opts)))

(defn xml-bytes->dom [bytes & [opts]]
  (lib/xml-bytes->dom bytes (merge-dynvars-with-opts opts)))

(defn xml->doc [thing & [opts]]
  (lib/xml->doc thing (merge-dynvars-with-opts opts)))

(def attrs lib/attrs)
(def text lib/text)
(def node-name lib/node-name)
(def node->map lib/node->map)

(defn xp:compile [xpexpr]
  (lib/xp:compile *xpath-compiler* xpexpr))

(defn $x [xp xml-thing & [opts]]
  (lib/$x *xpath-compiler* xp xml-thing (merge-dynvars-with-opts opts)))

(def summarize lib/summarize)

(defn $x:tag* [xp xml & [opts]]
  (map :tag (lib/$x *xpath-compiler* xp xml (merge-dynvars-with-opts opts))))

(defn $x:tag? [xp xml & [opts]]
  (lib/$x:tag? *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:tag+
  "Perform an xpath search, resulting in one or more nodes.  Return only the tag name."
  [xp xml & [opts]]
  (lib/$x:tag+ *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:tag [xp xml & [opts]]
  (lib/$x:tag *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:text*
  "Perform an xpath search, resulting in zero or more nodes.  Return only each the node's text."
  [xp xml & [opts]]
  (map :text (lib/$x *xpath-compiler* xp xml (merge-dynvars-with-opts opts))))

(defn $x:text?
  "Perform an xpath search, resulting in zero or one node.  Return only the node's text."
  [xp xml & [opts]]
  (lib/$x:text? *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:text+
  "Perform an xpath search, resulting in one or more nodes.  Return only each the node's text."
  [xp xml & [opts]]
  (lib/$x:text+ *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:text
  "Perform an xpath search, resulting in one and only one node.  Return only the node's text."
  [xp xml & [opts]]
  (lib/$x:text *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:attrs*
  "Perform an xpath search, resulting in zero or more nodes.  When an attr-name is passed, return only each the node's attrs."
  ([xp xml]
   (lib/$x:attrs* *xpath-compiler* xp xml (merge-dynvars-with-opts nil)))
  ([xp xml attr-name]
   (lib/$x:attrs* *xpath-compiler* xp xml attr-name (merge-dynvars-with-opts nil))))

(defn $x:attrs?
  "Perform an xpath search, resulting in zero or one node.  Return only the node's attrs."
  [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:attrs+
  "Perform an xpath search, resulting in one or more nodes.  Return only each the node's attrs."
  [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))


(defn $x:attrs
  "Perform an xpath search, resulting in one and only one node.  Return only the node's attrs."
  [xp xml]
  (let [res (map attrs (map :node ($x xp xml)))]
    (if (not (= 1 (count res)))
      (throwf "Error, more (or less) than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:node*
  "Perform an xpath search, resulting in zero or more nodes.  Returns the nodes."
  [xp xml]
  (map :node ($x xp xml)))

(defn $x:node?
  "Perform an xpath search, resulting in zero or one node.  Returns the node."
  [xp xml]
  (let [res ($x:node* xp xml)]
    (if (next res)
      (throwf "Error, more than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    (first res)))

(defn $x:node+
  "Perform an xpath search, resulting in one or more nodes.  Returns the nodes."
  [xp xml]
  (let [res ($x:node* xp xml)]
    (if (< (count res) 1)
      (throwf "Error, less than 1 result (%d) from xml(%s) for xpath(%s)"
              (count res)
              (summarize xml 10)
              xp))
    res))

(defn $x:node
  "Perform an xpath search, resulting in one and only one node.  Returns the node."
  [xp xml]
  (let [res ($x:node* xp xml)]
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
  [xml]
  (xmlnsmap-from-node (first ($x:node* "//*" xml))))

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
     (lib/node-list->seq (.getChildNodes node)))))

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


(defn with-namespace-awareness*
  "Wrap the call to f with a binding setting *namespace-aware* to true and a XPath factory constructed with namespace awareness."
  [f]
  (binding [*namespace-aware* true
            *xpath-compiler*  (.newXPath *xpath-factory*)]
    (f)))

(defmacro with-namespace-awareness
  "Helper macro for with-namespace-awareness*."
  [& body]
  `(with-namespace-awareness* (fn [] ~@body)))

(defn set-namespace-context!
  "Set the namespace context on the XPath Compiler in scope.  See with-namespace-awareness* and with-namespace-awareness."
  [context-map]
  (.setNamespaceContext *xpath-compiler* (nscontext context-map)))

(defn with-namespace-context*
  "Call f in a context with namespace awareness enabled, and the given context map."
  [context-map f]
  (binding [*namespace-aware* true
            *xpath-compiler*  (.newXPath *xpath-factory*)]
    (.setNamespaceContext *xpath-compiler* (nscontext context-map))
    (f)))

(defmacro with-namespace-context
  "Helper macro for with-namespace-context."
  [context-map & body]
  `(with-namespace-context* ~context-map (fn [] ~@body)))

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

(defonce xpath-functions (atom {}))

(def xpath-function-resolver
  (reify
    javax.xml.xpath.XPathFunctionResolver
    (resolveFunction [this fname arity]
      (get
       (->>
        @xpath-functions
        (filter
         (fn [[qname arities]]
           (= qname fname)))
        first
        second)
       arity))))

(defn register-xpath-function [qualified-name arity f]
  (let [qname (->qualified-name qualified-name)
        xpfn  (reify javax.xml.xpath.XPathFunction
                (evaluate [this args]
                  (f this args)))]
    (swap! xpath-functions
           assoc-in
           [qname arity]
           xpfn)
    (when (nil? (.getXPathFunctionResolver *xpath-compiler*))
      (.setXPathFunctionResolver *xpath-compiler* xpath-function-resolver))
    qname))

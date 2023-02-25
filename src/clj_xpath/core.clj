(ns clj-xpath.core
  (:require
   [clj-xpath.lib  :as lib]
   [schema.core    :as s])
  (:import
   [javax.xml.parsers           DocumentBuilderFactory])
  (:gen-class))

(def ^{:dynamic true} *namespace-aware*  false)
(def ^{:dynamic true :tag String} *default-encoding* "UTF-8")
(def ^{:dynamic true} *validation* false)
(def ^{:dynamic true :tag javax.xml.xpath.XPathFactory} *xpath-factory* (javax.xml.xpath.XPathFactory/newInstance))
(def ^{:dynamic true :tag javax.xml.xpath.XPath} *xpath-compiler* (.newXPath *xpath-factory*))

(def dom-node-list->seq lib/dom-node-list->seq)

(s/defn merge-dynvars-with-opts :- lib/Options [opts :- (s/maybe lib/Options)]
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
  [xp xml & [opts]]
  (lib/$x:attrs? *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:attrs+
  "Perform an xpath search, resulting in one or more nodes.  Return only each the node's attrs."
  [xp xml & [opts]]
  (lib/$x:attrs+ *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

;; here
(defn $x:attrs
  "Perform an xpath search, resulting in one and only one node.  Return only the node's attrs."
  [xp xml & [opts]]
  (lib/$x:attrs *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:node*
  "Perform an xpath search, resulting in zero or more nodes.  Returns the nodes."
  [xp xml & [opts]]
  (lib/$x:node* *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:node?
  "Perform an xpath search, resulting in zero or one node.  Returns the node."
  [xp xml & opts]
  (lib/$x:node? *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:node+
  "Perform an xpath search, resulting in one or more nodes.  Returns the nodes."
  [xp xml & [opts]]
  (lib/$x:node+ *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(defn $x:node
  "Perform an xpath search, resulting in one and only one node.  Returns the node."
  [xp xml & [opts]]
  (lib/$x:node *xpath-compiler* xp xml (merge-dynvars-with-opts opts)))

(def format-tag lib/format-tag)
(def tag lib/tag)
(def xmlnsmap-from-node lib/xmlnsmap-from-node)

(defn xmlnsmap-from-root-node [xml & [opts]]
  (lib/xmlnsmap-from-root-node *xpath-compiler* xml opts))

(def xmlnsmap-from-document lib/xmlnsmap-from-document)
(def nscontext lib/nscontext)

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

(def abs-path lib/abs-path)

(def node->xml lib/node->xml)

(def ->qualified-name lib/->qualified-name)

(defn register-xpath-function [qualified-name arity f]
  (lib/register-xpath-function *xpath-compiler* qualified-name arity f))

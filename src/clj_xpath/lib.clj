(ns clj-xpath.lib
  (:require
   [clojure.string :as str-utils]
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
   [javax.xml.namespace QName]))

(def disallow-doctype-decl       "http://apache.org/xml/features/disallow-doctype-decl")
(def load-external-dtd           "http://apache.org/xml/features/nonvalidating/load-external-dtd")
(def external-general-entities   "http://xml.org/sax/features/external-general-entities")
(def external-parameter-entities "http://xml.org/sax/features/external-parameter-entities")

(def Options
  {(s/optional-key :validation)                   s/Bool
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


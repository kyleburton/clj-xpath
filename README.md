[![https://clojars.org/com.github.kyleburton/clj-xpath](http://clojars.org/com.github.kyleburton/clj-xpath/latest-version.svg)](https://clojars.org/com.github.kyleburton/clj-xpath)

# Overview

[clj-xpath](http://kyleburton.github.io/clj-xpath/site/) is a library that makes it easier to work with XPath from Clojure.

## [Documentation](http://kyleburton.github.io/clj-xpath/site/)

Documentation is available on [GH Pages for clj-xpath](http://kyleburton.github.io/clj-xpath/site/)

## Description

Simplified XPath Library for Clojure.  XML Parsers and an XPath implementation now comes with Java 6, though using the api directly can be verbose and confusing.  This library provides a thin layer around basic parsing and XPath interaction for common use cases.  I have personally found the ability to interactively tweak my xpath expressions to be a great productivity boost - even using this library only for that has helped me in my learning of and using xpath.  I hope you find it useful and would love to hear your feedback and suggestions.


## Usage

The main functions in the library are `$x` and those named with a prefix of `$x:` (eg: `$x:text`).  The rationale for choosing `$x` as a name was based on the FireBug xpath function and it being a short and uncommon name.  These xpath functions all take the xpath expression to be executed and an XML document.  They attempt to be flexible with respect to the form of the XML document may represent.  If it is a string it is treated as XML, if a byte array it is used directly, if already a Document or Node (from org.w3c.dom) they are used as-is.

There are four forms of most of the core functions, each with a different suffix borrowed from regular expression syntax: none, &#42; + and ?.  For example, `$x:tag` has the following four implementations:

* `($x:tag  "//books")`: '1 and only 1', returns the single node found, throwing an exception if none or more than 1 are found.
* `($x:tag? "//books")`: '0 or 1', returns the single node found or nil, throwing an exception if more than 1 are found.
* `($x:tag* "//book")`: '0 or more', returns a sequence of the nodes found (which may be empty)
* `($x:tag+ "//book")`: '1 or more'returns a sequence of the nodes found, throwing an exception if none are found

If you are interested in the entire node found by the XPath expressions and not just in particular aspects the node (tag, attributes, text content), `$x` function returns a map containing the XML tag (as a symbol), dom Node, the text (as a string), and a map of the attributes where the keys have been converted into keywords and the values remain Strings.

```clojure
(ns example
  (use [clj-xpath.core :only [$x $x:tag $x:text $x:attrs $x:attrs* $x:node]]))

(def *some-xml*
     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<books>
  <book title=\"Some Guide To XML\">
    <author>
      <name>P.T. Xarnum</name>
      <email>pt@x.m.l</email>
    </author>
    <description>
      Simply the most comprehensive XML Book on the market today.
    </description>
  </book>
  <book title=\"Some Guide To Functional Programming\">
    <author>
      <name>S. Hawking</name>
      <email>universe@cambridge.ed.u</email>
    </author>
    <description>
      This book is too smart for you, try 'Head first Quantum Mechanics for Dummies' instead.
    </description>
  </book>
</books>")


;; get the top level tag:
(prn ($x:tag "/*" *some-xml*))
;; :books

;; find all :book nodes, pull the title from the attributes:
(prn (map #(-> % :attrs :title) ($x "//book" *some-xml*)))
;; ("Some Guide To XML" "Some Guide To Functional Programming")

;; same result using the $x:attrs* function:
(prn ($x:attrs* "//book" *some-xml* :title))
;; ("Some Guide To XML" "Some Guide To Functional Programming")

;; first select the :book element who's title has 'XML' in it
;; from that node, get and print the author's name (text content):
(prn ($x:text "./author/name"
              ($x:node "//book[contains(@title,'XML')]" *some-xml*)))
;; "P.T. Xarnum"
```

## Parsing and XPath Compilation

The `$x` and related functions support Strings, and in many cases, other convenient types for these arguments.  In all cases where it expects an XML Document it can be given a String, a byte array or a Document.  Where an xpath expression is expected it will take either a String or a pre-compiled XPathExpression.  The act of parsing an XML document or compiling an xpath expression is an expensive activity.  With this flexibility, clj-xpath supports the convenience of in-line usage (with String data), as well as pre-parsed and pre-compiled instances for better performance.

```clojure
  (let [expr (xp:compile "/*")
        doc  (xml->doc "<authors><author><name>P.T. Xarnum</name></author></authors>")]
    ($x:tag expr doc))
```

### `(xml->doc doc) => Document`

This function takes xml that is of one of the following types and returns a Document:  String, byte array or org.w3c.dom.Document.  In cases of repeated usage of the document (eg: executing multiple xpath expressions against the same document) this will improve performance.

### `(xp:compile xpexpr) => javax.xml.xpath.XPathExpression`

Pre-compiles the xpath expression.  In cases of repeated execution of the xpath expression this will improve performance.


## Validation

Validation now off by default.  Validation is controlled by optional parameters passed to `xml-bytes->dom`, or by overriding the atom `*validation*` to false:

```clojure
  (ns your.namespace
    (:use clj-xpath.core))

  (binding [*validation* false]
    ($x:text "/this" "<this>foo</this>"))
```


## XPath and XML Namespaces

To use the xpath library with an XML document that utilizes XML namespaces, you can make use of the `with-namespace-context` macro providing a map of aliases to the xmlns URL:

```clojure
  (def xml (slurp "fixtures/namespace1.xml"))
  (with-namespace-context {"atom" "http://www.w3.org/2005/Atom"}
    ($x:text "//atom:title" xml-doc))
  ;; => BookingCollection
```

There is also a utility function that can pull the namespace declarations from the root node of your XML document:

```clojure
  (def xml (slurp "fixtures/namespace1.xml"))
  (with-namespace-context (xmlnsmap-from-root-node xml-doc)
    ($x:text "//atom:title" xml-doc))
  ;; => BookingCollection
```

These two examples assume the following XML document:

```xml
<atom:feed xml:base="http://nplhost:8042/sap/opu/sdata/IWFND/RMTSAMPLEFLIGHT/"
                  xmlns:atom="http://www.w3.org/2005/Atom"
                  xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices"
                  xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"
                  xmlns:sap="http://www.sap.com/Protocols/SAPData">

<atom:title>BookingCollection</atom:title>
<atom:updated>2012-03-19T20:27:30Z</atom:updated>

<atom:entry>
<atom:author/>
</atom:entry>

<atom:entry>
<atom:author/>
<atom:content type="application/xml"/>
</atom:entry>

</atom:feed>
```

## Changes

##### Version 1.4.13 : Sat Feb 25 12:37:09 2023 -0800

* fixes #36 | Remove Apache Xalan dependency, no longer necessary for recent Java versions (from 7 onward)
* drop support for clojure 1.6 and below
* fix all flycheck warnings and errors
* add Bakefile for local dev

##### Version 1.4.12 : Sun Dec 12 14:08:04 2021 -0800

* fix log4j vulnerability

##### Version 1.4.11 : Sun Jan  1 09:56:21 PST 2017

* Merged [#34 implement abs-path for attribute elements](https://github.com/kyleburton/clj-xpath/pull/34)

##### Version 1.4.3  : Sat Sep 14 10:11:56 EDT 2013

* Compatibility with Clojure 1.2, 1.3, 1.4, 1.5 and 1.6-SNAPSHOT

##### Version 1.4.1 : Sat Sep  7 21:10:16 EDT 2013

* Support leiningen 2
* create profiles for clojure 1.2 through 1.6
* resolve reflection warnings: NB: two remain for clojure 1.3

##### Version 1.4.1 : Sat Feb 16 12:15:26 EST 2013

Changed project group from org.clojars.kyleburton to com.github.kyleburton.

##### Version 1.4.0 : Tue Dec 18 15:10:19 EST 2012

* `:children` lazy seq of a Node's children added by mtnygard
* idiomatic use of next

## Hacking

```console
# to run an nrepl that you can connect to with `M-x cider-connect-clj`:

# if you have bake installed
$ bake nrepl

# with leiningen
$ lein with-profile dev run -m clj-xpath.nrepl
```

## Deploying

* https://github.com/clojars/clojars-web/wiki/Pushing
* https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#authentication
* https://github.com/clojars/clojars-web/wiki/Deploy-Tokens

Create `~/.lein/credentials.clj`

```clojure
{#"https://repo.clojars.org"
 {:username "<<clojars-user-name>>" :password "CLOJARS_<<deploy-token>>"}}
```

Your clojars deploy tokens are managed at https://clojars.org/tokens

Encrypt it:
```console
$ gpg --default-recipient-self -e ~/.lein/credentials.clj > ~/.lein/credentials.clj.gpg
```

Verify

```console
$ gpg --decrypt ~/.lein/credentials.clj.gpg
```

Deploy

```console
$ lein deploy clojars
$ lein release
```

## Authors

* Kyle Burton <kyle.burton@gmail.com>
* Trotter Cashion <cashion@gmail.com>
* Michael Nygard <mtnygard@gmail.com>

(ns example
  (use [com.github.kyleburton.clj-xpath :only [$x $x:tag $x:text $x:attrs $x:attrs* $x:node]]))


(def *some-xml*
     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<books>
  <book title=\"Some Guide To XML\">
    <author>
      <name>P.T. Xarnum</name>
      <email>pt@x.m.l</email>
    </author>
    <description>
      Simply the most comprehensive XML Book on the marktet today.
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


(comment

;; This is an example REPL session to show more simply how to use it:
(use 'com.github.kyleburton.clj-xpath)
(use 'clojure.contrib.duck-streams)
(def xdox (slurp* "http://github.com/kyleburton/clj-xpath/raw/master/pom.xml"))
;; or:
(def xdoc (xml->doc (slurp* "/Users/kburton/personal/projects/clj-xpath/pom.xml")))
($x:tag "/*" xdoc)
($x:text* "/project/developers/developer/name" xdoc)
(doseq [node ($x "/project/dependencies/dependency" xdoc)]
  (prn (format "%s %s %s"
               ($x:text "./groupId"    node)
               ($x:text "./artifactId" node)
               ($x:text "./version"    node))))


  )
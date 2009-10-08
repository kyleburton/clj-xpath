set -e
set -x

test -d clojure || git clone git://github.com/richhickey/clojure.git
cd clojure
ant clean jar
exit 0
mvn install:install-file \
  -Dfile=clojure-1.0.0.jar \
  -DgroupId=org.clojure \
  -DartifactId=clojure \
  -Dversion=1.0.0 \
  -Dpackaging=jar
cd ..

test -d clojure-contrib || git clone git://github.com/richhickey/clojure-contrib.git
cd clojure-contrib
ant clean jar
mvn install:install-file \
  -Dfile=clojure-contrib.jar \
  -DgroupId=org.clojure \
  -DartifactId=clojure-contrib \
  -Dversion=1.0.0 \
  -Dpackaging=jar
cd ..

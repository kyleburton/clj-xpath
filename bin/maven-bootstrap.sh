set -e
set -x

test -d clojure-contrib || git clone git://github.com/richhickey/clojure-contrib.git
cd clojure-contrib
git checkout origin/clojure-1.0-compatible
ant clean jar
mvn install:install-file \
  -Dfile=clojure-contrib.jar \
  -DgroupId=org.clojure \
  -DartifactId=clojure-contrib \
  -Dversion=1.0.0 \
  -Dpackaging=jar
cd ..

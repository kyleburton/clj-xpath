set -e 
set -x
lein deps
lein jar
lein pom

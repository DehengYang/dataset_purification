
#mvn install:install-file -Dfile=/home/apr/apr_tools/PatchParser/Parser/target/Parser-0.0.1-SNAPSHOT-jar-with-dependencies.jar -DgroupId=edu.lu.uni.serval -DartifactId=Parser -Dversion=0.0.1-SNAPSHOT-jar-with-dependencies -Dpackaging=jar

#mvn dependency:copy-dependencies -f ../pom.xml

# first dependency: PatchParser
git clone git@gitee.com:dalewushuang/PatchParser.git
cd PatchParser
./run.sh

# second dependency: javaparser
git clone git@gitee.com:dalewushuang/javaparser.git
cd javaparser
git checkout dale-parser
#git checkout 6a2d46c 
mvn clean install -DskipTests

# third dependency: gzoltar v1.7.2
git clone git@gitee.com:dalewushuang/gzoltar.git
cd gzoltar
git checkout v1.7.2
mvn package 
cd -

[ ! -d libs/ ]  & mkdir libs/
cp javaparser/javaparser-symbol-solver-core/target/javaparser-symbol-solver-core-3.15.14.jar libs/
cp PatchParser/Parser/target/Parser-0.0.1-SNAPSHOT-jar-with-dependencies.jar libs/
cp gzoltar/com.gzoltar.cli/target/com.gzoltar.cli-1.7.2-jar-with-dependencies.jar  libs/
cp gzoltar/com.gzoltar.agent.rt/target/com.gzoltar.agent.rt-1.7.2-all.jar  libs/

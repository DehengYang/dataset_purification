# first dependency: PatchParser
mvn install:install-file -Dfile=./libs/Parser-0.0.1-SNAPSHOT-jar-with-dependencies.jar -DgroupId=edu.lu.uni.serval -DartifactId=Parser -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar

# second dependency: javaparser
git clone git@gitee.com:dalewushuang/javaparser.git
cd javaparser
git checkout dale-parser
#git checkout 6a2d46c 
mvn clean install -DskipTests
if [ ! -d versions ];then
	echo "now mkdir versions/"
	mkdir versions
fi

cd ~/env
java -version
./change-jdk-version.sh 8
cd -
mvn clean package -DskipTests

cp target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar versions/
cd -
./change-jdk-version.sh 8

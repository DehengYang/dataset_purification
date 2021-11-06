BASE="$(cd $(dirname $0); pwd)"
cd $BASE

purifyTargetDir="$BASE/../../../../target/"
purifyJar="$BASE/../../../../target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

[ ! -f $purifyJar ] && echo "$purifyJar does not exist" && exit
#echo "purifyJar: $purifyJar"
#exit

#/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../repair_tools/purify.jar
#purifyJar=/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar
#/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar

export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="/home/apr/env/jdk1.8.0_202/bin/:$PATH";
export JAVA_HOME="/home/apr/env/jdk1.8.0_202/bin/";



## need to revise!!
cd /mnt/purify/repairDir/Purify_Defects4J_Chart_1/;

#time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y  -Xmx4g -Xms1g -cp $purifyJar apr.purify.Main \
#-Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y  
#time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
#time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
    -dataset Defects4J \
    -projDir /mnt/purify/repairDir/Purify_Defects4J_Chart_1 \
    -srcJavaDir /mnt/purify/repairDir/Purify_Defects4J_Chart_1/source/ \
	-binJavaDir /mnt/purify/repairDir/Purify_Defects4J_Chart_1/build/ \
	-binTestDir /mnt/purify/repairDir/Purify_Defects4J_Chart_1/build-tests/ \
	-testExecPath /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/junit_external/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    -jvmPath /home/apr/env/jdk1.7.0_80/bin/ \
	-failedTestsStr org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660: \
	-gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 \
    -logDir /mnt/purify/buggylocs/Defects4J/Defects4J_Chart_1 \
    -patchDiffPath /mnt/purify/buggylocs/Defects4J/Defects4J_Chart_1/patchDiff.txt \
	-dependencies /mnt/purify/repairDir/Purify_Defects4J_Chart_1/build/:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/build-tests/:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/build/:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/build-tests/:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/lib/junit.jar:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/lib/iText-2.1.4.jar:/mnt/purify/repairDir/Purify_Defects4J_Chart_1/lib/servlet.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/lib/junit-4.11.jar

BASE="$(cd $(dirname $0); pwd)"
cd $BASE

purifyTargetDir="$BASE/../../../../../target/"
purifyJar="$purifyTargetDir/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

[ ! -f $purifyJar ] && echo "$purifyJar does not exist" && exit
#echo "purifyJar: $purifyJar"
#exit

#/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../repair_tools/purify.jar
#purifyJar=/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar
#/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar

#cd /mnt/purify/repairDir/Purify_Defects4J_Chart_2/;
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="/home/apr/env/jdk1.8.0_202/bin/:$PATH";
export JAVA_HOME="/home/apr/env/jdk1.8.0_202/bin/";



## need to revise!!
projDir=Purify_Defects4J_Lang_62
cd /mnt/purify/repairDir/${projDir}/;
#time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
#time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
cmd1="time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
cmd2="time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
$cmd1  \
    -dataset Defects4J \
    -projDir /mnt/purify/repairDir/Purify_Defects4J_Lang_62 \
    -srcJavaDir /mnt/purify/repairDir/Purify_Defects4J_Lang_62/src/java/ \
	-binJavaDir /mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/classes/ \
	-binTestDir /mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/tests/ \
	-testExecPath /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/junit_external/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    -jvmPath /home/apr/env/jdk1.7.0_80/bin/ \
	-failedTestsStr org.apache.commons.lang.EntitiesTest#testNumberOverflow: \
	-gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 \
    -logDir /mnt/purify/buggylocs/Defects4J/Defects4J_Lang_62 \
    -patchDiffPath /mnt/purify/buggylocs/Defects4J/Defects4J_Lang_62/patchDiff.txt \
	-dependencies /mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/classes/:/mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/tests/:/mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/classes/:/mnt/purify/repairDir/Purify_Defects4J_Lang_62/target/tests/:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Lang/lib/asm.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Lang/lib/easymock.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Lang/lib/commons-io.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Lang/lib/cglib.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/lib/junit-4.11.jar

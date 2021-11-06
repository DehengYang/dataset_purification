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

#cd /mnt/purify/repairDir/Purify_Defects4J_Chart_2/;
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="/home/apr/env/jdk1.8.0_202/bin/:$PATH";
export JAVA_HOME="/home/apr/env/jdk1.8.0_202/bin/";



## need to revise!!
projDir=Purify_Defects4J_Time_3
cd /mnt/purify/repairDir/${projDir}/;
#time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
#time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
cmd1="time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
cmd2="time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
$cmd1  \
    -dataset Defects4J \
    -projDir /mnt/purify/repairDir/Purify_Defects4J_Time_3 \
    -srcJavaDir /mnt/purify/repairDir/Purify_Defects4J_Time_3/src/main/java/ \
	-binJavaDir /mnt/purify/repairDir/Purify_Defects4J_Time_3/target/classes/ \
	-binTestDir /mnt/purify/repairDir/Purify_Defects4J_Time_3/target/test-classes/ \
	-testExecPath /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/junit_external/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    -jvmPath /home/apr/env/jdk1.7.0_80/bin/ \
	-failedTestsStr org.joda.time.TestMutableDateTime_Adds#testAddMonths_int_dstOverlapWinter_addZero:org.joda.time.TestMutableDateTime_Adds#testAdd_DurationFieldType_int_dstOverlapWinter_addZero:org.joda.time.TestMutableDateTime_Adds#testAddYears_int_dstOverlapWinter_addZero:org.joda.time.TestMutableDateTime_Adds#testAddWeeks_int_dstOverlapWinter_addZero:org.joda.time.TestMutableDateTime_Adds#testAddDays_int_dstOverlapWinter_addZero: \
	-gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 \
    -logDir /mnt/purify/buggylocs/Defects4J/Defects4J_Time_3 \
    -patchDiffPath /mnt/purify/buggylocs/Defects4J/Defects4J_Time_3/patchDiff.txt \
	-dependencies /mnt/purify/repairDir/Purify_Defects4J_Time_3/target/classes/:/mnt/purify/repairDir/Purify_Defects4J_Time_3/target/test-classes/:/mnt/purify/repairDir/Purify_Defects4J_Time_3/target/classes/:/mnt/purify/repairDir/Purify_Defects4J_Time_3/target/test-classes/:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Time/lib/joda-convert-1.2.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Time/lib/junit-3.8.2.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Time/lib/junit/junit/3.8.2/junit-3.8.2.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Time/lib/org/joda/joda-convert/1.2/joda-convert-1.2.jar

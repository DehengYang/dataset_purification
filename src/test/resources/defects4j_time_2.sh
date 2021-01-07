#!/bin/bash

BASE="$(cd $(dirname $0); pwd)"
cd $BASE
purifyTargetDir="$BASE/../../../target/"
purifyJar="$purifyTargetDir/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

[ ! -f $purifyJar ] && echo "$purifyJar does not exist" && exit

export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="/home/apr/env/jdk1.8.0_202/bin/:$PATH";
export JAVA_HOME="/home/apr/env/jdk1.8.0_202/bin/";

projDir="$BASE/../../../example/Defects4J_Time_2"
cd $projDir;
cmd="time java -Xmx4g -Xms1g -cp /home/apr/purify/for-saner-open-source-repo/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar apr.purify.Main "
$cmd  \
    -dataset Defects4J \
    -projDir $projDir \
    -srcJavaDir $projDir/src/main/java/ \
    -binJavaDir $projDir/target/classes/ \
    -binTestDir $projDir/target/test-classes/ \
    -jvmPath /home/apr/env/jdk1.7.0_80/bin/ \
    -failedTestsStr org.joda.time.TestPartial_Basics#testWith_baseAndArgHaveNoRange: \
    -gzoltarDir $purifyTargetDir/../ \
    -logDir ./output/ \
    -patchDiffPath $projDir/../Time_2_files/patchDiff.txt \
    -dependencies $projDir/target/classes/:$projDir/target/test-classes/:$projDir/target/classes/:$projDir/../Time_2_files/joda-convert-1.2.jar:$projDir/../Time_2_files/junit-3.8.2.jar

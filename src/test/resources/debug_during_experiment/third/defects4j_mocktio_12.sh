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
projDir=Purify_Defects4J_Mockito_12
cd /mnt/purify/repairDir/${projDir}/;
#time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
#time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main \
cmd1="time java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y   -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
cmd2="time java -Xmx4g -Xms1g -cp ${purifyTargetDir}/classes:$purifyTargetDir/dependency/* apr.purify.Main "
$cmd1  \
    -dataset Defects4J \
    -projDir /mnt/purify/repairDir/Purify_Defects4J_Mockito_12 \
    -srcJavaDir /mnt/purify/repairDir/Purify_Defects4J_Mockito_12/src \
	-binJavaDir /mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/classes \
	-binTestDir /mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/test-classes \
	-testExecPath /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/junit_external/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    -jvmPath /home/apr/env/jdk1.7.0_80/bin/ \
	-failedTestsStr org.mockitousage.annotation.CaptorAnnotationBasicTest#shouldUseCaptorInOrdinaryWay:org.mockitousage.annotation.CaptorAnnotationBasicTest#shouldCaptureGenericList:org.mockitousage.annotation.CaptorAnnotationTest#shouldLookForAnnotatedCaptorsInSuperClasses:org.mockitousage.annotation.CaptorAnnotationTest#shouldScreamWhenInitializingCaptorsForNullClass:org.mockito.internal.util.reflection.GenericMasterTest#shouldDealWithNestedGenerics:org.mockitousage.annotation.CaptorAnnotationTest#shouldScreamWhenMoreThanOneMockitoAnnotaton:org.mockitousage.annotation.CaptorAnnotationBasicTest#shouldUseAnnotatedCaptor:org.mockitousage.annotation.CaptorAnnotationTest#shouldScreamWhenWrongTypeForCaptor:org.mockitousage.annotation.CaptorAnnotationBasicTest#shouldUseGenericlessAnnotatedCaptor:org.mockitousage.annotation.CaptorAnnotationTest#testNormalUsage: \
	-gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 \
    -logDir /mnt/purify/buggylocs/Defects4J/Defects4J_Mockito_12 \
    -patchDiffPath /mnt/purify/buggylocs/Defects4J/Defects4J_Mockito_12/patchDiff.txt \
	-dependencies /mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/classes:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/test-classes:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/classes:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/target/test-classes:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/repackaged/cglib-and-asm-1.0.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/run/com.springsource.org.objenesis-1.0.0.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/run/com.springsource.org.hamcrest.core-1.1.0.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/jarjar-1.0.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/pmd-4.1.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/jaxen-1.1.1.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/maven-ant-tasks-2.0.9.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/sorcerer.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/asm-3.1.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/build/bnd-0.0.313.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/compile/com.springsource.org.junit-4.5.0.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/test/fest-util-1.1.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/test/powermock-reflect-1.2.5.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/test/fest-assert-1.1.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/sources/cglib-and-asm-1.0-sources.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/sources/com.springsource.org.objenesis-1.0.0-sources.jar:/mnt/purify/repairDir/Purify_Defects4J_Mockito_12/lib/sources/com.springsource.org.hamcrest.core-1.1.0-sources.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/fest-assert-1.3.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/objenesis-2.2.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/fest-util-1.1.4.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/cglib-and-asm-1.0.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/cobertura-2.0.3.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/hamcrest-core-1.1.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/asm-all-5.0.4.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/assertj-core-2.1.0.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/hamcrest-all-1.3.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/powermock-reflect-1.2.5.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/Mockito/lib/objenesis-2.1.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/lib/junit-4.11.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/lib/cobertura-2.0.3.jar

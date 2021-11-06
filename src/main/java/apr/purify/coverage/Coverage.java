/**
 * 
 */
package apr.purify.coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.Configuration;
import apr.purify.Main;
import apr.purify.location.SuspiciousLocation;
import apr.purify.location.TestCaseResult;
import apr.purify.utils.ClassFinder;
import apr.purify.utils.CmdUtil;
import apr.purify.utils.CoverageUtil;
import apr.purify.utils.FileUtil;

/**
 * This is for obtaining coverage of failing test cases using GZoltar that implements Jacoco
 * @author apr
 * @version Oct 4, 2020
 *
 */
public class Coverage {
	final static Logger logger = LoggerFactory.getLogger(Coverage.class); 
	
	private static Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = new HashMap<>();
	private static Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests =  new HashMap<>();
	
	public Coverage(){
		
	}
	
	/**
	 * @Description
	 * refer to: <a href="https://github.com/GZoltar/gzoltar/issues/22">https://github.com/GZoltar/gzoltar/issues/22</a> 
	 * @author apr
	 * @version Oct 4, 2020
	 */
	public void getCoverage(){
		// 1) define the gzoltar jars path
		String gzoltarCliJar = FileUtil.gzoltarDir + "/com.gzoltar.cli-1.7.2-jar-with-dependencies.jar";
		String gzoltarAgentJar = FileUtil.gzoltarDir + "/com.gzoltar.agent.rt-1.7.2-all.jar";
		
		String classpath = FileUtil.dependencies;
		String testClassDir = FileUtil.binTestDir;
		String srcClassDir = FileUtil.binJavaDir;
		
		// 2) do by myself: find classes firstly use my own methods
		ClassFinder cf = new ClassFinder();
		Set<String> testClasses = cf.getTestClasses(FileUtil.binTestDir, FileUtil.binJavaDir, FileUtil.depsList);
		Set<String> srcClassesFromSrcDir = cf.getJavaClassesOldVersion(FileUtil.srcJavaDir, "java");
		Set<String> srcClasses = cf.getJavaClasses(FileUtil.binJavaDir, FileUtil.depsList);
		FileUtil.writeLinesToFile(FileUtil.toolDir + "/srcClasses.txt", srcClasses);
		FileUtil.writeLinesToFile(FileUtil.toolDir + "/srcClassesFromSrcDir.txt", srcClassesFromSrcDir);
		FileUtil.writeLinesToFile(FileUtil.toolDir + "/testClasses.txt", testClasses);
		String srcCommonName = getCommonName(srcClasses);
		String testCommonName = getCommonName(testClasses);
		FileUtil.writeToFileWithFormat("testClasses size: %s, srcClassesFromSrcDir size: %s, srcClasses size: %s", testClasses.size(),
				srcClassesFromSrcDir.size(), srcClasses.size());
		FileUtil.writeToFileWithFormat("srcCommonName: %s, testCommonName: %s", srcCommonName, testCommonName);
		
		// 3) list tests
		/*
		 * java -cp $src_classes_dir:$test_classes_dir:$classpath:$GZOLTAR_CLI_JAR \
	    com.gzoltar.cli.Main listTestMethods \
	      "$test_classes_dir" \
	      --outputFile "$list_of_tests_to_run_file" \
	      --includes "org.openmrs.module.webservices.rest.*"
		 */
		String testsPath = FileUtil.toolDir + "/all_tests.txt";
		String cmdGetTests = String.format(
				"export PATH=%s/bin:$PATH;" +
				"export JAVA_HOME=%s;" + 
				"java -cp %s:%s:%s:%s " +
				"com.gzoltar.cli.Main listTestMethods " +
				"%s " +
				"--outputFile %s " +
				"--includes %s;", 
				Configuration.JAVA7_HOME,
				Configuration.JAVA7_HOME,
				srcClassDir, testClassDir, classpath, gzoltarCliJar,
				testClassDir,
				testsPath,
				testCommonName);
		String result = CmdUtil.runCmd(cmdGetTests);
		logger.debug("cmdGetTests: {}\nresult: {}", cmdGetTests, result);
		
		for(String fTest:FileUtil.oriFailedTests){ 
			FileUtil.writeToFileWithFormat("failingTestCase: %s", fTest); // log
		}
		List<String> allTestCases = FileUtil.readFile(testsPath);
		List<String> testToExecute = new ArrayList<>();
		for (String tcase : allTestCases){
			if (belongTo(tcase, FileUtil.oriFailedTests)){
				testToExecute.add(tcase);
				FileUtil.writeToFileWithFormat("testToExecute in gzoltar: %s", tcase);
			}
		}
		FileUtil.writeToFileWithFormat("testToExecute size: %s", testToExecute.size());
		String testToExecPath = FileUtil.toolDir + "/test_to_exec.txt";
		FileUtil.writeLinesToFile(testToExecPath, testToExecute);
		
		// 4) collect coverage
		/*
		 * java -XX:MaxPermSize=2048M -javaagent:$GZOLTAR_AGENT_JAR=destfile=$ser_file,buildlocation=$src_classes_dir,includes=$classes_to_debug,excludes="",inclnolocationclasses=false,output="FILE" \
        -cp $src_classes_dir:$test_classes_dir:$classpath:$GZOLTAR_CLI_JAR \
        com.gzoltar.cli.Main runTestMethods \
          --testMethods "$list_of_tests_to_run_file" \
          --collectCoverage
		 */
		String ser_file = FileUtil.toolDir + "/gzoltar.ser";
		String cmdRmSerFile = String.format("[ -f %s ] && rm %s && echo \"rm %s\"", ser_file, ser_file, ser_file);
		result = CmdUtil.runCmd(cmdRmSerFile);
		logger.debug("cmdRmSerFile: {}\nresult: {}", cmdRmSerFile, result);
		String cmdGetCoverage = String.format(
				"export PATH=%s/bin:$PATH;" +
				"export JAVA_HOME=%s;" + 
				"java -XX:MaxPermSize=2048M -javaagent:%s=destfile=%s,buildlocation=%s,"+
				"includes=%s,excludes=\"\",inclnolocationclasses=false,output=\"FILE\" " + 
				"-cp %s:%s:%s:%s " +
				"com.gzoltar.cli.Main runTestMethods " + 
				"--testMethods %s " + 
				"--collectCoverage;", 
				Configuration.JAVA7_HOME,
				Configuration.JAVA7_HOME,
				gzoltarAgentJar, ser_file, srcClassDir,
				srcCommonName,
				srcClassDir, testClassDir, classpath, gzoltarCliJar,
				testToExecPath
				);
		result = CmdUtil.runCmd(cmdGetCoverage);
		logger.debug("cmdGetCoverage: {}\nresult: {}", cmdGetCoverage, result);
		
		// 5) get matrix and spectrum by running fl
		/*
		 * 		java -cp $src_classes_dir:$test_classes_dir:$classpath:$GZOLTAR_CLI_JAR \
	      com.gzoltar.cli.Main faultLocalizationReport \
	        --buildLocation "$src_classes_dir" \
	        --granularity "line" \
	        --inclPublicMethods \
	        --inclStaticConstructors \
	        --inclDeprecatedMethods \
	        --dataFile "$ser_file" \
	        --outputDirectory "/tmp/openmrs-openmrs-module-webservices.rest-455565885-458312291_issue/bears-benchmark/omod-1.9/target" \
	        --family "sfl" \
	        --formula "ochiai" \
	        --metric "entropy" \
	        --formatter "txt"
		 */
		String outputDirectory = FileUtil.toolDir + "/"; //GZoltar
		String cmdMkDir = String.format("[ ! -d %s ] && mkdir %s && echo \"mkdir %s\"", outputDirectory, outputDirectory, outputDirectory);
		result = CmdUtil.runCmd(cmdMkDir);
		logger.debug("cmdMkDir: {}\nresult: {}", cmdMkDir, result);
		String cmdGetMatrix = String.format(
				"export PATH=%s/bin:$PATH;" +
				"export JAVA_HOME=%s;" + 
				"java -cp %s:%s:%s:%s " +
				"com.gzoltar.cli.Main faultLocalizationReport " +
				"--buildLocation %s " +
				"--granularity \"line\" " +
				"--inclPublicMethods " +
				"--inclStaticConstructors " +
				"--inclDeprecatedMethods " +
				"--dataFile %s " +
				"--outputDirectory  %s " +
				"--family sfl " +
				"--formula ochiai " +
				"--metric entropy " +
				"--formatter txt;",
				Configuration.JAVA7_HOME,
				Configuration.JAVA7_HOME,
				srcClassDir, testClassDir, classpath, gzoltarCliJar,
				srcClassDir,
				ser_file,
				outputDirectory);
		result = CmdUtil.runCmd(cmdGetMatrix);
		logger.debug("cmdGetMatrix: {}\nresult: {}", cmdGetMatrix, result);
	}
	
	/**
	 * @Description 
	 * get covered stmt for each Test
	 * @author apr
	 * @version Oct 12, 2020
	 *
	 * @return
	 */
	public void calculateCoveredStmtsPerTest(){
		String fl_dir = FileUtil.toolDir + "/sfl/txt";
		String spec_file = fl_dir + "/spectra.csv";
		String matrix_file = fl_dir + "/matrix.txt";
		String test_file = fl_dir + "/tests.csv";
		
		List<SuspiciousLocation> slList = CoverageUtil.readStmtFile(spec_file);
		List<TestCaseResult> testList = CoverageUtil.readTestFile(test_file);
		char[][] matrix = CoverageUtil.getMatrixFromFile(matrix_file, slList, testList);
		
		// exposed by lang 29
		if (matrix.length == 0){
			FileUtil.writeToFile("matrix.length == 0, and return empty map!\n");
			return;
		}
		
		// size check
		if (matrix.length != testList.size()){
			FileUtil.raiseException("tests size in matrix: %s, testList size: %s\n", matrix.length, testList.size());
		}else{
			FileUtil.writeToFileWithFormat("tests size in matrix: %s, testList size: %s\n", matrix.length, testList.size());
		}
		if (matrix[0].length != slList.size() + 1){
			FileUtil.raiseException("stmts size in matrix: %s, slList size: %s\n", matrix[0].length, slList.size());
		}else{
			FileUtil.writeToFileWithFormat("stmts size in matrix: %s, slList size: %s (+1 TestResult)\n", matrix[0].length, slList.size());
		}
		
		stmtToTests = CoverageUtil.parseMatrixForStmts(matrix, slList, testList);
		testToStmts = CoverageUtil.parseMatrixForTests(matrix, slList, testList);
		
		// check
		if (!Main.reRun && FileUtil.oriFailedTests.size() != FileUtil.gzFailingTestCases.size()){
			// for mockito  1, 3, 18, 20. many of the failing test cases cannot be reproduced!
			// provide a workaround here for future TODO
//			FileUtil.raiseException("!Main.reRun && FileUtil.oriFailedTests.size() != FileUtil.gzFailingTestCases.size()");
			FileUtil.writeToFile("!Main.reRun && FileUtil.oriFailedTests.size() != FileUtil.gzFailingTestCases.size()\n");
		}
		
		// log
		for (String fCase : FileUtil.gzFailingTestCases){
			// extra failing in coverage on fixed version. lang 30. testContainsAny_StringCharArrayWithBadSupplementaryChars is not included in tests.csv.
			List<SuspiciousLocation> fCaseSlList = testToStmts.get(new TestCaseResult(fCase, false));
			if (fCaseSlList == null){
				FileUtil.writeToFileWithFormat("fCaseSlList == null. fCase: %s. continue.", fCase);
				continue;
			}
			FileUtil.writeToFileWithFormat("failing test case: %s, number of covered stmts: %s", 
					fCase, fCaseSlList.size());
			for (SuspiciousLocation sl : fCaseSlList){
				FileUtil.writeToFile(sl.toString() + "\n");
			}
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 4, 2020
	 * tcase: JUNIT,org.jfree.chart.axis.junit.CategoryAnchorTests#testHashCode
	 * oriFailedTest: org.jfree.chart.axis.junit.CategoryAnchorTests
	 * check if tcase is in oriFailedTest
	 * 
	 * But is is a temporal solution. I'd like to ask the dataset provide the really failing test, ideally.
	 * 
	 * @param tcase
	 * @param oriFailedTests
	 * @return
	 */
	private static boolean belongTo(String tcase, List<String> oriFailedTests) {
//		String testCase = tcase.split(",")[1].split("#")[0];  // for test class
		String testCase = tcase.split(",")[1]; // for test method/case
		
		return oriFailedTests.contains(testCase);
	}

	/** @Description 
	 * 
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param srcClasses
	 * @return
	 */
	private static String getCommonName(Set<String> classes) {
		List<String> commonNames = new ArrayList<>();
		
		for (String clazz : classes){
			if (clazz.contains(".")){
				String[] clazzSet = clazz.split("\\.");
				if (!commonNames.contains(clazzSet[0] + ".*")){
					commonNames.add(clazzSet[0] + ".*");
				}
			}else{
				if (!commonNames.contains(clazz + ".*")){
					commonNames.add(clazz + ".*");
				}
			}
		}
		
		String commonName = FileUtil.concatAndDropLastChar(commonNames);
		return commonName;
	}

	public static Map<TestCaseResult, List<SuspiciousLocation>> getTestToStmts() {
		return testToStmts;
	}

	public static void setTestToStmts(Map<TestCaseResult, List<SuspiciousLocation>> testToStmts) {
		Coverage.testToStmts = testToStmts;
	}

	public static Map<SuspiciousLocation, List<TestCaseResult>> getStmtToTests() {
		return stmtToTests;
	}

	public static void setStmtToTests(Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests) {
		Coverage.stmtToTests = stmtToTests;
	}
}
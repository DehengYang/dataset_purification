package apr.purify.coverage;

import java.io.File;
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

public class Coverage {
	final static Logger logger = LoggerFactory.getLogger(Coverage.class); 
	
	private static Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = new HashMap<>();
	private static Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests =  new HashMap<>();
	
	public Coverage(){
		
	}
	
	public void getCoverage(){
		
		String gzoltarCliJar = FileUtil.gzoltarDir + "/lib/com.gzoltar.cli-1.7.2-jar-with-dependencies.jar";
		String gzoltarAgentJar = FileUtil.gzoltarDir + "/lib/com.gzoltar.agent.rt-1.7.2-all.jar";
		
		String classpath = FileUtil.dependencies;
		String testClassDir = FileUtil.binTestDir;
		String srcClassDir = FileUtil.binJavaDir;
		
		
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
			FileUtil.writeToFileWithFormat("failingTestCase: %s", fTest); 
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
		
		String outputDirectory = FileUtil.toolDir + "/"; 
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
	
	public void calculateCoveredStmtsPerTest(){
		String fl_dir = FileUtil.toolDir + "/sfl/txt";
		String spec_file = fl_dir + "/spectra.csv";
		String matrix_file = fl_dir + "/matrix.txt";
		String test_file = fl_dir + "/tests.csv";
		
		List<SuspiciousLocation> slList = CoverageUtil.readStmtFile(spec_file);
		List<TestCaseResult> testList = CoverageUtil.readTestFile(test_file);
		char[][] matrix = CoverageUtil.getMatrixFromFile(matrix_file, slList, testList);
		
		
		if (matrix.length == 0){
			FileUtil.writeToFile("matrix.length == 0, and return empty map!\n");
			return;
		}
		
		
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
		
		
		if (!Main.reRun && FileUtil.oriFailedTests.size() != FileUtil.gzFailingTestCases.size()){
			
			

			FileUtil.writeToFile("!Main.reRun && FileUtil.oriFailedTests.size() != FileUtil.gzFailingTestCases.size()\n");
		}
		
		
		for (String fCase : FileUtil.gzFailingTestCases){
			
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

	private static boolean belongTo(String tcase, List<String> oriFailedTests) {

		String testCase = tcase.split(",")[1]; 
		
		return oriFailedTests.contains(testCase);

	}


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
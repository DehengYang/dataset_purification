/**
 * 
 */
package apr.purify.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import apr.purify.Configuration;
import apr.purify.mutant.DeltaDebugging;
import apr.purify.utils.CmdUtil;
import apr.purify.utils.FileUtil;
import apr.purify.utils.GeneralUtil;

/**
 * @author apr
 * @version Oct 15, 2020
 *
 */
public class D4JExecutor extends Executor{
	private List<String> failedCases = new ArrayList<>();

	@Override
	public boolean compile() {
		if(new File(FileUtil.binJavaDir).exists()){
			try {
				FileUtils.deleteDirectory(new File(FileUtil.binJavaDir));
			} catch (IOException e) {
				e.printStackTrace();
			}
			FileUtil.writeToFileWithFormat("[compile] delete %s before re-compiliation", FileUtil.binJavaDir);
		}
		
		// if the binTestDir is not removed. the test will fail for Lang 29 in:
		/*
		 * 			boolean runOriginal2 = DeltaDebugging.assertBeforeMut(simplifiedLinesBkForDDMin, chunksBkForDDMin, "simplifyPatch.diff", "[ddmin] original patch version does not pass!");
			if (!runOriginal2){
				FileUtil.raiseException("[ddmin] original patch version does not pass!");
				
				this is very ridiculous... as I have never changed the test classes.
				Anyway, I think this is the problem of defects4j. For example, it compile test command will compile both src & test classes...
				
				So I recommend to delete both src and test classes dir before compile
		 */
		if(new File(FileUtil.binTestDir).exists()){
			try {
				FileUtils.deleteDirectory(new File(FileUtil.binTestDir));
			} catch (IOException e) {
				e.printStackTrace();
			}
			FileUtil.writeToFileWithFormat("[compile] delete %s before re-compiliation", FileUtil.binTestDir);
		}
		
		// only compile src code
//		String cmd = String.format(
//				"cd %s;"
//				+ "export PATH=%s/bin:$PATH;"
//				+ "export JAVA_HOME=%s;"
//				+ "ant -f %s/framework/projects/defects4j.build.xml "
//				+ "-Dd4j.home=%s "
//				+ "-Dd4j.dir.projects=%s/framework/projects "
//				+ "-Dbasedir=%s compile;", 
//				FileUtil.projDir,
//				Configuration.JAVA7_HOME,
//				Configuration.JAVA7_HOME,
//				Configuration.D4J_DIR,
//				Configuration.D4J_DIR,
//				Configuration.D4J_DIR,
//				FileUtil.projDir
//				);
		
		// bug fix: mockito 12 ant compile deletes the test class dir
		// DONE_TODO: I recommend using d4j compile to compile both src and test dir. rather than just compile src dir using ant command. The former is more stable!
//		if (FileUtil.projDir.contains("_Mockito_")){
//			cmd = String.format(
//					"cd %s;"
//					+ "export PATH=%s/bin:$PATH;"
//					+ "export JAVA_HOME=%s;"
//					+ "timeout %s defects4j compile;", 
//					FileUtil.projDir,
//					Configuration.JAVA7_HOME,
//					Configuration.JAVA7_HOME,
//					Configuration.TIMEOUT_TEST_ALL);
//		}
		
		String cmd = String.format(
				"cd %s;"
				+ "export PATH=%s/bin:$PATH;"
				+ "export JAVA_HOME=%s;"
				+ "timeout %s defects4j compile;", 
				FileUtil.projDir,
				Configuration.JAVA7_HOME,
				Configuration.JAVA7_HOME,
				Configuration.TIMEOUT_TEST_ALL);
		
		String output = CmdUtil.runCmd(cmd, false);
		FileUtil.writeToFileWithFormat("[compile] cmd:%s\noutput:%s\n", cmd, output);
		
//		String passKeyWord = "BUILD SUCCESSFUL";
//		return checkPass(output, passKeyWord);
		
		String failKeyWord = "BUILD FAILED";
		return checkPassCompile(output, failKeyWord);
	}

	

	@Override
	public boolean testFailCases() {
		String passKeyWord = "Failing tests: 0";

		/*
		 * /mnt/purify/repairDir/Purify_Defects4J_Lang_24 && ant -f /mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j/framework/projects/defects4j.build.xml -Dd4j.home=/mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j -Dd4j.dir.projects=/mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j/framework/projects -Dbasedir=/mnt/purify/repairDir/Purify_Defects4J_Lang_24 -DOUTFILE=/mnt/purify/repairDir/Purify_Defects4J_Lang_24/failing_tests -Dtest.entry.class=org.apache.commons.lang3.math.NumberUtilsTest -Dtest.entry.method=testIsNumber run.dev.tests 2>&1
		 */
		for (String fCase : FileUtil.oriFailedTests){ //FileUtil.gzFailingTestCases
			String cmd = String.format(
					"cd %s;"
					+ "export PATH=%s/bin:$PATH;"
					+ "export JAVA_HOME=%s;"
					+ "timeout %s defects4j test -t %s::%s;", 
					FileUtil.projDir,
					Configuration.JAVA7_HOME,
					Configuration.JAVA7_HOME,
					Configuration.TIMEOUT_TEST_SINGLE,
					fCase.split("#")[0], fCase.split("#")[1]);
			String output = CmdUtil.runCmd(cmd, false);
			FileUtil.writeToFileWithFormat("[testFailCases] cmd:%s\noutput:%s\n", cmd, output);
			
			if (! checkPass(output, passKeyWord)){ // fail
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean testAll() {
		/*
		 * cd /mnt/purify/repairDir/Purify_Defects4J_Lang_24 && ant -f /mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j/framework/projects/defects4j.build.xml -Dd4j.home=/mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j -Dd4j.dir.projects=/mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j/framework/projects -Dbasedir=/mnt/purify/repairDir/Purify_Defects4J_Lang_24 -DOUTFILE=/mnt/purify/repairDir/Purify_Defects4J_Lang_24/failing_tests  run.dev.tests 
		 */
		String cmd = String.format(
				"cd %s;"
				+ "export PATH=%s/bin:$PATH;"
				+ "export JAVA_HOME=%s;"
				+ "timeout %s defects4j test;", 
				FileUtil.projDir,
				Configuration.JAVA7_HOME,
				Configuration.JAVA7_HOME,
				Configuration.TIMEOUT_TEST_ALL);
		String output = CmdUtil.runCmd(cmd, false);
		FileUtil.writeToFileWithFormat("[testAll] cmd:%s\noutput:%s\n", cmd, output);
		
		String passKeyWord = "Failing tests: 0";
		boolean pass = checkPass(output, passKeyWord);
		
		if (!pass){
			failedCases.clear();
			getFailedCasesFromOutput(output);
		}
		
		return pass;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 * e.g.,
	 * Failing tests: 2
  - org.jfree.data.general.junit.DatasetUtilitiesTests::testBug2849731_2
  - org.jfree.data.general.junit.DatasetUtilitiesTests::testBug2849731_3
	 * @param output
	 * @param passKeyWord
	 * @return
	 */
	private void getFailedCasesFromOutput(String output) {
		List<String> outputLines = Arrays.asList(output.split("\n"));
		
		int failedCnt = 0;
		boolean failedLineStart = false;
		
		for(String outputLine : outputLines){
			if (outputLine.startsWith("Failing tests: ")){
				failedCnt = Integer.parseInt(GeneralUtil.getLastSplit(outputLine, " "));
				failedLineStart = true;
				continue;
			}
			
			if(failedLineStart){
				// get org.jfree.data.general.junit.DatasetUtilitiesTests#testBug2849731_2
				if (outputLine.startsWith("  - ")){
					failedCases.add(outputLine.substring("  - ".length()).replaceAll("::", "#"));
				}
			}
		}
		
		FileUtil.writeToFileWithFormat("[testAll] failed test cases during running defects4j test:\n%s", GeneralUtil.listToStringAddLineBreak(failedCases));
		
		if (failedCases.size() != failedCnt){
			FileUtil.raiseException("[getFailedCasesFromOutput] failedCases.size() != failedCnt");
		}
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 15, 2020
	 *
	 * @param output
	 * @param passKeyWord
	 * @return
	 */
	private boolean checkPass(String output, String passKeyWord) {
		List<String> outputLines = Arrays.asList(output.split("\n"));
		for(String outputLine : outputLines){
			if (outputLine.equals(passKeyWord)){
				return true;
			}
		}
		return false;
	}
	
	private boolean checkPassCompile(String output, String failKeyWord) {
		List<String> outputLines = Arrays.asList(output.split("\n"));
		for(String outputLine : outputLines){
			if (outputLine.equals(failKeyWord)){
				return false;
			}
		}
		return true;
	}

	public List<String> getFailedCases() {
		return failedCases;
	}

	public void setFailedCases(List<String> failedCases) {
		this.failedCases = failedCases;
	}

	/* (non-Javadoc)
	 * @see apr.purify.execution.Executor#getFailingCasesInTestAll()
	 */
	@Override
	public List<String> getFailingCasesInTestAll() {
		return failedCases;
	}

}

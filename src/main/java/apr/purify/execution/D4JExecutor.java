
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
		
		

		if(new File(FileUtil.binTestDir).exists()){
			try {
				FileUtils.deleteDirectory(new File(FileUtil.binTestDir));
			} catch (IOException e) {
				e.printStackTrace();
			}
			FileUtil.writeToFileWithFormat("[compile] delete %s before re-compiliation", FileUtil.binTestDir);
		}
		
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
		
		String failKeyWord = "BUILD FAILED";
		return checkPassCompile(output, failKeyWord);
	}

	

	@Override
	public boolean testFailCases() {
		String passKeyWord = "Failing tests: 0";
	
		for (String fCase : FileUtil.oriFailedTests){ 
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
			
			if (! checkPass(output, passKeyWord)){ 
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean testAll() {
		
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


	@Override
	public List<String> getFailingCasesInTestAll() {
		return failedCases;
	}

}

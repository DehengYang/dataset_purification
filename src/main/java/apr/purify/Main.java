package apr.purify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.coverage.Coverage;
import apr.purify.diff.Chunk;
import apr.purify.diff.Diff;
import apr.purify.diff.DiffUtil;
import apr.purify.execution.D4JExecutor;
import apr.purify.execution.Executor;
import apr.purify.location.MutantUtil;
import apr.purify.location.SuspiciousLocation;
import apr.purify.location.TestCaseResult;
import apr.purify.mutant.Mutant;
import apr.purify.utils.FileUtil;
import apr.purify.utils.FileUtil.Dataset;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import apr.purify.utils.GeneralUtil;
import apr.purify.utils.Pair;


public class Main 
{
	final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static String[] args;

	public static List<String> failingCasesInTestAll = new ArrayList<>();
	public static boolean reRun = false;

	public static boolean gzoltarOnPatchFail = false;
	
    public static void main( String[] args )
    {	
    	Main.args = args;
    	
    	long mainStartT = System.currentTimeMillis();
    	
    	setParameters(args);
    	
    	FileUtil.mark("[main] init");
    	Executor exec = new D4JExecutor();
    	init(exec);
    	FileUtil.writeToFileWithFormat("[time cost] of init: %s", FileUtil.countTime(mainStartT));
    	
    	FileUtil.mark("[main] count project loc");
    	GeneralUtil.countLoc(FileUtil.srcJavaDir);
    	
    	long covStartT = System.currentTimeMillis();
    	FileUtil.mark("[main] get coverage of buggyVersion: testToStmts & stmtToTests");
    	Coverage coverage = new Coverage();
    	coverage.getCoverage(); 
    	coverage.calculateCoveredStmtsPerTest();
		Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = Coverage.getTestToStmts(); 
		Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests = Coverage.getStmtToTests(); 
		FileUtil.writeToFileWithFormat("[time cost] of coverage analysis on buggy version: %s", FileUtil.countTime(covStartT));
		
		long getChunksStartT = System.currentTimeMillis();
		FileUtil.mark("[main] get chunks from patchDiff.txt: chunks");
		Diff diff = new Diff(FileUtil.patchDiffPath, FileUtil.srcJavaDir);
		List<Chunk> chunks = diff.getChunks();
		FileUtil.writeToFileWithFormat("[time cost] of getting human-patch chunks: %s", FileUtil.countTime(getChunksStartT));
		
		covStartT = System.currentTimeMillis();
		FileUtil.mark("[main] reinit & get coverage of human-fixed version: testToStmtsPatch & stmtToTestsPatch");
		int fCaseSizeBefore = FileUtil.gzFailingTestCases.size();
		String patchFolder = FileUtil.toolDir + "/patch";
		FileUtil.remove(patchFolder);
		DiffUtil.applyAllChunksForPatch(chunks, FileUtil.srcJavaDir, patchFolder, "oriPatch.diff");
		try {
			FileUtils.copyDirectory(new File(patchFolder), new File(FileUtil.srcJavaDir));
			exec.compile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		coverage.getCoverage();
    	coverage.calculateCoveredStmtsPerTest();
    	int fCaseSizeAfter = FileUtil.gzFailingTestCases.size();
    	if (fCaseSizeBefore != fCaseSizeAfter){
    		FileUtil.writeToFileWithFormat("Patch is wrong with extra failing tests! Before: %s, After: %s", fCaseSizeBefore, fCaseSizeAfter);
    		Main.gzoltarOnPatchFail = true;
    	}
		Map<TestCaseResult, List<SuspiciousLocation>> testToStmtsPatch = Coverage.getTestToStmts(); 
		Map<SuspiciousLocation, List<TestCaseResult>> stmtToTestsPatch = Coverage.getStmtToTests(); 
		FileUtil.restore();
		FileUtil.writeToFileWithFormat("[time cost] of coverage analysis on fixed version: %s", FileUtil.countTime(covStartT));
		
		long mutStartT = System.currentTimeMillis();
		FileUtil.mark("[main] get mutants from chunks and coverage info, and compile & test");
		Mutant mutant = new Mutant(FileUtil.srcJavaDir, patchFolder, FileUtil.toolDir + "/mutants/", chunks, testToStmts, stmtToTests, testToStmtsPatch, stmtToTestsPatch);
		FileUtil.writeToFileWithFormat("[time cost] of purification via coverage and dd: %s", FileUtil.countTime(mutStartT));
		
		FileUtil.writeToFileWithFormat("oriChunks start: ");
		for(Chunk chunk:chunks){
			chunk.updateMethods(FileUtil.srcJavaDir, patchFolder);
		}
		MutantUtil.logChunks(chunks);
		FileUtil.writeToFileWithFormat("oriChunks end.");
		
		List<Chunk> deltaChunks = mutant.getDeltaChunks();
		FileUtil.writeToFileWithFormat("deltaChunks start: ");
		MutantUtil.logChunks(deltaChunks);
		FileUtil.writeToFileWithFormat("deltaChunks end.");
		
		String purifyFolder = FileUtil.toolDir + "/purifiedComment";
		DiffUtil.applyChunksAsMutants(deltaChunks, FileUtil.srcJavaDir, purifyFolder, "purifyWithCommentPatch.diff");
		
		MutantUtil.removePurifyComment(deltaChunks);
		
		long actionStartT = System.currentTimeMillis();
		DiffUtil.getOriAndPurifyActions(patchFolder, chunks, deltaChunks);
		FileUtil.writeToFileWithFormat("[time cost] of comparing repair actions: %s", FileUtil.countTime(actionStartT));
		
		FileUtil.writeToFileWithFormat("deltaChunks after removePurifyComment start: ");
		for(Chunk chunk:deltaChunks){
			chunk.updateMethods(FileUtil.srcJavaDir, FileUtil.toolDir + "/purified");
		}
		MutantUtil.logChunks(deltaChunks);
		FileUtil.writeToFileWithFormat("deltaChunks after removePurifyComment end.");
		
		FileUtil.writeToFileWithFormat("oriChunks lines: %s, simplifiedChunks lines: %s, deltaChunks lines: %s", MutantUtil.getLinesCnt(chunks),
				MutantUtil.getLinesCnt(mutant.getSimplifiedChunks()), MutantUtil.getLinesCnt(deltaChunks));
		
		FileUtil.writeToFileWithFormat("[time cost] of main: %s", FileUtil.countTime(mainStartT));
    }
    
	private static void someTest() {
		Pair<Integer, String> pair = new Pair<Integer, String> (10000, "1212");
		int lineNo = 10000;
		Integer lineNo2 = 10000;
		
		boolean b1 = pair.getLeft() == lineNo;
		boolean b2 = pair.getLeft() == lineNo2;
		
		boolean b3 = pair.getLeft().equals(lineNo);
		boolean b4 = pair.getLeft().equals(lineNo2);
		
		logger.debug("{} {} {} {}", b1, b2, b3, b4);
	}

	private static void debugMain(List<Chunk> chunks) {
		List<String> fileClasses = DiffUtil.getFileClasses(chunks);
		Map<Chunk, List<HierarchicalActionSet>> patchChunkActionMap = new HashMap<>();
		Map<String, List<HierarchicalActionSet>> patchActionsMap = DiffUtil.getActionsFromFileClasses(fileClasses, FileUtil.toolDir + "/patch", chunks, patchChunkActionMap);
	}

	private static void reInitForGZ() {
		FileUtil.gzFailingTestCases.clear();
	}

	private static void init(Executor exec) {
		Configuration.srcJavaDirBk = GeneralUtil.getBkDir(FileUtil.srcJavaDir);
    	FileUtil.backup(FileUtil.srcJavaDir, Configuration.srcJavaDirBk);
    	FileUtil.restore(FileUtil.srcJavaDir, Configuration.srcJavaDirBk);
    	
		exec.compile();
    	
    	String bkMutDir = Configuration.mutDir;
    	removeMutDir(bkMutDir);
    	
    	String assertBeforeMutDir = FileUtil.toolDir + "/assertBeforeMut";
    	removeMutDir(assertBeforeMutDir);
    	
    	FileUtil.remove(FileUtil.toolDir + "/patch");
    	FileUtil.remove(FileUtil.toolDir + "/purified");

    	String filePath = FileUtil.toolDir + "/" + "oriPatch.diff";
    	FileUtil.removeFile(filePath);
    	filePath = FileUtil.toolDir + "/" + "patchAfterCov.diff";
    	FileUtil.removeFile(filePath);
    	filePath = FileUtil.toolDir + "/" + "purifyWithCommentPatch.diff";
    	FileUtil.removeFile(filePath);
    	filePath = FileUtil.toolDir + "/" + "purifiedPatch.diff";
    	FileUtil.removeFile(filePath);
	}

	
	private static void removeMutDir(String mutDir) {
		for (int i = 0; i < Configuration.MAX_MUTANTS; i ++){
    		String dir = String.format("%s_%s", mutDir, i);
    		if (!new File(dir).exists()){
    			break;
    		}else{
    			FileUtil.remove(dir);
    		}
    	}
	}

	
    public static void setParameters(String[] args) {		    	
    	Options options = new Options();
    	
        Option opt = new Option("srcJavaDir","srcJavaDir",true,"e.g., /mnt/benchmarks/repairDir/Kali_Defects4J_Mockito_10/src");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("binJavaDir","binJavaDir",true,"e.g., /mnt/benchmarks/repairDir/Kali_Defects4J_Mockito_10/build/classes/main/");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("binTestDir","binTestDir",true,"e.g., /mnt/benchmarks/repairDir/Kali_Defects4J_Mockito_10/build/classes/test/ ");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("dependencies","dependencies",true,"all dependencies");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("testExecPath","testExecPath",true,"Path to run junit tests. e.g., /home/apr/apr_tools/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
        opt.setRequired(false);
        options.addOption(opt);
        
        opt = new Option("jvmPath","jvmPath",true,"java path to run junit tests (e.g., -jvmPath /home/apr/env/jdk1.7.0_80/bin/ )");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("failedTestsStr","failedTestsStr",true,"e.g., -failedTestsStr  org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests:");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("gzoltarDir","gzoltarDir",true,"e.g., -gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 ");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("logDir","logDir",true,"e.g., -logDir output/Defects4J_Chart_1");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("timeout","timeout",true,"180");
        opt.setRequired(false); 
        options.addOption(opt);
        
        opt = new Option("patchDiffPath","patchDiffPath",true,"e.g., -patchDiffPath /home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Chart1_patchDiff.txt");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("dataset","dataset",true,"e.g., Bears");
        opt.setRequired(true);
        options.addOption(opt);
        
        opt = new Option("projDir","projDir",true,"e.g., ");
        opt.setRequired(true);
        options.addOption(opt);
        
        logger.debug("total number of required options: {}", options.getRequiredOptions().size());
        logger.debug("total number of options: {}", options.getOptions().size());

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> [ParseException] cli options", options);
            e.printStackTrace();
        } 
        
        try {
	        if (cli.hasOption("srcJavaDir")){
	        	String srcJavaDir = cli.getOptionValue("srcJavaDir");
	        	FileUtil.srcJavaDir = new File(srcJavaDir).getCanonicalPath();
	        }
	        if(cli.hasOption("binJavaDir")){
	        	String binJavaDir = cli.getOptionValue("binJavaDir");
	        	FileUtil.binJavaDir = new File(binJavaDir).getCanonicalPath();
	        }
	        if(cli.hasOption("binTestDir")){
	        	String binTestDir = cli.getOptionValue("binTestDir");
	        	FileUtil.binTestDir = new File(binTestDir).getCanonicalPath();
	        }
	        if(cli.hasOption("dependencies")){
	        	FileUtil.dependencies = cli.getOptionValue("dependencies");
	        	logger.debug("current dir: {}", System.getProperty("user.dir"));
	        	for(String dep : FileUtil.dependencies.split(":")){
	        		if(! dep.isEmpty() && new File(dep).exists()){
	        				String path = new File(dep).getCanonicalPath();
	        				if (FileUtil.depsList.contains(path)) continue;
	        				
							FileUtil.depsList.add(path);
	        		}else{
	        			System.out.format("dependency: %s is empty or does not exist.\n", dep);
	        		}
	        	}
	        }
	        if(cli.hasOption("logDir")){
	        	String buggylocDir = cli.getOptionValue("logDir");
	        	String toolDir = new File(buggylocDir).getAbsolutePath() + "/" + FileUtil.toolName;
	        	
	        	FileUtil.toolDir = toolDir;
	        	
	        	if (FileUtil.toolDir.equals("/" + FileUtil.toolName)){
	        		FileUtil.raiseException(FileUtil.toolDir);
	        	}
	        	
	        	FileUtil.logPath = toolDir + "/purify.log";
	        	if (! Main.reRun){
	        		FileUtil.writeToFile(FileUtil.logPath, "", false);
	        	}
	        }
	        if(cli.hasOption("testExecPath")){
	        	FileUtil.testExecPath = cli.getOptionValue("testExecPath");
	        }
	        if(cli.hasOption("jvmPath")){
	        	try {
					FileUtil.jvmPath = new File(cli.getOptionValue("jvmPath")).getCanonicalPath() + "/java";
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	        if(cli.hasOption("failedTestsStr")){ 
	        	if (! Main.failingCasesInTestAll.isEmpty()){ 
					FileUtil.gzFailingTestCases.clear(); 
	        		
	        		for (String fCase : Main.failingCasesInTestAll){
	        			FileUtil.oriFailedTests.add(fCase);
	        		}
					Main.failingCasesInTestAll.clear(); 
	        	}else{
	        		FileUtil.failedTestsStr = cli.getOptionValue("failedTestsStr");
		        	List<String> fCases = Arrays.asList(FileUtil.failedTestsStr.split(":"));
		        	FileUtil.oriFailedTests.addAll(fCases);
	        	}
	        }
	        if(cli.hasOption("gzoltarDir")){
	        	FileUtil.gzoltarDir = new File(cli.getOptionValue("gzoltarDir")).getCanonicalPath();
	        }
	        if(cli.hasOption("timeout")){
	        	FileUtil.timeout = Integer.parseInt(cli.getOptionValue("timeout"));
	        }
	        if(cli.hasOption("patchDiffPath")){
	        	FileUtil.patchDiffPath = cli.getOptionValue("patchDiffPath");
	        }
	        if(cli.hasOption("dataset")){
	        	String dataset = cli.getOptionValue("dataset").toLowerCase();
	        	if (dataset.equals("defects4j")){
	        		FileUtil.dataset = Dataset.DEFECTS4J;
	        	}else if(dataset.equals("bears")){
	        		FileUtil.dataset = Dataset.BEARS;
	        	}else if(dataset.equals("bugs.jar")){
	        		FileUtil.dataset = Dataset.BUGSJAR;
	        	}
	        }
	        if(cli.hasOption("projDir")){
	        	FileUtil.projDir = cli.getOptionValue("projDir");
	        }
        } catch (IOException e) {
			e.printStackTrace();
		}
        
     	if( ! FileUtil.depsList.contains(FileUtil.srcJavaDir)){
     		FileUtil.depsList.add(0, FileUtil.srcJavaDir);
     	}
     	if( ! FileUtil.depsList.contains(FileUtil.binJavaDir)){
     		FileUtil.depsList.add(0, FileUtil.binJavaDir);
     	}
     	if( ! FileUtil.depsList.contains(FileUtil.binTestDir)){
     		FileUtil.depsList.add(0, FileUtil.binTestDir);
     	}
     	
     	FileUtil.dependencies = FileUtil.concatAndDropLastChar(FileUtil.depsList);
     	logger.debug("Number of deps: {}", FileUtil.depsList.size());
     	logger.debug("Deps: {}", FileUtil.dependencies);
    }
}

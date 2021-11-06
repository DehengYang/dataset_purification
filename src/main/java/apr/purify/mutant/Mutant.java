/**
 * 
 */
package apr.purify.mutant;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import apr.purify.Configuration;
import apr.purify.Main;
import apr.purify.diff.Chunk;
import apr.purify.diff.DiffUtil;
import apr.purify.execution.TimeOut;
import apr.purify.location.MutantUtil;
import apr.purify.location.SuspiciousLocation;
import apr.purify.location.TestCaseResult;
import apr.purify.utils.FileUtil;
import apr.purify.utils.GeneralUtil;
import apr.purify.utils.Pair;

/**
 * @author apr
 * @version Oct 14, 2020
 *
 */
public class Mutant {
	final static Logger logger = LoggerFactory.getLogger(Mutant.class);
	
	//mutant folder
	private String mutFolder;
	private List<Chunk> chunks = new ArrayList<>();
	private List<Chunk> chunksBk = new ArrayList<>();
	private List<Chunk> chunksBkForDDMin = new ArrayList<>();
	private Map<TestCaseResult, List<SuspiciousLocation>> testToStmts;
	private Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests;
	private Map<TestCaseResult, List<SuspiciousLocation>> testToStmtsPatch;
	private Map<SuspiciousLocation, List<TestCaseResult>> stmtToTestsPatch;
	
	private List<Chunk> simplifiedChunks = new ArrayList<>(); 
	private List<Pair<Integer, String>> simplifiedLines = new ArrayList<>();
	private List<Chunk> deltaChunks = new ArrayList<>(); 
	private List<Pair<Integer, String>> deltaLines = new ArrayList<>();
	
	private String srcFolder;
	private String patchFolder;
	
	private boolean useChunksBk;
	
	/**
	 * 
	 * @param mutFolder
	 * @param chunks
	 * @param testToStmts
	 * @param stmtToTests
	 * @param stmtToTestsPatch 
	 * @param testToStmtsPatch 
	 */
	public Mutant(String srcFolder, String patchFolder, String mutFolder, List<Chunk> chunks, Map<TestCaseResult, List<SuspiciousLocation>> testToStmts,
			Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests, Map<TestCaseResult, List<SuspiciousLocation>> testToStmtsPatch, Map<SuspiciousLocation, List<TestCaseResult>> stmtToTestsPatch){
		this.srcFolder = srcFolder;
		this.patchFolder = patchFolder;
		
		this.mutFolder = mutFolder;
//		this.chunks.addAll(chunks); // is not really cloned. just change reference.
		
		//real clone
		for (Chunk chunk : chunks){
			this.chunks.add(chunk.clone());
		}
		for (Chunk chunk : chunks){
			this.chunksBk.add(chunk.clone());
		}
		for (Chunk chunk : chunks){
			this.chunksBkForDDMin.add(chunk.clone());
		}
		
		this.testToStmts = testToStmts;
		this.stmtToTests = stmtToTests;
		this.testToStmtsPatch = testToStmtsPatch;
		this.stmtToTestsPatch = stmtToTestsPatch;
		
		
		useChunksBk = false;  // does not use bk for ddmin at the beginning
		simplifyChunks(); // to get simplifiedChunks
		applyChunks(); // based on simplifiedChunks
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 */
	private void applyChunks() {
		long ddStartT = System.currentTimeMillis();
		
		// log simplified chunks
		FileUtil.writeToFileWithFormat("simplifiedChunks start: ");
		MutantUtil.logChunks(simplifiedChunks);
		FileUtil.writeToFileWithFormat("simplifiedChunks end.");
		
		// get a backup copy for lines
		List<Pair<Integer, String>> simplifiedLinesBkForDDMin = MutantUtil.getLinesFromChunks(chunksBkForDDMin);

		// TODO: run blame for this commit
		simplifiedLines = MutantUtil.getLinesFromChunks(simplifiedChunks);
		// for the case "FileUtil.raiseException("[ddmin] original patch version does not pass!");"
		boolean runOriginal = DeltaDebugging.assertBeforeMut(simplifiedLines, simplifiedChunks, "simplifyPatch.diff", "[ddmin] original patch version does not pass!");
		boolean useBk = false;
		if (! runOriginal){ // this means that the coverage analysis leads to the failure.
			FileUtil.writeToFile("Coverage analysis leads to the failure of patch fail before mutation!");
			boolean runOriginal2 = DeltaDebugging.assertBeforeMut(simplifiedLinesBkForDDMin, chunksBkForDDMin, "simplifyPatch.diff", "[ddmin] original patch version does not pass!");
			if (!runOriginal2){
				FileUtil.raiseException("[ddmin] original patch version does not pass!");
			}else{
				FileUtil.writeToFile("Use backup lines and chunks for ddmin now!");
				useBk = true;
			}
		}
		
		// new approach: via delta debugging
//		simplifiedLines = MutantUtil.getLinesFromChunks(simplifiedChunks);
		int beforeSize = simplifiedLines.size();
		if (useBk || useChunksBk || Main.gzoltarOnPatchFail){
			FileUtil.writeToFile("useBk || useChunksBk is true\n");
			FileUtil.writeToFileWithFormat("simplifiedLinesBkForDDMin: %s", GeneralUtil.listToStringAddLineBreak(simplifiedLinesBkForDDMin));
			deltaLines = runDDWithTimeOut(simplifiedLinesBkForDDMin, chunksBkForDDMin);
		}else{
			FileUtil.writeToFile("useBk || useChunksBk is false\n");
			FileUtil.writeToFileWithFormat("simplifiedLines: %s", GeneralUtil.listToStringAddLineBreak(simplifiedLines));
			deltaLines = runDDWithTimeOut(simplifiedLines, simplifiedChunks);
		}
		
		int beforeSizeDelta = deltaLines.size();
		deltaChunks = MutantUtil.getCommentedChunksFromLines(deltaLines, simplifiedChunks);
		
		// for closure 21. the purified patch cannot compile.
		boolean runDDAfter = DeltaDebugging.assertBeforeMut(deltaLines, deltaChunks, "patchAfterRunDD.diff", "[ddmin] purified patch version does not pass!");
		if (!runDDAfter){
			FileUtil.writeToFile("runDDAfter is false! the pacth after dd cannot pass!\n");
			if (runOriginal){
				FileUtil.writeToFile("purified patch is simplifiedChunks\n");
				deltaChunks.clear();
				deltaChunks.addAll(simplifiedChunks);
				deltaLines.clear();
				deltaLines.addAll(simplifiedLines);
			}else{ // add original
				FileUtil.writeToFile("purified patch is chunksBkForDDMin\n");
				deltaChunks.clear();
				deltaChunks.addAll(chunksBkForDDMin);
				deltaLines.clear();
				deltaLines.addAll(simplifiedLinesBkForDDMin);
			}
		}
		
		if (simplifiedLines.size() != beforeSize){
			FileUtil.raiseException("simplifiedLines.size() != beforeSize");
		}
//		if (deltaLines.size() != beforeSizeDelta){ // does not need since I have changed the logic of deltalines collection
//			FileUtil.raiseException("deltaLines.size() != beforeSizeDelta");
//		}
		
		FileUtil.writeToFileWithFormat("[time cost] of dd: %s", FileUtil.countTime(ddStartT));
	}
	

	/** @Description 
	 * @author apr
	 * @version Oct 23, 2020
	 * 
	 * this is for mockito 6. timeout problem.
	 *
	 * @param simplifiedLinesBkForDDMin
	 * @param chunksBkForDDMin2
	 * @return
	 */
	private List<Pair<Integer, String>> runDDWithTimeOut(List<Pair<Integer, String>> lines,
			List<Chunk> chunks) {
		List<Pair<Integer, String>> deltaLines = new ArrayList<>();
		try {
			TimeOut.runWithTimeout(new Runnable() {
				@Override
				public void run() {
					deltaLines.addAll(DeltaDebugging.ddmin(lines, chunks));
				}
			}, Configuration.TIMEOUT_DD, TimeUnit.MINUTES);
		} catch (Exception e1) {
			FileUtil.writeToFileWithFormat("dd execution timeout! (%s min)", Configuration.TIMEOUT_DD);
			e1.printStackTrace();
		}catch (Error er) {
			FileUtil.writeToFileWithFormat("dd execution timeout! (%s min)", Configuration.TIMEOUT_DD);
			er.printStackTrace();
		}
		
		return deltaLines;
	}

	/**
	 * @Description
	 * simplify chunks according to coverage info (testToStmts and stmtToTests)
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 */
	public void simplifyChunks(){
		//DONE.TODO: need to debug subclass for matching lines: this has been done in CoverageUtil by String javaFilePath = FileUtil.srcJavaDir + "/" + wholeName.replaceAll("\\.", "/") + ".java";
		
		long simplifyStartT = System.currentTimeMillis();
		
		//print chunks
		FileUtil.writeToFile("[simplifyChunks] original chunks before simplifyChunks:\n");
		for(Chunk chunk:chunks){
			FileUtil.writeToFileWithFormat("%s", chunk);
		}
		
		simplifiedChunks.clear(); 
		int simplifyCnt = 0;
		
		for (Chunk chunk : chunks){
//			org.apache.wicket.request.mapper.CompoundRequestMapper:43,0.0
			String clazz = chunk.getClazz().replaceAll("/", "\\.");
			List<Pair<Integer, String>> lines = chunk.getLines();
			List<Pair<Integer, String>> linesSimplify = new ArrayList<>();
			
			for (Pair<Integer, String> pair : lines){
				SuspiciousLocation sl = new SuspiciousLocation(clazz, pair.getLeft());
				if (chunk.isDel(pair)){ // for buggy
					if (isInFailingTestTrace(sl, testToStmts, pair)){
						linesSimplify.add(pair);
					}
//					else if (isNotMeaningfulCodeLine(pair.getRight())){
//							linesSimplify.add(pair);
//							FileUtil.writeToFileWithFormat("isNotMeaningful chunk line: %s is not covered by any failing test.", pair);
//					}
					else if (checkIsSubseqStatementAndShouldBeIncluded(pair, chunk, linesSimplify, chunks).getRight()){
						linesSimplify.add(pair);
					}else{
						//
						FileUtil.writeToFileWithFormat("deleted chunk line: %s is not covered by any failing test. [end]", pair);
						simplifyCnt ++;
					}
				}else{ // is add
					if (isInFailingTestTrace(sl, testToStmtsPatch, pair)){
						linesSimplify.add(pair);
					}
//					else if (isNotMeaningfulCodeLine(pair.getRight())){
//						linesSimplify.add(pair);
//					}
					else if (checkIsSubseqStatementAndShouldBeIncluded(pair, chunk, linesSimplify, chunks).getRight()){
						linesSimplify.add(pair);
					}
					else{
						//
						FileUtil.writeToFileWithFormat("added chunk line: %s is not covered by any failing test.", pair);
						simplifyCnt ++;
					}
				}
			}
			
			//bug fix: empty chunk should not be added! (exposed by Time_2)
			if (!linesSimplify.isEmpty()){
				chunk.setLines(linesSimplify); // this changes the original chunks!
				chunk.resetByLines();
				simplifiedChunks.add(chunk);
			}
		}
		
		FileUtil.writeToFileWithFormat("[time cost] of simplifyChunks: %s", FileUtil.countTime(simplifyStartT));
		FileUtil.writeToFileWithFormat("[simplifyChunks] Number of simplified lines: %s", simplifyCnt);
		
		// need to update chunks here. -> exposed by time 11. the simplified chunks need to be updated.
		updateSimplifyChunks(simplifiedChunks, chunksBk);
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param simplifiedChunks2
	 * @param chunks2
	 */
	private void updateSimplifyChunks(List<Chunk> simplifiedChunks, List<Chunk> chunksBk) {
//		List<Chunk> updSimplChunks = new ArrayList<>();
		
		List<Pair<Integer, String>> simpLines = new ArrayList<>();
		for (Chunk simpChunk : simplifiedChunks){
			simpLines.addAll(simpChunk.getLines());
		}
		
		List<Chunk> chunkList = updateSimpChunkByLines(simpLines, chunksBk);
//		updSimplChunks.addAll(chunkList);
		
		simplifiedChunks.clear();
		simplifiedChunks.addAll(chunkList); // bug fix exposed by time 12
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 * this is based on  public static List<Chunk> getCommentedChunksFromLines(List<Pair<Integer, String>> lines, List<Chunk> oriChunks) { 
	 * from MutantUtil class.
	 * but i modified to disable the addition of commented add lines
	 * @param lines
	 * @param chunks2
	 * @return
	 */
	private List<Chunk> updateSimpChunkByLines(List<Pair<Integer, String>> simpLines, List<Chunk> chunks) {
		List<Chunk> simpChunks = new ArrayList<>();
		
		for (Chunk chunk : chunks){
			Chunk remainedChunk = new Chunk();
			boolean addLineHasBeenExcluded = false;
			
			for (Pair<Integer, String> line : chunk.getLines()){
				if (simpLines.contains(line)){
					remainedChunk.getLines().add(line);
				}else{
					// the line need to be commented
					// bug fix: when the line is commented, the oriChunks are changed. There are two ways to deal with this: 1) use a new copy of oriChunks
					// 2) use a new copy of line before line.setRight()
					// only add del
					if (chunk.isDel(line)){
						String commentedLine = line.getRight().substring(0, 1) + Configuration.COMMENT + line.getRight().substring(1);
						Pair<Integer, String> lineCp = new Pair<Integer, String>(line.getLeft(), line.getRight());
						lineCp.setRight(commentedLine);
						remainedChunk.getLines().add(lineCp);
					}else{ // exposed by lang 62, has to consider add, otherwise will raise FileUtil.raiseException("[updateSimpChunkByLines] remainedChunk lines is empty!"); 
						addLineHasBeenExcluded = true;
					}
				}
			}
			
			if (!remainedChunk.getLines().isEmpty()){
				remainedChunk.setClazz(chunk.getClazz());
				remainedChunk.setReplaceRange(chunk.replaceRange);
				
				// bug fix: for chart 1. the chunk may not have - lines. so the replaceRange should be updated.
				if (remainedChunk.hasCommentedDelLine()){
					List<Chunk> updChunks = DiffUtil.updateChunkRange(remainedChunk);
					simpChunks.addAll(updChunks);
				}else{
					simpChunks.add(remainedChunk);
				}
			}else{
				if (! addLineHasBeenExcluded){
					FileUtil.raiseException("[updateSimpChunkByLines] remainedChunk lines is empty!");
				}
			}
		}
		
		return simpChunks;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *e.g., +                        throw new IllegalArgumentException("Types array must not contain duplicate unsupported: " +
+                                        types[i - 1].getName() + " and " + loopType.getName());
the second line is subsequent statement
	 * @param pair
	 * @param chunk 
	 * @param linesSimplify 
	 * @param chunks2 
	 * @return
	 */
	private Pair<String, Boolean> checkIsSubseqStatementAndShouldBeIncluded(Pair<Integer, String> pair, Chunk chunk, List<Pair<Integer, String>> linesSimplify, List<Chunk> allChunks) {
		Pair<String, Boolean> boolPair = null;
		
		// may be: } else {    -> time 1
		// I think such line should be included
		/*
		 * however, when it comes to time 6, more should be considered.
		 * i.e., if the if block of the }else{ line is not covered.
		 */
		String lineCode = pair.getRight().substring(1).trim();
		String allAlphabets = lineCode.replaceAll( "[^a-z^A-Z]", "");
		if (allAlphabets.contains("elseif")){
			FileUtil.writeToFile("allAlphabets.contains(\"elseif\")");
		}
		if (allAlphabets.equals("else")){ // first check    || allAlphabets.equals("elseif")
			// TODO: deal with  ("else if")
			allAlphabets = lineCode.replaceAll("\\s+"," ");
			if (allAlphabets.contains("else")){ // double check  || allAlphabets.contains("else if")
				FileUtil.writeToFileWithFormat("\n\nthe chunk line is an else or else if: %s\n", pair);
				
				int lineNo = pair.getLeft();
				if (chunk.isDel(pair)){
					String filePath = srcFolder + "/" + chunk.getClazz() + ".java";
					int ifStmtFirstLineNo = getIfStmtFirstLineNo(filePath, lineNo);
					//first check if in allChunks
					Pair<Integer, String> ifFirstChunkLine = checkLineNoInAllChunks(ifStmtFirstLineNo, allChunks, true);
					if (ifFirstChunkLine != null){
						if (checkLinePairInAlreadySimplify(ifFirstChunkLine, linesSimplify)){
							boolPair = new Pair<String, Boolean>("false", true);
						}else{
							boolPair = new Pair<String, Boolean>("false", false);
						}
					}else{
						// ifStmtFirstLineNo corresponds to a stmt not in patch
						boolPair = new Pair<String, Boolean>("false", true);
					}
				}else{
					String filePath = patchFolder + "/" + chunk.getClazz() + ".java";
					int ifStmtFirstLineNo = getIfStmtFirstLineNo(filePath, lineNo);
					//first check if in allChunks
					Pair<Integer, String> ifFirstChunkLine = checkLineNoInAllChunks(ifStmtFirstLineNo, allChunks, false); // false -> is not del -> is add
					if (ifFirstChunkLine != null){
						if (checkLinePairInAlreadySimplify(ifFirstChunkLine, linesSimplify)){
							boolPair = new Pair<String, Boolean>("false", true);
						}else{
							boolPair = new Pair<String, Boolean>("false", false);
						}
					}else{
						// ifStmtFirstLineNo corresponds to a stmt not in patch
						boolPair = new Pair<String, Boolean>("false", true);
					}
				}
				
				//boolPair = new Pair<String, Boolean>("false", true);	
				// return
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}
		
		Pair<String, Node> stmtInfo = new Pair<String, Node> (null, null);
		// check
		int lineNo = pair.getLeft();
		String filePath;
		String delOrAdd;
		List<Pair<Integer, String>> delOrAddLines = new ArrayList<>();
		if (chunk.isDel(pair)){ // is del
			filePath = srcFolder + "/" + chunk.getClazz() + ".java";
			delOrAdd = "-";
			delOrAddLines = chunk.getDelLines();
		}else{ // is add
			filePath = patchFolder + "/" + chunk.getClazz() + ".java";
			delOrAdd = "+";
			delOrAddLines = chunk.getAddLines();
		}
		
		// before obtaining the cur stmt range. we first check if the node of this line is in a method
		Pair<Boolean, Boolean> isInMethod = getCurMethodRange(filePath, lineNo); // bug fix exposed by time 11
		if (isInMethod == null){ // has no method
			boolPair = new Pair<String, Boolean>("notInAnyMethod", true);
			FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
			return boolPair;
		}else{
			if (isInMethod.getRight()){ // is the first line of the method
				boolPair = new Pair<String, Boolean>("inMethodButNotInBlock", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}//else, go to next code lines
		}
		
		// get range
		// here the lineRange is a stmt range, the stmt sometimes is the blockstmt
		Pair<Integer, Integer> lineRange = getCurStmtRange(filePath, lineNo, stmtInfo);
		// seemingly duplicate to above getCurMethodRange()
//		if(lineRange == null){ // bug fix: exposed by time 10.  +    private static final long START_1972 = 2L * 365L * 86400L * 1000L;
//			showCurNode(filePath, lineNo, stmtInfo); // is not in method
////			FileUtil.writeToFileWithFormat("chunk line: %s is not in any method", pair);
//			boolPair = new Pair<String, Boolean>("notInAnyMethod", true);
//			FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
//			return boolPair;
//		}
		
		// exposed by chart 21
		/*
		 * if (minv < this.minimumRangeValue || Double.isNaN(
+                                    this.minimumRangeValue)) { this line should not be deleted.
	stmtInfo: Pair<BlockStmt,{
    this.minimumRangeValue = minv;
    this.minimumRangeValueRow = r;
    this.minimumRangeValueColumn = c;
}>
		 */
		if(stmtInfo.getLeft().equals("BlockStmt")){
			FileUtil.writeToFileWithFormat("check if curLine: %s is before this block stmt.", pair);
			boolean isBeforeBlockStmt = isBeforeBlockStmt((BlockStmt) stmtInfo.getRight(), pair);
			if (isBeforeBlockStmt){
				boolPair = new Pair<String, Boolean>("isBeforeBlockStmt", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}
		
		
		// judge based on range
		boolPair = isSubAndShouldBeIncluded(pair, lineRange, delOrAddLines, linesSimplify, delOrAdd, allChunks, stmtInfo);
		
		FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
		return boolPair;
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 21, 2020
	 *
	 * @param right
	 * @param pair
	 * @return
	 */
	private boolean isBeforeBlockStmt(BlockStmt bs, Pair<Integer, String> pair) {
		if (bs.getChildNodes().isEmpty()){
			FileUtil.writeToFile("bs.getChildNodes().isEmpty()");
			return true; // should be included
		}else{
			Node node = bs.getChildNodes().get(0);
			int startRow = node.getBegin().get().line;
//			int endRow = node.getEnd().get().line;
			
			if (pair.getLeft() < startRow){
				return true; // before the first node of the block
			}
		}
		
		return false;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param ifStmtFirstLineNo
	 * @param allChunks
	 * @param b 
	 * @return
	 */
	private Pair<Integer, String> checkLineNoInAllChunks(int ifStmtFirstLineNo, List<Chunk> allChunks, boolean isDel) {
		if(isDel){
			for (Chunk chunk:allChunks){
				for (Pair<Integer, String> pair : chunk.getDelLines()){
					if (pair.getLeft().equals(ifStmtFirstLineNo)){
						return pair;
					}
				}
			}
		}else{
			for (Chunk chunk:allChunks){
				for (Pair<Integer, String> pair : chunk.getAddLines()){
					if (pair.getLeft().equals(ifStmtFirstLineNo)){
						return pair;
					}
				}
			}
		}
		
		return null;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param filePath
	 * @param lineNo
	 * @return
	 */
	private int getIfStmtFirstLineNo(String filePath, int lineNo) {
//		Pair<Integer, Integer> lineRange = new Pair<Integer, Integer> (null, null);
		List<Node> nodes = new ArrayList<>();
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof Statement){
					Statement stmt = (Statement) n;
					int startRow = stmt.getBegin().get().line;
					int endRow = stmt.getEnd().get().line;
					
					if (lineNo >= startRow && lineNo <= endRow){
						if (n instanceof IfStmt){
							nodes.add(n);
						}
					}
				}
			};
			cu.walk(TreeTraversal.PREORDER, cuConsumer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (nodes.isEmpty()){
			FileUtil.raiseException("[getIfStmtFirstLineNo] nodes.isEmpty()!");
		}
		
		int ifStmtFirstLineNo = nodes.get(nodes.size() - 1).getBegin().get().line;
		return ifStmtFirstLineNo;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 * @param stmtInfo 
	 *
	 * @return
	 */
	private Pair<String, Boolean> isSubAndShouldBeIncluded(Pair<Integer, String> linePair, Pair<Integer, Integer> lineStmtRange, List<Pair<Integer, String>> chunkLines, 
			List<Pair<Integer, String>> linesSimplify, String delOrAdd, List<Chunk> allChunks, Pair<String, Node> stmtInfo) {
		// bug fix: exposed by Time2: blockStmt should be considered.
//		boolean b1 = stmtInfo.getLeft().equals("BlockStmt");
//		boolean b2 = linePair.getLeft().equals(lineStmtRange.getRight()); // == will return false for Integers!!! refer to: https://stackoverflow.com/questions/3637936/java-integer-equals-vs
//		logger.debug("{} {}", linePair.getLeft(), lineStmtRange.getRight());
//		boolean b3 = 2 == 2;
		boolean isLastLineInBlock = stmtInfo.getLeft().equals("BlockStmt") && linePair.getLeft().equals(lineStmtRange.getRight()); // for debugging usage
		if (isLastLineInBlock){ // is the last line of the block
			//check if first line is in chunk, I think it must be in chunk
			boolean isInAllChunks = false;
			for (Chunk chunk : allChunks){
				Pair<Integer, String> firstLinePair = chunk.getPairByLineNo(lineStmtRange.getLeft(), delOrAdd);
				if (firstLinePair != null){
					isInAllChunks = true;
					
					if (checkLinePairInAlreadySimplify(firstLinePair, linesSimplify)){//linesSimplify.contains(firstLinePair)
						return new Pair<String, Boolean>("inBlockStmt", true);
					}else{
						return new Pair<String, Boolean>("inBlockStmt", false);
					}
				}
			}
			
			if(!isInAllChunks){
				useChunksBk = true;
				FileUtil.writeToFileWithFormat("useChunksBk = true; for linePair: %s", linePair);
//				FileUtil.raiseException("isInAllChunks is false!");
			}
		}
		
		
		/* 
		 * the linePair is the chunkLine we'd like to judge,
		 * the lineRange is the stmtRange containing the chunk line
		 * chunkLines is the lines on the chunk containing the chunk line
		 * lineSimplify is the lines that have been considered as cannot be simplified.
		 * allChunks is the original chunks. (seemingly not needed now!)
		 */
		int lineNo = linePair.getLeft();
		if (lineNo != lineStmtRange.getLeft()){ // is a subseq
			int firstLineNo = lineStmtRange.getLeft(); 
			// check if in chunk
			boolean isInChunk = false;
			for (Pair<Integer, String> line : chunkLines){
				if (line.getLeft().equals(firstLineNo)){ // is in chunk
					isInChunk = true;
					// then check if the first line of the stmt is included by linesSimplify
					boolean isInSimplify = false;
					for (Pair<Integer, String> simpLine : linesSimplify){
						if (simpLine.getRight().startsWith(delOrAdd) && simpLine.getLeft() == firstLineNo){
							isInSimplify = true;
							break;
						}
					}
					
					if (isInSimplify){ // should also be added
						return new Pair<String, Boolean>("true", true);
					}else{ // not in simplify but in chunk, then the stmt is not covered and should not be included
						return new Pair<String, Boolean>("true", false);
					}
				}
			}
			
			if (! isInChunk){
				// bug fix: there is another condition exposed by Time-3.
				/*
				 * +        if (weekyears != 0) {  this is not covered
					             setMillis(getChronology().weekyears().add(getMillis(), weekyears));
					+        }  this is not covered, but it's isInChunk is false. So it previously returns true.
					Actually, it should not be included!
					
					other cases:
					methodCall(para1, // this is not covered
					 para2,
					 para3)  //this is not covered
					 
					 this case also should not include the para3
					 
					 I know it, if any line in the stmt is contained in allChunks and covered by lineSimplify. this line should be included.
					 otherwise, should not.
					 
					 There is a pre-condition: this is a blockStmt! if or else block or other blocks
					 In this case, I only need to check the first line in the block!
					 if the first line in allChunks and is not covered, the line should be not included.
					 if the first line not in allChunks, seemingly this is impossible.
					 if the first line is in allChunks and is covered, the line must be included.
					 
					 I tried to move this to the head of this function!
				 */
//				for(int lineNoTmp = lineStmtRange.getLeft(); lineNoTmp <= lineStmtRange.getRight(); lineNoTmp ++){
//					// check if lineNoTmp is in other chunks
//					for(Chunk chunk : allChunks){
//						if(delOrAdd.equals("+") && chunk.getAddLineNos().contains(lineNoTmp)){ // has line in this chunk
//							// then check if this line is covered
//							Pair<Integer, String> curPair = chunk.getPairByLineNo(lineNoTmp, "+");
//							if (! linesSimplify.contains(pair)){
//								
//							}
//							break;
//						}
//					}
//				}
				
				
				// not in chunk, should be included
				return new Pair<String, Boolean>("true", true);
			}
			
		}else{ // is the first line of the stmt. should not be included
			return new Pair<String, Boolean>("false", false);
		}
		
		FileUtil.raiseException("[isSubAndShouldBeIncluded] return null.");
		return null;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param firstLinePair
	 * @param linesSimplify 
	 * @return
	 */
	private boolean checkLinePairInAlreadySimplify(Pair<Integer, String> firstLinePair, List<Pair<Integer, String>> linesSimplify) {
		for(Chunk chunk : simplifiedChunks){
			if(chunk.getLines().contains(firstLinePair)){
				return true;
			}
		}
		
		// bug fix: by time 5, sometimes the lines are still not added into simplifiedChunks. so we need to make such check.
		if (linesSimplify.contains(firstLinePair)){
			return true;
		}
		
		return false;
	}

	public Pair<Integer, Integer> getCurStmtRange(String filePath, int lineNo, Pair<String, Node> stmtInfo){
		Pair<Integer, Integer> lineRange = new Pair<Integer, Integer> (null, null);
		logger.debug("[getCurStmtRange]");
		
		// debug for time 1
//		if (lineNo == 221){
//			lineNo = 223;
//		}
		int lineNo2 = lineNo;
		
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof Statement){
					Statement stmt = (Statement) n;
					int startRow = stmt.getBegin().get().line;
					int endRow = stmt.getEnd().get().line;
					
					if (lineNo2 >= startRow && lineNo2 <= endRow){
						// need to first init
						if (lineRange.getLeft() == null && lineRange.getRight() == null){
							logger.debug("null init. lineNo: {}, start: {}, end:{}", lineNo2, startRow, endRow);
							lineRange.setLeft(startRow);
							lineRange.setRight(endRow);
						}
						
						// check if smaller
						if (lineRange.getLeft() <= startRow && endRow <= lineRange.getRight()){
							logger.debug("lineNo: {}, start: {}, end:{}", lineNo2, startRow, endRow);
							lineRange.setLeft(startRow);
							lineRange.setRight(endRow);
							
							String type = stmt.getClass().toString();
							stmtInfo.setLeft(type);
							stmtInfo.setRight(stmt);
							if (stmt instanceof BlockStmt){
								stmtInfo.setLeft("BlockStmt");
							}
						}
					}
				}
			};
			cu.walk(TreeTraversal.PREORDER, cuConsumer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (lineRange.getLeft() == null || lineRange.getRight() == null){
//			FileUtil.raiseException("lineRange.getLeft() == null || lineRange.getRight() == null");
			return null;
		}
		
		return lineRange;
	}
	
	public void showCurNode(String filePath, int lineNo, Pair<String, String> stmtInfo){
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof Node){
					int startRow = n.getBegin().get().line;
					int endRow = n.getEnd().get().line;
					
					if (lineNo >= startRow && lineNo <= endRow){
						FileUtil.writeToFileWithFormat("[getCurNodeRange] node: %s", n.getClass().toString());
					}
				}
			};
			cu.walk(TreeTraversal.PREORDER, cuConsumer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Pair<Boolean, Boolean> getCurMethodRange(String filePath, int lineNo){
		Pair<Pair<Integer, Integer>, MethodDeclaration> lineRange = new Pair<Pair<Integer, Integer>, MethodDeclaration> (new Pair<Integer, Integer> (null, null), null);
		logger.debug("[getCurMethodRange]");
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof MethodDeclaration){
					MethodDeclaration md = (MethodDeclaration) n;
					int startRow = md.getBegin().get().line;
					int endRow = md.getEnd().get().line;
					
					if (lineNo >= startRow && lineNo <= endRow){
						// need to first init
						if (lineRange.getLeft().getLeft() == null && lineRange.getLeft().getRight() == null){
							logger.debug("null init. lineNo: {}, start: {}, end:{}", lineNo, startRow, endRow);
							lineRange.getLeft().setLeft(startRow);
							lineRange.getLeft().setRight(endRow);
							lineRange.setRight(md);
						}
						
						// check if smaller
						if (lineRange.getLeft().getLeft() <= startRow && endRow <= lineRange.getLeft().getRight()){
							logger.debug("lineNo: {}, start: {}, end:{}", lineNo, startRow, endRow);
							lineRange.getLeft().setLeft(startRow);
							lineRange.getLeft().setRight(endRow);
							lineRange.setRight(md);
						}
					}
				}
			};
			cu.walk(TreeTraversal.PREORDER, cuConsumer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (lineRange.getLeft().getLeft() == null || lineRange.getLeft().getRight() == null){
//			FileUtil.raiseException("lineRange.getLeft() == null || lineRange.getRight() == null");
			return null;
		}
		
		// judge
//		boolean isFirstLineOfMethod = false;
//		if (lineRange.getLeft().equals(lineNo)){
//			isFirstLineOfMethod = true;
//		}
		// exposed by lang 23  @override.
		// get the block stmt of this method
		boolean isNotInBlock = false; // if true,should be included!
		MethodDeclaration md = lineRange.getRight();
		if (md.getBody().isPresent()){
			BlockStmt bs = md.getBody().get();
			int startRow = bs.getBegin().get().line;
			int endRow = bs.getEnd().get().line;
			if (lineNo > endRow || lineNo < startRow){
				isNotInBlock = true;
			}else{
				//another situation:
				if (bs.getChildNodes().isEmpty()){
					isNotInBlock = true;
				}else{
					Node firstNode = bs.getChildNodes().get(0);
					if (lineNo < firstNode.getBegin().get().line){
						isNotInBlock = true;   //is before than the first node of the blockstmt of the md.
					}
				}
			}
		}else{
			isNotInBlock = true; // has no block, should be included
		}
		
		boolean isInMethod = true;
		
		return new Pair<Boolean, Boolean>(isInMethod, isNotInBlock); //isFirstLineOfMethod
	}
	
//	public Pair<Boolean, Boolean> isInMethod(String filePath, int lineNo){
//		Pair<Boolean, Boolean> isInMethod = new Pair<Boolean, Boolean>(false, false);
//		try {
//			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
//			
//			Consumer<Node> cuConsumer = n -> {
//				if (n instanceof MethodDeclaration){
//					int startRow = n.getBegin().get().line;
//					int endRow = n.getEnd().get().line;
//					
//					if (lineNo >= startRow && lineNo <= endRow){
//						isInMethod.setLeft(true);
//					}
//				}
//			};
//			cu.walk(TreeTraversal.PREORDER, cuConsumer);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		
//		return isInMethod.getLeft();
//	}
	

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param sl
	 * @param testToStmts2
	 * @param pair 
	 * @return
	 */
	private boolean isInFailingTestTrace(SuspiciousLocation sl,
			Map<TestCaseResult, List<SuspiciousLocation>> testToStmts2, Pair<Integer, String> pair) {
		boolean covered = false;
		
		List<String> coveredTests = new ArrayList<>();
		
		if (Main.reRun){
//			for (String fCase : FileUtil.oriFailedTests){ // bug fix. for re-run. time 5
//				TestCaseResult tcr = new TestCaseResult(fCase, false);
//				List<SuspiciousLocation> slList = testToStmts2.get(tcr);
//				if (slList.contains(sl)){
//					covered = true;
//					coveredTests.add(fCase);
//				}
//			}
			covered = checkCoveredByTests(sl, testToStmts2, FileUtil.oriFailedTests, coveredTests);// bug fix. for re-run. time 5
		}else{
			covered = checkCoveredByTests(sl, testToStmts2, FileUtil.gzFailingTestCases, coveredTests);
		}
		
		if (covered){
			FileUtil.writeToFileWithFormat("Chunk line: %s is covered by %s test cases:\n%s\nChunk line end.", pair, coveredTests.size(), 
					GeneralUtil.listToStringAddLineBreak(coveredTests));
		}
		
		return covered;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 * @param sl 
	 * @param gzFailingTestCases 
	 * @param testToStmts2 
	 * @param coveredTests 
	 *
	 */
	private boolean checkCoveredByTests(SuspiciousLocation sl, Map<TestCaseResult, List<SuspiciousLocation>> testToStmts2, List<String> testCases, List<String> coveredTests) {
		boolean covered = false;
		for (String fCase : testCases){
			TestCaseResult tcr = new TestCaseResult(fCase, false);
			List<SuspiciousLocation> slList = testToStmts2.get(tcr);
			
			// lang 30
			if (slList == null){
				FileUtil.writeToFileWithFormat("slList == null fCase: %s. continue.", fCase);
				continue;
			}
			
			if (slList.contains(sl)){
				covered = true;
				coveredTests.add(fCase);
			}
		}
		
		return covered;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 16, 2020
	 *
	 * @param left
	 * @return
	 */
	private boolean isNotMeaningfulCodeLine(String lineCode) {
		String line = lineCode.substring(1).trim();
		if (line.equals("{") || line.equals("}")){
			return true;
		}
		
		return false;
	}

	public String getMutFolder() {
		return mutFolder;
	}

	public void setMutFolder(String mutFolder) {
		this.mutFolder = mutFolder;
	}

	public List<Chunk> getChunks() {
		return chunks;
	}

	public void setChunks(List<Chunk> chunks) {
		this.chunks = chunks;
	}

	public Map<TestCaseResult, List<SuspiciousLocation>> getTestToStmts() {
		return testToStmts;
	}

	public void setTestToStmts(Map<TestCaseResult, List<SuspiciousLocation>> testToStmts) {
		this.testToStmts = testToStmts;
	}

	public Map<SuspiciousLocation, List<TestCaseResult>> getStmtToTests() {
		return stmtToTests;
	}

	public void setStmtToTests(Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests) {
		this.stmtToTests = stmtToTests;
	}

	public Map<TestCaseResult, List<SuspiciousLocation>> getTestToStmtsPatch() {
		return testToStmtsPatch;
	}

	public void setTestToStmtsPatch(Map<TestCaseResult, List<SuspiciousLocation>> testToStmtsPatch) {
		this.testToStmtsPatch = testToStmtsPatch;
	}

	public Map<SuspiciousLocation, List<TestCaseResult>> getStmtToTestsPatch() {
		return stmtToTestsPatch;
	}

	public void setStmtToTestsPatch(Map<SuspiciousLocation, List<TestCaseResult>> stmtToTestsPatch) {
		this.stmtToTestsPatch = stmtToTestsPatch;
	}

	public List<Chunk> getSimplifiedChunks() {
		return simplifiedChunks;
	}

	public void setSimplifiedChunks(List<Chunk> simplifiedChunks) {
		this.simplifiedChunks = simplifiedChunks;
	}

	public List<Pair<Integer, String>> getSimpliedLines() {
		return simplifiedLines;
	}

	public void setSimpliedLines(List<Pair<Integer, String>> simpliedLines) {
		this.simplifiedLines = simpliedLines;
	}

	public List<Pair<Integer, String>> getDeltaLines() {
		return deltaLines;
	}

	public void setDeltaLines(List<Pair<Integer, String>> deltaLines) {
		this.deltaLines = deltaLines;
	}

	public List<Chunk> getDeltaChunks() {
		return deltaChunks;
	}

	public void setDeltaChunks(List<Chunk> deltaChunks) {
		this.deltaChunks = deltaChunks;
	}

	public String getSrcFolder() {
		return srcFolder;
	}

	public void setSrcFolder(String srcFolder) {
		this.srcFolder = srcFolder;
	}

	public String getPatchFolder() {
		return patchFolder;
	}

	public void setPatchFolder(String patchFolder) {
		this.patchFolder = patchFolder;
	}

	public List<Chunk> getChunksBkForDDMin() {
		return chunksBkForDDMin;
	}

	public void setChunksBkForDDMin(List<Chunk> chunksBkForDDMin) {
		this.chunksBkForDDMin = chunksBkForDDMin;
	}
}

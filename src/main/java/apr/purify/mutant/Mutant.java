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

public class Mutant {
	final static Logger logger = LoggerFactory.getLogger(Mutant.class);
	
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
	
	public Mutant(String srcFolder, String patchFolder, String mutFolder, List<Chunk> chunks, Map<TestCaseResult, List<SuspiciousLocation>> testToStmts,
			Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests, Map<TestCaseResult, List<SuspiciousLocation>> testToStmtsPatch, Map<SuspiciousLocation, List<TestCaseResult>> stmtToTestsPatch){
		this.srcFolder = srcFolder;
		this.patchFolder = patchFolder;
		
		this.mutFolder = mutFolder;
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
		
		
		useChunksBk = false;  
		simplifyChunks(); 
		applyChunks(); 
	}
	
	private void applyChunks() {
		long ddStartT = System.currentTimeMillis();
		
		FileUtil.writeToFileWithFormat("simplifiedChunks start: ");
		MutantUtil.logChunks(simplifiedChunks);
		FileUtil.writeToFileWithFormat("simplifiedChunks end.");
		
		List<Pair<Integer, String>> simplifiedLinesBkForDDMin = MutantUtil.getLinesFromChunks(chunksBkForDDMin);

		simplifiedLines = MutantUtil.getLinesFromChunks(simplifiedChunks);
		boolean runOriginal = DeltaDebugging.assertBeforeMut(simplifiedLines, simplifiedChunks, "patchAfterCov.diff", "[ddmin] original patch version does not pass!");
		boolean useBk = false;
		if (! runOriginal){ 
			FileUtil.writeToFile("Coverage analysis leads to the failure of patch fail before mutation!");
			boolean runOriginal2 = DeltaDebugging.assertBeforeMut(simplifiedLinesBkForDDMin, chunksBkForDDMin, "patchAfterCov.diff", "[ddmin] original patch version does not pass!");
			if (!runOriginal2){
				FileUtil.raiseException("[ddmin] original patch version does not pass!");
			}else{
				FileUtil.writeToFile("Use backup lines and chunks for ddmin now!");
				useBk = true;
			}
		}
	
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
		
		
		boolean runDDAfter = DeltaDebugging.assertBeforeMut(deltaLines, deltaChunks, "patchAfterRunDD.diff", "[ddmin] purified patch version does not pass!");
		if (!runDDAfter){
			FileUtil.writeToFile("runDDAfter is false! the pacth after dd cannot pass!\n");
			if (runOriginal){
				FileUtil.writeToFile("purified patch is simplifiedChunks\n");
				deltaChunks.clear();
				deltaChunks.addAll(simplifiedChunks);
				deltaLines.clear();
				deltaLines.addAll(simplifiedLines);
			}else{ 
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

		FileUtil.writeToFileWithFormat("[time cost] of dd: %s", FileUtil.countTime(ddStartT));
	}
	

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

	public void simplifyChunks(){
		
		long simplifyStartT = System.currentTimeMillis();
		
		FileUtil.writeToFile("[simplifyChunks] original chunks before simplifyChunks:\n");
		for(Chunk chunk:chunks){
			FileUtil.writeToFileWithFormat("%s", chunk);
		}
		
		simplifiedChunks.clear(); 
		int simplifyCnt = 0;
		
		for (Chunk chunk : chunks){
			String clazz = chunk.getClazz().replaceAll("/", "\\.");
			List<Pair<Integer, String>> lines = chunk.getLines();
			List<Pair<Integer, String>> linesSimplify = new ArrayList<>();
			
			for (Pair<Integer, String> pair : lines){
				SuspiciousLocation sl = new SuspiciousLocation(clazz, pair.getLeft());
				if (chunk.isDel(pair)){ 
					if (isInFailingTestTrace(sl, testToStmts, pair)){
						linesSimplify.add(pair);
					}
					else if (checkIsSubseqStatementAndShouldBeIncluded(pair, chunk, linesSimplify, chunks).getRight()){
						linesSimplify.add(pair);
					}else{
						FileUtil.writeToFileWithFormat("deleted chunk line: %s is not covered by any failing test. [end]", pair);
						simplifyCnt ++;
					}
				}else{ 
					if (isInFailingTestTrace(sl, testToStmtsPatch, pair)){
						linesSimplify.add(pair);
					}
					else if (checkIsSubseqStatementAndShouldBeIncluded(pair, chunk, linesSimplify, chunks).getRight()){
						linesSimplify.add(pair);
					}
					else{
						FileUtil.writeToFileWithFormat("added chunk line: %s is not covered by any failing test.", pair);
						simplifyCnt ++;
					}
				}
			}
			
			if (!linesSimplify.isEmpty()){
				chunk.setLines(linesSimplify); 
				chunk.resetByLines();
				simplifiedChunks.add(chunk);
			}
		}
		FileUtil.writeToFileWithFormat("[time cost] of simplifyChunks: %s", FileUtil.countTime(simplifyStartT));
		FileUtil.writeToFileWithFormat("[simplifyChunks] Number of simplified lines: %s", simplifyCnt);
		
		updateSimplifyChunks(simplifiedChunks, chunksBk);
	}

	private void updateSimplifyChunks(List<Chunk> simplifiedChunks, List<Chunk> chunksBk) {
		
		List<Pair<Integer, String>> simpLines = new ArrayList<>();
		for (Chunk simpChunk : simplifiedChunks){
			simpLines.addAll(simpChunk.getLines());
		}
		
		List<Chunk> chunkList = updateSimpChunkByLines(simpLines, chunksBk);
		
		simplifiedChunks.clear();
		simplifiedChunks.addAll(chunkList); 
	}

	private List<Chunk> updateSimpChunkByLines(List<Pair<Integer, String>> simpLines, List<Chunk> chunks) {
		List<Chunk> simpChunks = new ArrayList<>();
		
		for (Chunk chunk : chunks){
			Chunk remainedChunk = new Chunk();
			boolean addLineHasBeenExcluded = false;
			
			for (Pair<Integer, String> line : chunk.getLines()){
				if (simpLines.contains(line)){
					remainedChunk.getLines().add(line);
				}else{
					if (chunk.isDel(line)){
						String commentedLine = line.getRight().substring(0, 1) + Configuration.COMMENT + line.getRight().substring(1);
						Pair<Integer, String> lineCp = new Pair<Integer, String>(line.getLeft(), line.getRight());
						lineCp.setRight(commentedLine);
						remainedChunk.getLines().add(lineCp);
					}else{ 
						addLineHasBeenExcluded = true;
					}
				}
			}
			
			if (!remainedChunk.getLines().isEmpty()){
				remainedChunk.setClazz(chunk.getClazz());
				remainedChunk.setReplaceRange(chunk.replaceRange);
				
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


	private Pair<String, Boolean> checkIsSubseqStatementAndShouldBeIncluded(Pair<Integer, String> pair, Chunk chunk, List<Pair<Integer, String>> linesSimplify, List<Chunk> allChunks) {
		Pair<String, Boolean> boolPair = null;
		

		String lineCode = pair.getRight().substring(1).trim();
		String allAlphabets = lineCode.replaceAll( "[^a-z^A-Z]", "");
		if (allAlphabets.contains("elseif")){
			FileUtil.writeToFile("allAlphabets.contains(\"elseif\")");
		}
		if (allAlphabets.equals("else")){ 
			
			allAlphabets = lineCode.replaceAll("\\s+"," ");
			if (allAlphabets.contains("else")){ 
				FileUtil.writeToFileWithFormat("\n\nthe chunk line is an else or else if: %s\n", pair);
				
				int lineNo = pair.getLeft();
				if (chunk.isDel(pair)){
					String filePath = srcFolder + "/" + chunk.getClazz() + ".java";
					int ifStmtFirstLineNo = getIfStmtFirstLineNo(filePath, lineNo);
					
					Pair<Integer, String> ifFirstChunkLine = checkLineNoInAllChunks(ifStmtFirstLineNo, allChunks, true);
					if (ifFirstChunkLine != null){
						if (checkLinePairInAlreadySimplify(ifFirstChunkLine, linesSimplify)){
							boolPair = new Pair<String, Boolean>("false", true);
						}else{
							boolPair = new Pair<String, Boolean>("false", false);
						}
					}else{
						
						boolPair = new Pair<String, Boolean>("false", true);
					}
				}else{
					String filePath = patchFolder + "/" + chunk.getClazz() + ".java";
					int ifStmtFirstLineNo = getIfStmtFirstLineNo(filePath, lineNo);
					
					Pair<Integer, String> ifFirstChunkLine = checkLineNoInAllChunks(ifStmtFirstLineNo, allChunks, false); 
					if (ifFirstChunkLine != null){
						if (checkLinePairInAlreadySimplify(ifFirstChunkLine, linesSimplify)){
							boolPair = new Pair<String, Boolean>("false", true);
						}else{
							boolPair = new Pair<String, Boolean>("false", false);
						}
					}else{
						
						boolPair = new Pair<String, Boolean>("false", true);
					}
				}
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}
		
		Pair<String, Node> stmtInfo = new Pair<String, Node> (null, null);
		
		int lineNo = pair.getLeft();
		String filePath;
		String delOrAdd;
		List<Pair<Integer, String>> delOrAddLines = new ArrayList<>();
		if (chunk.isDel(pair)){ 
			filePath = srcFolder + "/" + chunk.getClazz() + ".java";
			delOrAdd = "-";
			delOrAddLines = chunk.getDelLines();
		}else{ 
			filePath = patchFolder + "/" + chunk.getClazz() + ".java";
			delOrAdd = "+";
			delOrAddLines = chunk.getAddLines();
		}
		Pair<Boolean, Boolean> isInMethod = getCurMethodRange(filePath, lineNo); 
		if (isInMethod == null){ 
			boolPair = new Pair<String, Boolean>("notInAnyMethod", true);
			FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
			return boolPair;
		}else{
			if (isInMethod.getRight()){ 
				boolPair = new Pair<String, Boolean>("inMethodButNotInBlock", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}
		
		
		
		Pair<Integer, Integer> lineRange = getCurStmtRange(filePath, lineNo, stmtInfo);

		/*if(stmtInfo.getLeft().equals("BlockStmt")){
			FileUtil.writeToFileWithFormat("check if curLine: %s is before this block stmt.", pair);
			boolean isBeforeBlockStmt = isBeforeBlockStmt((BlockStmt) stmtInfo.getRight(), pair);
			if (isBeforeBlockStmt){
				boolPair = new Pair<String, Boolean>("isBeforeBlockStmt", true);
				FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
				return boolPair;
			}
		}*/
		
		boolPair = isSubAndShouldBeIncluded(pair, lineRange, delOrAddLines, linesSimplify, delOrAdd, allChunks, stmtInfo);
		
		FileUtil.writeToFileWithFormat("chunk line: %s is not covered by any failing test. It is a sub-stmt? %s It should be included? %s [end]", pair, boolPair.getLeft(), boolPair.getRight());
		return boolPair;
	}
	

	private boolean isBeforeBlockStmt(BlockStmt bs, Pair<Integer, String> pair) {
		if (bs.getChildNodes().isEmpty()){
			FileUtil.writeToFile("bs.getChildNodes().isEmpty()");
			return true; 
		}else{
			Node node = bs.getChildNodes().get(0);
			int startRow = node.getBegin().get().line;
			// int endRow = node.getEnd().get().line;

			
			if (pair.getLeft() < startRow){
				// FileUtil.writeToFileWithFormat("DebugIsBeforeBlockStmt: %s %s %s %s %s", pair.getLeft(), pair.getRight(), startRow, endRow, bs.toString());
				return true; 
			}
		}
		
		return false;
	}


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


	private int getIfStmtFirstLineNo(String filePath, int lineNo) {

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


	private Pair<String, Boolean> isSubAndShouldBeIncluded(Pair<Integer, String> linePair, Pair<Integer, Integer> lineStmtRange, List<Pair<Integer, String>> chunkLines, 
			List<Pair<Integer, String>> linesSimplify, String delOrAdd, List<Chunk> allChunks, Pair<String, Node> stmtInfo) {
		
		boolean isLastLineInBlock = stmtInfo.getLeft().equals("BlockStmt") && linePair.getLeft().equals(lineStmtRange.getRight()); 
		if (isLastLineInBlock){ 
			
			boolean isInAllChunks = false;
			for (Chunk chunk : allChunks){
				Pair<Integer, String> firstLinePair = chunk.getPairByLineNo(lineStmtRange.getLeft(), delOrAdd);
				if (firstLinePair != null){
					isInAllChunks = true;
					
					if (checkLinePairInAlreadySimplify(firstLinePair, linesSimplify)){
						return new Pair<String, Boolean>("inBlockStmt", true);
					}else{
						return new Pair<String, Boolean>("inBlockStmt", false);
					}
				}
			}
			
			if(!isInAllChunks){
				useChunksBk = true;
				FileUtil.writeToFileWithFormat("useChunksBk = true; for linePair: %s", linePair);

			}
		}
		
		

		int lineNo = linePair.getLeft();
		if (lineNo != lineStmtRange.getLeft()){ 
			int firstLineNo = lineStmtRange.getLeft(); 
			
			boolean isInChunk = false;
			for (Pair<Integer, String> line : chunkLines){
				if (line.getLeft().equals(firstLineNo)){ 
					isInChunk = true;
					
					boolean isInSimplify = false;
					for (Pair<Integer, String> simpLine : linesSimplify){
						if (simpLine.getRight().startsWith(delOrAdd) && simpLine.getLeft() == firstLineNo){
							isInSimplify = true;
							break;
						}
					}
					
					if (isInSimplify){ 
						return new Pair<String, Boolean>("true", true);
					}else{ 
						return new Pair<String, Boolean>("true", false);
					}
				}
			}
			
			if (! isInChunk){
				return new Pair<String, Boolean>("true", true);
			}
			
		}else{ 
			return new Pair<String, Boolean>("false", false);
		}
		
		FileUtil.raiseException("[isSubAndShouldBeIncluded] return null.");
		return null;
	}


	private boolean checkLinePairInAlreadySimplify(Pair<Integer, String> firstLinePair, List<Pair<Integer, String>> linesSimplify) {
		for(Chunk chunk : simplifiedChunks){
			if(chunk.getLines().contains(firstLinePair)){
				return true;
			}
		}
		
		if (linesSimplify.contains(firstLinePair)){
			return true;
		}
		
		return false;
	}

	public Pair<Integer, Integer> getCurStmtRange(String filePath, int lineNo, Pair<String, Node> stmtInfo){
		Pair<Integer, Integer> lineRange = new Pair<Integer, Integer> (null, null);
		logger.debug("[getCurStmtRange]");
		

		int lineNo2 = lineNo;
		
		try {
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			
			Consumer<Node> cuConsumer = n -> {
				if (n instanceof Statement){
					Statement stmt = (Statement) n;
					int startRow = stmt.getBegin().get().line;
					int endRow = stmt.getEnd().get().line;
					
					if (lineNo2 >= startRow && lineNo2 <= endRow){
						
						if (lineRange.getLeft() == null && lineRange.getRight() == null){
							logger.debug("null init. lineNo: {}, start: {}, end:{}", lineNo2, startRow, endRow);
							lineRange.setLeft(startRow);
							lineRange.setRight(endRow);
						}
						
						
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
						
						if (lineRange.getLeft().getLeft() == null && lineRange.getLeft().getRight() == null){
							logger.debug("null init. lineNo: {}, start: {}, end:{}", lineNo, startRow, endRow);
							lineRange.getLeft().setLeft(startRow);
							lineRange.getLeft().setRight(endRow);
							lineRange.setRight(md);
						}
						
						
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

			return null;
		}
		
		boolean isNotInBlock = false; 
		MethodDeclaration md = lineRange.getRight();
		if (md.getBody().isPresent()){
			BlockStmt bs = md.getBody().get();
			int startRow = bs.getBegin().get().line;
			int endRow = bs.getEnd().get().line;
			if (lineNo > endRow || lineNo < startRow){
				isNotInBlock = true;
			}else{
				
				if (bs.getChildNodes().isEmpty()){
					isNotInBlock = true;
				}else{
					Node firstNode = bs.getChildNodes().get(0);
					if (lineNo < firstNode.getBegin().get().line){
						isNotInBlock = true;   
					}
				}
			}
		}else{
			isNotInBlock = true; 
		}
		
		boolean isInMethod = true;
		
		return new Pair<Boolean, Boolean>(isInMethod, isNotInBlock); 
	}
	private boolean isInFailingTestTrace(SuspiciousLocation sl,
			Map<TestCaseResult, List<SuspiciousLocation>> testToStmts2, Pair<Integer, String> pair) {
		boolean covered = false;
		
		List<String> coveredTests = new ArrayList<>();
		
		if (Main.reRun){

			covered = checkCoveredByTests(sl, testToStmts2, FileUtil.oriFailedTests, coveredTests);
		}else{
			covered = checkCoveredByTests(sl, testToStmts2, FileUtil.gzFailingTestCases, coveredTests);
		}
		
		if (covered){
			FileUtil.writeToFileWithFormat("Chunk line: %s is covered by %s test cases:\n%s\nChunk line end.", pair, coveredTests.size(), 
					GeneralUtil.listToStringAddLineBreak(coveredTests));
		}
		
		return covered;
	}


	private boolean checkCoveredByTests(SuspiciousLocation sl, Map<TestCaseResult, List<SuspiciousLocation>> testToStmts2, List<String> testCases, List<String> coveredTests) {
		boolean covered = false;
		for (String fCase : testCases){
			TestCaseResult tcr = new TestCaseResult(fCase, false);
			List<SuspiciousLocation> slList = testToStmts2.get(tcr);
			
			
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

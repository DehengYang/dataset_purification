package apr.purify.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gumtreediff.actions.model.Action;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import apr.purify.Configuration;
import apr.purify.utils.CmdUtil;
import apr.purify.utils.FileUtil;
import apr.purify.utils.GeneralUtil;
import apr.purify.utils.Pair;
import edu.lu.uni.serval.diffentry.DiffEntryHunk;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;

public class DiffUtil {
	final static Logger logger = LoggerFactory.getLogger(DiffUtil.class);
	
	public static void parseDiff(String patchDiffPath, List<Chunk> chunks) {
		
		
		logger.debug("current dir: {}", System.getProperty("user.dir"));
		

		String output = CmdUtil.runCmd(String.format("python2 %s %s", Configuration.diffPyPath, patchDiffPath));
		logger.debug("output: {}", output);
		
		
		List<Pair<String, String>> addLines = new ArrayList<>();
		List<Pair<String, String>> delLines = new ArrayList<>();
		List<Pair<String, String>> buggyLines = new ArrayList<>();
		String clazz = null;
		int chunkCnt = 0;
		for (String line : output.split("\n")){
			if (line.isEmpty()){
				continue;
			}
			
			
			if (line.startsWith("del-add info in this chunk of java class")){
				String[] split = line.split(" ");
				clazz = split[split.length - 1]; 
				
				if (addLines.size() + delLines.size() + buggyLines.size() > 0){
					setChunk(chunks, addLines, delLines, buggyLines, clazz);
				}else{
					if (chunkCnt != 0){
						FileUtil.raiseException("this pacth chunk has no diffs!");
					}else{
						
					}
				}
				
				chunkCnt ++;
				continue;
			}
			
			
			if (line.startsWith("buggy locations cnt: ")){
				int locCntFromPy = Integer.parseInt(FileUtil.getLastSplit(line, " "));
				FileUtil.writeToFileWithFormat("buggy locations cnt from getDiff2.py: %s", locCntFromPy);
				continue;
			}
			
			if (line.split(":").length < 3){ 
				logger.info("diffInfo line ({}) does not contain diff line", line);
				continue;
			}
			
			Pair<String, String> pair = parseLineOfDiff(line);
			
			if (line.startsWith("buggyLine:")){
				buggyLines.add(pair);
				continue;
			}
			if (line.startsWith("delLine:")){
				delLines.add(pair);
				continue;
			}
			if (line.startsWith("addLine:")){
				addLines.add(pair);
				continue;
			}
		}
		setChunk(chunks, addLines, delLines, buggyLines, clazz);
		
		
		FileUtil.writeToFileWithFormat("chunks size: %s, number of chunks in getDiff output: %s", chunks.size(), chunkCnt);
		printChunksInfo(chunks);
	} 
	
	public static void printChunksInfo(List<Chunk> chunks) {
		int addCnt = 0;
		int delCnt = 0;
		int bugCnt = 0;
		List<String> fileClasses = new ArrayList<>();
		for (Chunk chunk: chunks){
			addCnt += chunk.getAddLines().size();
			delCnt += chunk.getDelLines().size();
			bugCnt += chunk.getBuggyLines().size();
			if (! fileClasses.contains(chunk.getClazz())){
				fileClasses.add(chunk.getClazz());
			}
			FileUtil.writeToFileWithFormat("\nchunk info: %s", chunk);
		}
		FileUtil.writeToFileWithFormat("addCnt: %s, delCnt: %s, bugCnt: %s", addCnt, delCnt, bugCnt);
		for (String clazzTmp1 : fileClasses){
			FileUtil.writeToFileWithFormat("unique fileClazz: %s", clazzTmp1);
		}
	}

	private static void setChunk(List<Chunk> chunks, List<Pair<String, String>> addLines, List<Pair<String, String>> delLines,
			List<Pair<String, String>> buggyLines, String clazz) {
		Chunk chunk = new Chunk();
		chunk.setAddLines(getLineNos(addLines));
		chunk.setDelLines(getLineNos(delLines));
		chunk.setBuggyLines(getLineNos(buggyLines));
		chunk.setClazz(clazz);
		
		addLines.clear();
		delLines.clear();
		buggyLines.clear();
		
		chunks.add(chunk);
		
		logger.debug("chunk: {}", chunk);
	}

	private static List<Pair<Integer, String>> getLineNos(List<Pair<String, String>> addLines) {
		List<Pair<Integer, String>> lineNos = new ArrayList<>();
		
		for (Pair<String, String> pair : addLines){
			int lineNo = Integer.parseInt(pair.getLeft().split(":")[1]);
			lineNos.add(new Pair<Integer, String>(lineNo, pair.getRight()));
		}
		
		return lineNos;
	}

	public static void getAllChunks(List<Chunk> chunks, String patchDiffPath, String srcFolder){
		List<Pair<String, String>> patchPathMap = new ArrayList<>();
		
		
		List<String> diffLines = FileUtil.readFile(patchDiffPath);
		
		
		boolean isCode = false;
		int patchStartLine = -1;
		int bugStartLine = -1;
		String buggyFilePath = "";
		List<String> buggyCodeAll = new ArrayList<>();
		List<String> codeSnippet = new ArrayList<>();
		String patchFilePath = "";
		Chunk chunk = new Chunk();
		String clazz = null;
		
		
		for (int cnt = 0; cnt < diffLines.size(); cnt ++){
			String curLine = diffLines.get(cnt);
			
			
			if (curLine.startsWith("diff -Naur ")){ 
				isCode = false;
				continue;
			}
			
			
			if (curLine.startsWith("@@ ") && curLine.endsWith(" @@")){ 
				
				Pair<Integer, Integer> startLineNoPair = getStartLineNoPair(curLine);
				bugStartLine = startLineNoPair.getLeft() - 1;
				patchStartLine = startLineNoPair.getRight() - 1;
				
				
				if (diffLines.get(cnt - 1).startsWith("+++ ") 
					&& diffLines.get(cnt - 2).startsWith("--- ")){
					
					
					clazz = getClazzFromDiffLine(diffLines.get(cnt - 2));
					Pair<String, String> pair = getBuggyFilePath(diffLines.get(cnt - 2), srcFolder);
					String buggyFilePath2 = pair.getLeft();
					
					
					patchFilePath = pair.getRight();
					buggyFilePath = buggyFilePath2;
					
					
					buggyCodeAll = FileUtil.readFile(buggyFilePath);
					logger.debug("buggyFilePath: {}, lines size in buggyFilePath: {}", buggyFilePath, buggyCodeAll.size());
					isCode = true;
				}
				
				continue;
			}
			
			
			if (isCode){
				
				if (isAdd(curLine) || isDel(curLine)){
					if (!chunk.hasLine()){
						chunk.replaceRange.setLeft(bugStartLine);
					}
				}
				
				if (isDel(curLine)){
					bugStartLine ++;
					
					chunk.getLines().add(new Pair<Integer, String>(bugStartLine, curLine));
					chunk.getDelLines().add(new Pair<Integer, String>(bugStartLine, curLine));
					
				}else if (isAdd(curLine)){
					patchStartLine ++;
					
					chunk.getLines().add(new Pair<Integer, String>(patchStartLine, curLine));
					chunk.getAddLines().add(new Pair<Integer, String>(patchStartLine, curLine));
				}else{
					if(chunk.hasLine()){ 
						operateChunk(chunk, clazz);
						
						chunks.add(chunk);
						chunk = new Chunk(); 
					}
					
					bugStartLine ++;
					patchStartLine ++; 
				}
			}
		}
		
		if (chunk.hasLine()){
			operateChunk(chunk, clazz);
			
			chunks.add(chunk);
		}
		
		
		printChunksInfo(chunks);
	}
	
	private static void operateChunk(Chunk chunk, String clazz) {
		chunk.setClazz(clazz);
		if(! chunk.getDelLines().isEmpty()){
			int start = chunk.getDelLines().get(0).getLeft();
			int end = chunk.getDelLines().get(chunk.getDelLines().size() - 1).getLeft();
			chunk.replaceRange.setLeft(start);
			chunk.replaceRange.setRight(end);
		}
	}
	private static boolean isDel(String curLine) {
		return curLine.startsWith("-");
	}

	private static boolean isAdd(String curLine) {
		return curLine.startsWith("+");
	}
	private static Pair<Integer, Integer> getStartLineNoPair(String curLine) {
		int indexStart;
		int indexEnd;
		
		int bugStartLine;
		if (curLine.contains(",")){
			indexStart = curLine.indexOf("-");
			indexEnd = curLine.indexOf(",");
			bugStartLine = Integer.parseInt(curLine.substring(indexStart + 1, indexEnd));
		}else{
			indexStart = curLine.lastIndexOf("-");
			bugStartLine = Integer.parseInt(curLine.substring(indexStart + 1).split(" ")[0]);
		}
		
		int patchStartLine;
		if (curLine.contains(",")){
			indexStart = curLine.lastIndexOf("+");
			indexEnd = curLine.lastIndexOf(",");
			String str = curLine.substring(indexStart + 1, indexEnd);
			patchStartLine = Integer.parseInt(str);
		}else{
			indexStart = curLine.lastIndexOf("+");
			patchStartLine = Integer.parseInt(curLine.substring(indexStart + 1).split(" ")[0]);
		}
		
		return new Pair<Integer, Integer>(bugStartLine, patchStartLine);
	}

	public static List<Pair<String, String>>  produceJavaFilesFromDiff(String patchDiffPath, String srcFolder){
		List<Pair<String, String>> patchPathMap = new ArrayList<>();
		
		
		List<String> diffLines = FileUtil.readFile(patchDiffPath);
		
		
		boolean isCode = false;
		int patchStartLine = -1;
		String buggyFilePath = "";
		List<String> buggyCodeAll = new ArrayList<>();
		List<String> codeSnippet = new ArrayList<>();
		int buggyCodeSnippetSize = 0;
		String patchFilePath = "";
		
		
		for (int cnt = 0; cnt < diffLines.size(); cnt ++){
			String curLine = diffLines.get(cnt);
			
			
			if (curLine.startsWith("diff -Naur ")){ 
				isCode = false;
				continue;
			}
			
			
			if (curLine.startsWith("@@ ")){ 
				if (!codeSnippet.isEmpty()){
					writeCodeSnippet(codeSnippet, buggyCodeAll, patchStartLine, buggyCodeSnippetSize);
				}
				
				
				
				
				if (curLine.contains(",")){
					int indexStart = curLine.lastIndexOf("+");
					int indexEnd = curLine.lastIndexOf(",");
					patchStartLine = Integer.parseInt(curLine.substring(indexStart, indexEnd));
				}else{
					int indexStart = curLine.lastIndexOf("+");
					patchStartLine = Integer.parseInt(curLine.substring(indexStart).split(" ")[0]);
				}
				
				
				if (diffLines.get(cnt - 1).startsWith("+++ ") 
					&& diffLines.get(cnt - 2).startsWith("--- ")){
					
					
					Pair<String, String> pair = getBuggyFilePath(diffLines.get(cnt - 2), srcFolder);
					String buggyFilePath2 = pair.getLeft();
					
					
					if (!buggyFilePath.isEmpty()){ 
						codeSnippet.clear(); 
						buggyCodeSnippetSize = 0; 
						FileUtil.writeLinesToFile(patchFilePath, buggyCodeAll, false);
					}
					
					
					patchFilePath = pair.getRight();
					buggyFilePath = buggyFilePath2;
					patchPathMap.add(new Pair<String, String>(buggyFilePath, patchFilePath)); 
					
					
					buggyCodeAll = FileUtil.readFile(buggyFilePath);
					logger.debug("buggyFilePath: {}, lines size in buggyFilePath: {}", buggyFilePath, buggyCodeAll.size());
					isCode = true;
				}else{
					
					codeSnippet.clear();
					buggyCodeSnippetSize = 0;
				}
				
				continue;
			}
			
			
			if (isCode){
				if (curLine.startsWith("-")){
					buggyCodeSnippetSize += 1;
					
				}else if (curLine.startsWith("+")){
					codeSnippet.add(curLine.substring(1));
				}else{
					buggyCodeSnippetSize += 1;
					
					if (curLine.startsWith(" ")){
						curLine = curLine.substring(1);
					}else{
						FileUtil.raiseException("diffLine: %s in patchDiff.txt: %s does not contain whitespace", curLine, patchDiffPath);
					}
					codeSnippet.add(curLine);
				}
			}
		}
		
		
		if (!codeSnippet.isEmpty()){
			writeCodeSnippet(codeSnippet, buggyCodeAll, patchStartLine, buggyCodeSnippetSize);
			FileUtil.writeLinesToFile(patchFilePath, buggyCodeAll,false);
		}
		
		
		for (Pair<String, String> pair : patchPathMap){
			FileUtil.writeToFileWithFormat("patchPathMap element: %s %s", pair.getLeft(), pair.getRight());
		}
		FileUtil.writeToFileWithFormat("\n---verify the patchPathMap---");
		String fileDiff = "";
		for (Pair<String, String> pair : patchPathMap){
			String cmd = String.format("diff -Naur %s %s", pair.getLeft(), pair.getRight());
			String output = CmdUtil.runCmd(cmd);
			fileDiff += output;
			FileUtil.writeToFileWithFormat("diffCmd: %s", cmd);
		}
		FileUtil.writeToFileWithFormat("\ntotalDiffOutput:\n", fileDiff);
		boolean equal = compareDiff(fileDiff, FileUtil.readFileToStr(patchDiffPath));
		if (!equal){
			FileUtil.raiseException("\n[verify patchFilePath diff info fail!]");
		}else{
			FileUtil.writeToFileWithFormat("\n[verify patchFilePath diff info pass]");
		}
		
		
		return patchPathMap;
	}
	private static boolean compareDiff(String newDiff, String oriDiff) {
		List<String> newDiffLines = Arrays.asList(newDiff.split("\n"));
		List<String> initOriDiffLines = Arrays.asList(oriDiff.split("\n"));
		
		List<String> oriDiffLines = new ArrayList<>();
		
		for(String line : initOriDiffLines){
			if (line.startsWith("diff -Naur ")){
				
			}else{
				oriDiffLines.add(line);
			}
		}
		
		
		if (newDiffLines.size() != oriDiffLines.size()){
			return false;
		}
		
		
		for (int i = 0; i < newDiffLines.size(); i++){
			String newLine = newDiffLines.get(i);
			String oriLine = oriDiffLines.get(i);
			
			
			if (newLine.startsWith("--- ") && oriLine.startsWith("--- ")){
				continue;
			}
			if (newLine.startsWith("+++ ") && oriLine.startsWith("+++ ")){
				continue;
			}
			
			if (! newLine.equals(oriLine)){
				return false;
			}
		}
		
		return true;
	}

	public static Pair<String, String> parseLineOfDiff(String line){
		int index = line.lastIndexOf("==line==");
		
		
		if (index < 0){ 
			int lineNo = Integer.parseInt(line.split(":")[2]); 
			String clazz = line.split(":")[1];
			return new Pair<>(clazz + ":" + lineNo, "");
		}
		
		
		String lineCode = line.substring(index + "==line==".length());
		String linePart = line.substring(0, index);
		int lineNo = Integer.parseInt(linePart.split(":")[2]); 
		String clazz = linePart.split(":")[1];
		
		return new Pair<>(clazz + ":" + lineNo, lineCode);
	}
	
	public static void writeCodeSnippet(List<String> codeSnippet, List<String> buggyCodeAll, int patchStartLine,
			int buggyCodeSnippetSize) {
		for (int cnt = 0; cnt < buggyCodeSnippetSize; cnt ++){
			logger.debug("removed lineNo: {} {}", cnt + patchStartLine, buggyCodeAll.get(patchStartLine - 1));
			buggyCodeAll.remove(patchStartLine - 1);
		}
		
		
		String codeStr = "";
		for(int cnt = 0; cnt < codeSnippet.size(); cnt ++){
			codeStr += codeSnippet.get(cnt) + "\n";
		}
		logger.debug("codeSnippet: {}", codeStr);
		
		buggyCodeAll.addAll(patchStartLine - 1, codeSnippet);
	}

	public static Pair<String, String> getBuggyFilePath(String line, String srcFolder) {
		String clazz = getClazzFromDiffLine(line);
		

		String buggyFilePath = srcFolder + "/" + clazz + ".java";
		String patchFilePath = FileUtil.toolDir + "/humanPatch/" + clazz + "-fixed.java";
		logger.debug("buggyFilePath: {}\npatchFilePath:{}", buggyFilePath, patchFilePath);
		if (!new File(buggyFilePath).exists()){
			FileUtil.raiseException(String.format("[getBuggyFilePath] buggyFilePath (%s) does not exist!", buggyFilePath));
		}
		
		return new Pair<String, String> (buggyFilePath, patchFilePath);
	}
	
	private static String getClazzFromDiffLine(String line) {
		int javaIndex = line.lastIndexOf(".java");
		String buggyFilePath = "";
		int index = -1;
		
		
		if (line.contains("/java/")){
			index = line.indexOf("/java/") + "/java/".length();
		}else if(line.contains("/src/")){
			index = line.indexOf("/src/") + "/src/".length();
		}else if(line.contains("/source/")){
			index = line.indexOf("/source/") + "/source/".length();
		}else{
			FileUtil.raiseException("[getBuggyFilePath] cannot get buggy file path!");
		}
		
		String clazz = line.substring(index, javaIndex);
		return clazz;
	}

	public static Map<Pair<String, String>, List<HierarchicalActionSet>> getGumTreeActions(List<Pair<String, String>> patchPathMap){
		Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = new HashMap<>();
		
		for(Pair<String, String> pair : patchPathMap){
			String buggyFilePath = pair.getLeft();
			String patchFilePath = pair.getRight();
			
			CompilationUnit cu = null;
			try {
				cu = StaticJavaParser.parse(new File(patchFilePath));
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			}
			
			String patchCodeStr = null;
			try {
				patchCodeStr = FileUtils.readFileToString(new File(patchFilePath));
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			logger.info("patch diff for two files: \n{} \n{}", buggyFilePath, patchFilePath);
			List<HierarchicalActionSet> actionSets = PatchParserUtil.parseChangedSourceCodeWithGumTree(buggyFilePath, patchFilePath);
			
			logger.info("Number of actions for this patchDiff: {}", actionSets.size());
			for (HierarchicalActionSet actionSet : actionSets){
				logger.debug("actionSet:\n{}\n", actionSet.toString());
				



				



				logger.debug("actionSet info:\n{}\n", actionSet.getInfo());
				
				Action act = actionSet.getAction();
				logger.debug("action info: pos: {}  length: {}, name: {}, substring: {}", act.getPosition(), act.getLength(), act.getName(),
						patchCodeStr.substring(act.getPosition(), act.getPosition() + act.getLength()));
				logger.info("getLineNumber: {} for position: {}", PatchParserUtil.getLineNumber(patchFilePath, act.getPosition()), act.getPosition());
				logger.info("getLineNumberFromCU: {} for position: {}", PatchParserUtil.getLineNumberFromCU(patchFilePath, act.getPosition()), act.getPosition());
			}
			
			actionsPerFileMap.put(new Pair<String, String>(buggyFilePath, patchFilePath), actionSets);
			
		}
		
		return actionsPerFileMap;
	}
	
	public static void getModifications(Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap) {
		for(Map.Entry<Pair<String, String>, List<HierarchicalActionSet>> entry : actionsPerFileMap.entrySet()){
			Pair<String, String> pair = entry.getKey(); 
			String buggyFilePath = pair.getLeft();
			String patchFilePath = pair.getRight();
			List<HierarchicalActionSet> actionSets = entry.getValue();
			
			List<FileModification> fileModifs = new ArrayList<>();
			for (HierarchicalActionSet actionSet : actionSets){
				fileModifs.add(new FileModification(buggyFilePath, patchFilePath, actionSet));
			}
		}
		
	}
	
	public static void applyChunksAsMutants(List<Chunk> chunks, String srcJavaDir, String targetDir) {
		applyAllChunksForPatch(chunks, srcJavaDir, targetDir);
	}
	
	public static void applyChunksAsMutants(List<Chunk> chunks, String srcJavaDir, String targetDir, String fileName) {
		applyAllChunksForPatch(chunks, srcJavaDir, targetDir, fileName);
	}

	public static void applyAllChunksForPatch(List<Chunk> chunks, String srcJavaDir, String targetDir) {
		applyAllChunksForPatch(chunks, srcJavaDir, targetDir, "");
	}
	
	public static void applyAllChunksForPatch(List<Chunk> chunks, String srcJavaDir, String targetDir, String fileName) {
		Map<String, List<Chunk>> chunksPerFileMap = getChunksPerFileMap(chunks);
		
		if (! fileName.isEmpty()){  
			FileUtil.removeFile(FileUtil.toolDir + "/" + fileName);
		}
		
		for (Map.Entry<String, List<Chunk>> entry : chunksPerFileMap.entrySet()){
			List<Chunk> chunksPerFile = entry.getValue();
			Chunk firstChunk = chunksPerFile.get(0);
			String buggyFilePath = srcJavaDir + "/" + firstChunk.getClazz() + ".java";
			String patchFilePath = targetDir + "/" + firstChunk.getClazz() + ".java";
			
			List<String> buggyLines = FileUtil.readFile(buggyFilePath);
			int chunksSize = chunksPerFile.size();
			
			String patchTotalStr = "";
			int nextOriCodeStartLineNo = 1;
			for (Chunk chunk : chunksPerFile){
				
				
				
				if (chunk.replaceRange.getRight() == null){
					String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, chunk.replaceRange.getLeft());
					patchTotalStr += oriCode;
					patchTotalStr += chunk.getAllChanges();
					nextOriCodeStartLineNo = chunk.replaceRange.getLeft() + 1;
				}else{ 
					String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, chunk.replaceRange.getLeft() - 1);
					patchTotalStr += oriCode;
					patchTotalStr += chunk.getAllChanges();
					nextOriCodeStartLineNo = chunk.replaceRange.getRight() + 1;
				}
			}
			
			String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, buggyLines.size());
			patchTotalStr += oriCode;
			
			FileUtil.writeToFile(patchFilePath, patchTotalStr, false);
			FileUtil.writeToFile(buggyFilePath, FileUtil.readFileToStr(buggyFilePath), false); 
			
			String cmd = String.format("diff -Naur %s %s", buggyFilePath, patchFilePath);
			String output = CmdUtil.runCmd(cmd);
			FileUtil.writeToFileWithFormat("\ndiffCmd: %s", cmd);
			FileUtil.writeToFileWithFormat("\noutput: %s\n\noutput end.", output);
			
			if (! fileName.isEmpty()){
				FileUtil.writeToFile(FileUtil.toolDir + "/" + fileName, output, true);
			}
		}
	}

	private static Map<String, List<Chunk>> getChunksPerFileMap(List<Chunk> chunks) {
		Map<String, List<Chunk>> chunksPerFileMap = new HashMap<>();
		
		List<Chunk> chunksPerFile = new ArrayList<>();
		for (Chunk chunk : chunks){



			
			if(chunksPerFile.isEmpty()){
				chunksPerFile.add(chunk);
				continue;
			}
			
			if(chunksPerFile.get(0).getClazz().equals(chunk.getClazz())){
				chunksPerFile.add(chunk);
			}else{
				operateChunksPerFileMap(chunksPerFile, chunksPerFileMap);
				chunksPerFile.add(chunk); 
			}
		}
		
		if (!chunksPerFile.isEmpty()){
			operateChunksPerFileMap(chunksPerFile, chunksPerFileMap);
		}
		
		return chunksPerFileMap;
	}

	private static void operateChunksPerFileMap(List<Chunk> chunksPerFile, Map<String, List<Chunk>> chunksPerFileMap) {
		Chunk chunk = chunksPerFile.get(0);
		List<Chunk> chunksPerFileCopy = new ArrayList<>();
		chunksPerFileCopy.addAll(chunksPerFile);
		chunksPerFileMap.put(chunk.getClazz(), chunksPerFileCopy);
		chunksPerFile.clear();
		
		logger.debug("size: {}", chunksPerFileMap.get(chunk.getClazz()).size());
	}

	public static void deployMutant(String srcFolder, String mutFolder) {
		String bkMutDir = GeneralUtil.removeSlash(mutFolder);
		boolean getMutDir = false; 
		for (int i = 0; i < Configuration.MAX_MUTANTS; i ++){
			String dir = String.format("%s_%s", bkMutDir, i);
			if (!new File(dir).exists()){
				bkMutDir =  dir;
				getMutDir = true;
				break;
			}
		}
		
		if (!getMutDir){
			FileUtil.raiseException("Fail to get a bkMutDir!");
		}
		
		try {
			FileUtils.copyDirectory(new File(mutFolder), new File(srcFolder));
			FileUtils.moveDirectoryToDirectory(new File(mutFolder), new File(bkMutDir), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void getOriAndPurifyActions(String patchFolder, List<Chunk> oriChunks, List<Chunk> deltaChunks) {
		
		FileUtil.mark("[getOriAndPurifyActions] get patchActionsMap");
		List<String> oriFileClasses = getFileClasses(oriChunks);
		Map<Chunk, List<HierarchicalActionSet>> patchChunkActionMap = new HashMap<>();
		Map<String, List<HierarchicalActionSet>> patchActionsMap = getActionsFromFileClasses(oriFileClasses, patchFolder, oriChunks, patchChunkActionMap);
		writeActionMapInfo(patchChunkActionMap, patchActionsMap);
		FileUtil.mark("[getOriAndPurifyActions] get patchActionsMap end");
		
		
		String purifyFolder = FileUtil.toolDir + "/purified";
		applyChunksAsMutants(deltaChunks, FileUtil.srcJavaDir, purifyFolder, "purifiedPatch.diff");
		
		
		FileUtil.mark("[getOriAndPurifyActions] get purifyActionsMap");
		List<String> deltaFileClasses = getFileClasses(deltaChunks);
		Map<Chunk, List<HierarchicalActionSet>> purifyChunkActionMap = new HashMap<>();
		Map<String, List<HierarchicalActionSet>> purifyActionsMap = getActionsFromFileClasses(deltaFileClasses, purifyFolder, deltaChunks, purifyChunkActionMap);
		writeActionMapInfo(purifyChunkActionMap, purifyActionsMap);
		FileUtil.mark("[getOriAndPurifyActions] get purifyActionsMap end");
	}

	public static void writeActionMapInfo(Map<Chunk, List<HierarchicalActionSet>> patchChunkActionMap,
			Map<String, List<HierarchicalActionSet>> patchActionsMap) {
		FileUtil.mark("[writeActionMapInfo]");
		for (Map.Entry<Chunk, List<HierarchicalActionSet>> entry : patchChunkActionMap.entrySet()){
			FileUtil.writeToFileWithFormat("Chunk: %s\nChunk end.", entry.getKey());
			FileUtil.writeToFileWithFormat("The chunk's hierarchicalActionSets: \n%s\nhierarchicalActionSets end.", GeneralUtil.listToStringAddLineBreak(entry.getValue()));
		}
		
		for (Map.Entry<String, List<HierarchicalActionSet>> entry : patchActionsMap.entrySet()){
			String fileClass = entry.getKey();
			List<HierarchicalActionSet> actionSets = entry.getValue();
			
			FileUtil.writeToFileWithFormat("Number of repair actions: %s", actionSets.size());
			int depth = getBiggestDepth(actionSets);
			FileUtil.writeToFileWithFormat("Biggest depth of repair actions: %s", depth);
		}
	}

	private static int getBiggestDepth(List<HierarchicalActionSet> actionSets) {
		int depth = 0;
		
		for(HierarchicalActionSet action : actionSets){
			int curDepth = getDepth(action);
			if (curDepth > depth){
				depth = curDepth;
			}
		}
		
		return depth;
	}

	private static int getDepth(HierarchicalActionSet action) {
		int depth = 1;
		
		String[] actionStr = action.toString().split("\n");
		
		boolean hasNull = false;
		
		if (actionStr[0].equals("null")){
			hasNull = true;
		}
		for(String line : actionStr){
			int curDepth = 1;
			while(line.startsWith("---")){
				curDepth += 1;
				line = line.substring("---".length());
			}
			
			if (curDepth > depth){
				depth = curDepth;
			}
		}
		
		if(hasNull){
			depth --;
		}
		
		return depth;
	}

	public static Map<String, List<HierarchicalActionSet>> getActionsFromFileClasses(List<String> fileClasses,
			String srcCodeFolder, List<Chunk> chunks, Map<Chunk, List<HierarchicalActionSet>> chunkActionMap) {
		Map<String, List<HierarchicalActionSet>> patchActionsMap = new HashMap<>();
		
		FileUtil.mark("[getActionsFromFileClasses]");
		FileUtil.writeToFileWithFormat("number of fileClasses: %s", fileClasses.size());
		
		
		for (String fileClass : fileClasses){
			String subFilePath = "/" + fileClass.replaceAll("\\.", "/") + ".java";
			String buggyPath = Configuration.srcJavaDirBk + subFilePath;
			String patchPath = srcCodeFolder + subFilePath;
			
			List<HierarchicalActionSet> actionSets = PatchParserUtil.parseChangedSourceCodeWithGumTree(buggyPath, patchPath);
			
			chunkActionMap.putAll(PatchParserUtil.getChunkActionMap(chunks, actionSets, new File(buggyPath), new File(patchPath)));
			
			FileUtil.writeToFileWithFormat("number of actions for fileClass (%s): %s", actionSets.size(), fileClasses);
			FileUtil.writeToFileWithFormat("its corresponding actionSets:\n%s\nits corresponding actionSets end.", GeneralUtil.listToStringAddLineBreakWithHeader("==actionSets start==", "==actionSets end==", actionSets));
			
			patchActionsMap.put(fileClass, actionSets);
		}
		
		return patchActionsMap;
	}

	public static List<String> getFileClasses(List<Chunk> deltaChunks) {
		List<String> fileClasses = new ArrayList<>();
		for (Chunk chunk : deltaChunks){
			String clazz = chunk.getClazz();
			if (! fileClasses.contains(clazz)){
				fileClasses.add(clazz);
			}
		}
		return fileClasses;
	}

	public static List<Chunk> updateChunkRange(Chunk chunk) {
		List<Chunk> updChunks = new ArrayList<>();
		
		if (chunk.replaceRange.getRight() != null){ 

			
			List<Pair<Integer, String>> tmpChunkLines = new ArrayList<>();
			int rangeStartLineNo = chunk.replaceRange.getRight();  
			for (Pair<Integer, String> line : chunk.getLines()){
				if (chunk.isCommentDel(line)){
					rangeStartLineNo = line.getLeft(); 
					
					if (tmpChunkLines.isEmpty()){
						continue;
					}
					
					
					Chunk newChunk = constructChunk(tmpChunkLines, chunk, line.getLeft() - 1);
					updChunks.add(newChunk);
					tmpChunkLines.clear();
				}else{
					tmpChunkLines.add(line);
				}
			}
			
			
			if (!tmpChunkLines.isEmpty()){
				Chunk newChunk = constructChunk(tmpChunkLines, chunk, rangeStartLineNo);
				updChunks.add(newChunk);
			}
		}
		
		return updChunks;
	}

	private static Chunk constructChunk(List<Pair<Integer, String>> tmpChunkLines, Chunk oriChunk, int insertAfterLineNo) {
		Chunk newChunk = new Chunk();
		newChunk.setClazz(oriChunk.getClazz());
		
		newChunk.setLines(tmpChunkLines);
		
		
		newChunk.resetByLines();
		List<Pair<Integer, String>> delLines = newChunk.getDelLines();
		if (delLines.isEmpty()){ 
			
			newChunk.replaceRange.setLeft(insertAfterLineNo); 
			newChunk.replaceRange.setRight(null);
		}else{ 
			int startLineNo = delLines.get(0).getLeft();
			int endLineNo = delLines.get(delLines.size() - 1).getLeft();
			newChunk.replaceRange.setLeft(startLineNo);
			newChunk.replaceRange.setRight(endLineNo);
		}
		
		return newChunk;
	}
	
}

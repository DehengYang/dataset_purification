/**
 * 
 */
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

/**
 * I create this class (DiffInfo) because I think current Diff class is lengthy enough. So I need to create a new class to save info I want,
 * such as chunks, addLines, modLines, files...
 * 
 * no... I now plan to consider this class as a util class. Therefore, I change the name from DiffInfo
 * into DiffUtil
 * @author apr
 * @version Oct 12, 2020
 *
 */
public class DiffUtil {
	final static Logger logger = LoggerFactory.getLogger(DiffUtil.class);
	
	// just a backup for assisting in confirming the results of produceJavaFilesFromDiff()
//	public static Map<String, String> addLinesBk = new HashMap<>();// line_no, real_code
//	public static Map<String, String> delLinesBk = new HashMap<>();
//	public static Map<String, String> modifiedLinesBk = new HashMap<>(); 
	
	/**
	 * @Description 
	 * @author apr
	 * @version Oct 8, 2020
	 *
	 * @param patchDiffPath
	 * @param chunks
	 */
	public static void parseDiff(String patchDiffPath, List<Chunk> chunks) {
		// python2 /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/libs/getDiff2.py  patchDiff.txt
		// /home/apr/apr_tools/datset_purification_2020/purification/purify/src/main/resources/getDiff2.py
		logger.debug("current dir: {}", System.getProperty("user.dir"));
		
//		String diffPyPath = FileUtil.readAndWriteResourceByPath("/getDiff2.py");
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
			
			//del-add info in this chunk of java class org.apache.wicket.markup.html.form.Form
			if (line.startsWith("del-add info in this chunk of java class")){
				String[] split = line.split(" ");
				clazz = split[split.length - 1]; // get chunk class name
				
				if (addLines.size() + delLines.size() + buggyLines.size() > 0){
					setChunk(chunks, addLines, delLines, buggyLines, clazz);
				}else{
					if (chunkCnt != 0){
						FileUtil.raiseException("this pacth chunk has no diffs!");
					}else{
						// is the first chunk.
					}
				}
				
				chunkCnt ++;
				continue;
			}
			
			// buggy locations cnt: 
			if (line.startsWith("buggy locations cnt: ")){
				int locCntFromPy = Integer.parseInt(FileUtil.getLastSplit(line, " "));
				FileUtil.writeToFileWithFormat("buggy locations cnt from getDiff2.py: %s", locCntFromPy);
				continue;
			}
			
			if (line.split(":").length < 3){ // not line
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
		
		// print chunk info
		FileUtil.writeToFileWithFormat("chunks size: %s, number of chunks in getDiff output: %s", chunks.size(), chunkCnt);
		printChunksInfo(chunks);
	} 
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param chunks
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 13, 2020
	 *
	 * @param chunk
	 * @param addLines
	 * @param delLines
	 * @param buggyLines
	 * @param clazz 
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param addLines
	 * @return
	 */
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
		
		// 1) get diff lines from patchDiff.txt
		List<String> diffLines = FileUtil.readFile(patchDiffPath);
		
		// 2) init
		boolean isCode = false;
		int patchStartLine = -1;
		int bugStartLine = -1;
		String buggyFilePath = "";
		List<String> buggyCodeAll = new ArrayList<>();
		List<String> codeSnippet = new ArrayList<>();
		String patchFilePath = "";
		Chunk chunk = new Chunk();
		String clazz = null;
		
		// 3) start to traverse diff lines
		for (int cnt = 0; cnt < diffLines.size(); cnt ++){
			String curLine = diffLines.get(cnt);
			
			// code snippet start
			if (curLine.startsWith("diff -Naur ")){ 
				isCode = false;
				continue;
			}
			
			// 4) get the start line of this chunk.
			if (curLine.startsWith("@@ ") && curLine.endsWith(" @@")){ 
				// 4.1) get bug and patch start lineNo
				Pair<Integer, Integer> startLineNoPair = getStartLineNoPair(curLine);
				bugStartLine = startLineNoPair.getLeft() - 1;
				patchStartLine = startLineNoPair.getRight() - 1;
				
				// 4.2) identify codeSnippet
				if (diffLines.get(cnt - 1).startsWith("+++ ") 
					&& diffLines.get(cnt - 2).startsWith("--- ")){
					//--- /mnt/benchmarks/bugs/Defects4J/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java	2020-03-01 00:02:32.146887487 -0800
					// 4.3) get buggyFile path and [patchFile path to write]
					clazz = getClazzFromDiffLine(diffLines.get(cnt - 2));
					Pair<String, String> pair = getBuggyFilePath(diffLines.get(cnt - 2), srcFolder);
					String buggyFilePath2 = pair.getLeft();
					
					// 4.5) change bug file path
					patchFilePath = pair.getRight();
					buggyFilePath = buggyFilePath2;
					
					// 4.6) read bug file to buggyCodeAll list
					buggyCodeAll = FileUtil.readFile(buggyFilePath);
					logger.debug("buggyFilePath: {}, lines size in buggyFilePath: {}", buggyFilePath, buggyCodeAll.size());
					isCode = true;
				}
				
				continue;
			}
			
			// 5) receive code snippet and count lines
			if (isCode){
				// get chunk replace range start
				if (isAdd(curLine) || isDel(curLine)){
					if (!chunk.hasLine()){
						chunk.replaceRange.setLeft(bugStartLine);
					}
				}
				
				if (isDel(curLine)){
					bugStartLine ++;//add 1
					
					chunk.getLines().add(new Pair<Integer, String>(bugStartLine, curLine));
					chunk.getDelLines().add(new Pair<Integer, String>(bugStartLine, curLine));
					//do nothing on codeSnippet, because the delLines should not be included in patchPathFile
				}else if (isAdd(curLine)){
					patchStartLine ++;//add 1
					
					chunk.getLines().add(new Pair<Integer, String>(patchStartLine, curLine));
					chunk.getAddLines().add(new Pair<Integer, String>(patchStartLine, curLine));
				}else{
					if(chunk.hasLine()){ // is next chunk.so should deal with current chunk
						operateChunk(chunk, clazz);
						
						chunks.add(chunk);
						chunk = new Chunk(); // re-init
					}
					
					bugStartLine ++;
					patchStartLine ++; //add 1
				}
			}
		}
		
		if (chunk.hasLine()){
			operateChunk(chunk, clazz);
			
			chunks.add(chunk);
		}
		
		// print chunks info
		printChunksInfo(chunks);
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param chunk
	 * @param clazz
	 */
	private static void operateChunk(Chunk chunk, String clazz) {
		chunk.setClazz(clazz);//buggyFilePath
		if(! chunk.getDelLines().isEmpty()){
			int start = chunk.getDelLines().get(0).getLeft();
			int end = chunk.getDelLines().get(chunk.getDelLines().size() - 1).getLeft();
			chunk.replaceRange.setLeft(start);
			chunk.replaceRange.setRight(end);
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param curLine
	 * @return
	 */
	private static boolean isDel(String curLine) {
		return curLine.startsWith("-");
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param curLine
	 * @return
	 */
	private static boolean isAdd(String curLine) {
		return curLine.startsWith("+");
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 *@@ -89,6 +89,7 @@
	@@ -1 +1 @@
	4.1) get the start line of this chunk. e.g., get 89 or 1
	 * @param curLine
	 * @return
	 */
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

	/**
	 * @Description
	 * produce patch file and buggy file
	 * <br>
	 * 1) parse patch diff.txt <br>
	 * 2) write patched code snippet (with context) into a patchFilePathToWrite <br>
	 * 3) save <buggyFilePath, patchFilePathToWrite> to patchPathMap, and return it.<br>
	 * 
	 * @author apr
	 * @version Oct 9, 2020
	 *
	 */
	public static List<Pair<String, String>>  produceJavaFilesFromDiff(String patchDiffPath, String srcFolder){
		List<Pair<String, String>> patchPathMap = new ArrayList<>();
		
		// 1) get diff lines from patchDiff.txt
		List<String> diffLines = FileUtil.readFile(patchDiffPath);
		
		// 2) init
		boolean isCode = false;
		int patchStartLine = -1;
		String buggyFilePath = "";
		List<String> buggyCodeAll = new ArrayList<>();
		List<String> codeSnippet = new ArrayList<>();
		int buggyCodeSnippetSize = 0;
		String patchFilePath = "";
		
		// 3) start to traverse diff lines
		for (int cnt = 0; cnt < diffLines.size(); cnt ++){
			String curLine = diffLines.get(cnt);
			
			// code snippet start
			if (curLine.startsWith("diff -Naur ")){ 
				isCode = false;
				continue;
			}
			
			// 4) get the start line of this chunk.
			if (curLine.startsWith("@@ ")){ 
				if (!codeSnippet.isEmpty()){
					writeCodeSnippet(codeSnippet, buggyCodeAll, patchStartLine, buggyCodeSnippetSize);
				}
				
				// @@ -89,6 +89,7 @@
				// @@ -1 +1 @@
				// 4.1) get the start line of this chunk. e.g., get 89 or 1
				if (curLine.contains(",")){
					int indexStart = curLine.lastIndexOf("+");
					int indexEnd = curLine.lastIndexOf(",");
					patchStartLine = Integer.parseInt(curLine.substring(indexStart, indexEnd));
				}else{
					int indexStart = curLine.lastIndexOf("+");
					patchStartLine = Integer.parseInt(curLine.substring(indexStart).split(" ")[0]);
				}
				
				// 4.2) identify codeSnippet
				if (diffLines.get(cnt - 1).startsWith("+++ ") 
					&& diffLines.get(cnt - 2).startsWith("--- ")){
					//--- /mnt/benchmarks/bugs/Defects4J/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java	2020-03-01 00:02:32.146887487 -0800
					// 4.3) get buggyFile path and [patchFile path to write]
					Pair<String, String> pair = getBuggyFilePath(diffLines.get(cnt - 2), srcFolder);
					String buggyFilePath2 = pair.getLeft();
					
					// 4.4) write code to patch file
					if (!buggyFilePath.isEmpty()){ // bug fix
						codeSnippet.clear(); //bug fix
						buggyCodeSnippetSize = 0; 
						FileUtil.writeLinesToFile(patchFilePath, buggyCodeAll, false);
					}
					
					// 4.5) change bug file path
					patchFilePath = pair.getRight();
					buggyFilePath = buggyFilePath2;
					patchPathMap.add(new Pair<String, String>(buggyFilePath, patchFilePath)); // add pair
					
					// 4.6) read bug file to buggyCodeAll list
					buggyCodeAll = FileUtil.readFile(buggyFilePath);
					logger.debug("buggyFilePath: {}, lines size in buggyFilePath: {}", buggyFilePath, buggyCodeAll.size());
					isCode = true;
				}else{
					// 4.7) is next code snippet, re-init
					codeSnippet.clear();
					buggyCodeSnippetSize = 0;
				}
				
				continue;
			}
			
			// 5) receive code snippet and count lines
			if (isCode){
				if (curLine.startsWith("-")){
					buggyCodeSnippetSize += 1;
					//do nothing on codeSnippet, because the delLines should not be included in patchPathFile
				}else if (curLine.startsWith("+")){
					codeSnippet.add(curLine.substring(1));
				}else{
					buggyCodeSnippetSize += 1;
					// should s
					if (curLine.startsWith(" ")){
						curLine = curLine.substring(1);
					}else{
						FileUtil.raiseException("diffLine: %s in patchDiff.txt: %s does not contain whitespace", curLine, patchDiffPath);
					}
					codeSnippet.add(curLine);
				}
			}
		}
		
		// 6) write final chunk to patchFilePath
		if (!codeSnippet.isEmpty()){
			writeCodeSnippet(codeSnippet, buggyCodeAll, patchStartLine, buggyCodeSnippetSize);
			FileUtil.writeLinesToFile(patchFilePath, buggyCodeAll,false);
		}
		
		// print info & verify patchFilePath
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
		
		// 7) return patchPathMap
		return patchPathMap;
	}
	
	/** @Description
	 * this is to verify if the patchPathFile is correctly written. 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param fileDiff
	 * @param readFileToStr
	 * @return
	 */
	private static boolean compareDiff(String newDiff, String oriDiff) {
		List<String> newDiffLines = Arrays.asList(newDiff.split("\n"));
		List<String> initOriDiffLines = Arrays.asList(oriDiff.split("\n"));
		
		List<String> oriDiffLines = new ArrayList<>();
		// as initOriDiffLines contains Diff -Naur line, but newDiff does not. so I remove these lines.
		for(String line : initOriDiffLines){
			if (line.startsWith("diff -Naur ")){
				//skip
			}else{
				oriDiffLines.add(line);
			}
		}
		
		// directly return false when sizes are different
		if (newDiffLines.size() != oriDiffLines.size()){
			return false;
		}
		
		// compare each line in the two list
		for (int i = 0; i < newDiffLines.size(); i++){
			String newLine = newDiffLines.get(i);
			String oriLine = oriDiffLines.get(i);
			
			// skip as the file name (especially the patchFilePath) might be different
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

	/**
	 * @Description
	 * parse the class:lineNo and its relevant code from a single line of getDiff2.py output
	 * @author apr
	 * @version Oct 9, 2020
	 *
	 * @param line
	 * @return
	 */
	public static Pair<String, String> parseLineOfDiff(String line){
		int index = line.lastIndexOf("==line==");
		
		// diff loc, i.e., buggyloc
		if (index < 0){ 
			int lineNo = Integer.parseInt(line.split(":")[2]); //line.split(":").length - 1
			String clazz = line.split(":")[1];
			return new Pair<>(clazz + ":" + lineNo, "");
		}
		
		// del or add loc
		String lineCode = line.substring(index + "==line==".length());
		String linePart = line.substring(0, index);
		int lineNo = Integer.parseInt(linePart.split(":")[2]); //line.split(":").length - 1
		String clazz = linePart.split(":")[1];
		
		return new Pair<>(clazz + ":" + lineNo, lineCode);
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 9, 2020
	 * write codeSnippet to buggyCodeAll.
	 * @param codeSnippet
	 * @param buggyCodeAll
	 * @param patchStartLine: start line
	 * @param buggyCodeSnippetSize: code snippet size.
	 */
	public static void writeCodeSnippet(List<String> codeSnippet, List<String> buggyCodeAll, int patchStartLine,
			int buggyCodeSnippetSize) {
		for (int cnt = 0; cnt < buggyCodeSnippetSize; cnt ++){
			logger.debug("removed lineNo: {} {}", cnt + patchStartLine, buggyCodeAll.get(patchStartLine - 1));
			buggyCodeAll.remove(patchStartLine - 1);
		}
		
		// debug
		String codeStr = "";
		for(int cnt = 0; cnt < codeSnippet.size(); cnt ++){
			codeStr += codeSnippet.get(cnt) + "\n";
		}
		logger.debug("codeSnippet: {}", codeStr);
		
		buggyCodeAll.addAll(patchStartLine - 1, codeSnippet);
	}

	/** @Description 
	 * @author apr
	 * @version Oct 9, 2020
	 * --- /mnt/benchmarks/bugs/Defects4J/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java	2020-03-01 00:02:32.146887487 -0800
	 * learn from getDiff2.py
	 * @param string
	 * @return
	 */
	public static Pair<String, String> getBuggyFilePath(String line, String srcFolder) {
		String clazz = getClazzFromDiffLine(line);
		
//		buggyFilePath = srcFolder + "/" + line.substring(index, javaIndex + ".java".length());
		String buggyFilePath = srcFolder + "/" + clazz + ".java";
		String patchFilePath = FileUtil.toolDir + "/humanPatch/" + clazz + "-fixed.java";
		logger.debug("buggyFilePath: {}\npatchFilePath:{}", buggyFilePath, patchFilePath);
		if (!new File(buggyFilePath).exists()){
			FileUtil.raiseException(String.format("[getBuggyFilePath] buggyFilePath (%s) does not exist!", buggyFilePath));
		}
		
		return new Pair<String, String> (buggyFilePath, patchFilePath);
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param line
	 * @return
	 */
	private static String getClazzFromDiffLine(String line) {
		int javaIndex = line.lastIndexOf(".java");
		String buggyFilePath = "";
		int index = -1;
		
		// java, src, source
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

	/**
	 * @Description
	 * obtain change actions for each patched file (maybe more than one patched file) 
	 * @author apr
	 * @version Oct 9, 2020
	 *
	 */
	public static Map<Pair<String, String>, List<HierarchicalActionSet>> getGumTreeActions(List<Pair<String, String>> patchPathMap){
		Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = new HashMap<>();
		
		for(Pair<String, String> pair : patchPathMap){
			String buggyFilePath = pair.getLeft();
			String patchFilePath = pair.getRight();
			
			CompilationUnit cu = null;
			try {
				cu = StaticJavaParser.parse(new File(patchFilePath));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String patchCodeStr = null;
			try {
				patchCodeStr = FileUtils.readFileToString(new File(patchFilePath));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			logger.info("patch diff for two files: \n{} \n{}", buggyFilePath, patchFilePath);
			List<HierarchicalActionSet> actionSets = PatchParserUtil.parseChangedSourceCodeWithGumTree(buggyFilePath, patchFilePath);
			// some attempts on actionSets
			logger.info("Number of actions for this patchDiff: {}", actionSets.size());
			for (HierarchicalActionSet actionSet : actionSets){
				logger.debug("actionSet:\n{}\n", actionSet.toString());
				
//				logger.debug("startPos: {}, startPosStr: {} === {}", actionSet.getStartPosition(), 
//						patchCodeStr.substring(actionSet.getStartPosition() - 20, actionSet.getStartPosition()),
//						patchCodeStr.substring(actionSet.getStartPosition(), actionSet.getStartPosition() + 20));
				
//				logger.debug(cu.getTokenRange().get().getBegin().toString());
//				logger.debug(cu.getTokenRange().get().getEnd().toString());
//				logger.debug(cu.getBegin().get().toString());
				logger.debug("actionSet info:\n{}\n", actionSet.getInfo());
				
				Action act = actionSet.getAction();
				logger.debug("action info: pos: {}  length: {}, name: {}, substring: {}", act.getPosition(), act.getLength(), act.getName(),
						patchCodeStr.substring(act.getPosition(), act.getPosition() + act.getLength()));
				logger.info("getLineNumber: {} for position: {}", PatchParserUtil.getLineNumber(patchFilePath, act.getPosition()), act.getPosition());
				logger.info("getLineNumberFromCU: {} for position: {}", PatchParserUtil.getLineNumberFromCU(patchFilePath, act.getPosition()), act.getPosition());
			}
			
			/*
			 * 
			 * to support actionSet.getInfo(), I change patchparser project and push a commit. 
			 * refer to:
			 * apr@apr:~/apr_tools/PatchParser$ git cm "some changes for data purification projects"
[master d7df006] some changes for data purification projects
 3 files changed, 49 insertions(+), 3 deletions(-)
apr@apr:~/apr_tools/PatchParser$ git pom
Enumerating objects: 29, done.
Counting objects: 100% (29/29), done.
Delta compression using up to 2 threads
Compressing objects: 100% (10/10), done.
Writing objects: 100% (15/15), 2.30 KiB | 2.30 MiB/s, done.
Total 15 (delta 6), reused 0 (delta 0), pack-reused 0
remote: Powered by GITEE.COM [GNK-5.0]
To gitee.com:dalewushuang/PatchParser.git
   dd4aacc..d7df006  master -> master


then run: 
apr@apr:~/apr_tools/PatchParser/Parser$ mvn clean install -DskipTests
			 */
			
			// for closure 32
//			logger.debug("codeSnippet: {}", patchCodeStr.substring(49155, 49155+473));
//			logger.debug("codeSnippet from cu: {}", cu.toString().substring(49155, 49155+473));
			
//			FileUtil.writeToFile(FileUtil.toolDir + "/test1.java", patchCodeStr, false);
//			FileUtil.writeToFile(FileUtil.toolDir + "/test2.java", cu.toString(), false);
			
			actionsPerFileMap.put(new Pair<String, String>(buggyFilePath, patchFilePath), actionSets);
			
		}
		
		return actionsPerFileMap;
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 9, 2020
	 *
	 * @param actionsPerFileMap2
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param chunks
	 */
	public static void applyAllChunksForPatch(List<Chunk> chunks, String srcJavaDir, String targetDir) {
		applyAllChunksForPatch(chunks, srcJavaDir, targetDir, "");
	}
	
	public static void applyAllChunksForPatch(List<Chunk> chunks, String srcJavaDir, String targetDir, String fileName) {
		Map<String, List<Chunk>> chunksPerFileMap = getChunksPerFileMap(chunks);
		
		if (! fileName.isEmpty()){  //init! keep clean  before writing
			FileUtil.removeFile(FileUtil.toolDir + "/" + fileName);
		}
		
		for (Map.Entry<String, List<Chunk>> entry : chunksPerFileMap.entrySet()){
			List<Chunk> chunksPerFile = entry.getValue();
			Chunk firstChunk = chunksPerFile.get(0);
			String buggyFilePath = srcJavaDir + "/" + firstChunk.getClazz() + ".java";
			String patchFilePath = targetDir + "/" + firstChunk.getClazz() + ".java";
			
			List<String> buggyLines = FileUtil.readFile(buggyFilePath);
			
			String patchTotalStr = "";
			int nextOriCodeStartLineNo = 1;
			for (Chunk chunk : chunksPerFile){
				// apply each chunk 
				
				// just add
				if (chunk.replaceRange.getRight() == null){
					String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, chunk.replaceRange.getLeft());
					patchTotalStr += oriCode;
					patchTotalStr += chunk.getAllChanges();
					nextOriCodeStartLineNo = chunk.replaceRange.getLeft() + 1;
				}else{ // del
					String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, chunk.replaceRange.getLeft() - 1);
					patchTotalStr += oriCode;
					patchTotalStr += chunk.getAllChanges();
					nextOriCodeStartLineNo = chunk.replaceRange.getRight() + 1;
				}
			}
			
			String oriCode = GeneralUtil.listToStringAddLineBreak(buggyLines, nextOriCodeStartLineNo, buggyLines.size());
			patchTotalStr += oriCode;
			
			FileUtil.writeToFile(patchFilePath, patchTotalStr, false);
			// I find the reason now: each has two \n...
			/*
			 * however, the original buggy file already have two \n for each line when reading file using python:
			 * 	refer to: /home/apr/apr_tools/datset_purification_2020/purification/purify/experiment/parser/test.py
			 * 	from utils import read_file_to_str
				bugFile = "/mnt/purify/repairDir/Purify_Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java"
				string = read_file_to_str(bugFile)
				print()
			 * so there is no way dealing with this at present. 
			 * Now I think this is just the problem of python, as it read diff file also with two \n even for the first diff header--- line and +++ line
			 */
			FileUtil.writeToFile(buggyFilePath, FileUtil.readFileToStr(buggyFilePath), false); // fix a bug: exposed by chart 2. the \n might lead to diff failure. so I overwrite buggyFile
			
			String cmd = String.format("diff -Naur %s %s", buggyFilePath, patchFilePath);
			String output = CmdUtil.runCmd(cmd);
			FileUtil.writeToFileWithFormat("\ndiffCmd: %s", cmd);
			FileUtil.writeToFileWithFormat("\noutput: %s\n\noutput end.", output);
			
			if (! fileName.isEmpty()){
				FileUtil.writeToFile(FileUtil.toolDir + "/" + fileName, output, true);
			}
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param chunks
	 * @return
	 */
	private static Map<String, List<Chunk>> getChunksPerFileMap(List<Chunk> chunks) {
		Map<String, List<Chunk>> chunksPerFileMap = new HashMap<>();
		
		List<Chunk> chunksPerFile = new ArrayList<>();
		for (Chunk chunk : chunks){
//			if (isAllCommentedChunk(chunk)){
//				continue;
//			}
			
			if(chunksPerFile.isEmpty()){
				chunksPerFile.add(chunk);
				continue;
			}
			
			if(chunksPerFile.get(0).getClazz().equals(chunk.getClazz())){
				chunksPerFile.add(chunk);
			}else{
				operateChunksPerFileMap(chunksPerFile, chunksPerFileMap);
				chunksPerFile.add(chunk); // add into a new chunksPerFile
			}
		}
		
		if (!chunksPerFile.isEmpty()){
			operateChunksPerFileMap(chunksPerFile, chunksPerFileMap);
		}
		
		return chunksPerFileMap;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param chunksPerFile
	 * @param chunksPerFileMap
	 */
	private static void operateChunksPerFileMap(List<Chunk> chunksPerFile, Map<String, List<Chunk>> chunksPerFileMap) {
		Chunk chunk = chunksPerFile.get(0);
		List<Chunk> chunksPerFileCopy = new ArrayList<>();
		chunksPerFileCopy.addAll(chunksPerFile);
		chunksPerFileMap.put(chunk.getClazz(), chunksPerFileCopy);
		chunksPerFile.clear();
		
		logger.debug("size: {}", chunksPerFileMap.get(chunk.getClazz()).size());
	}

	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param srcFolder
	 * @param tarFolder
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param patchFolder
	 * @param deltaChunks
	 */
	public static void getOriAndPurifyActions(String patchFolder, List<Chunk> oriChunks, List<Chunk> deltaChunks) {
		// original info
		FileUtil.mark("[getOriAndPurifyActions] get patchActionsMap");
		List<String> oriFileClasses = getFileClasses(oriChunks);
		Map<Chunk, List<HierarchicalActionSet>> patchChunkActionMap = new HashMap<>();
		Map<String, List<HierarchicalActionSet>> patchActionsMap = getActionsFromFileClasses(oriFileClasses, patchFolder, oriChunks, patchChunkActionMap);
		writeActionMapInfo(patchChunkActionMap, patchActionsMap);
		FileUtil.mark("[getOriAndPurifyActions] get patchActionsMap end");
		
		// first get purified dir
		String purifyFolder = FileUtil.toolDir + "/purified";
		applyChunksAsMutants(deltaChunks, FileUtil.srcJavaDir, purifyFolder, "purifyPatch.diff");
		
		// purify
		FileUtil.mark("[getOriAndPurifyActions] get purifyActionsMap");
		List<String> deltaFileClasses = getFileClasses(deltaChunks);
		Map<Chunk, List<HierarchicalActionSet>> purifyChunkActionMap = new HashMap<>();
		Map<String, List<HierarchicalActionSet>> purifyActionsMap = getActionsFromFileClasses(deltaFileClasses, purifyFolder, deltaChunks, purifyChunkActionMap);
		writeActionMapInfo(purifyChunkActionMap, purifyActionsMap);
		FileUtil.mark("[getOriAndPurifyActions] get purifyActionsMap end");
	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param patchChunkActionMap
	 * @param patchActionsMap
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param actionSets
	 * @return
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 18, 2020
	 *
	 * @param action
	 * @return
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param fileClasses
	 * @param oriChunks 
	 * @param patchFolder
	 * @return
	 */
	public static Map<String, List<HierarchicalActionSet>> getActionsFromFileClasses(List<String> fileClasses,
			String srcCodeFolder, List<Chunk> chunks, Map<Chunk, List<HierarchicalActionSet>> chunkActionMap) {
		Map<String, List<HierarchicalActionSet>> patchActionsMap = new HashMap<>();
		
		FileUtil.mark("[getActionsFromFileClasses]");
		FileUtil.writeToFileWithFormat("number of fileClasses: %s", fileClasses.size());
		
		// get buggy-patched or buggy-purified actions
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

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param deltaChunks
	 * @return 
	 */
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

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param remainedChunk
	 */
	public static List<Chunk> updateChunkRange(Chunk chunk) {
		List<Chunk> updChunks = new ArrayList<>();
		
		if (chunk.replaceRange.getRight() != null){ // this is a chunk with del lines
//			chunk.resetByLines();
			
			List<Pair<Integer, String>> tmpChunkLines = new ArrayList<>();
			int rangeStartLineNo = chunk.replaceRange.getRight();  // end, for the situation that final tmpChunkLines are all with added lines -> Chunk newChunk = constructChunk(tmpChunkLines, chunk, rangeStartLineNo);
			for (Pair<Integer, String> line : chunk.getLines()){
				if (chunk.isCommentDel(line)){
					rangeStartLineNo = line.getLeft(); //update lineNo
					
					if (tmpChunkLines.isEmpty()){
						continue;
					}
					
					// I have to construct a new chunk here.
					Chunk newChunk = constructChunk(tmpChunkLines, chunk, line.getLeft() - 1);
					updChunks.add(newChunk);
					tmpChunkLines.clear();
				}else{
					tmpChunkLines.add(line);
				}
			}
			
			// add the rest
			if (!tmpChunkLines.isEmpty()){
				Chunk newChunk = constructChunk(tmpChunkLines, chunk, rangeStartLineNo);
				updChunks.add(newChunk);
			}
		}
		
		return updChunks;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param tmpChunk
	 * @param chunk
	 * @param line: the chunk line which is commented. i.e., not deleted in the mutant
	 * @return
	 */
	private static Chunk constructChunk(List<Pair<Integer, String>> tmpChunkLines, Chunk oriChunk, int insertAfterLineNo) {
		Chunk newChunk = new Chunk();
		newChunk.setClazz(oriChunk.getClazz());
		
		newChunk.setLines(tmpChunkLines);
		
		// set replace range
		newChunk.resetByLines();
		List<Pair<Integer, String>> delLines = newChunk.getDelLines();
		if (delLines.isEmpty()){ // no delLines
			// so we insert this chunk after the commentedline - 1 lineN0 
			newChunk.replaceRange.setLeft(insertAfterLineNo); // commentedline.getLeft() - 1
			newChunk.replaceRange.setRight(null);
		}else{ // has delLine. get the start and end LineNos of these lines
			int startLineNo = delLines.get(0).getLeft();
			int endLineNo = delLines.get(delLines.size() - 1).getLeft();
			newChunk.replaceRange.setLeft(startLineNo);
			newChunk.replaceRange.setRight(endLineNo);
		}
		
		return newChunk;
	}
	
}

/**
 * 
 */
package apr.purify.location;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.Configuration;
import apr.purify.Main;
import apr.purify.diff.Chunk;
import apr.purify.diff.DiffUtil;
import apr.purify.execution.D4JExecutor;
import apr.purify.execution.Executor;
import apr.purify.utils.FileUtil;
import apr.purify.utils.Pair;

/**
 * @author apr
 * @version Oct 16, 2020
 *
 */
public class MutantUtil {
	final static Logger logger = LoggerFactory.getLogger(MutantUtil.class);
	
	/** @Description 
	 * @author apr
	 * @version Oct 16, 2020
	 *
	 * @param simplifiedChunks2
	 * @return
	 */
	public static List<Pair<Integer, String>> getLinesFromChunks(List<Chunk> simplifiedChunks) {
		List<Pair<Integer, String>> simplifiedLines = new ArrayList<>();
		for (Chunk chunk : simplifiedChunks){
			simplifiedLines.addAll(chunk.getLines());
		}
		
		return simplifiedLines;
	}
	
//	public static boolean test(List<Pair<Integer, String>> lines, List<Chunk> oriChunks){
//		return test(lines, oriChunks, true);
//	}
	public static boolean test(List<Pair<Integer, String>> lines, List<Chunk> oriChunks){//, boolean isMutation
		
		
		String srcFolder = FileUtil.srcJavaDir;
		String mutFolder = Configuration.mutDir;
		
		FileUtil.mark(String.format("Mutant %s", FileUtil.mutantCnt));	
		
		logger.info(String.format("Mutant %s", FileUtil.mutantCnt));
		
		long startT = System.currentTimeMillis();
		List<Chunk> chunks = getCommentedChunksFromLines(lines, oriChunks);
		DiffUtil.applyChunksAsMutants(chunks, srcFolder, mutFolder);
		DiffUtil.deployMutant(srcFolder, mutFolder);
		FileUtil.writeToFileWithFormat("[time cost] of applying mutant: %s", FileUtil.countTime(startT));
		
		
		Pair<Boolean, String> pass = compileAndTest();
		
		if (pass.getLeft()){
			FileUtil.mark(String.format("Mutant %s result: passed", FileUtil.mutantCnt));
		}else{
			FileUtil.mark(String.format("Mutant %s result: failed: %s", FileUtil.mutantCnt, pass.getRight()));
		}
		FileUtil.mutantCnt ++;
		
		FileUtil.restore();//bug fix: must restore after testing
		return pass.getLeft();
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 16, 2020
	 *
	 * @return
	 */
	public static Pair<Boolean, String> compileAndTest() {
		return compileAndTest(false);
	}
	public static Pair<Boolean, String> compileAndTest(boolean isOriBeforeMutant) {
		Executor executor = new D4JExecutor();
		
		long startT = System.currentTimeMillis();
		if (! executor.compile()){
			FileUtil.writeToFile("Fail to re-compile!\n");
			FileUtil.writeToFileWithFormat("[time cost] of compile: %s", FileUtil.countTime(startT));
			return new Pair<Boolean, String>(false, "failCompile");
		}else{
			FileUtil.writeToFileWithFormat("[time cost] of compile: %s", FileUtil.countTime(startT)); // improve
		}
		
		startT = System.currentTimeMillis();
		if (! executor.testFailCases()){
			FileUtil.writeToFile("Fail to pass all failing tests!\n");
			FileUtil.writeToFileWithFormat("[time cost] of testing fail: %s", FileUtil.countTime(startT));
			return new Pair<Boolean, String>(false, "failFail");
		}else{
			FileUtil.writeToFileWithFormat("[time cost] of testing fail: %s", FileUtil.countTime(startT)); // improve
		}
		
		startT = System.currentTimeMillis();
		if (! executor.testAll()){
			FileUtil.writeToFile("Fail to pass all tests!\n");
			FileUtil.writeToFileWithFormat("[time cost] of testing all: %s", FileUtil.countTime(startT));
			
			if (isOriBeforeMutant){ // need re-run! exposed by Time 5
				Main.failingCasesInTestAll.addAll(FileUtil.oriFailedTests);
				Main.failingCasesInTestAll.addAll(executor.getFailingCasesInTestAll());
				Main.reRun = true;
				FileUtil.writeToFileWithFormat("very special cases! There are %s extra failing cases in testing all. The coverage need to be re-run!", executor.getFailingCasesInTestAll().size());
				Main.main(Main.args);
				System.exit(0);
			}
			
			return new Pair<Boolean, String>(false, "failAll");
		}else{
			FileUtil.writeToFileWithFormat("[time cost] of testing all: %s", FileUtil.countTime(startT)); // improve
		}
		
		return new Pair<Boolean, String>(true, "passAll");
	}

	/** @Description 
	 * @author apr
	 * @version Oct 16, 2020
	 *
	 * @param lines
	 * @param oriChunks 
	 * @return
	 */
//	public static List<Chunk> getChunksFromLines(List<Pair<Integer, String>> lines, List<Chunk> oriChunks) {
//		List<Chunk> chunks = new ArrayList<>();
//		
//		for (Chunk chunk : oriChunks){
//			Chunk remainedChunk = new Chunk();
//			
//			for (Pair<Integer, String> line : lines){
//				if (chunk.getLines().contains(line)){
//					remainedChunk.getLines().add(line);
//				}
//			}
//			if (!remainedChunk.getLines().isEmpty()){
//				remainedChunk.setClazz(chunk.getClazz());
//				remainedChunk.replaceRange = chunk.replaceRange;
//				chunks.add(remainedChunk);
//			}
//		}
//		
//		return chunks;
//	}
	
	/**
	 * @Description 
	 * I want to inserted commented chunks into file 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param lines
	 * @param oriChunks
	 * @return
	 */
	public static List<Chunk> getCommentedChunksFromLines(List<Pair<Integer, String>> lines, List<Chunk> oriChunks) {
		List<Chunk> chunks = new ArrayList<>();
		
		for (Chunk chunk : oriChunks){
			Chunk remainedChunk = new Chunk();
			
			for (Pair<Integer, String> line : chunk.getLines()){
				if (lines.contains(line)){
					remainedChunk.getLines().add(line);
				}else{
					// the line need to be commented
					// bug fix: when the line is commented, the oriChunks are changed. There are two ways to deal with this: 1) use a new copy of oriChunks
					// 2) use a new copy of line before line.setRight()
					String commentedLine = line.getRight().substring(0, 1) + Configuration.COMMENT + line.getRight().substring(1);
					Pair<Integer, String> lineCp = new Pair<Integer, String>(line.getLeft(), line.getRight());
					lineCp.setRight(commentedLine);
					remainedChunk.getLines().add(lineCp);
				}
			}
			
			if (!remainedChunk.getLines().isEmpty()){
				remainedChunk.setClazz(chunk.getClazz());
				remainedChunk.setReplaceRange(chunk.replaceRange);
				
				// bug fix: for chart 1. the chunk may not have - lines. so the replaceRange should be updated.
				if (remainedChunk.hasCommentedDelLine()){
					List<Chunk> updChunks = DiffUtil.updateChunkRange(remainedChunk);
					chunks.addAll(updChunks);
				}else{
					chunks.add(remainedChunk);
				}
			}else{
				FileUtil.raiseException("[getCommentedChunksFromLines] remainedChunk lines is empty!");
			}
		}
		
		return chunks;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 17, 2020
	 *
	 * @param deltaChunks
	 * @return
	 */
	public static void removePurifyComment(List<Chunk> deltaChunks) {
		for (Chunk chunk : deltaChunks){
			List<Pair<Integer, String>> lines = chunk.getLines();
			List<Pair<Integer, String>> linesWithoutComment = new ArrayList<>();
			
			for (Pair<Integer, String> line : lines){
				if (! line.getRight().substring(1).startsWith(Configuration.COMMENT)){
					linesWithoutComment.add(line);
				}
			}
			
			chunk.setLines(linesWithoutComment);
		}
		
	}

	/** @Description 
	 * @author apr
	 * @version Oct 18, 2020
	 *
	 * @param deltaChunks
	 */
	public static void logChunks(List<Chunk> deltaChunks) {
		for(Chunk chunk : deltaChunks){
			FileUtil.writeToFileWithFormat("%s", chunk.toString());
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param chunks
	 * @return
	 */
	public static int getLinesCnt(List<Chunk> chunks) {
		int cnt = 0;
		for(Chunk chunk:chunks){
			cnt += chunk.getLines().size();
		}
		
		return cnt;
	}
}

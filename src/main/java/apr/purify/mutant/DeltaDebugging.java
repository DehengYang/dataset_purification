/**
 * 
 */
package apr.purify.mutant;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import apr.purify.Main;
import apr.purify.diff.Chunk;
import apr.purify.diff.DiffUtil;
import apr.purify.location.MutantUtil;
import apr.purify.utils.FileUtil;
import apr.purify.utils.Pair;

/**
 * this class has learned a lot from https://github.com/benjholla/ddmin/tree/master/DeltaDebug/src/dd
 * @author apr
 * @version Oct 16, 2020
 *
 */
/**
 * A simple Java implementation of Andreas Zeller's Delta Debugging algorithm
 * 
 * Reference: https://www.st.cs.uni-saarland.de/whyprogramsfail/code/dd/DD.java
 * 
 * @author Ben Holland
 */
public class DeltaDebugging {
	/**
	 * Given an input that causes a failing/error condition, this implementation of a divide and conquer
	 * algorithm splits the input data into smaller chunks and checks if the smaller input reproduces 
	 * the failing/error condition with a smaller input.

	 * @param input The pre-chunked test input to reduce
	 * @param chunksBkForDDMin 
	 * @param simplifiedLinesBkForDDMin 
	 * @param harness A test harness implementation that returns true (pass) or false (fail/error) for a given input
	 * 
	 * @return A minimized input that reproduces the failing/error condition
	 */
	public static List<Pair<Integer, String>> ddmin(List<Pair<Integer, String>> input, List<Chunk> chunks) {
		
//		assert MutantUtil.test(new ArrayList<>(), chunks) == false;
//	    assert MutantUtil.test(input, chunks) == true;
		
//		boolean expectedFalse = MutantUtil.test(new ArrayList<>(), chunks); // put this in main
//		if (expectedFalse){ 
//			FileUtil.raiseException("[ddmin] original buggy version does not fail!");
//		}
		
		// old approach
//		boolean expectedTrue = MutantUtil.test(input, chunks);
//		if (!expectedTrue){
//			FileUtil.raiseException("[ddmin] original patch version does not pass!");
//		}
//		// new approach  -> now move out of this func -> no, not move. just minor change for the case "FileUtil.raiseException("[ddmin] original patch version does not pass!");"
//		boolean runOriginal = assertBeforeMut(input, chunks);
//		if (! runOriginal){ // this means that the coverage analysis leads to the failure.
//			FileUtil.writeToFile("Coverage analysis leads to the failure of patch fail before mutation!");
//			runOriginal = assertBeforeMut(simplifiedLinesBkForDDMin, chunksBkForDDMin);
//		}
		
		
		int n = 2;
		while(input.size() >= 2) {
			List<List<Pair<Integer, String>>> subsets = split(input, n);
			boolean complementFailing = false;
			
			for(List<Pair<Integer, String>> subset : subsets){
				List<Pair<Integer, String>> complement = difference(input, subset);
				if (MutantUtil.test(complement, chunks) == true) {
					input = complement;
					n = Math.max(n - 1, 2);
					complementFailing = true;
					break;
				}
			}

			if (!complementFailing) {
				if (n == input.size()) {
					break;
				}
				
				// increase set granularity
				n = Math.min(n * 2, input.size());
			}
		}

		return input;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 21, 2020
	 * @param chunks 
	 * @param input 
	 *
	 * @return
	 */
	public static boolean assertBeforeMut(List<Pair<Integer, String>> input, List<Chunk> chunks, String diffFileName, String failMessage) {
		String assertBeforeMutFolder = FileUtil.toolDir + "/assertBeforeMut";
		FileUtil.remove(assertBeforeMutFolder); // for init before writing
		
		List<Chunk> patchChunks = MutantUtil.getCommentedChunksFromLines(input, chunks);
		DiffUtil.applyChunksAsMutants(patchChunks, FileUtil.srcJavaDir, assertBeforeMutFolder, diffFileName);
		DiffUtil.deployMutant(FileUtil.srcJavaDir, assertBeforeMutFolder);
		boolean pass = MutantUtil.compileAndTest(true).getLeft();
		if (!pass){
			FileUtil.writeToFile(failMessage);
//			FileUtil.raiseException("[ddmin] original patch version does not pass!");
		}
		FileUtil.restore();
		
		return pass;
	}

	/**
	 * Returns the difference of set a and set b
	 * @param a
	 * @param b
	 * @return
	 */
	private static <E> List<E> difference(List<E> a, List<E> b) {
		List<E> result = new LinkedList<E>();
		result.addAll(a);
		result.removeAll(b);
		return result;
	}

	/**
	 * Splits the input set s into n subsets
	 * @param s
	 * @param n
	 * @return
	 */
	private static <E> List<List<E>> split(List<E> s, int n) {
		List<List<E>> subsets = new LinkedList<List<E>>();
		int position = 0;
		for (int i = 0; i < n; i++) {
			List<E> subset = s.subList(position, position + (s.size() - position) / (n - i));
			subsets.add(subset);
			position += subset.size();
		}
		return subsets;
	}
}

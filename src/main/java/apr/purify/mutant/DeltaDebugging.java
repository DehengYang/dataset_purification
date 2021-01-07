
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


public class DeltaDebugging {

	public static List<Pair<Integer, String>> ddmin(List<Pair<Integer, String>> input, List<Chunk> chunks) {

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
				
				n = Math.min(n * 2, input.size());
			}
		}

		return input;
	}


	public static boolean assertBeforeMut(List<Pair<Integer, String>> input, List<Chunk> chunks, String diffFileName, String failMessage) {
		String assertBeforeMutFolder = FileUtil.toolDir + "/assertBeforeMut";
		FileUtil.remove(assertBeforeMutFolder); 
		
		List<Chunk> patchChunks = MutantUtil.getCommentedChunksFromLines(input, chunks);
		DiffUtil.applyChunksAsMutants(patchChunks, FileUtil.srcJavaDir, assertBeforeMutFolder, diffFileName);
		DiffUtil.deployMutant(FileUtil.srcJavaDir, assertBeforeMutFolder);
		boolean pass = MutantUtil.compileAndTest(true).getLeft();
		if (!pass){
			FileUtil.writeToFile(failMessage);
		}
		FileUtil.restore();
		
		return pass;
	}


	private static <E> List<E> difference(List<E> a, List<E> b) {
		List<E> result = new LinkedList<E>();
		result.addAll(a);
		result.removeAll(b);
		return result;
	}


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

/**
 * 
 */
package apr.purify.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.model.Action;

import apr.purify.MainTest;
import edu.lu.uni.serval.gumtree.GumTreeComparer;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalRegrouper;
import edu.lu.uni.serval.utils.ListSorter;
import junit.framework.TestCase;

/**
 * @author apr
 * @version Oct 7, 2020
 *
 */
public class DiffPatchParserTest extends TestCase{
	/**
	 * @Description 
	 * This is for testing the possibility and feasibility of using PatchParser, before writing Java code in src/main/java
	 * At present, it is feasible. So I plan to write relevant Java code to implement it.
	 * test patchparser
	 * @author apr
	 * @version Oct 7, 2020
	 *
	 */
	public void testDiffGumtree_Closure99(){
		File prevFile = new File(MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_buggy_java.txt"));
		File revFile = new File(MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_fixed_java.txt"));
		List<HierarchicalActionSet> actionSets = parseChangedSourceCodeWithGumTree(prevFile, revFile);
		
		assertNotNull(actionSets);
	}
	
	public List<HierarchicalActionSet> parseChangedSourceCodeWithGumTree(File prevFile, File revFile) {
		List<HierarchicalActionSet> actionSets = new ArrayList<>();
		// GumTree results
		List<Action> gumTreeResults = new GumTreeComparer().compareTwoFilesWithGumTree(prevFile, revFile);
		if (gumTreeResults == null) {
			return null;
		} else if (gumTreeResults.size() == 0){
			return actionSets;
		} else {
			// Regroup GumTre results.
			List<HierarchicalActionSet> allActionSets = new HierarchicalRegrouper().regroupGumTreeResults(gumTreeResults);
			
			ListSorter<HierarchicalActionSet> sorter = new ListSorter<>(allActionSets);
			actionSets = sorter.sortAscending();
			
			return actionSets;
		}
	}
}

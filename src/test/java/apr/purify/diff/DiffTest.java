/**
 * 
 */
package apr.purify.diff;

import java.util.List;
import java.util.Map;

import apr.purify.utils.FileUtil;
import apr.purify.utils.Pair;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import junit.framework.TestCase;

/**
 * @author apr
 * @version Oct 8, 2020
 *
 */
public class DiffTest extends TestCase{
	public void testDiff_Chart1(){
		String patchDiffPath = "examples/Chart1_patchDiff.txt";
		String srcFolder = "examples/Defects4J_Chart_1/source/";
		FileUtil.toolDir = "output/Defects4J_Chart_1/purify/";
		
		Diff diff = new Diff(patchDiffPath, srcFolder);
		
		List<String> buggyCodeAll = FileUtil.readFile("examples/Defects4J_Chart_1/source//org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java");
		System.out.println("buggyCodeAll.size(): " + buggyCodeAll.size());
		assertEquals(buggyCodeAll.size(), 1994);
		
		List<String> patchCodeAll = FileUtil.readFile("output/Defects4J_Chart_1/purify//humanPatch/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer-fixed.java");
		System.out.println("patchCodeAll.size(): " + patchCodeAll.size());
		assertEquals(patchCodeAll.size(), 1994);
		
		diff.getGumTreeActions();
		Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = diff.getActionsPerFileMap();
		assertTrue(!actionsPerFileMap.isEmpty());
	}
	
	public void testDiff_Closure32(){
		String patchDiffPath = "examples/Closure32_patchDiff.txt";
		String srcFolder = "examples/Defects4J_Closure_32/src/";
		FileUtil.toolDir = "output/Defects4J_Closure_32/purify/";
		FileUtil.logPath = FileUtil.toolDir + "/log.txt";
		FileUtil.writeToFile(FileUtil.logPath, "", false);
		
		Diff diff = new Diff(patchDiffPath, srcFolder);
		
		List<String> buggyCodeAll = FileUtil.readFile("examples/Defects4J_Closure_32/src/com/google/javascript/jscomp/parsing/JsDocInfoParser.java");
		System.out.println("buggyCodeAll.size(): " + buggyCodeAll.size());
		assertEquals(buggyCodeAll.size(), 2350);
		
		List<String> patchCodeAll = FileUtil.readFile("output/Defects4J_Closure_32//purify/humanPatch/com/google/javascript/jscomp/parsing/JsDocInfoParser-fixed.java");
		System.out.println("patchCodeAll.size(): " + patchCodeAll.size());
		assertEquals(patchCodeAll.size(), 2350 + 12);
	}
	
	public void testDiff_Time2(){
		String patchDiffPath = "examples/Time2_patchDiff.txt";
		String srcFolder = "examples/Defects4J_Time_2/src/main/java";
		FileUtil.toolDir = "output/Defects4J_Time_2/purify/";
		FileUtil.logPath = FileUtil.toolDir + "/log.txt";
		FileUtil.writeToFile(FileUtil.logPath, "", false);
		
		Diff diff = new Diff(patchDiffPath, srcFolder);
		
//		diff.getGumTreeActions();
//		Map<Pair<String, String>, List<HierarchicalActionSet>> actionsPerFileMap = diff.getActionsPerFileMap();
//		assertTrue(!actionsPerFileMap.isEmpty());
	}
	
	
}

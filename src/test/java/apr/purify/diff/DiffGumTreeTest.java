/**
 * 
 */
package apr.purify.diff;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.github.gumtreediff.actions.ActionGenerator;
//import com.github.gumtreediff.actions.model.Action;
//import com.github.gumtreediff.client.Run;
//import com.github.gumtreediff.gen.Generators;
//import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
//import com.github.gumtreediff.matchers.CompositeMatchers;
//import com.github.gumtreediff.matchers.MappingStore;
//import com.github.gumtreediff.matchers.Matcher;
//import com.github.gumtreediff.tree.ITree;

//import com.github.gumtreediff.actions.ActionGenerator;
//import com.github.gumtreediff.actions.model.Action;
//import com.github.gumtreediff.gen.Generators;
//import com.github.gumtreediff.gen.TreeGenerator;
//import com.github.gumtreediff.matchers.MappingStore;
//import com.github.gumtreediff.matchers.Matcher;
//import com.github.gumtreediff.matchers.Matchers;
//import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
//import com.github.gumtreediff.tree.ITree;

import apr.purify.MainTest;
import apr.purify.utils.CmdUtil;
//import gumtree.spoon.AstComparator;
//import gumtree.spoon.diff.Diff;
//import gumtree.spoon.diff.operations.Operation;
import junit.framework.TestCase;

/**
 * This is for testing the possibility and feasibility of using GumTree old version (2.1.2 in year 2019) in apache commons, before writing Java code in src/main/java
 * At present, it is not that good for this task.
 * Therefore, the dependency in the pom.xml is commented.
 * @author apr
 * @version Oct 7, 2020
 *
 */
public class DiffGumTreeTest extends TestCase{
	final static Logger logger = LoggerFactory.getLogger(DiffGumTreeTest.class);
	
	public void test_null(){
		assertTrue(true);
	}
//	/**
//	 * @Description 
//	 * refer to: <a href="https://github.com/GumTreeDiff/gumtree/wiki/GumTree-API">https://github.com/GumTreeDiff/gumtree/wiki/GumTree-API</a>
//	 * @author apr
//	 * @version Oct 7, 2020
//	 *
//	 */
//	public void testDiffGumtree_Closure99(){
//		Run.initGenerators();
//	    String srcFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_buggy.java");
//	    String dstFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_fixed.java");
//	    ITree src = null;
//	    ITree dst = null;
//		try {
//			src = Generators.getInstance().getTree(srcFile).getRoot();
//			dst = Generators.getInstance().getTree(dstFile).getRoot();
//		} catch (UnsupportedOperationException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    
//	    GreedySubtreeMatcher matcher = new GreedySubtreeMatcher(src, dst, new MappingStore());
//	    matcher.match();
//	    matcher.getMappings();
//	    
//	    
//	    
////	    Matcher defaultMatcher = Matchers.getInstance().getMatcher();
////	    MappingStore mappings = defaultMatcher.match(src, dst);
////	    EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
////	    EditScript actions = editScriptGenerator.computeActions(mappings);
//	}
	
//	/**
//	 * @Description 
//	 * has error:
//	 * java.lang.UnsupportedOperationException: No generator found for file: /home/apr/apr_tools/datset_purification_2020/purification/purify/src/test/resources/Closure_99_CheckGlobalThis_buggy_java.java
//	 * @author apr
//	 * @version Oct 10, 2020
//	 *
//	 */
//	public void testGumTree_Closure99_with_error(){
//		Run.initGenerators();
//		String srcFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_buggy_java.java");
//		String dstFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_fixed_java.java");
//		ITree src = null;
//		ITree dst = null;
//		try {
//			src = Generators.getInstance().getTree(srcFile).getRoot();
//			dst = Generators.getInstance().getTree(dstFile).getRoot();
//		} catch (UnsupportedOperationException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		MappingStore mappings = new MappingStore();
//		Matcher defaultMatcher = Matchers.getInstance().getMatcher(src, dst);
//		defaultMatcher.match();
//		final ActionGenerator actionGenerator = new ActionGenerator(src, dst, defaultMatcher.getMappings());
//		actionGenerator.generate();
//		List<Action> actions = actionGenerator.getActions();
//		for(Action act : actions){
//			logger.info("act: {}", act.toString());
//		}
//		
////		MappingStore mappings = defaultMatcher.match(src, dst);
////		EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
////		EditScript actions = editScriptGenerator.computeActions(mappings);
//	}
	
//	/**
//	 * @Description
//	 * for https://mvnrepository.com/artifact/fr.inria.gforge.spoon.labs/gumtree-spoon-ast-diff/1.27 
//	 * this project 
//	 * @author apr
//	 * @version Oct 10, 2020
//	 *
//	 */
//	public void testGumTreeDiff_Closure99(){
//		String srcFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_buggy_java.java");
//		String dstFile = MainTest.getResourceFilePath("src/test/resources", "Closure_99_CheckGlobalThis_fixed_java.java");
//		AstComparator diff = new AstComparator();
//		
////		srcFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
////		dstFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/AbstractCategoryItemRenderer.java";
//		
////		srcFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/gumtree-ast-diff/AbstractCategoryItemRenderer.java";
////		dstFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/gumtree-ast-diff/AbstractCategoryItemRenderer-buggy.java";
//		
//		logger.info("srcFile: {}\n"
//				+ "dstFile: {}", srcFile, dstFile);
//		
//		//diff -Naur  /home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java    /home/apr/apr_tools/datset_purification_2020/purification/purify/output/Defects4J_Chart_1/purify/humanPatch/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer-fixed.java
//		//diff -Naur  /home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java   /home/apr/apr_tools/datset_purification_2020/purification/purify/output/Defects4J_Chart_1/purify/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer-fixed.java
//		String cmd = "diff -Naur " + srcFile + " " + dstFile;
//		logger.info("output of running cmd: {}\n{}", cmd, CmdUtil.runCmd(cmd));
//		
//		Diff result = null;
//		try {
//			result = diff.compare(new File(srcFile), new File(dstFile)); // must add srcFile!!!
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
////		result.getAllOperations().get(0).getAction();
////		@SuppressWarnings("rawtypes")
////		List<Operation> actions = result.getAllOperations();
////		for(Operation act : actions){
////			logger.info("Operation: {}", act.toString());
////			logger.info("Operation2: {}", act.getAction().toString());
////		}
//		
//		List<Action> actions = result.getActions();
//		for(Action act : actions){
//			logger.info("Action: {}", act.toString()); 
//		}
//	}
	
//	/**
//	 * @Description
//	 * 
//	 *  after learning from test case testGumTreeDiff_Closure99, I know how to use Gumtree 2.1.2
//	 * @author apr
//	 * @version Oct 10, 2020
//	 * @throws IOException 
//	 * @throws UnsupportedOperationException 
//	 *
//	 */
//	public void testGumtree_2() throws UnsupportedOperationException, IOException{
//		String srcFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
//		String dstFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/AbstractCategoryItemRenderer.java";
//		
//		/* there is a very big problem here: No generator error when parse java files
//		 * refer to: No generator error when parse C file #163 https://github.com/GumTreeDiff/gumtree/issues/163 
//		 * so I cannot use Run.initGenerators()
//		 */
////		Run.initGenerators();
////		ITree rootSpoonLeft = Generators.getInstance().getTree(srcFile).getRoot();;
////		ITree rootSpoonRight = Generators.getInstance().getTree(dstFile).getRoot();
//		ITree rootSpoonLeft = new JdtTreeGenerator().generateFromFile(srcFile).getRoot();
//		ITree rootSpoonRight = new JdtTreeGenerator().generateFromFile(dstFile).getRoot();
//		final MappingStore mappingsComp = new MappingStore();
//		final Matcher matcher = new CompositeMatchers.ClassicGumtree(rootSpoonLeft, rootSpoonRight, mappingsComp);
//		matcher.match();
//
//		final ActionGenerator actionGenerator = new ActionGenerator(rootSpoonLeft, rootSpoonRight,
//				matcher.getMappings());
//		actionGenerator.generate();
//		List<Action> actions = actionGenerator.getActions();
//		for(Action act : actions){
//			logger.info("Action: {}", act.toString()); 
//			logger.info("Name: {} - {} - {}", act.getName(), act.getClass(), act.getNode());
//			logger.info("Node: {} - {} - {}", act.getNode().getLength(), act.getNode().getPos(), act.getNode().getParent()); 
//			logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos()), 
//					PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos() + act.getNode().getLength()));
//			logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos()), 
//					PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos() + act.getNode().getLength()));
//		}
//	}
	
	/*
	 * this is a test that I wrote on the newest version of gumtree. 
	 * commit 8ad77b5f882aa9e102401f71ac5a42133a29867d (HEAD -> develop, origin/develop, origin/HEAD)
Author: Jean-Rémy Falleri <jr.falleri@gmail.com>
Date:   Fri Oct 2 21:00:14 2020 +0200
Now I copy it here to record my attempt.

Finally I decide to use Gumtree in PatchParser. This is the most stable and effective version according to my attempts.
	 */
//	 @Test
//	 public void testIds() throws IOException {
//		 String input = "class Foo { String a; void foo(int a, String b) {}; void bar() { } }";
//		 TreeContext ct = new JdtTreeGenerator().generateFrom().string(input);
//		 assertEquals(ct.getRoot().getChild(0).getMetadata("id"), "Type Foo");
//		 assertEquals(ct.getRoot().getChild("0.2").getMetadata("id"), "Field a");
//		 assertEquals(ct.getRoot().getChild("0.3").getMetadata("id"), "Method foo( int String)");
//		 assertEquals(ct.getRoot().getChild("0.4").getMetadata("id"), "Method bar()");
//
//
//		 //	        Run.initGenerators();
//		 String srcFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
//		 String dstFile = "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/AbstractCategoryItemRenderer.java";
//		 //	        ITree src = Generators.getInstance().getTree(srcFile).getRoot();
//		 //	        ITree dst = Generators.getInstance().getTree(dstFile).getRoot();
//		 ITree src = new JdtTreeGenerator().generate(Files.newBufferedReader(Paths.get(srcFile), Charset.forName("UTF-8"))).getRoot();
//		 ITree dst = new JdtTreeGenerator().generate(Files.newBufferedReader(Paths.get(dstFile), Charset.forName("UTF-8"))).getRoot();
//		 Matcher defaultMatcher = Matchers.getInstance().getMatcher();
//		 MappingStore mappings = defaultMatcher.match(src, dst);
//		 EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
//		 EditScript actions = editScriptGenerator.computeActions(mappings);
//
//		 for(int i = 0; i < actions.size(); i ++) {
//			 Action act = actions.get(i);
//			 System.out.println(act.toString());
//		 }
//	 }
}

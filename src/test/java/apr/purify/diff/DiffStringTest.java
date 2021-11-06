/**
 * 
 */
package apr.purify.diff;

import apr.purify.MainTest;
import junit.framework.TestCase;

/**
 * This is for testing the possibility and feasibility of using StringsComparator in apache commons, before writing Java code in src/main/java
 * At present, it is not that good for this task.
 * Therefore, the dependency in the pom.xml is commented.
 * @author apr
 * @version Oct 7, 2020
 *
 */
public class DiffStringTest extends TestCase{
	public void test_null(){
		assertTrue(true);
	}
//	public void testDiff_Chart1(){
//		String buggyTxt = MainTest.readFile("src/test/resources", "Chart_1_buggy.txt");
//		String fixedTxt = MainTest.readFile("src/test/resources", "Chart_1_fixed.txt");
//		StringsComparator comparator = new StringsComparator(buggyTxt, fixedTxt);
//		EditScript<Character> script = comparator.getScript();
////		System.out.format("diff: %s\n", script.toString());
//	    int mod = script.getModifications();
//	    assertEquals(2, mod);
//	    MyCommandsVisitor myCommandsVisitor = new MyCommandsVisitor();
//	    comparator.getScript().visit(myCommandsVisitor);
////	    System.out.println("FINAL DIFF = " + myCommandsVisitor.left + " | " + myCommandsVisitor.right);
//	}
//	
//	public void testDiff_Closure99(){
//		String buggyTxt = MainTest.readFile("src/test/resources", "Closure_99_buggy.txt");
//		String fixedTxt = MainTest.readFile("src/test/resources", "Closure_99_fixed.txt");
//		StringsComparator comparator = new StringsComparator(buggyTxt, fixedTxt);
//		EditScript<Character> script = comparator.getScript();
////		System.out.format("diff: %s\n", script.toString());
//	    int mod = script.getModifications();
////	    assertEquals(2, mod);
//	    MyCommandsVisitor myCommandsVisitor = new MyCommandsVisitor();
//	    comparator.getScript().visit(myCommandsVisitor);
////	    System.out.println("FINAL DIFF = " + myCommandsVisitor.left + " | " + myCommandsVisitor.right);
//	}
//	
	
}
//
///*
// * Custom visitor.
// */
//class MyCommandsVisitor implements CommandVisitor<Character> {
//
//	String left = "";
//	String right = "";
////	String keep = "";
//
//	@Override
//	public void visitKeepCommand(Character c) {
//		// Character is present in both files.
//		left = left + c;
//		right = right + c;
////		System.out.println("keep: " + c + "\n");
//	}
//
//	@Override
//	public void visitInsertCommand(Character c) {
//		/*
//		 * Character is present in right file but not in left. Method name
//		 * 'InsertCommand' means, c need to insert it into left to match right.
//		 */
//		right = right + "(" + c + ")";
//		System.out.println("insert: " + c + "\n");
//	}
//
//	@Override
//	public void visitDeleteCommand(Character c) {
//		/*
//		 * Character is present in left file but not right. Method name 'DeleteCommand'
//		 * means, c need to be deleted from left to match right.
//		 */
//		left = left + "{" + c + "}";
//		System.out.println("delete: " + c + "\n");
//	}
//}

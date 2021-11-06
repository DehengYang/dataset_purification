/**
 * 
 */
package apr.purify.coverage;

import java.io.File;
import java.util.List;
import java.util.Map;

import apr.purify.MainTest;
import apr.purify.location.SuspiciousLocation;
import apr.purify.location.TestCaseResult;
import apr.purify.utils.FileUtil;
import apr.purify.utils.GeneralUtil;
import junit.framework.TestCase;

/**
 * @author apr
 * @version Oct 4, 2020
 *
 */
public class CoverageTest extends TestCase{
	public void testCoverageChart1(){
		// 1) parse parameters from file
		MainTest.parseParameters("Chart_1.txt");
		
		GeneralUtil.countLoc(FileUtil.srcJavaDir);
		
		// 2) run gzoltar
		Coverage coverage = new Coverage();
		coverage.getCoverage();
		coverage.calculateCoveredStmtsPerTest();
		
		// assert file
		File flDir = new File("output/Defects4J_Chart_1/purify/sfl/txt/");
		assertTrue(flDir.exists());
		
		// 3) get covered stmts for each test
		Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = coverage.getTestToStmts();
		
		// assert failing test case
		TestCaseResult tcResult = new TestCaseResult("org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660", false);
		TestCaseResult tcResult2 = new TestCaseResult("org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#test2947660", false);
		assertTrue(tcResult2.equals(tcResult));
//		for (Map.Entry<TestCaseResult, List<SuspiciousLocation>> entry : testToStmts.entrySet()){
//			System.out.format(entry.getKey().toString() + "\n");
//			System.out.println(entry.getKey().equals(tcResult));
//		}
//		System.out.println(testToStmts.containsKey(tcResult)); 
		assertTrue(testToStmts.containsKey(tcResult));  // bug fix: must override both equals and hashCode methods! Otherwise, the testToStmts.containsKey(tcResult) will always return false!
		
		
		// assert faulty location
		List<SuspiciousLocation> slList = testToStmts.get(tcResult);
		// the faulty location
		SuspiciousLocation sl = new SuspiciousLocation("org.jfree.chart.renderer.category.AbstractCategoryItemRenderer", 1797);
		assertTrue(slList.contains(sl));
//		for(SuspiciousLocation slEach : slList){
//			System.out.println(slEach.toString());
//		}
		
	}
	
	
}

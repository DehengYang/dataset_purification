/**
 * 
 */
package apr.purify.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.location.SuspiciousLocation;
import apr.purify.location.TestCaseResult;

/**
 * @author apr
 * @version Oct 4, 2020
 *
 */
public class CoverageUtil {
	final static Logger logger = LoggerFactory.getLogger(CoverageUtil.class);
	
	/**
	 * @Description read from fl 1.7.2 spectra.csv file
	 * @author apr
	 * @version Apr 18, 2020
	 *
	 * @param path
	 * @return
	 */
	public static List<SuspiciousLocation> readStmtFile(String path){
		List<SuspiciousLocation> stmtList = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            in.readLine(); // skip first row: name 
            while ((line = in.readLine()) != null) {
            	// e.g., org.jfree.chart.renderer.category.junit$AbstractCategoryItemRendererTests#AbstractCategoryItemRendererTests():74
            	if (line.length() == 0) logger.error("Empty line in %s", path);
            	String packageName = line.split("\\$")[0];
            	String clazz = line.split("\\$")[1].split("#")[0];
            	String method = line.split("#")[1].split(":")[0];
            	int lineNo = Integer.parseInt(line.split("#")[1].split(":")[1]);
            	String wholeName = packageName + "." + clazz;
            	String srcJavaFilePath = FileUtil.srcJavaDir + "/" + wholeName.replaceAll("\\.", "/") + ".java";
            	String binTestFilePath = FileUtil.binTestDir + "/" + wholeName.replaceAll("\\.", "/") + ".class";
            	// bug fix exposed by chart 26: JFreeChartInfo only has srcClass and does not have a java file.
            	String binJavaFilePath = FileUtil.binJavaDir + "/" + wholeName.replaceAll("\\.", "/") + ".class";
//            	if (srcJavaFilePath.equals("/mnt/purify/repairDir/Purify_Defects4J_Chart_2/source/org/jfree/data/general/junit/DatasetUtilitiesTests.java")){
//            		logger.debug("");
//            	}
            	if (!new File(srcJavaFilePath).exists() && !new File(binTestFilePath).exists() && !new File(binJavaFilePath).exists()){
            		FileUtil.raiseException("suspiciousLocation: %s:%s has no corresponding java file: %s, srcJava class: %s or test class: %s", wholeName, lineNo, srcJavaFilePath, binJavaFilePath, binTestFilePath);
            	}
            	SuspiciousLocation sl = new SuspiciousLocation(wholeName, method, lineNo);
            	stmtList.add(sl); // add sl
            }
            logger.info(String.format("The total suspicious statements: %d", stmtList.size()));
        } catch (final IOException e) {
            e.printStackTrace();
        }finally {
            try {
            	in.close();
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
        }
        return stmtList;
	}
	
	/**
	 * @Description read from e.g., /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Mockito_15/MY_APR/FLDir/tests.csv   fl v0.1.1
	 * @author apr
	 * @version Apr 18, 2020
	 *
	 * @param path
	 * @return
	 */
	public static List<TestCaseResult> readTestFile(String path){
		List<TestCaseResult> testsList = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            in.readLine(); // skip first row: name,outcome,runtime,stacktrace
            while ((line = in.readLine()) != null) {
            	// e.g., org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests#testCloning1,PASS,43154107,
            	if (line.length() == 0) logger.error("Empty line in %s", path);
            	
            	String testName = line.split(",")[0]; // add test
            	boolean passed = true;
            	if (line.split(",")[1].equals("FAIL")){
            		passed = false;
            		FileUtil.writeToFileWithFormat("failing test case: %s", testName);
            		FileUtil.gzFailingTestCases.add(testName);
            	}
            	TestCaseResult tcResult = new TestCaseResult(testName, passed);
            	testsList.add(tcResult);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }finally {
            try {
            	in.close();
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
        }
		
		FileUtil.writeToFileWithFormat("testsList (from gzoltar output tests.csv) size: %s", testsList.size());
        return testsList;
	}
	
	public static Map<SuspiciousLocation, List<TestCaseResult>> parseMatrixForStmts(String path, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		char[][] matrix = getMatrixFromFile(path, slList, testList);
		return parseMatrixForStmts(matrix, slList, testList);
	}
	/** @Description 
	 * This is for get covered test cases for each stmt.
	 * @author apr
	 * @version Apr 18, 2020
	 *
	 * @param matrix_file
	 * @param slList
	 * @param testList
	 */
	public static Map<SuspiciousLocation, List<TestCaseResult>> parseMatrixForStmts(char[][] matrix, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		// init
		int specSize = slList.size();
		int testSize = testList.size();

	    Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests = new HashMap<SuspiciousLocation, List<TestCaseResult>>();
	    
	    // identify total tests info
	    int totalPassedCnt = 0;
	    int totalFailedCnt = 0;
	    for (TestCaseResult ti : testList){
	    	if (ti.isSuccessful()){
	    		totalPassedCnt ++;
	    	}else{
	    		totalFailedCnt ++;
	    	}
	    }
	    
	    // each stmt 
	    List<SuspiciousLocation> sslList = new ArrayList<>(); // for suspiciousness
	    for (int i = 0; i < specSize; i++){ // i_th column
	    	int executedPassedCount = 0;
	    	int executedFailedCount = 0;
	    	
	    	SuspiciousLocation sl = slList.get(i);
	    	//TODO: 
	    	//Chart 1
	    	//2020-10-04 12:49:29 WARN  CoverageUtil:154 - [parseMatrixFile] repeated SourceLocation in spectra: org.jfree.chart.urls.StandardCategoryURLGenerator:77,0.0
	    	//From two constructed method of the StandardCategoryURLGenerator class. 
	    	//So I think this should not be neglected. To be done in the future...
	    	if (stmtToTests.containsKey(sl)){
	    		logger.warn("[parseMatrixFile] repeated SourceLocation in spectra: " + sl.toString());
	    		continue;
	    	}
	    	stmtToTests.put(sl, new ArrayList<TestCaseResult>());
	    	
			for (int j = 0; j < testSize; j++){ // j_th row. Test result
				if(matrix[j][i] == '1'){  // executed by j_th row test case
					stmtToTests.get(sl).add(testList.get(j));
					
					if(matrix[j][specSize] == '+'){ // passed
						executedPassedCount ++;
					}else{
						executedFailedCount ++;
					}
				}
			}
			
			sl.setExecFailed(executedFailedCount);
			sl.setExecPassed(executedPassedCount);
			sl.setTotalPassed(totalPassedCnt);
			sl.setTotalFailed(totalFailedCnt);
			sl.calculateSuspicious();
			sslList.add(sl);
	    }
		
	    Collections.sort(sslList, new Comparator<SuspiciousLocation>() {
			@Override
			public int compare(final SuspiciousLocation o1, final SuspiciousLocation o2) {
				// reversed parameters because we want a descending order list
				return Double.compare(o2.getSuspValue(), o1.getSuspValue());
			}
		});
	    
	    String flOutputPath = FileUtil.toolDir + "/fl.txt";
	    FileUtil.writeToFile(flOutputPath, "", false);
	    
	    LinkedHashMap<SuspiciousLocation, List<TestCaseResult>> map = new LinkedHashMap<>();
		for (SuspiciousLocation source : sslList) {
			map.put(source, stmtToTests.get(source));
			//org.mockito.internal.stubbing.BaseStubbing:49,0.1889822365046136
			FileUtil.writeToFile(flOutputPath, String.format("%s:%s,%s\n", source.getClassName(), source.getLineNo(),
					source.getSuspValue()), true);
		}

		stmtToTests = map;
		return stmtToTests;
	}
	
	
	public static Map<TestCaseResult, List<SuspiciousLocation>> parseMatrixForTests(String path, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		char[][] matrix = getMatrixFromFile(path, slList, testList);
		return parseMatrixForTests(matrix, slList, testList);
	}
	/** @Description 
	 * This is for get covered stmts for each test case.
	 * @author apr
	 * @version Apr 18, 2020
	 *
	 * @param matrix_file
	 * @param slList
	 * @param testList
	 */
	public static Map<TestCaseResult, List<SuspiciousLocation>> parseMatrixForTests(char[][] matrix, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		// init
		int specSize = slList.size();
		int testSize = testList.size();
	    
	    Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = new HashMap<TestCaseResult, List<SuspiciousLocation>>();
	    
	    // each test case
	    for (int i = 0; i < testSize; i++){ //i_th row
	    	TestCaseResult tcResult = testList.get(i);
	    	
	    	// init
	    	if (testToStmts.containsKey(tcResult)){
	    		logger.warn("[parseMatrixFile] repeated TestCaseResult in matrix: " + tcResult.toString());
	    		continue;
	    	}
	    	testToStmts.put(tcResult, new ArrayList<SuspiciousLocation>());
	    	
	    	for (int j = 0; j < specSize; j++){ //j_th colum
	    		if(matrix[i][j] == '1'){
	    			testToStmts.get(tcResult).add(slList.get(j));
	    		}
	    	}
	    }
	   
		return testToStmts;
	}
	
	/**
	 * @Description
	 * get matrix from matrix file
	 * /home/apr/apr_tools/datset_purification_2020/purification/purify/output/Defects4J_Chart_1/purify/sfl/txt/matrix.txt 
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param path
	 * @param slList
	 * @param testList
	 * @return
	 */
	public static char[][] getMatrixFromFile(String path, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		// init
		int specSize = slList.size();
		int testSize = testList.size();
		char[][] matrix = new char[testSize][specSize + 1];
		int rowCnt = 0;

		// read lines
		String line;
		try(BufferedReader in = new BufferedReader(new FileReader(path))) {
			while ((line = in.readLine()) != null) {
				int realCol = 0; // bug fix.
				for(int col = 0; col < line.length() - 1; col = col + 2){ // should minus 1
//					if (col == 7400){
//						logger.debug("debug");
//					}
					matrix[rowCnt][realCol] = line.charAt(col); // bug fix.
					realCol++;
				}
				
				rowCnt++;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
		return matrix;
	}

}

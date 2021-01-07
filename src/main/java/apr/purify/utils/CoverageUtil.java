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


public class CoverageUtil {
	final static Logger logger = LoggerFactory.getLogger(CoverageUtil.class);
	
	public static List<SuspiciousLocation> readStmtFile(String path){
		List<SuspiciousLocation> stmtList = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            in.readLine(); 
            while ((line = in.readLine()) != null) {
            	if (line.length() == 0) logger.error("Empty line in %s", path);
            	String packageName = line.split("\\$")[0];
            	String clazz = line.split("\\$")[1].split("#")[0];
            	String method = line.split("#")[1].split(":")[0];
            	int lineNo = Integer.parseInt(line.split("#")[1].split(":")[1]);
            	String wholeName = packageName + "." + clazz;
            	String srcJavaFilePath = FileUtil.srcJavaDir + "/" + wholeName.replaceAll("\\.", "/") + ".java";
            	String binTestFilePath = FileUtil.binTestDir + "/" + wholeName.replaceAll("\\.", "/") + ".class";
            	String binJavaFilePath = FileUtil.binJavaDir + "/" + wholeName.replaceAll("\\.", "/") + ".class";
            	if (!new File(srcJavaFilePath).exists() && !new File(binTestFilePath).exists() && !new File(binJavaFilePath).exists()){
            		FileUtil.raiseException("suspiciousLocation: %s:%s has no corresponding java file: %s, srcJava class: %s or test class: %s", wholeName, lineNo, srcJavaFilePath, binJavaFilePath, binTestFilePath);
            	}
            	SuspiciousLocation sl = new SuspiciousLocation(wholeName, method, lineNo);
            	stmtList.add(sl); 
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
	

	public static List<TestCaseResult> readTestFile(String path){
		List<TestCaseResult> testsList = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            in.readLine(); 
            while ((line = in.readLine()) != null) {
            	if (line.length() == 0) logger.error("Empty line in %s", path);
            	
            	String testName = line.split(",")[0]; 
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
	public static Map<SuspiciousLocation, List<TestCaseResult>> parseMatrixForStmts(char[][] matrix, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		int specSize = slList.size();
		int testSize = testList.size();

	    Map<SuspiciousLocation, List<TestCaseResult>> stmtToTests = new HashMap<SuspiciousLocation, List<TestCaseResult>>();
	    
	    int totalPassedCnt = 0;
	    int totalFailedCnt = 0;
	    for (TestCaseResult ti : testList){
	    	if (ti.isSuccessful()){
	    		totalPassedCnt ++;
	    	}else{
	    		totalFailedCnt ++;
	    	}
	    }
	    
	    List<SuspiciousLocation> sslList = new ArrayList<>();
	    for (int i = 0; i < specSize; i++){
	    	int executedPassedCount = 0;
	    	int executedFailedCount = 0;
	    	
	    	SuspiciousLocation sl = slList.get(i);
	    	if (stmtToTests.containsKey(sl)){
	    		logger.warn("[parseMatrixFile] repeated SourceLocation in spectra: " + sl.toString());
	    		continue;
	    	}
	    	stmtToTests.put(sl, new ArrayList<TestCaseResult>());
	    	
			for (int j = 0; j < testSize; j++){ 
				if(matrix[j][i] == '1'){  
					stmtToTests.get(sl).add(testList.get(j));
					
					if(matrix[j][specSize] == '+'){ 
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
				return Double.compare(o2.getSuspValue(), o1.getSuspValue());
			}
		});
	    
	    String flOutputPath = FileUtil.toolDir + "/fl.txt";
	    FileUtil.writeToFile(flOutputPath, "", false);
	    
	    LinkedHashMap<SuspiciousLocation, List<TestCaseResult>> map = new LinkedHashMap<>();
		for (SuspiciousLocation source : sslList) {
			map.put(source, stmtToTests.get(source));
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

	public static Map<TestCaseResult, List<SuspiciousLocation>> parseMatrixForTests(char[][] matrix, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		int specSize = slList.size();
		int testSize = testList.size();
	    
	    Map<TestCaseResult, List<SuspiciousLocation>> testToStmts = new HashMap<TestCaseResult, List<SuspiciousLocation>>();
	    
	    for (int i = 0; i < testSize; i++){ 
	    	TestCaseResult tcResult = testList.get(i);
	    	
	    	if (testToStmts.containsKey(tcResult)){
	    		logger.warn("[parseMatrixFile] repeated TestCaseResult in matrix: " + tcResult.toString());
	    		continue;
	    	}
	    	testToStmts.put(tcResult, new ArrayList<SuspiciousLocation>());
	    	
	    	for (int j = 0; j < specSize; j++){ 
	    		if(matrix[i][j] == '1'){
	    			testToStmts.get(tcResult).add(slList.get(j));
	    		}
	    	}
	    }
	   
		return testToStmts;
	}

	public static char[][] getMatrixFromFile(String path, List<SuspiciousLocation> slList, List<TestCaseResult> testList) {
		
		int specSize = slList.size();
		int testSize = testList.size();
		char[][] matrix = new char[testSize][specSize + 1];
		int rowCnt = 0;


		String line;
		try(BufferedReader in = new BufferedReader(new FileReader(path))) {
			while ((line = in.readLine()) != null) {
				int realCol = 0; 
				for(int col = 0; col < line.length() - 1; col = col + 2){ 

					matrix[rowCnt][realCol] = line.charAt(col); 
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

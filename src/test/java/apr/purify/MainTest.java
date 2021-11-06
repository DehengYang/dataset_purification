package apr.purify;

import java.io.File;

import apr.purify.utils.FileUtil;
import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class MainTest extends TestCase{
	
    public void testSetParaMetersChart1()
    {
    	/*
    	 * For D4J Chart_1
    	 */
    	String input = parseParameters("Chart_1.txt");
    	
    	String expectedInput = "-srcJavaDir examples/Defects4J_Chart_1/source/" + "\n" + 
    			"-binJavaDir examples/Defects4J_Chart_1/build/" + "\n" + 
    			"-binTestDir examples/Defects4J_Chart_1/build-tests/" + "\n" + 
    			"-dependencies   examples/Defects4J_Chart_1/build/:examples/Defects4J_Chart_1/build-tests/:examples/Defects4J_Chart_1/lib/junit.jar:examples/Defects4J_Chart_1/lib/iText-2.1.4.jar:examples/Defects4J_Chart_1/lib/servlet.jar:/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../benchmarks/defects4j/framework/projects/lib/junit-4.11.jar" +"\n" +  
    			"-testExecPath /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/junit_external/PatchTest-0.0.1-SNAPSHOT-jar-with-dependencies.jar" +  "\n" + 
    			"-jvmPath /home/apr/env/jdk1.7.0_80/bin/" +  "\n" + 
    			"-failedTestsStr  org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests:" +"\n" +  
    			"-gzoltarDir /mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/../libs/gzoltar1.7.3 "+"\n" + 
    			"-logDir output/Defects4J_Chart_1" + "\n";
    	assertEquals(input, expectedInput.replaceAll("\n", " ").replaceAll("\\s+"," "));
    	assertEquals(FileUtil.binJavaDir, "/home/apr/apr_tools/datset_purification_2020/purification/purify/examples/Defects4J_Chart_1/build");
    	assertEquals(FileUtil.gzoltarDir, "/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/libs/gzoltar1.7.3");
    }
    
    /**
     * @Description 
     * projId: Chart_1.txt
     * @author apr
     * @version Oct 4, 2020
     *
     * @param projId
     * @return
     */
    public static String parseParameters(String projId){
    	File resDirectory = new File("src/test/resources");
    	String chartFilePath = resDirectory.getAbsoluteFile() + "/" + projId;
    	
    	String input = FileUtil.readFileToStr(chartFilePath);
    	input = input.replaceAll("\n", " ").replaceAll("\\s+"," ");
    	
    	String[] args = input.split(" ");
        
    	Main.setParameters(args);
    	
    	return input;
    }
    
    public static String readFile(String path, String fileName){
		File resDirectory = new File(path); //"src/test/resources"
    	String chartFilePath = resDirectory.getAbsoluteFile() + "/" + fileName; //
    	
    	String input = FileUtil.readFileToStr(chartFilePath);
    	
    	return input;
	}
    
    public static String getResourceFilePath(String path, String fileName){
		File resDirectory = new File(path); //"src/test/resources"
    	String chartFilePath = resDirectory.getAbsoluteFile() + "/" + fileName; //
    	
    	return chartFilePath;
	}
}

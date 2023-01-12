/**
 * 
 */
package apr.purify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.utils.FileUtil;
import apr.purify.utils.GeneralUtil;

/**
 * @author apr
 * @version Oct 14, 2020
 *
 */
public class Configuration {
	final static Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	public static String diffPyPath = FileUtil.readAndWriteResourceByPath("/getDiff2.py");
	//public static String diffPyPath = "/home/apr/apr_tools/datset_purification_2020/purification/purify/src/main/resources/getDiff2.py";

	public static String srcJavaDirBk;

	public static int MAX_MUTANTS = 10000;
	public static String mutDir = GeneralUtil.removeSlash(FileUtil.toolDir + "/mutant");
	
	public static String D4J_DIR; //= "/mnt/recursive-repairthemall/RepairThemAll/benchmarks/defects4j";
	
	public static String JAVA7_HOME; //= "/home/apr/env/jdk1.7.0_80/";
	public static String JAVA8_HOME; //= "/home/apr/env/jdk1.8.0_202/";

	public static String COMMENT = "//[apr-purify-comment]";
	
	// time out in seconds when testing defects4j bugs 
	public static int TIMEOUT_TEST_SINGLE = 200;  
	public static int TIMEOUT_TEST_ALL = 600;
	// time out for dd in minutes
	public static int TIMEOUT_DD = 90;
}

/**
 * 
 */
package apr.purify.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apr.purify.Configuration;
import apr.purify.diff.DiffUtil;

/**
 * @author apr
 * @version Oct 3, 2020
 *
 */
public class FileUtil {
	final static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	// parameters
	public static String srcJavaDir;
	public static String binJavaDir;
	public static String binTestDir;
	public static String dependencies;
//	public static String testExecPath;
	public static String jvmPath;
	public static String failedTestsStr;
	public static String gzoltarDir;
	public static int timeout;
	public static String patchDiffPath;
	public static Dataset dataset;
	public static String projDir;
	
	public enum Dataset
	{
	    BEARS, DEFECTS4J, BUGSJAR;
	}
	//
	public static ArrayList<String> depsList = new ArrayList<>();
	public static List<String> oriFailedTests = new ArrayList<>();
	public static String logPath;
	public static String toolDir;
	public static List<String> gzFailingTestCases = new ArrayList<>();
	
	// constant vars
	public static String toolName = "purify"; 
	
	// added vars 
	public static int mutantCnt = 0;
	
	/**
	 * @Description 
	 * default: logPath & append is true
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param content
	 */
	public static void writeToFile(String content){
		writeToFile(logPath, content, true);
	}
	
	/**
	 * @Description
	 * write to file with format (add "\n" at the end) 
	 * @author apr
	 * @version Oct 12, 2020
	 *
	 * @param format
	 * @param args
	 */
	public static void writeToFileWithFormat(String format, Object... args){
		writeToFile(logPath, String.format(format + "\n", args), true);
	}

	public static void writeToFile(String path, String content){
		writeToFile(path, content, true);
	}

	public static void writeToFile(String path, String content, boolean append){
		// get dir
		if (path.contains("/")){
			String dirPath = path.substring(0, path.lastIndexOf("/"));
			File dir = new File(dirPath);
			if (!dir.exists()){
				dir.mkdirs();
				System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
			}
		}

		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(path, append));
			output.write(content);
			//				output.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}finally {
			try {
				output.close();
			} catch (IOException ex) {
				// Log error writing file and bail out.
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * @Description
	 * write a list of lines to file 
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param path
	 * @param lines
	 */
	public static void writeLinesToFile(String path, List<String> lines){
		writeLinesToFile(path, lines, false);
	}
	
	public static void writeLinesToFile(String path, Set<String> lines){
		writeLinesToFile(path, lines, false);
	}
	
	public static void writeLinesToFile(String path, Set<String> lines,  boolean append){
		List<String> linesList = new ArrayList<>();
		linesList.addAll(lines);
		writeLinesToFile(path, linesList, append);
	}
	
	public static void writeLinesToFile(String path, List<String> lines, boolean append){
		// get dir
//		path = "1.txt";//debug
		if (path.contains("/")){
			String dirPath = path.substring(0, path.lastIndexOf("/"));
			File dir = new File(dirPath);
			if (!dir.exists()){
				dir.mkdirs();
				System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
			}
		}
		
		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(path, append));
			for(String line : lines){
				output.write(line + "\n");
			}
        } catch (final IOException e) {
            e.printStackTrace();
        }finally {
            try {
            	output.close();
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
        }
	}
	
	/**
	 * @Description
	 * read file from its path
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param path
	 * @return
	 */
	public static List<String> readFile(String path){
		List<String> list = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
//            	if (line.length() == 0) System.err.println(String.format("Empty line in %s", path));
            	list.add(line); // add line
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }finally {
            try {
            	in.close();
            } catch (IOException ex) {
                // Log error writing file and bail out.
            	ex.printStackTrace();
            }
        }
        return list;
	}
	
	public static List<String> readFileForLocCnt(String path){
		List<String> list = new ArrayList<>();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
            	if (line.isEmpty()) {
            		continue;
            	}
            	if (line.startsWith("//")){
            		continue;
            	}
            	list.add(line); // add line
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }finally {
            try {
            	in.close();
            } catch (IOException ex) {
                // Log error writing file and bail out.
            	ex.printStackTrace();
            }
        }
        return list;
	}
	
	/**
	 * @Description (add \n) read file from file path and output String
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param input
	 * @return
	 */
	public static String readFileToStr(String path){
		StringBuilder buffer = new StringBuilder();
		BufferedReader in = null;
		try {
            in = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
            	String lineWithBreak = line + "\n";
            	buffer.append(lineWithBreak); // add line
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
        return buffer.toString();
	}
	
	/**
	 * @Description
	 * raise an exception when necessary 
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param expInfo
	 */
	public static void raiseException(String expInfo) {
		try {
			throw new Exception(String.format("===raiseException: %s", expInfo));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	/**
	 * @Description
	 * string with format 
	 * @author apr
	 * @version Oct 12, 2020
	 *
	 * @param format
	 * @param args
	 */
	public static void raiseException(String format, Object... args){
		try {
			throw new Exception(String.format(format, args));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	/**
	 * @Description
	 * concat strings with : and drop the last : 
	 * @author apr
	 * @version Oct 4, 2020
	 *
	 * @param list
	 * @return
	 */
	public static String concatAndDropLastChar(List<String> list){
		String output = "";
		for (String str : list){
			output += str + ":";
		}
		output = output.substring(0, output.length() - 1);
		return output;
	}

	/**
	 * @Description 
	 * @author apr
	 * @version Oct 12, 2020
	 *
	 *refer to: <br>
	 *<a href="https://stackoverflow.com/questions/17351043/how-to-get-absolute-path-to-file-in-resources-folder-of-your-project">How to get absolute path to file in /resources folder of your project</a>
	 * 
	 * however, this still does not work. (the res can be obtained but file is null)
	 * I leave it as future work.
	 * 
	 * @param rscName: resourceFileName
	 * @return
	 */
	public static String getResourceFilePath(String rscName){
		//rscName: getDiff2.py without /
		URL res = FileUtil.class.getClassLoader().getResource(rscName);
		File file = null;
		try {
			file = Paths.get(res.toURI()).toFile();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		String absolutePath = file.getAbsolutePath();
		
		return absolutePath;
	}
	
	/**
	 * @Description
	 * e.g., write getDiff2.py from src/main/resource/ to work folder.
	 * <br>
	 * Must notice that getDiff2.py obtained from the FileUtil.class.getResourceAsStream() 
	 * is always the file in jar rather than in the directory we general edit!
	 * <br>
	 * So every time we modify the getDiff2.py, we need to re-package if we want to do remote debugging from terminal cmd
	 * 
	 * @author apr
	 * @version Oct 13, 2020
	 *
	 * @param rscName
	 */
	public static String readAndWriteResourceByPath(String rscName){
		String pathToWrite = System.getProperty("user.dir") + "/" + rscName;
		if (new File(pathToWrite).exists()){
			boolean delete = new File(pathToWrite).delete(); // first delete, otherwise, the Inputstream will be from this pathToWrite rather than my project jar.
			logger.debug("delete file before writing: {}, result: {}", pathToWrite, delete);
		}
		
		//rscName: /getDiff2.py
		InputStream in = FileUtil.class.getResourceAsStream(rscName);
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder out = new StringBuilder();
		Reader reader = null;
		try {
			reader = new InputStreamReader(in, "UTF-8");
			for (; ; ) {
			    int rsz = reader.read(buffer, 0, buffer.length);
			    if (rsz < 0)
			        break;
			    out.append(buffer, 0, rsz);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
//		logger.debug(out.toString());
		logger.info("write fileName: {} (length: {}) into path: {}", rscName, out.toString().length(), pathToWrite);
		FileUtil.writeToFile(pathToWrite, out.toString(), false);
		
		return pathToWrite;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 13, 2020
	 *
	 * @param line
	 * @param separator
	 * @return
	 */
	public static String getLastSplit(String line, String separator) {
		String[] splits = line.split(separator);
		String last = splits[splits.length - 1];
		return last;
	}

	public static void backup(){
		backup(FileUtil.srcJavaDir, Configuration.srcJavaDirBk);
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param srcJavaDir2
	 */
	public static void backup(String folder, String targetDir) {
		File file = new File(targetDir);
		if (!file.exists()) {
			try {
				FileUtils.copyDirectory(new File(folder), file);
				writeToFileWithFormat("Backup folder: %s to folder: %s", folder, targetDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			writeToFileWithFormat("Folder: %s exists! Skip backup.", folder);
		}
	}
	
	public static void restore(){
		restore(FileUtil.srcJavaDir, Configuration.srcJavaDirBk);
	}
			
	public static void restore(String folder, String targetDir){
		File file = new File(targetDir);
		if (file.exists()) {
			try {
				FileUtils.deleteDirectory(new File(folder));
				FileUtils.copyDirectory(file, new File(folder));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			FileUtil.raiseException("Restore folder: %s failed! Cannot find folder: %s", folder, targetDir);
		}
	}

	/** @Description 
	 * @author apr
	 * @version Oct 15, 2020
	 *
	 * @param patchFolder
	 */
	public static void remove(String patchFolder) {
		if (new File(patchFolder).exists()){
			try {
				FileUtils.deleteDirectory(new File(patchFolder));
				logger.info("[init] remove folder: {}", patchFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
	}

	/** @Description 
	 * @author apr
	 * @version Oct 15, 2020
	 *
	 * @param string
	 */
	public static void mark(String string) {
		writeToFile(String.format("\n\n======= %s ======\n\n", string));
	}
	
	public static void printTime(){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("Current time: " + df.format(new Date()));
	}
	
	public static String countTime(long startTime){
		DecimalFormat dF = new DecimalFormat("0.0000");
		return dF.format((float) (System.currentTimeMillis() - startTime)/1000);
	}

	/** @Description 
	 * @author apr
	 * @version Oct 20, 2020
	 *
	 * @param filePath
	 */
	public static void removeFile(String filePath) {
		File file = new File(filePath);
		if (file.isFile() && file.exists()){
			file.delete();
		}
	}
	
}

/**
 * 
 */
package apr.purify.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apr
 * @version Oct 12, 2020
 *
 */
public class GeneralUtil {
	final static Logger logger = LoggerFactory.getLogger(GeneralUtil.class); 
	public static int locCnt = 0;
	public static int locCntNotEmpty = 0;
	
	/**
	 * @Description 
	 * learned from patchparser but revised according to my need.</br>
	 * refer to: <br>
	 * <a href="https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java">How to read all files in a folder from Java?</a>
	 * <br>
	 * https://www.runoob.com/java/java8-streams.html
	 * <br>
	 * <a href="https://stackoverflow.com/questions/51686465/local-variable-count-defined-in-an-enclosing-scope-must-be-final-or-effectively">https://stackoverflow.com/questions/51686465/local-variable-count-defined-in-an-enclosing-scope-must-be-final-or-effectively</a>
	 * @author apr
	 * @version Oct 12, 2020
	 *
	 * @param folder
	 * @return
	 */
	public static int countLoc(String folder){
		locCnt = 0; //init
		locCntNotEmpty = 0;
		
//		// try-with-resources
//		try (Stream<Path> paths = Files.walk(Paths.get(folder))) { //"/home/you/Desktop"
//		    paths
//		        .filter(path -> path.toAbsolutePath().toString().endsWith(".java")) //Files::isRegularFile
//		        .forEach(path -> {
//		        	String filePath = path.toAbsolutePath().toString();
//		        	locCnt += FileUtil.readFile(filePath).size();
//		        	locCntNotEmpty += FileUtil.readFileForLocCnt(filePath).size();
//		        	logger.debug("path: {}", path.toAbsolutePath().toString());
//		        });
//		    FileUtil.writeToFileWithFormat("all files cnt in %s: %s, all java files cnt: %s", 
//		    		folder, paths.count(), paths.filter(path -> path.toAbsolutePath().toString().endsWith(".java")).count());
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
		
		// more flexible approach
		// try-with-resources
		List<Path> allPaths = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(Paths.get(folder))) { //"/home/you/Desktop"
			paths
//			.filter(path -> path.toAbsolutePath().toString().endsWith(".java")) //Files::isRegularFile
			.forEach(path -> {
				String filePath = path.toAbsolutePath().toString();
				if (new File(filePath).isFile()){
					allPaths.add(path);
					if(!filePath.endsWith(".java")){
						FileUtil.writeToFileWithFormat("non_java file: %s", path.toAbsolutePath().toString());
					}
				}
			});
			
			int javaFileCnt = 0;
			for(Path path : allPaths){
				if (path.toAbsolutePath().toString().endsWith(".java")){
					javaFileCnt ++;
					String filePath = path.toAbsolutePath().toString();
					locCnt += FileUtil.readFile(filePath).size();
		        	locCntNotEmpty += FileUtil.readFileForLocCnt(filePath).size();
				}
			}
			
			FileUtil.writeToFileWithFormat("all files cnt in %s: %s, all java files cnt: %s", folder, allPaths.size(), javaFileCnt);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		// old approach in patchParser
//		List<File> allJavaFiles = FileHelper.getAllFiles(folder, ".java");
//		int locCnt = 0;
//		for (File file : allJavaFiles) {
//			if (file.getPath().toLowerCase(Locale.ENGLISH).contains("test")) continue;
//			locCnt += FileUtil.readFile(file.getPath()).size();
//		}
		FileUtil.writeToFileWithFormat("Total loc of path (%s): %s", folder, locCnt);
		FileUtil.writeToFileWithFormat("Total loc of path (%s) without empty lines and // comments: %s", folder, locCntNotEmpty);
		
		return locCnt;
	}

	public static String listToString(List<?> list){
		StringBuilder sb = new StringBuilder();
		for (Object ob : list){
			sb.append(ob);
		}
		return sb.toString();
	}
	
	public static String listToStringWithSep(List<?> list, String separator){
		StringBuilder sb = new StringBuilder();
		for (Object ob : list){
			sb.append(ob + separator);
		}
		return sb.toString();
	}
	
	public static String listToStringAddLineBreakWithHeader(String header, String ender, List<?> list){
//		return header + "\n" + listToStringWithSep(list, "\n") + ender + "\n";
		
		String separator = "\n";
		
		StringBuilder sb = new StringBuilder();
		for (Object ob : list){
			sb.append(header + "\n" + ob + separator + ender + "\n");
		}
		return sb.toString();
		
//		StringBuilder sb = new StringBuilder();
//		for (Object ob : list){
//			sb.append(ob + "\n");
//		}
//		return sb.toString();
	}
	
	public static String listToStringAddLineBreak(List<?> list){
		return listToStringWithSep(list, "\n");
//		StringBuilder sb = new StringBuilder();
//		for (Object ob : list){
//			sb.append(ob + "\n");
//		}
//		return sb.toString();
	}
	
//	public static String listToString(List<?> list, int startLineNo, int endLineNo){
//		StringBuilder sb = new StringBuilder();
//		int lineNo = 1;
//		for (Object ob : list){
//			if (startLineNo <= lineNo && lineNo <= endLineNo){
//				sb.append(ob);
//			}
//			lineNo ++;
//			
//			if (lineNo > endLineNo){
//				break;
//			}
//		}
//		return sb.toString();
//	}
	
	public static String listToStringAddLineBreak(List<?> list, int startLineNo, int endLineNo){
		StringBuilder sb = new StringBuilder();
		int lineNo = 1;
		for (Object ob : list){
			if (startLineNo <= lineNo && lineNo <= endLineNo){
//				if (ob instanceof String){
//					if (((String) ob).endsWith("\n")){
//						logger.debug("");
//					}
//				}
				
				sb.append(ob + "\n");
			}
			lineNo ++;
			
			if (lineNo > endLineNo){
				break;
			}
		}
		return sb.toString();
	}
	
	public static String removeSlash(String path){
		if (path.endsWith("/")){
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
	
	/** @Description 
	 * @author apr
	 * @version Oct 14, 2020
	 *
	 * @param srcJavaDir
	 * @return
	 */
	public static String getBkDir(String folder) {
		if (folder.endsWith("/")){
			folder = folder.substring(0, folder.length() - 1);
		}
		
		return folder + "_ori";
	}
	
	public static String getLastSplit(String str, String separator){
		String[] split = str.split(separator);
		String last = split[split.length - 1];
		
		return last;
	}
}

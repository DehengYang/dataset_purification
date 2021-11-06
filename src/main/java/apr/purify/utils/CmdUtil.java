package apr.purify.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdUtil {
	final static Logger logger = LoggerFactory.getLogger(CmdUtil.class);
	
	public static String runCmd(String cmd) {
		return runCmd(cmd, true);
	}
		
	public static String runCmd(String cmd, boolean writeCmdAndOutput) {	
		if (writeCmdAndOutput){
			FileUtil.writeToFileWithFormat("cmd to run: %s", cmd);
		}
		
		logger.info("cmd to run: {}", cmd);
		
		StringBuilder sb = new StringBuilder();
		
		try{
			String[] commands = {"bash", "-c", cmd};
			Process proc = Runtime.getRuntime().exec(commands);
			
			// buggy code
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			
			// still buggy.
//			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			// fixed
			sb.append(IOUtils.toString(proc.getInputStream(), Charset.defaultCharset()));
			String stderr = IOUtils.toString(proc.getErrorStream(), Charset.defaultCharset()); // fix a bug. seemingly proc.getErrorStream() should be after proc.getInputStream()
			
			// read error if exists
			if(!stderr.equals("")){
				System.err.println(String.format("Error/Warning occurs:\n %s\n", stderr)); // .substring(0, 300): avoid printing too long cmd string.
			}
		}catch (Exception err){
			err.printStackTrace();
		}
		
		return sb.toString();
	}
	
	/**
	 * @Description
	 * SimFix project 
	 * @author apr
	 * @version Oct 15, 2020
	 *
	 * @param command
	 * @return
	 */
	public static List<String> execute(String[] command) {
		Process process = null;
		final List<String> message = new ArrayList<String>();
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			final InputStream inputStream = process.getInputStream();
			
			Thread processReader = new Thread(){
				public void run() {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					String line;
					try {
						while((line = reader.readLine()) != null) {
							message.add(line);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			processReader.start();
			try {
				processReader.join();
				process.waitFor();
			} catch (InterruptedException e) {
				return new LinkedList<>();
			}
		} catch (IOException e) {
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
		
		return message;
	}
	
	public static void runCmdNoOutput(String cmd) {
		logger.info("cmd to run: {}", cmd);
		try{
			String[] commands = {"bash", "-c", cmd};
			Process proc = Runtime.getRuntime().exec(commands);

			// fixed
//			String stderr = IOUtils.toString(proc.getErrorStream(), Charset.defaultCharset());
			
			IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
//			proc.getInputStream();  // this is not correct.
			
			// read error if exists
//			if(!stderr.equals("")){
//				System.err.println(String.format("Error/Warning occurs:\n %s\n", stderr)); // .substring(0, 300): avoid printing too long cmd string.
//			}
		}catch (Exception err){
			err.printStackTrace();
		}
	}
	
	
	// a bug exposed by Closure 103 run all tests.
	// the while ((line = stdInput.readLine()) != null){ will get stuck... after reading several lines.
	// the solution is to use commons-io, refer to : https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program
	
//	public static String runCmd(String cmd) {
//		String output = "";
//		
//		try{
//			String[] commands = {"bash", "-c", cmd};
//			Process proc = Runtime.getRuntime().exec(commands);
//			
//			// buggy code
////			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
////			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
//			
//			// still buggy.
//			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//			
//			// fixed
//			String stderr = IOUtils.toString(proc.getErrorStream(), Charset.defaultCharset());
//			String stdout = IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
//			
//			// read output
//			String line = null;
//			while ((line = stdInput.readLine()) != null){
//				System.out.println(line);
//				output += line + "\n";
//			}
//			
//			// read error if exists
//			String error = "";
//			while ((line = stdError.readLine()) != null){
//				error += line + "\n";
//			}
//			if(!error.equals("")){
//				System.err.println(String.format("Error/Warning occurs:\n %s\n", error)); // .substring(0, 300): avoid printing too long cmd string.
//			}
//		}catch (Exception err){
//			err.printStackTrace();
//		}
//		
//		return output;
//	}
	
	/**
	 * @Description the second version of running cmd 
	 * @author apr
	 * @version Mar 29, 2020
	 *
	 * @param cmd
	 * @return
	 */
	public static String runCmd2(String cmd) {
		logger.info("cmd to run: {}", cmd);
		StringBuilder output = new StringBuilder();
		
		try{			
			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.command("bash", "-c", cmd);
			Process process = processBuilder.start();
			
//			InputStream is = process.getInputStream();
//			String stderr = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
			String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			// read output
			String line = null;
			while ((line = stdInput.readLine()) != null){
				System.out.println(line);
				output.append(line + "\n");
			}
			
			// read error if exists
			String error = "";
			while ((line = stdError.readLine()) != null){
				error += line + "\n";
			}
			if(!error.equals("")){
				System.err.println(String.format("Error/Warning occurs:\n %s\n", error)); // .substring(0, 300): avoid printing too long cmd string.
			}
		}catch (Exception err){
			err.printStackTrace();
		}
		
		return output.toString();
	}
}
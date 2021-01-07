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
			

			sb.append(IOUtils.toString(proc.getInputStream(), Charset.defaultCharset()));
			String stderr = IOUtils.toString(proc.getErrorStream(), Charset.defaultCharset()); 
			
			if(!stderr.equals("")){
				System.err.println(String.format("Error/Warning occurs:\n %s\n", stderr)); 
			}
		}catch (Exception err){
			err.printStackTrace();
		}
		
		return sb.toString();
	}
	

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

			

			
			IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());

			
			



		}catch (Exception err){
			err.printStackTrace();
		}
	}
	public static String runCmd2(String cmd) {
		logger.info("cmd to run: {}", cmd);
		StringBuilder output = new StringBuilder();
		
		try{			
			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.command("bash", "-c", cmd);
			Process process = processBuilder.start();
			


			String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			
			String line = null;
			while ((line = stdInput.readLine()) != null){
				System.out.println(line);
				output.append(line + "\n");
			}
			
			
			String error = "";
			while ((line = stdError.readLine()) != null){
				error += line + "\n";
			}
			if(!error.equals("")){
				System.err.println(String.format("Error/Warning occurs:\n %s\n", error)); 
			}
		}catch (Exception err){
			err.printStackTrace();
		}
		
		return output.toString();
	}
}
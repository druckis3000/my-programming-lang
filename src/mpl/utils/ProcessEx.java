package mpl.utils;

import java.io.File;
import java.io.IOException;

import mpl.utils.io.StreamGobbler;

public class ProcessEx {

	private String[] cmdArray;
	private String command;
	private String dir;
	//private boolean waitFor;
	
	public ProcessEx(String ...cmds) {
		cmdArray = cmds;
	}
	
	public ProcessEx(String dir, String ...cmds) {
		cmdArray = cmds;
		this.dir = dir;
	}
	
	public ProcessEx(String command/*, boolean waitFor*/) {
		this.command = command;
		//this.waitFor = waitFor;
	}
	
	public ProcessEx(String command, String dir/*, boolean waitFor*/) {
		this.command = command;
		this.dir = dir;
		//this.waitFor = waitFor;
	}
	
	public int start(){
		Process p = null;
		
		try {
			if(cmdArray != null) {
				ProcessBuilder procBuilder = new ProcessBuilder(cmdArray);
				if(dir != null) {
					procBuilder.directory(new File(dir));
				}
				p = procBuilder.start();
			}else
				if(dir != null)
					p = Runtime.getRuntime().exec(command, new String[]{}, new File(dir));
				else
					p = Runtime.getRuntime().exec(command);
			
			new StreamGobbler(p.getInputStream()).start();
			new StreamGobbler(p.getErrorStream()).start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int exitValue = p.exitValue();

		if(exitValue != 0){
			String cmd = "";
			if(cmdArray != null) cmd = cmdArray[0];
			else cmd = command.substring(0, command.indexOf(" "));
			
			System.out.println(cmd + ": returned " + p.exitValue() + " exit status");
		}
		
		return exitValue;
	}
	
	public String getCommand(){
		return command;
	}
}
package mpl.utils;


public class Env {

	public static String getElfExtension(){
		switch(OperatingSystem.getOS()){
		case OperatingSystem.OS_WINDOWS:
			return "obj";
		case OperatingSystem.OS_UNIX:
			return "o";
		default:
			return "o";
		}
	}
	
	public static String getElfFormat(){
		switch(OperatingSystem.getOS()){
		case OperatingSystem.OS_WINDOWS:
			return "win";
		case OperatingSystem.OS_UNIX:
			return "elf";
		default:
			return "elf";
		}
	}
	
	public static String getExecutableExtension(){
		switch(OperatingSystem.getOS()){
		case OperatingSystem.OS_WINDOWS:
			return "exe";
		case OperatingSystem.OS_UNIX:
			return "";
		default:
			return "";
		}
	}
}
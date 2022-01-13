package mpl.utils;

public class OperatingSystem {
	public static final int OS_UNIX = 1;
	public static final int OS_WINDOWS = 2;
	
	private static int operatingSystem = 0;
	
	public static final int getOS(){
		if(operatingSystem == 0){
			String osName = System.getProperty("os.name").toLowerCase();
			
			if(osName.indexOf("win") >= 0)
				operatingSystem = OS_WINDOWS;
			else if(osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") >= 0)
				operatingSystem = OS_UNIX;
		}
		
		return operatingSystem;
	}
}
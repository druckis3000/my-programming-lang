package mpl.compiler;

public class CompilerDebugger {
	public static final int DEBUG_NONE = 0;
	public static final int DEBUG_REGISTER_USAGE = 1;
	
	public static int debugType = DEBUG_NONE;
	
	public static boolean isDebugEnabled(int debug){
		return (debugType & debug) == debug;
	}
}
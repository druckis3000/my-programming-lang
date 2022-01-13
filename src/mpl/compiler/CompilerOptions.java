package mpl.compiler;

import java.io.File;

public class CompilerOptions {

	public static final int COMPILE_FORMAT_BIN = 1;
	public static final int COMPILE_FORMAT_ELF = 2;
	public static final int COMPILE_FORMAT_ASM = 3;
	public static final int COMPILE_FORMAT_ALL = 4;
	public static final int COMPILE_FORMAT_LIB = 5;
	
	public static final int COMPILE_ARCH_32 = 5;
	public static final int COMPILE_ARCH_64 = 6;
	
	public File inputFile = null;
	public File outputFile = null;
	public int outputFormat = COMPILE_FORMAT_BIN;
	public int outputArch = COMPILE_ARCH_32;
}
package mpl.compiler.asm;

import mpl.compiler.CompilerOptions;

public class AsmSizes {

	public static String getSizeDirective(){
		return AssemblyCodeBuilder.ARCH == CompilerOptions.COMPILE_ARCH_64 ? "QWORD" : "DWORD";
	}
}

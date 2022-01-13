package mpl.compiler.asm;

import mpl.compiler.CompilerOptions;
import mpl.syntactic.parts.PDataType;

public enum AsmRegister {
	EAX("al", "ax", "eax", "rax"),
	EBX("bl", "bx", "ebx", "rbx"),
	ECX("cl", "cx", "ecx", "rcx"),
	EDX("dl", "dx", "edx", "rdx"),
	
	ESI("", "si", "esi", "rsi"),
	EDI("", "di", "edi", "rdi"),
	
	ESP("", "sp", "esp", "rsp"),
	EBP("", "bp", "ebp", "rbp");

	private String byteName;
	private String wordName;
	private String dwordName;
	private String qwordName;
	
	private AsmRegister(String byteName, String wordName, String dwordName, String qwordName){
		this.byteName = byteName;
		this.wordName = wordName;
		this.dwordName = dwordName;
		this.qwordName = qwordName;
	}
	
	public String getNameByDataType(PDataType dt){
		// All pointers are 4-bytes in 32-bit architecture
		// And 8-bytes in 64-bit architecture
		if(dt.isPointer())
			return AssemblyCodeBuilder.ARCH == CompilerOptions.COMPILE_ARCH_64 ? qwordName : dwordName;

		// Char is only 1-byte
		if(dt.plain() == PDataType.CHAR || dt.plain() == PDataType.BOOL)
			return byteName;
		
		// Short is only 2-bytes
		if(dt.plain() == PDataType.SHORT)
			return wordName;
		
		// Other data types depends on the architecture 4 or 8 bytes
		return AssemblyCodeBuilder.ARCH == CompilerOptions.COMPILE_ARCH_64 ? qwordName : dwordName;
	}
	
	public String getNameBySizeDirective(String sizeDirective){
		if(sizeDirective == "WORD")
			return wordName;
		else if(sizeDirective == "DWORD")
			return dwordName;
		else if(sizeDirective == "BYTE")
			return byteName;
		
		return dwordName;
	}
	
	public String getName(){
		if(AssemblyCodeBuilder.ARCH == CompilerOptions.COMPILE_ARCH_64)
			return qwordName;
		return dwordName;
	}
	
	public static boolean isRegister(String str){
		switch(str){
		case "rax":
		case "rbx":
		case "rcx":
		case "rdx":
		case "rsi":
		case "rdi":
		case "rsp":
		case "rbp":
			
		case "eax":
		case "ebx":
		case "ecx":
		case "edx":
		case "esi":
		case "edi":
		case "esp":
		case "ebp":
			
		case "ax":
		case "bx":
		case "cx":
		case "dx":
		case "si":
		case "di":
		case "sp":
		case "bp":

		case "al":
		case "bl":
		case "cl":
		case "dl":
			
			return true;
			
		default:
			return false;
		}
	}
}
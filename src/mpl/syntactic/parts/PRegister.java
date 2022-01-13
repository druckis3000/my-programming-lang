package mpl.syntactic.parts;

import mpl.compiler.asm.AsmRegister;

public class PRegister extends PProgramPart {
	public AsmRegister register = null;
	
	public PRegister(AsmRegister reg, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.register = reg;
	}
	
	public PRegister(AsmRegister reg) {
		this(reg, "", -1, -1, null);
	}

	public static PRegister fromString(String register){
		PRegister reg = null;
		
		switch(register){
		case "rax":
		case "eax":
		case "ax":
		case "al":
			reg = new PRegister(AsmRegister.EAX); break;
		case "rbx":
		case "ebx":
		case "bx":
		case "bl":
			reg = new PRegister(AsmRegister.EBX); break;
		case "rcx":
		case "ecx":
		case "cx":
		case "cl":
			reg = new PRegister(AsmRegister.ECX); break;
		case "rdx":
		case "edx":
		case "dx":
		case "dl":
			reg = new PRegister(AsmRegister.EDX); break;
		case "rsi":
		case "esi":
		case "si":
			reg = new PRegister(AsmRegister.ESI); break;
		case "rdi":
		case "edi":
		case "di":
			reg = new PRegister(AsmRegister.EDI); break;
		case "rsp":
		case "esp":
		case "sp":
			reg = new PRegister(AsmRegister.ESP); break;
		case "rbp":
		case "ebp":
		case "bp":
			reg = new PRegister(AsmRegister.EBP); break;
			
		default:
			reg = null;
		}
		
		return reg;
	}
	
	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		return register.getName();
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		return new PRegister(register, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
}

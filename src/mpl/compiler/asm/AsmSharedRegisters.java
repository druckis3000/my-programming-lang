package mpl.compiler.asm;

import java.util.Stack;

import mpl.compiler.CompilerDebugger;

public class AsmSharedRegisters {
	private Stack<AsmRegister> freeRegisters = new Stack<AsmRegister>();
	
	public AsmSharedRegisters() {
		freeRegisters.push(AsmRegister.ECX);
		freeRegisters.push(AsmRegister.EBX);
	}

	public void freeUpRegister(AsmRegister register){
		if(CompilerDebugger.isDebugEnabled(CompilerDebugger.DEBUG_REGISTER_USAGE)){
			StackTraceElement el[] = Thread.currentThread().getStackTrace();
			System.out.println("freeUpRegister(" + el[2].getClassName() + "." + el[2].getMethodName() + "): " + register);
		}
		
		if(!freeRegisters.contains(register))
			freeRegisters.push(register);
	}
	
	public AsmRegister getFreeRegister(){
		if(freeRegisters.empty())
			return null;

		AsmRegister reg = freeRegisters.pop();
		
		if(CompilerDebugger.isDebugEnabled(CompilerDebugger.DEBUG_REGISTER_USAGE)){
			StackTraceElement el[] = Thread.currentThread().getStackTrace();
			System.out.println("getFreeRegister(" + el[2].getClassName() + "." + el[2].getMethodName() + "): " + reg);
		}
		
		return reg;
	}
}
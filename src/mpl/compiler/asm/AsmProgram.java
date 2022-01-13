package mpl.compiler.asm;

import mpl.compiler.asm.sections.AsmSectionData;
import mpl.compiler.asm.sections.AsmSymbols;
import mpl.syntactic.parts.PProgram;

public class AsmProgram {

	public PProgram program;
	public AsmSharedRegisters registers = new AsmSharedRegisters();
	public AsmSectionData roData = new AsmSectionData();
	public AsmSymbols symbols = new AsmSymbols();
	
	public AsmProgram(PProgram program) {
		this.program = program;
	}
}
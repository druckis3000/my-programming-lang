package mpl.parser.syntactic.parts;

import mpl.compiler.asm.AsmRegister;
import mpl.compiler.asm.AsmSizes;

public class PStackElement extends PProgramPart {
	public int stackOffset = 0;
	
	public PStackElement(int stackOffset) {
		super("", -1, -1, null);
		this.stackOffset = stackOffset;
	}
	
	public PStackElement() {
		this(0);
	}
	
	public String getSizeDirective(){
		return AsmSizes.getSizeDirective();
	}

	@Override
	protected String getAstCode(String padding) {
		return null;
	}

	@Override
	public String toString() {
		return "[" + AsmRegister.ESP.getName() + " + " + stackOffset + "]";
	}
	
	public String toStringWithSizeDirective() {
		return AsmSizes.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + stackOffset + "]";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		return new PStackElement(stackOffset);
	}
}
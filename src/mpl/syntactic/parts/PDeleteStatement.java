package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PDeleteStatement extends PProgramPart {
	public PVarAccessor var;
	
	public PDeleteStatement(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}

	public void verify(){
		var.verify(true);
		
		// Check if variable is a pointer
		if(!var.getDataType().isPointer()){
			// If no, throw an error
			Console.throwError(Console.ERROR_DELETING_NON_POINTER, sourceFileName, var.lineInSourceCode, var.columnInSourceCode, var.target.name);
		}
	}
	
	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString(){
		return "delete " + var;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		return new PDeleteStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
}
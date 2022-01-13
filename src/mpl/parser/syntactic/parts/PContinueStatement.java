package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PContinueStatement extends PProgramPart {

	public PContinueStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void verify(){
		if(findParentFlowControlStatement() == null){
			Console.throwError(Console.ERROR_UNEXPECTED_STATEMENT, sourceFileName, lineInSourceCode, columnInSourceCode, "continue");
		}
	}
	
	public PProgramPart findParentFlowControlStatement(){
		// Loop through parent parts, and find flow control statement
		PProgramPart flowControlStatement = parent;
		while(!(flowControlStatement instanceof PProgram)){
			flowControlStatement = flowControlStatement.parent;
			
			if((flowControlStatement instanceof PDoStatement) || (flowControlStatement instanceof PWhileStatement)
					|| (flowControlStatement instanceof PForStatement)){
				// We found an appropriate flow control statement
				return flowControlStatement;
			}
		}
		return null;
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		return tab + "continue";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		return new PContinueStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
}
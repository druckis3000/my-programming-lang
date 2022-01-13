package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PElseIfStatement extends PFlowControlStatement {
	
	public PElseIfStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent, TYPE_CHOICE);
		body = new PBody(sourceFile, -1, -1, this);
	}
	
	public void verify(PProgramPart previousStatement) {
		// Find the parent if statement and make sure
		// that else-if statement is after if or else-if statement
		if(!(previousStatement instanceof PIfStatement) && !(previousStatement instanceof PElseIfStatement)){
			Console.throwError("Unexpected else-if statement in line: " + lineInSourceCode);
		}
		
		// Verify the condition
		condition.verify();
		
		// Make sure that body is not empty
		if(body.statements.size() == 0){
			Console.printWarning("Empty else-if statement body in line: " + lineInSourceCode);
		}else{
			// If it's not empty, process the body
			body.processBody();
		}
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + "else if (" + condition + ")" + System.lineSeparator();
		total += body.getAstCode(null);
		return total;
	}
	
	@Override
	public String toString(){
		String total = "else";
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PElseIfStatement elseIfSt = new PElseIfStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			elseIfSt.body = body == null ? null : (PBody)body.clone(true);
			elseIfSt.condition = condition == null ? null : (PConditionExp)condition.clone(true);
		}else{
			elseIfSt.body = body;
			elseIfSt.condition = condition;
		}
		
		return elseIfSt;
	}
}
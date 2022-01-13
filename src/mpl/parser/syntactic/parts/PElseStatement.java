package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PElseStatement extends PFlowControlStatement {
	
	public PElseStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent, TYPE_CHOICE);
		body = new PBody(sourceFile, -1, -1, this);
	}
	
	public void verify(PProgramPart previousStatement) {
		// Find the parent if statement and make sure
		// that else statement is either after if or else-if statement
		if(!(previousStatement instanceof PIfStatement) && !(previousStatement instanceof PElseIfStatement)){
			Console.throwError("Unexpected else statement in line: " + lineInSourceCode);
		}
		
		// Make sure that body is not empty
		if(body.statements.size() == 0){
			Console.printWarning("Empty else statement body in line: " + lineInSourceCode);
		}else{
			// If it's not empty, process the body
			body.processBody();
		}
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + "else" + System.lineSeparator();
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
		PElseStatement elseSt = new PElseStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			elseSt.body = body == null ? null : (PBody)body.clone(true);
		}else{
			elseSt.body = body;
		}
		
		return elseSt;
	}
}
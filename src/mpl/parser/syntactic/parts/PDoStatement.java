package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PDoStatement extends PFlowControlStatement {
	private static int numberOfWhileStatement = 0;
	
	public String nameInAsm = "";
	
	public PDoStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent, TYPE_LOOP);
		body = new PBody(sourceFile, -1, -1, this);

		nameInAsm = ".do" + numberOfWhileStatement++;
	}
	
	public void verify() {
		// Process body, if it's not an empty body
		if(body.statements.size() == 0){
			Console.printWarning("Empty do statement body in line: " + lineInSourceCode);
		}else{
			body.processBody();
		}
		
		// Verify the condition
		condition.verify();
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + "do {" + System.lineSeparator();
		total += body.getAstCode(null) + System.lineSeparator(); 
		total += tab + "} while " + condition.getAstCode(null) + System.lineSeparator();
		
		return total;
	}
	
	@Override
	public String toString(){
		return "do-while(" + condition + ")";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PDoStatement doSt = new PDoStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			doSt.body = body == null ? null : (PBody)body.clone(true);
			doSt.condition = condition == null ? null : (PConditionExp)condition.clone(true);
		}else{
			doSt.body = body;
			doSt.condition = condition;
		}
		
		return doSt;
	}
}
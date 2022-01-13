package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PWhileStatement extends PFlowControlStatement {
	private static int numberOfWhileStatement = 0;
	
	public String nameInAsm = "";
	
	public PWhileStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent,TYPE_LOOP);
		body = new PBody("", -1, -1, this);
		
		nameInAsm = ".while" + numberOfWhileStatement++;
	}
	
	public void verify() {
		// Verify the condition
		condition.verify();
		
		// And then process body, if it's not an empty body
		if(body.statements.size() == 0){
			Console.printWarning("Empty while statement body in line: " + lineInSourceCode);
		}else{
			body.processBody();
		}
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + "while " + condition + System.lineSeparator();
		total += body.getAstCode(null) + System.lineSeparator(); 
		
		return total;
	}
	
	@Override
	public String toString(){
		String total = "while(" + condition + ")";
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PWhileStatement whileSt = new PWhileStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			whileSt.body = body == null ? null : (PBody)body.clone(true);
			whileSt.condition = condition == null ? null : (PConditionExp)condition.clone(true);
		}else{
			whileSt.body = body;
			whileSt.condition = condition;
		}
		
		return whileSt;
	}
}

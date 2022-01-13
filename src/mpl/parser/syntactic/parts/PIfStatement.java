package mpl.parser.syntactic.parts;

public class PIfStatement extends PFlowControlStatement {
	private static int numberOfIfStatement = 0;
	
	public PBody elseBody;
	public String nameInAsm = "";
	
	public PIfStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent, TYPE_CHOICE);
		body = new PBody(sourceFile, -1, -1, this);
		elseBody = new PBody(sourceFile, -1, -1, this);
		
		nameInAsm = ".if" + numberOfIfStatement++;
	}
	
	public void verify() {
		// First of all - verify the condition
		condition.verify();
		
		// And then if body is not empty, process the body
		if(body.statements.size() > 0){
			body.processBody();
		}
		
		// Process else body if there is one
		if(elseBody.statements.size() > 0){
			elseBody.processBody();
		}
	}
	
	protected PBody findDeepestElseBody() {
		PBody deepestBody = elseBody;
		
		while(deepestBody.statements.size() != 0){
			PProgramPart firstStatement = deepestBody.statements.get(0);
			if(firstStatement instanceof PIfStatement){
				deepestBody = ((PIfStatement)firstStatement).elseBody;
			}
		}
		
		return deepestBody;
	}
	
	public PIfStatement findDeepestIf(){
		PIfStatement ifSt = this;
		
		System.out.println("finding deepest if, for: " + ifSt.toString());
		
		while(ifSt.body.statements.size() > 0){
			PProgramPart firstStatement = ifSt.body.statements.get(0);
			if(firstStatement instanceof PIfStatement)
				ifSt = (PIfStatement)firstStatement;
			else
				break;
		}

		System.out.println("deepest if: " + ifSt.toString());
		
		return ifSt;
	}
	
	@Override
	protected String getAstCode(String padding) {
		String code = "";
		
		code += padding + "if(" + condition + ") {" + System.lineSeparator();
		code += body.getAstCode(padding + "\t");
		code += System.lineSeparator() + padding + "}";
		
		if(elseBody.statements.size() > 0){
			code += "else{" + System.lineSeparator();
			code += elseBody.getAstCode(padding + "\t");
			code += System.lineSeparator() + padding + "}";
		}
		
		return code + System.lineSeparator();
	}
	
	@Override
	public String toString(){
		String total = "if(" + condition + ")";
		return total;
	}
	
	public PProgramPart clone(boolean recursive){
		PIfStatement ifSt = new PIfStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			ifSt.body = body == null ? null : (PBody)body.clone(true);
			ifSt.condition = condition == null ? null : (PConditionExp)condition.clone(true);
			ifSt.elseBody = elseBody == null ? null : (PBody)elseBody.clone(true);
		}else{
			ifSt.body = body;
			ifSt.condition = condition;
			ifSt.elseBody = elseBody;
		}
		
		return ifSt;
	}
}
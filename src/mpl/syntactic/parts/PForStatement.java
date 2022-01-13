package mpl.syntactic.parts;

import java.util.StringJoiner;

import mpl.utils.io.Console;

public class PForStatement extends PFlowControlStatement {
	private static int numberOfForStatements = 0;
	
	public PBody initBody;
	public PBody incrementBody;

	public String nameInAsm = "";
	
	public PForStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent, TYPE_LOOP);
		initBody = new PBody(sourceFile, -1, -1, this);
		incrementBody = new PBody(sourceFile, -1, -1, initBody);
		
		body = new PBody(sourceFile, -1, -1, this);
		
		nameInAsm = ".for" + numberOfForStatements++;
	}
	
	public void verify() {
		// We support only one statement in for loop
		// initialization and increment, at the moment
		if(initBody.statements.size() > 1){
			Console.throwError("Too many statements in for loop initialization! Line: " + lineInSourceCode);
		}
		if(incrementBody.statements.size() > 1){
			Console.throwError("Too many statements in for loop, increment part! Line: " + lineInSourceCode);
		}

		// Verify initialization statement
		// It should be PAssignmentStatement or nothing at all
		if(initBody.statements.size() > 0){
			// There's a statement in initialization,
			// make sure it's an assignment statement
			
			PProgramPart initStatement = initBody.statements.get(0);
			if(initStatement instanceof PAssignmentStatement){
				// Verify assignment statement
				((PAssignmentStatement)initStatement).verify();
				
				// If there's var definition in loop initializer
				// then move it to the loop body vars
				if(initBody.vars.size() > 0){
					body.vars.addAll(initBody.vars);
				}
			}else{
				Console.throwError("Unexpected statement in for loop initialization in line: " + lineInSourceCode);
			}
		}else{
			// There's no statement in initialization,
			// make sure there is no var declarations aswell
			if(initBody.vars.size() > 0){
				// There's a variable declaration, which is not
				// supported in the for loop initialization
				Console.throwError("Unexpected statement in for loop initialization in line: " + lineInSourceCode);
			}
		}
		
		// Verify condition
		// Here we use initBody, so that variables defined in the
		// for loop initialization, can be used in the condition
		condition.verify(); // initBody
		
		// Verify increment statement
		// It should be PUnaryOperation
		PProgramPart incrementStatement = incrementBody.statements.get(0);
		if(incrementStatement instanceof PUnaryExpression){
			// Verify unary operation
			// Here we use initBody aswell, so that variables defined in the
			// for loop initialization, can be used in the unary operation
			((PUnaryExpression)incrementStatement).verify();
		}else{
			// If it's not an unary operation, throw an error
			Console.throwError("Unexpected statement in for loop increment in line: " + lineInSourceCode);
		}
		
		// Process loop body
		body.processBody();
	}
	
	public String getAstHeadCode() {
		StringJoiner initStatements = new StringJoiner(", ");
		for(PProgramPart statement : initBody.statements){
			initStatements.add(statement.getAstCode(null).trim());
		}
		
		StringJoiner incrementStatements = new StringJoiner(", ");
		for(PProgramPart statement : incrementBody.statements){
			incrementStatements.add(statement.getAstCode(null).trim());
		}
		
		return "for (" + initStatements + "; " + condition.getAstCode(null) + "; " + incrementStatements + ")";
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + getAstHeadCode() + System.lineSeparator();
		total += body.getAstCode(null);
		
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PForStatement forSt = new PForStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			forSt.body = body == null ? null : (PBody)body.clone(true);
			forSt.condition = condition == null ? null : (PConditionExp)condition.clone(true);
			forSt.initBody = initBody == null ? null : (PBody)initBody.clone(true);
			forSt.incrementBody = incrementBody == null ? null : (PBody)incrementBody.clone(true);
		}else{
			forSt.body = body;
			forSt.condition = condition;
			forSt.initBody = initBody;
			forSt.incrementBody = incrementBody;
		}
		
		return forSt;
	}
}
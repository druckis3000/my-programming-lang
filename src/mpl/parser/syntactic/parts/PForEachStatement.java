package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PForEachStatement extends PFlowControlStatement {
	private static int numberOfForEachStatements = 0;
	private static final String nl = System.lineSeparator();
	
	public PLocalVariable iteratingVar;
	public PVariable array;
	
	public String nameInAsm = "";
	
	public PForEachStatement(String sourceFile, int lineInSource, int columnInSource, PProgramPart parent) {
		super(sourceFile, lineInSource, columnInSource, parent, TYPE_LOOP);
		body = new PBody(sourceFile, -1, -1, this);
		
		nameInAsm = ".forE" + numberOfForEachStatements++;
	}
	
	public void verify(){
		// Find the array variable
		PVariable arrayVar = findParentBody().findVariable(array, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_PARENT);
		
		if(arrayVar == null){
			// Array variable is undefined
			Console.throwError(Console.ERROR_UNDEFINED_VAR, sourceFileName, array.lineInSourceCode, array.columnInSourceCode, array.name);
		}
		
		// Set array variable, to the real one
		array = arrayVar;
		
		// Make sure that array var is actually an array
		if(!array.dataType.isArray() && !array.dataType.isPointer()){
			// Not an array, throw an error
			Console.throwError(Console.ERROR_NOT_ARRAY, sourceFileName, array.lineInSourceCode, array.columnInSourceCode, array.name);
		}
		
		// Verify struct in case it's an array of structs
		array.verifyStruct();
		
		// Set iterating var data type to the type of an array
		iteratingVar.dataType = array.dataType.isPointer() ? array.dataType.plain().asPointer() : array.dataType.plain();
		iteratingVar.struct = array.struct;
		
		// Add iterating variable into for loop body
		body.vars.add(iteratingVar);
		
		// Process loop body
		body.processBody();
	}

	@Override
	protected String getAstCode(String padding) {
		return toString();
	}
	
	public String getAstHeadCode(){
		return "for " + iteratingVar + " in " + array;
	}
	
	@Override
	public String toString(){
		String str = getAstHeadCode() + nl;
		str += body.toString();
		return str;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PForEachStatement forEach = new PForEachStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			forEach.body = body == null ? null : (PBody)body.clone(true);
			forEach.condition = condition == null ? null : (PConditionExp)condition.clone(true);
			forEach.iteratingVar = iteratingVar == null ? null : (PLocalVariable)iteratingVar.clone(true);
			forEach.array = array == null ? null : (PVariable)array.clone(true);
		}else{
			forEach.body = body;
			forEach.condition = condition;
			forEach.iteratingVar = iteratingVar;
			forEach.array = array;
		}
		
		return forEach;
	}
}
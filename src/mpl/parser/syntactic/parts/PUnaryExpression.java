package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PUnaryExpression extends PProgramPart {
	public PVarAccessor variable;
	public POperator preOperator = null;
	public POperator postOperator = null;
	
	public PUnaryExpression(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}

	public void verify() {
		// Verify the variable
		variable.verify(true);
		
		// If that's an arithmetic unary expression
		// make sure that variable is integral
		if(isArithmetic()){
			if(!variable.getDataType().isIntegral()){
				Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, lineInSourceCode, columnInSourceCode, getOperator(), variable.getDataTypeUnmodified());
			}
		}
	}
	
	public boolean isArithmetic(){
		return getOperator().isArithmetic();
	}
	
	public POperator getOperator(){
		return preOperator == null ? postOperator : preOperator;
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		return tab + toString();
	}

	@Override
	public String toString(){
		if(postOperator != null)
			return variable.toString() + postOperator.toString();
		else
			return preOperator.toString() + variable.toString();
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PUnaryExpression unary = new PUnaryExpression(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			unary.variable = variable == null ? null : (PVarAccessor)variable.clone(true);
			unary.preOperator = preOperator == null ? null : (POperator)preOperator.clone(true);
			unary.postOperator = postOperator == null ? null : (POperator)postOperator.clone(true);
		}else{
			unary.variable = variable;
			unary.preOperator = preOperator;
			unary.postOperator = postOperator;
		}
		
		return unary;
	}
}

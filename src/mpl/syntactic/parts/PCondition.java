package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PCondition extends PProgramPart {
	private static int numberOfConditions = 0;
	
	public PExpression leftExp = null;
	public PExpression rightExp = null;
	public POperator operator = null;
	
	/* Used in assembly part */
	public boolean operandsSwapped = false;
	public String name = "con_" + (++numberOfConditions);
	
	public PCondition(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void verify() {
		PDataType leftDataType = null;
		PDataType rightDataType = null;
		
		// Get data types of operands
		leftDataType = leftExp.verify();
		rightDataType = rightExp.verify();
		
		// Make sure that operand data types are compatible with each other
		switch(operator.type){
		case EQUALS:
		case NOT_EQUALS:
			if(leftDataType == PDataType.STRING){
				if(rightDataType != PDataType.STRING && rightDataType != PDataType.NULL){
					Console.throwError(Console.ERROR_INCOMPATIBLE_OPERANDS, sourceFileName, lineInSourceCode, columnInSourceCode, leftDataType, rightDataType);
				}
			}

			if(rightDataType == PDataType.STRING){
				if(leftDataType != PDataType.STRING && leftDataType != PDataType.NULL){
					Console.throwError(Console.ERROR_INCOMPATIBLE_OPERANDS, sourceFileName, lineInSourceCode, columnInSourceCode, leftDataType, rightDataType);
				}
			}
			break;
		case GREATER:
		case LESSER:
		case GREATER_EQUAL:
		case LESSER_EQUAL:
			if(!leftDataType.isIntegral()){
				Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, leftExp.lineInSourceCode, leftExp.columnInSourceCode, operator.type, leftDataType);
			}
			if(!rightDataType.isIntegral()){
				Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, leftExp.lineInSourceCode, leftExp.columnInSourceCode, operator.type, rightDataType);
			}
			break;
		default:
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return padding + this.toString();
	}
	
	@Override
	public String toString(){
		if(operator == null){
			return leftExp.toString();
		}else{
			return leftExp.toString() + " " + operator.toString() + " " + rightExp.toString();
		}
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PCondition condition = new PCondition(sourceFileName, lineInSourceCode, numberOfConditions, parent);
		
		if(recursive){
			condition.leftExp = leftExp == null ? null : (PExpression)leftExp.clone(true);
			condition.rightExp = rightExp == null ? null : (PExpression)rightExp.clone(true);
			condition.operator = operator == null ? null : (POperator)operator.clone(true);
			condition.operandsSwapped = operandsSwapped;
		}else{
			condition.leftExp = leftExp;
			condition.rightExp = rightExp;
			condition.operator = operator;
			condition.operandsSwapped = operandsSwapped;
		}
		
		return condition;
	}
}
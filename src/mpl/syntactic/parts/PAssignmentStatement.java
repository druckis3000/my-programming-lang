package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PAssignmentStatement extends PProgramPart {
	public PVarAccessor variable;
	public PExpression expression;
	public POperand operand;
	
	public boolean checkIfVariableDefined = true;
	public boolean expressionAssignment = true;
	
	public PAssignmentStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public PAssignmentStatement(PVarAccessor variable, PExpression exp, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.variable = variable;
		this.expression = exp;
	}
	
	public void verify() {
		if(expression != null){
			// If there's only one element in the expression
			// move it to the operand, and verify as plain assignment
			if(expression.expression.size() == 1){
				if(expression.expression.get(0) instanceof PExpression){
					// That's a parentheses expression, move it out
					// of parentheses
					expression = (PExpression)expression.expression.get(0);
					verify();
				}else{
					// Single operand, move it to operand variable
					operand = (POperand)expression.expression.get(0);
					verifyPlain();
				}
			}else{
				verifyExpression();
			}
		}else{
			verifyPlain();
		}
	}
	
	private void verifyExpression(){
		// Verify target variable
		variable.verify(checkIfVariableDefined);
		
		// Find out variable data type
		PDataType varDataType = variable.getDataType();
		
		// Verify expression and find out it's data type
		PDataType expDataType = expression.verify(varDataType);
		
		// Check if expression data type matches var data type
		if(!PDataType.isOperandValid(varDataType, expDataType)){
			Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expDataType, varDataType);
		}
	}
	
	private void verifyPlain(){
		expressionAssignment = false;
		
		// Verify target variable
		variable.verify(checkIfVariableDefined);
		
		// Find out variable data type
		PDataType varDataType = variable.getDataType();
		
		// Verify operand and find out it's data type
		PDataType opDataType = null;
		if(operand instanceof PVarAccessor){
			PVarAccessor var = (PVarAccessor)operand;
			var.verify(true);
			
			opDataType = var.getDataType();
		}else if(operand instanceof PFunctionCall){
			PFunctionCall fCall = (PFunctionCall)operand;
			fCall.verify();
			
			// Get it's operand data type
			opDataType = fCall.getDataType();
			
			// Check if that's a struct to struct assignment
			if(varDataType.isStruct() && fCall.getDataType().isStruct() && !fCall.getDataType().isPointer()){
				// If so, create reference to target variable expression
				PExpression arg0 = new PExpression(fCall.sourceFileName, fCall.lineInSourceCode, fCall.columnInSourceCode, fCall);
				PVarAccessor targetReference = (PVarAccessor)variable.clone(true);
				
				if(!targetReference.getDataTypeUnmodified().isPointer()){
					// If target variable is not a pointer, get reference to it
					targetReference.accessType = PVarAccessor.ACCESS_TYPE_REFERENCE;
				}else{
					// If target variable is pointer, get address contained in it
					targetReference.accessType = PVarAccessor.ACCESS_TYPE_PLAIN;
				}
				arg0.expression.add(targetReference);
				
				// Insert reference to target variable as a first arg
				fCall.arguments.add(0, arg0);
			}
		}else{
			operand.verify();
			opDataType = operand.getDataType();
		}
		
		// Check if there's no type mismatch
		if(!PDataType.isOperandValid(varDataType, opDataType)){
			Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, opDataType, varDataType);
		}
		
		// If everything's ok, check if there's no struct mismatch
		if(varDataType.isStruct() && opDataType.isStruct()){
			if(!PDataType.isStructsEqual(variable.getStructType(), operand.getStructType())){
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, operand.getStructType().name, variable.getStructType().name);
			}
		}
		
		if(operand instanceof PNewOperand){
			// If this is a 'new' statement, modify assignment statement,
			// so that would be a call to the malloc function
			PNewOperand newOperand = (PNewOperand)operand;
			PFunctionCall mallocCall = new PFunctionCall(sourceFileName, lineInSourceCode, columnInSourceCode, this);
			mallocCall.functionName = "_c_malloc";
			mallocCall.isCFunctionCall = true;
			
			// Create argument for struct size
			PExpression argExp = new PExpression(sourceFileName, lineInSourceCode, columnInSourceCode, mallocCall);
			PIntegerLiteral structSize = new PIntegerLiteral(newOperand.getStructType().getSizeInBytes(), sourceFileName, lineInSourceCode, columnInSourceCode, mallocCall);
			argExp.expression.add(structSize);
			argExp.verify();
			mallocCall.arguments.add(argExp);
			
			// Add type cast to the struct type, for
			// the function call
			mallocCall.typeCast = new PTypeCast(sourceFileName, lineInSourceCode, columnInSourceCode, mallocCall);
			mallocCall.typeCast.structType = variable.getStructType();
			mallocCall.typeCast.pointer = true;
			
			// Set operand to the malloc function call
			operand = mallocCall;
			
			// Verify again
			verifyPlain();
			
			return;
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return padding + toString();
	}
	
	@Override
	public String toString(){
		return variable.toString() + " = " + (expression != null ? expression.toString() : operand.toString());
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PAssignmentStatement assignmentSt = new PAssignmentStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			assignmentSt.variable = variable == null ? null : (PVarAccessor)variable.clone(true);
			assignmentSt.expression = expression == null ? null : (PExpression)expression.clone(true);
			assignmentSt.operand = operand == null ? null : (POperand)operand.clone(true);
			assignmentSt.checkIfVariableDefined = checkIfVariableDefined;
			assignmentSt.expressionAssignment = expressionAssignment;
		}else{
			assignmentSt.variable = variable;
			assignmentSt.expression = expression;
			assignmentSt.operand = operand;
			assignmentSt.checkIfVariableDefined = checkIfVariableDefined;
			assignmentSt.expressionAssignment = expressionAssignment;
		}
		
		return null;
	}
}
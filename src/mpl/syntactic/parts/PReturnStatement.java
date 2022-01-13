package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PReturnStatement extends PProgramPart {
	public PFunction function;
	public PExpression returnData;
	
	public PReturnStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public PReturnStatement(PExpression returnData, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.returnData = returnData;
	}

	public void verify() {
		function = findParentFunction();
		if(function == null)
			Console.throwError(Console.ERROR_UNEXPECTED_RETURN_STATEMENT, sourceFileName, lineInSourceCode, columnInSourceCode);
		
		if(function.returnType != PDataType.VOID){
			if(returnData == null){
				// If it doesn't return anything, throw an error
				Console.throwError(Console.ERROR_NO_RETURN_RESULT, sourceFileName, lineInSourceCode, columnInSourceCode, function.returnType.toStringWithStruct(function.returnStruct));
			}
		}else{
			if(returnData != null){
				// If it returns any data, throw an error
				Console.throwError(Console.ERROR_UNEXPECTED_RETURN_VALUE, sourceFileName, lineInSourceCode, columnInSourceCode);
			}
		}
		
		// Check if function is returning struct type
		if(function.returnStruct != null){
			// Function is returning a struct, make sure,
			// that return expression is compatible with
			// function return type
			
			// Verify return expression
			PDataType expDt = returnData.verify();
			PStructType expStruct = returnData.expStructType;
			
			// Check if there's no type mismatch between return type
			// and returning expression type
			if(!PDataType.isOperandValid(function.returnType, expDt)){
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expDt, function.returnType);
			}
			
			// Expression struct may be null, because
			// it may be 'null' for struct pointer return type
			if(expStruct != null){
				// Make sure that struct's are the same
				if(!PDataType.isStructsEqual(function.returnStruct, expStruct)){
					Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, "struct " + expDt.toStringWithStruct(expStruct), "struct " + function.returnType.toStringWithStruct(function.returnStruct));
				}
			}
		}else{
			// If function should return any data, then make sure
			// that data is defined and compatible with return type
			PDataType expDt = returnData.verify();
			if(!PDataType.isOperandValid(function.returnType, expDt)){
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expDt, function.returnType);
			}
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return padding + toString();
	}
	
	@Override
	public String toString(){
		return "return" + (returnData == null ? "" : " " + returnData.toString());
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PReturnStatement returnSt = new PReturnStatement(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			returnSt.function = function;
			returnSt.returnData = returnData == null ? null : (PExpression)returnData.clone(true);
		}else{
			returnSt.function = function;
			returnSt.returnData = returnData;
		}
		
		return returnSt;
	}
}

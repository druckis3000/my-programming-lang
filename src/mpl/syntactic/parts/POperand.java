package mpl.syntactic.parts;

import mpl.utils.io.Console;

/** Classes that inherit POperand:
 * 		PIntegerLiteral
 * 		PStringLiteral
 * 		PCharLiteral
 * 		PFunctionCall
 * 		PVarAccessor
 * 		PNewOperand
 * 		PNullLiteral
 * 		PBooleanLiteral */
public abstract class POperand extends PProgramPart {
	public PTypeCast typeCast;
	public PStructType structType;
	
	protected boolean structFound = false;
	
	public POperand(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public abstract void verify();

	public void verifyTypeCast(){
		if(typeCast != null){
			// Verify type cast
			typeCast.verify(getOperandDataType());
		}
	}
	
	public void verifyStruct(){
		if(structType != null){
			if(!structFound){
				// Check if struct type is defined
				PStructType realStructType = getProgram().findStructType(structType.name);
				
				if(realStructType == null){
					// Undefined struct type
					Console.throwError(Console.ERROR_UNDEFINED_STRUCT_TYPE, sourceFileName, lineInSourceCode, columnInSourceCode, structType.name);
				}
				
				// Set struct type to it's real object
				this.structType = realStructType;
				
				structFound = true;
			}
			
			if(structFound){
				// Verify struct
				structType.verify();
			}
		}
	}
	
	protected abstract PDataType getOperandDataType();
	
	public PDataType getDataType(){
		if(typeCast != null)
			return typeCast.getDataType();
		
		return getOperandDataType();
	}
	
	protected abstract PStructType getOperandStructType();
	
	public PStructType getStructType(){
		if(typeCast != null)
			return typeCast.structType;
		
		return getOperandStructType();
	}
}

package mpl.syntactic.parts;

import mpl.utils.io.Console;

public class PGlobalVariable extends PVariable {
	public PProgramPart initialValue;
	
	// Used for accessing vars from different packages
	public String nameInAssembly;
	public boolean isPublic = false;
	
	public PGlobalVariable(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		scope = PScope.GLOBAL;
	}
	
	public void verify(){
		super.verifyStruct();
		
		// Set assembly name for the global var
		nameInAssembly = getProgram().source.fileNameWithoutExtension + "_" + name;
		
		// If it's a variable declaration, then there's nothing to verify
		if(initialValue == null)
			return;
		
		if(initialValue instanceof PExpression){
			PExpression exp = (PExpression)initialValue;
			
			if(exp.contains(PFunctionCall.class, PVarAccessor.class)){
				// If there's a function call or variable
				// in the initializer, then throw an error
				Console.throwError(Console.ERROR_NON_CONST_INITIALIZER, sourceFileName, lineInSourceCode, columnInSourceCode);
			}
			
			// Find out data type of the expression
			PDataType expType = exp.verify();
			
			// Compare data types
			if(!PDataType.isOperandValid(dataType, expType)){
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expType, dataType);
			}
		}else if(initialValue instanceof PArrayInitializer){
			PArrayInitializer arrayInit = (PArrayInitializer)initialValue;
			arrayInit.verify();
		}else if(initialValue instanceof PStructInitializer){
			PStructInitializer structInit = (PStructInitializer)initialValue;
			structInit.verify();
		}
	}
	
	@Override
	public String getAstCode(String padding){
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + this.toString();
		return total;
	}
	
	@Override
	public PProgramPart clone(boolean recursive){
		PGlobalVariable var = new PGlobalVariable(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			var.initialValue = initialValue.clone(true);
			var.nameInAssembly = new String(nameInAssembly);
			var.isPublic = isPublic;
			
			var.name = new String(name);
			var.struct = struct;
			var.arraySize = arraySize;
			var.structVerified = structVerified;
		}else{
			var.initialValue = initialValue;
			var.nameInAssembly = nameInAssembly;
			var.isPublic = isPublic;
			
			var.name = name;
			var.struct = struct;
			var.arraySize = arraySize;
			var.structVerified = structVerified;
		}
		
		return var;
	}
}
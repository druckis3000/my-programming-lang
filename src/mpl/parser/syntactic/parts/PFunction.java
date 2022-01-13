package mpl.parser.syntactic.parts;

import java.util.StringJoiner;

import mpl.utils.io.Console;

public class PFunction extends PProgramPart {
	// Return type of this function
	public PDataType returnType = PDataType.VOID;
	// If return type is of PDataType.STRUCT, this var
	// will hold the type of the struct this function returns
	public PStructType returnStruct = null;
	
	// Name of the function
	public String name = "";
	// Unique name used in assembly
	public String nameInAssembly = "";
	// Parameters of this function
	public PFunctionParams params;
	// Body of this function
	public PBody body;
	
	/* If it's a function declaration, then assembly code
	 * will not be created for this PFunction. */
	public boolean isDeclaration = false;
	/* If function is never called, then it's not necessary
	 * to create assembly code for it, unless it's main function. */
	public boolean isCalled = false;
	/* If function name starts with capital letter, then it's accessible
	 * publicly, otherwise, function is accessible only in source where it's defined */
	public boolean isPublic = false;
	
	/* Used for struct functions, to determine to
	 * which struct this function belongs. */
	public PFunctionParameter thisParam = null;
	
	private int stackSpaceUsedByVars = 0;
	private int stackSpaceUsedByArgs = 0; // This does not include fCalls in expression, since AsmExpression will calculate for that
	private int stackSpaceUsedByExpr = 0;
	
	public PFunction(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		
		params = new PFunctionParams(sourceFile, lineInSourceCode, columnInSourceCode, this);
		body = new PBody(sourceFile, lineInSourceCode + 1, columnInSourceCode + 1, this);
	}
	
	public PFunction(PDataType returnType, String name, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		
		this.returnType = returnType;
		this.name = name;
		
		params = new PFunctionParams(sourceFile, lineInSourceCode, columnInSourceCode, this);
		body = new PBody(sourceFile, lineInSourceCode + 1, columnInSourceCode + 1, this);
	}
	
	/** Verifies the function. If verification fails,
	 * function will not be processed. */
	public void verify(){
		// Assign it's name in assembly to nameInAssembly
		nameInAssembly = name;
		
		if(isStructFunction()){
			// Make sure that struct type is defined
			thisParam.verifyStruct();
			
			// If everything ok, insert 'this' param in front
			// of all other params
			params.params.add(0, thisParam);
			
			// Make 'this' parameter as a pointer so that
			// function can manipulate members of 'this' struct
			if(!thisParam.dataType.isPointer()){
				thisParam.dataType = thisParam.dataType.asPointer();
			}

			// Set assembly name
			nameInAssembly = thisParam.struct.name + "_Ptr_" + name;
		}
		
		// Add signature (except for declarations)
		if(!name.equals("main")){
			if(params.params.size() > 0 && !isDeclaration){
				String signature = "";
				
				// Iterate over params, and get their symbol
				for(PFunctionParameter param : params.params){
					signature += param.dataType.getSymbol();
				}
				
				nameInAssembly += "_" + signature;
			}
			
			// Add package prefix
			nameInAssembly = getProgram().source.fileNameWithoutExtension + "_" + nameInAssembly;
		}
		
		// If return type is struct, verify struct
		if(returnType.isStruct()){
			PStructType retStructReal = getProgram().findStructType(returnStruct.name);
			if(retStructReal == null){
				// Undefined struct
				Console.throwError(Console.ERROR_UNDEFINED_STRUCT_TYPE, sourceFileName, lineInSourceCode, columnInSourceCode, returnStruct.name);
			}
			returnStruct = retStructReal;
			returnStruct.verify();
			
			// If this function is returning plain struct,
			// not a pointer, then add parameter for the address
			// of the struct to which returned value will be assigned.
			if(!returnType.isPointer()){
				PFunctionParameter structPointerParam = new PFunctionParameter(PDataType.STRUCT.asPointer(), "_return", sourceFileName, lineInSourceCode, columnInSourceCode, this);
				structPointerParam.struct = returnStruct;
				params.params.add(0, structPointerParam);
			}
		}
		
		if(returnType != PDataType.VOID)
			findEffectiveReturnStatement();
		
		// Replace array params with pointers
		/*for(PFunctionParameter param : params.params){
			if(param.dataType.isArray()){
				param.dataType = param.dataType.plain().asPointer();
			}
		}*/
	}
	
	private void findEffectiveReturnStatement(){
		boolean found = false;
		
		// Find return statement not in sub-bodies
		for(PProgramPart statement : body.statements){
			if(statement instanceof PReturnStatement){
				found = true;
				break;
			}
		}
		
		if(!found){
			if(returnStruct != null)
				Console.throwError(Console.ERROR_FUNCTION_MUST_RETURN, sourceFileName, lineInSourceCode, columnInSourceCode, returnType.toStringWithStruct(returnStruct));
			else
				Console.throwError(Console.ERROR_FUNCTION_MUST_RETURN, sourceFileName, lineInSourceCode, columnInSourceCode, returnType.toString());
		}
	}
	
	/** Processes the function body */
	public void process(){
		body.processBody();
	}
	
	/** Set variables offsets (ebp) */
	public void setVariablesOffsets(){
		// Set function parameters offsets
		params.setEbpOffsets();
		
		// Calculate total using stack space by local variables
		int ebpOffset = 0;
		for(PBody body : body.findChildrenBodies(true, true)){
			for(PVariable var : body.vars){
				ebpOffset -= var.getSizeInBytes();
			}
		}
		
		// Set local variables ebp offsets
		for(PBody body : body.findChildrenBodies(true, true)){
			for(PLocalVariable var : body.vars){
				var.ebpOffset = ebpOffset;
				ebpOffset += var.getSizeInBytes();
			}
		}
	}
	
	/** Calculates how much stack space is being used
	 * by local variables and function call args.
	 * 
	 * Stack usage by expressions is calculated in AsmExpressionNewNew
	 * when creating assembly code for the expressions */
	public void calculateStackSpaceUsage(){
		for(PBody body : body.findChildrenBodies(true, true)){
			stackSpaceUsedByVars += body.getSizeOfLocalVars();
			stackSpaceUsedByArgs = Math.max(stackSpaceUsedByArgs, body.getHighestStackUsageByArgs());
		}
		
		// Align stack space to 4 bytes
		stackSpaceUsedByVars = (stackSpaceUsedByVars + 3) / 4 * 4;
		stackSpaceUsedByArgs = (stackSpaceUsedByArgs + 3) / 4 * 4;
	}
	
	/** Make sure there are no redefinitions in local scope variables. */
	public void checkForLocalVariablesRedefinition() {
		// If function has parameters
		if(params.params.size() > 0){
			
			// Check for redefinitions between parameters
			for(PFunctionParameter param1 : params.params){
				for(PFunctionParameter param2 : params.params){
					// Don't compare against itself
					if(param1 == param2)
						continue;
					
					if(param1.name.equals(param2.name)){
						// If there's one, throw an error
						Console.throwError(Console.ERROR_VAR_REDEFINITION, param2.sourceFileName, param2.lineInSourceCode, param2.columnInSourceCode, param2.name,
											param1.sourceFileName, param1.lineInSourceCode);
					}
				}
			}

			// Check for redefinitions between parameters and local variables
			for(PFunctionParameter param : params.params){
				int redefinitionLine = body.isVariableDefined(param, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_CHILD);
				if(redefinitionLine != -1){
					// If there's one, throw an error
					PVariable var = body.findVariable(param, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_CHILD);
					Console.throwError(Console.ERROR_VAR_REDEFINITION, var.sourceFileName, var.lineInSourceCode, var.columnInSourceCode, var.name,
										param.sourceFileName, param.lineInSourceCode);
				}
			}
		}
		
		// Check for local variables redefinition in the function body
		// and children bodies
		body.checkForVariablesRedefinition();
	}

	public void checkForUndefinedStructTypes() {
		for(PBody body : body.findChildrenBodies(true, true)){
			for(PVariable var : body.vars){
				var.verifyStruct();
			}
		}
	}

	/** Make sure there is no dead cod in the function.
	 * I.e.code after return or break statement. */
	public void checkForDeadCode() {
		for(PBody body : body.findChildrenBodies(true, true)){
			body.checkForDeadCode();
		}
	}
	
	public int getTotalUsingStackSpace(){
		// stackSpaceUsedByArgs is highest stack usage by single
		// function call arguments. All function calls are checked
		// inside every function. AsmExpressionNewNew on the other hand
		// finds highest stack usage inside it's own expression and uses
		// that values for stackBase, thus it's different for every expression,
		// but never higher than 'stackSpaceUsedByArgs' in current function.
		
		// TODO: New shit here. If stack space used by Expr + Args is higher or equals
		// to stack space used by Vars, then don't include stackSpaceUsedByVars into equation
		
	//	System.out.println(name + ", stack usage: [" + stackSpaceUsedByVars + ", " + stackSpaceUsedByExpr + ", " + stackSpaceUsedByArgs + "]");
		return stackSpaceUsedByVars + stackSpaceUsedByExpr + stackSpaceUsedByArgs;
	}
	
	public int getUsedStackSpaceByVars(){
		return stackSpaceUsedByVars;
	}
	
	public int getUsedStackSpaceByArgs(){
		return stackSpaceUsedByArgs;
	}
	
	public int getUsedStackSpaceByExpr(){
		return stackSpaceUsedByExpr;
	}
	
	public void setUsedStackSpaceByExpr(int newStackSpaceUsed){
		this.stackSpaceUsedByExpr = newStackSpaceUsed;
	}
	
	public int getTotalParametersSize(){
		int totalSize = 0;
		
		for(PFunctionParameter param : params.params){
			totalSize += param.getSizeInBytes();
		}
		
		return totalSize;
	}
	
	public boolean isStructFunction(){
		return thisParam != null;
	}
	
	@Override
	protected String getAstCode(String padding) {
		String code = "";
		
		// Function header
		code += padding + "function ";
		
		if(returnType.isStruct())
			code += returnStruct.name + (returnType.isPointer() ? "*" : "") + " ";
		else
			code += returnType + " ";
		
		code += name + "(";
		
		// Parameters
		StringJoiner paramsStr = new StringJoiner(", ");
		params.params.forEach(param -> paramsStr.add(param.toString()));
		
		code += paramsStr.toString() + ")";
		
		// Body
		if(isDeclaration){
			code += ";";
		}else{
			code += System.lineSeparator() + padding + "{" + System.lineSeparator();
			code += body.getAstCode(padding + "\t");
			code += System.lineSeparator() + padding + "}";
		}
		
		return code + System.lineSeparator();
	}
	
	@Override
	public String toString(){
		String total = name + System.lineSeparator();
		total += params.toString() + System.lineSeparator();
		total += body.toString();
		
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PFunction fnc = new PFunction(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			fnc.returnType = returnType;
			fnc.returnStruct = returnStruct;
			fnc.name = new String(name);
			fnc.nameInAssembly = new String(nameInAssembly);
			fnc.params = params == null ? null : (PFunctionParams)params.clone(true);
			fnc.body = body == null ? null : (PBody)body.clone(true);
			fnc.isDeclaration = isDeclaration;
			fnc.isCalled = isCalled;
			fnc.isPublic = isPublic;
			fnc.thisParam = thisParam == null ? null : (PFunctionParameter)thisParam.clone(true);
			fnc.stackSpaceUsedByArgs = stackSpaceUsedByArgs;
			fnc.stackSpaceUsedByExpr = stackSpaceUsedByExpr;
			fnc.stackSpaceUsedByVars = stackSpaceUsedByVars;
		}else{
			fnc.returnType = returnType;
			fnc.returnStruct = returnStruct;
			fnc.name = name;
			fnc.nameInAssembly = nameInAssembly;
			fnc.params = params;
			fnc.body = body;
			fnc.isDeclaration = isDeclaration;
			fnc.isCalled = isCalled;
			fnc.isPublic = isPublic;
			fnc.thisParam = thisParam;
			fnc.stackSpaceUsedByArgs = stackSpaceUsedByArgs;
			fnc.stackSpaceUsedByExpr = stackSpaceUsedByExpr;
			fnc.stackSpaceUsedByVars = stackSpaceUsedByVars;
		}
		
		return fnc;
	}
}

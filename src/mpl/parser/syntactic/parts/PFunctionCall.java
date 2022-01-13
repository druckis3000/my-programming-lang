package mpl.parser.syntactic.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mpl.parser.syntactic.functions.PBuiltInFunction;
import mpl.project.Source;
import mpl.utils.io.Console;

public class PFunctionCall extends POperand {
	
	public String functionName;
	public String functionNameInAssembly;
	public List<PExpression> arguments = new ArrayList<PExpression>();
	public boolean isCFunctionCall = false;
	private PDataType returnType = null;
	public PFunction targetFunction = null;
	
	// Used for struct function calls
	public PVarAccessor struct = null;
	
	// Used when calling function from
	// another package
	public Source targetPackage = null;
	
	public PFunctionCall(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public PFunctionCall(String funcName, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.functionName = funcName;
	}
	
	public PFunctionCall(String funcName, ArrayList<PExpression> args, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.functionName = funcName;
		arguments.addAll(args);
	}
	
	public int getTotalUsingStackSpace(){
		int totalUsingStackSpace = 0;
		
		if(isCFunctionCall) {
			// Since parameters are unknown, use argument's data type size 
			for(PExpression arg : arguments){
				totalUsingStackSpace += arg.expDataType.getSizeInBytes();
			}
		}else{
			// Use parameters instead of arguments, since literal like '1'
			// will be interpreted as char, where in reality, function
			// may actually take int variable
			for(PFunctionParameter param : targetFunction.params.params) {
				totalUsingStackSpace += param.getSizeInBytes();
			}
		}
		
		return totalUsingStackSpace;
	}
	
	/* Finds out total stack space usage in one function call.
	 * This function is recursive, thus it will look in arguments
	 * expressions and parenthesis expressions aswell. */
	public int getTotalUsingStackSpaceRec(){
		int totalUsingStackSpace = 0;
		
		// Sum arguments size
		totalUsingStackSpace = getTotalUsingStackSpace();
		
		// Check if there's a function call that uses more
		// stack space, inside arguments expressions.
		for(PExpression arg : arguments){
			totalUsingStackSpace = Math.max(totalUsingStackSpace, arg.findLargestArgsSize());
		}
		
		return totalUsingStackSpace;
	}

	/** Semantic part */
	
	/** Verifies function call. First of all it checks if function is
	 * declared/defined, then if it's a struct function call, it will
	 * checks if function belongs to the struct type on which it is
	 * being used. Lastly it will check if there's no type mismatch
	 * in the arguments. */
	public void verify() {
		// Lets see if function call starts with _c_
		if(functionName.startsWith("_c_")){
			// If so, skip some tests
			isCFunctionCall = true;
		}
		
		if(!isCFunctionCall){
			// Verify arguments
			for(PExpression arg : arguments) {
				arg.verify();
			}
			
			// Check if it's a struct function call
			if(isStructFncCall()){
				// Check if first struct access is not a package
				if(findParentBody().isVariableDefined(struct.target, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_PARENT) == -1){
					// First struct is undefined, let's check
					// if there's imported source with the struct's name
					Source src = getProgram().findImportedSource(struct.target.name);
					if(src != null){
						// That's a package
						this.targetPackage = src;
						
						// Remove first variable from struct access
						struct = struct.child;
					}
				}
				
				// After performing package access check
				// first var is removed from the struct access,
				// thus it may become null, so we perform null check
				if(struct != null){
					// Verify struct access
					struct.verify(true);
					
					if(struct.getGrandChild().target.struct == null){
						// Function called on variable which is not a struct
						Console.throwError(Console.ERROR_NON_STRUCT_FUNCTION_CALL, sourceFileName, lineInSourceCode, columnInSourceCode);
					}
					
					// Pass 'this' as first argument
					PExpression exp = new PExpression(sourceFileName, struct.lineInSourceCode, struct.columnInSourceCode, this);
					struct.accessType |= PVarAccessor.ACCESS_TYPE_REFERENCE;
					exp.expression.add(struct);
					arguments.add(0, exp);
				}
			}
			
			// Find suitable function
			PFunction function = findSuitableFunction();
			
			// Check if function is defined
			if(function == null){
				if(isStructFncCall()){
					// There's no suitable function defined
					Console.throwError(Console.ERROR_UNDEFINED_STRUCT_FUNCTION, sourceFileName, lineInSourceCode, columnInSourceCode, functionName, struct.target.struct.name);
				}else{
					Console.throwError(Console.ERROR_UNDEFINED_FUNCTION, sourceFileName, lineInSourceCode, columnInSourceCode, functionName);
				}
			}
			
			// Check if function is accessible
			if(targetPackage != null){
				if(!function.isPublic){
					// Function is non public
					Console.throwError(Console.ERROR_UNDEFINED_FUNCTION, sourceFileName, lineInSourceCode, columnInSourceCode, functionName);
				}
			}
			
			// Mark function as being used in the code
			function.isCalled = true;
			
			// Set return type
			returnType = function.returnType;
			
			// Set target function 
			targetFunction = function;
		}else{
			// It's a C function call, verify it in a little bit different way
			
			// Verify arguments
			for(PExpression arg : arguments){
				arg.verify();
			}
			
			// Set function name in assembly without '_c_' prefix
			functionNameInAssembly = functionName.substring(3);
			
			// We pretend that all C functions return VOID
			// data type, since void can be cast to anything
			returnType = PDataType.VOID;
		}
		
		// Verify type cast
		if(typeCast != null)
			typeCast.verify(returnType);
	}
	
	private PFunction findSuitableFunction(){
		// If we are calling function from another package,
		// then let's get function stream from that package,
		// otherwise, get it from parent package
		Stream<PFunction> functionsStream = targetPackage == null ? getProgram().functions.stream() : targetPackage.sourceAST.functions.stream();
		
		// Filter struct functions
		if(isStructFncCall())
			functionsStream = functionsStream.filter(fnc -> fnc.isStructFunction());
		else
			functionsStream = functionsStream.filter(fnc -> !fnc.isStructFunction());
		
		// Filter functions by name
		functionsStream = functionsStream.filter(fnc -> fnc.name.equals(functionName));
		
		// Create filtered functions list
		List<PFunction> functions = functionsStream.collect(Collectors.toList());
		
		// Check if function exists
		if(functions.size() == 0){
			if(isStructFncCall()){
				// Undefined function for struct
				Console.throwError(Console.ERROR_UNDEFINED_STRUCT_FUNCTION, sourceFileName, lineInSourceCode, columnInSourceCode, functionName, struct.target.struct.name);
			}else{
				// Undefined function
				Console.throwError(Console.ERROR_UNDEFINED_FUNCTION, sourceFileName, lineInSourceCode, columnInSourceCode, functionName);
			}
		}
		
		// Find suitable function
		for(int j=0; j<functions.size(); j++){
			PFunction fnc = functions.get(j);
			// If function return type is struct, then what actually happens
			// is that struct is not returned. Actually struct pointer is passed
			// to the function and all it's members assigned values at the end of
			// the function. Thus, first function paramter is struct pointer, which
			// we're ignoring in semantics stage, since function isn't actually
			// taking struct pointer as a first parameter.
			int skipParameters = fnc.returnType.isStruct() ? 1 : 0;
			
			// Find function that has same number
			// of parameters as this function call
			if(fnc.params.params.size() - skipParameters == arguments.size()){
				// Check if arguments are acceptable for the function
				boolean argsAcceptable = true;
				for(int i=0; i<arguments.size(); i++){
					PFunctionParameter param = fnc.params.params.get(skipParameters+i);
					PExpression arg = arguments.get(i);
					arg.verify();
					
					if(!PDataType.isOperandValid(param.dataType, arg.expDataType)){
						argsAcceptable = false;
						break;
					}
					
					// If arg and param are structs, then check
					// if there is no mismatch in struct types
					if(param.dataType.plain() == PDataType.STRUCT){
						PStructType argStruct = ((PVarAccessor)arguments.get(i).expression.get(0)).getGrandChild().target.struct;
						PStructType paramStruct = param.struct;
						
						if(!PDataType.isStructsEqual(paramStruct, argStruct)){
							argsAcceptable = false;
							break;
						}
					}
				}
				
				// If arguments are acceptable, this function is suitable
				if(argsAcceptable){
					return fnc;
				}
			}
		}
		
		// No suitable function found, throw an error
		PFunction firstFnc = functions.get(0);
		// If function return type is struct, then what actually happens
		// is that struct is not returned. Actually struct pointer is passed
		// to the function and all it's members assigned values at the end of
		// the function. Thus, first function paramter is struct pointer, which
		// we're ignoring in semantics stage, since function isn't actually
		// taking struct pointer as a first parameter.
		int skipParameters = firstFnc.returnType.isStruct() ? 1 : 0;
		
		if(arguments.size() > firstFnc.params.params.size() - skipParameters){
			Console.throwError(Console.ERROR_TOO_MANY_VALUES, sourceFileName, lineInSourceCode, columnInSourceCode, "arguments", "'" + functionName + "' function call");
		}else if(arguments.size() < firstFnc.params.params.size() - skipParameters){
			Console.throwError(Console.ERROR_TOO_FEW_VALUES, sourceFileName, lineInSourceCode, columnInSourceCode, "arguments", "'" + functionName + "' function call");
		}else{
			for(int i=0; i<arguments.size(); i++){
				// Find out non applicable argument
				PFunctionParameter param = firstFnc.params.params.get(skipParameters+i);
				PExpression arg = arguments.get(i);
				arg.verify();
				
				if(!PDataType.isOperandValid(param.dataType, arg.expDataType)){
					// Throw an error
					Console.throwError(Console.ERROR_NON_APPLICABLE_ARG, sourceFileName, lineInSourceCode, columnInSourceCode, firstFnc.name, i+1, arg.expDataType);
					break;
				}
				
				// If arg and param are structs, then check
				// if there's no mismatch in struct types
				if(param.dataType.plain() == PDataType.STRUCT){
					PStructType argStruct = ((PVarAccessor)arguments.get(i).expression.get(0)).getGrandChild().target.struct;
					PStructType paramStruct = param.struct;
					
					if(!PDataType.isStructsEqual(paramStruct, argStruct)){
						// Mismatch in struct types, throw an error
						Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, argStruct.name, paramStruct.name);
						break;
					}
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<PFunctionCall> findFunctionCallsInArgs(ArrayList<PFunctionCall> list, boolean includeSelf){
		if(list == null)
			list = new ArrayList<PFunctionCall>();
		
		if(includeSelf)
			list.add(this);
		
		for(PProgramPart arg : arguments){
			if(arg instanceof PFunctionCall){
				((PFunctionCall)arg).findFunctionCallsInArgs(list, true);
			}
		}
		
		return list;
	}
	
	public boolean isStructFncCall(){
		return struct != null;
	}
	
	public boolean isStructReturn(){
		return returnType.isStruct();
	}

	public boolean isBuiltInFunction() {
	//	for(String builtInFncName : BUILTIN_FUNCTIONS)
	//		if(this.functionName.equals(builtInFncName))
	//			return true;
		
		if(PBuiltInFunction.isBuiltInFunction(this.functionName)){
			return true;
		}
		
		return false;
	}
	
	@Override
	protected PDataType getOperandDataType() {
		return returnType;
	}

	@Override
	protected PStructType getOperandStructType() {
		// We don't know what c function's can return
		if(isCFunctionCall)
			return null;
		
		return targetFunction.returnStruct;
	}
	
	public PDataType getReturnDataType(){
		return getDataType();
	}
	
	public PStructType getReturnStructType(){
		return getStructType();
	}

	@Override
	protected String getAstCode(String padding) {
		return padding + toString();
	}
	
	@Override
	public String toString(){
		StringJoiner joiner = new StringJoiner(", ");
		arguments.stream().forEach(e -> joiner.add(e.toString()));
		return (typeCast != null ? typeCast.toString() : "") + functionName + "(" + (arguments.size() > 0 ? joiner.toString() : "") + ")";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PFunctionCall fCall = new PFunctionCall(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			fCall.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			fCall.structType = structType;
			fCall.structFound = structFound;
			fCall.functionName = new String(functionName);
			fCall.functionNameInAssembly = new String(functionNameInAssembly);
			
			for(PExpression arg : arguments)
				fCall.arguments.add((PExpression)arg.clone(true));
			
			fCall.isCFunctionCall = isCFunctionCall;
			fCall.returnType = returnType;
			fCall.targetFunction = targetFunction;
			fCall.struct = struct == null ? null : (PVarAccessor)struct.clone(true);
			fCall.targetPackage = targetPackage;
		}else{
			fCall.typeCast = typeCast;
			fCall.structType = structType;
			fCall.structFound = structFound;
			fCall.functionName = functionName;
			fCall.functionNameInAssembly = functionNameInAssembly;
			
			for(PExpression arg : arguments)
				fCall.arguments.add(arg);
			
			fCall.isCFunctionCall = isCFunctionCall;
			fCall.returnType = returnType;
			fCall.targetFunction = targetFunction;
			fCall.struct = struct;
			fCall.targetPackage = targetPackage;
		}
		
		return fCall;
	}
}
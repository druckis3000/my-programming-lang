package mpl.compiler.asm;

import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PFunction;
import mpl.parser.syntactic.parts.PFunctionCall;
import mpl.parser.syntactic.parts.PFunctionParameter;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PRegister;
import mpl.parser.syntactic.parts.PStackElement;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.PVarAccessor;

public class AsmFunctionCall {
	private static final String nl = System.lineSeparator();
	
	public String asmCode = "";
	
	// old name: fCallReturnedDataStackBase
	public int fCallStackBase = 0;
	// old name: fCallReturnedDataOffset
	public int fCallStackOffset = 0;
	
	public PFunctionCall call = null;
	public AsmProgram program = null;
	private AsmStack argStack = null;
	
	public AsmFunctionCall(PFunctionCall fCall, AsmProgram program) {
		this.call = fCall;
		this.program = program;
		this.argStack = new AsmStack(program);
	}
	
	public void createFunctionCallCode(int stackOffset, boolean insideExp){
		// If it's a C function call, add function
		// name to the extern symbols
		if(call.functionName.startsWith("_c_")){
			call.functionName = call.functionName.substring(3);
			program.symbols.addExtern(call.functionName);
		}
		
		// If it's a function call from another package
		// add function name to the extern symbols
		if(call.targetPackage != null){
			program.symbols.addExtern(call.targetFunction.nameInAssembly);
		}
		
		// If there's any argument, solve their
		// expressions and put results onto the stack
		if(call.arguments.size() > 0){
		//	ArrayList<AsmExpressionNewNew> arguments = new ArrayList<AsmExpressionNewNew>();
			
			// Set function call stack base.
			// stackOffset parameter already contains sum
			// of arguments passed to the function call,
			// but only if this function call is inside expression,
			// otherwise, calculate it right here & right now
			fCallStackBase = 0;
			if(!insideExp) {
				fCallStackBase = call.isCFunctionCall ? getCFunctionCallArgumentsSize(call) : call.targetFunction.getTotalParametersSize();
			}
			fCallStackBase += stackOffset;
			
			// Solve arguments expressions, and move them to the new list
			// Solve arguments expressions  and push them onto the stack
			for(int i=0; i<call.arguments.size(); i++){
				PExpression argExp = call.arguments.get(i);
				
				// Solve argument expression
				AsmExpressionNewNew exp = new AsmExpressionNewNew(argExp, fCallStackBase, true, program); // + fCallStackOffset
				exp.createExpCode();
				
				// Add argument expression code to asmCode
				asmCode += exp.asmCode;
				// Increase stack offset
				//fCallStackOffset += exp.stackOffset;
				
				// TODO: After solving argument expression, we can easily push
				// resultant onto the arguments stack, since it's reserved already
				// and nothing will overwrite it.
				// After doing so, fCallStackOffset is useless :)
				pushArgumentOntoStack(exp.resultant, i);
				
				// Add argument expression to the new arguments list
				//arguments.add(exp);
			}
			
			// Reset stack offset and push arguments onto the stack
			/*fCallStackOffset = 0;
			
			if(call.isCFunctionCall){
				// If it's a c function call, then we're not going to cast
				// argument data type into parameter data type, since we
				// don't know what it is
				
				for(int i=0; i<arguments.size(); i++){
					AsmExpressionNewNew argExp = (AsmExpressionNewNew)arguments.get(i);
					PProgramPart resultant = argExp.resultant;
					
					if(resultant instanceof PIntegerLiteral){
						asmCode += argStack.push((PIntegerLiteral)resultant, PDataType.INT);
					}else if(resultant instanceof PStringLiteral){
						PStringLiteral str = (PStringLiteral)resultant;
						program.roData.addData(str.roDataName, "db " + str.getAssemblyValue());
						asmCode += argStack.push(str);
					}else if(resultant instanceof PVarAccessor){
						PVarAccessor var = (PVarAccessor)resultant;
						// If we are pushing slice onto the stack, then it
						// uses 8 bytes of stack space, so we can't cast
						// it into INT data type
						PDataType dt = var.getDataTypeUnmodified().isSlice() ? var.getDataTypeUnmodified() : PDataType.INT;
						asmCode += argStack.push((PVarAccessor)resultant, dt);
					}else if(resultant instanceof PRegister){
						asmCode += argStack.push(((PRegister)resultant).register);
					}else if(resultant instanceof PStackElement){
						asmCode += argStack.pushMemory((PStackElement)resultant);
					}
				}
			}else{
				// If it's not a c function call, then we're going to cast
				// argument data type into parameter data type
				PFunction calleeFunction = call.targetFunction;
				
				for(int i=0; i<arguments.size(); i++){
					AsmExpressionNewNew argExp = (AsmExpressionNewNew)arguments.get(i);
					PProgramPart resultant = argExp.resultant;
					PFunctionParameter param = calleeFunction.params.params.get(i);
					
					if(resultant instanceof PIntegerLiteral){
						asmCode += argStack.push((PIntegerLiteral)resultant, param.dataType);
					}else if(resultant instanceof PStringLiteral){
						PStringLiteral str = (PStringLiteral)resultant;
						program.roData.addData(str.roDataName, "db " + str.getAssemblyValue());
						asmCode += argStack.push(str);
					}else if(resultant instanceof PVarAccessor){
						asmCode += argStack.push((PVarAccessor)resultant, param.dataType);
					}else if(resultant instanceof PRegister){
						asmCode += argStack.push(((PRegister)resultant).register);
					}else if(resultant instanceof PStackElement){
						asmCode += argStack.pushMemory((PStackElement)resultant);
					}
				}
			}*/
		}

		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "; " + call + nl;
		
		// Call the function
		asmCode += "call " + (call.isCFunctionCall ? call.functionNameInAssembly : call.targetFunction.nameInAssembly);
	}
	
	private void pushArgumentOntoStack(PProgramPart argExpResultant, int argIndex) {
		if(call.isCFunctionCall) {
			if(argExpResultant instanceof PIntegerLiteral){
				asmCode += argStack.push((PIntegerLiteral)argExpResultant, PDataType.INT);
			}else if(argExpResultant instanceof PStringLiteral){
				PStringLiteral str = (PStringLiteral)argExpResultant;
				program.roData.addData(str.roDataName, "db " + str.getAssemblyValue());
				asmCode += argStack.push(str);
			}else if(argExpResultant instanceof PVarAccessor){
				PVarAccessor var = (PVarAccessor)argExpResultant;
				// If we are pushing slice onto the stack, then it
				// uses 8 bytes of stack space, so we can't cast
				// it into INT data type
				PDataType dt = var.getDataTypeUnmodified().isSlice() ? var.getDataTypeUnmodified() : PDataType.INT;
				asmCode += argStack.push((PVarAccessor)argExpResultant, dt);
			}else if(argExpResultant instanceof PRegister){
				asmCode += argStack.push(((PRegister)argExpResultant).register);
			}else if(argExpResultant instanceof PStackElement){
				asmCode += argStack.pushMemory((PStackElement)argExpResultant);
			}
		}else{
			PFunction calleeFunction = call.targetFunction;
			PProgramPart resultant = argExpResultant;
			PFunctionParameter param = calleeFunction.params.params.get(argIndex);
			
			if(resultant instanceof PIntegerLiteral){
				asmCode += argStack.push((PIntegerLiteral)resultant, param.dataType);
			}else if(resultant instanceof PStringLiteral){
				PStringLiteral str = (PStringLiteral)resultant;
				program.roData.addData(str.roDataName, "db " + str.getAssemblyValue());
				asmCode += argStack.push(str);
			}else if(resultant instanceof PVarAccessor){
				asmCode += argStack.push((PVarAccessor)resultant, param.dataType);
			}else if(resultant instanceof PRegister){
				asmCode += argStack.push(((PRegister)resultant).register);
			}else if(resultant instanceof PStackElement){
				asmCode += argStack.pushMemory((PStackElement)resultant);
			}
		}
		
		asmCode += nl;
	}
	
	private int getCFunctionCallArgumentsSize(PFunctionCall call) {
		// Here we will assume that all arguments are 4-bytes size
		int totalSize = call.arguments.size() * 4;
		
		return totalSize;
	}
}
package mpl.compiler.asm;

import mpl.compiler.CompilerOptions;
import mpl.parser.syntactic.parts.PArrayInitializer;
import mpl.parser.syntactic.parts.PAsmCode;
import mpl.parser.syntactic.parts.PAssignmentStatement;
import mpl.parser.syntactic.parts.PBody;
import mpl.parser.syntactic.parts.PBreakStatement;
import mpl.parser.syntactic.parts.PCondition;
import mpl.parser.syntactic.parts.PConditionExp;
import mpl.parser.syntactic.parts.PContinueStatement;
import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PDeleteStatement;
import mpl.parser.syntactic.parts.PDoStatement;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PForEachStatement;
import mpl.parser.syntactic.parts.PForStatement;
import mpl.parser.syntactic.parts.PFunction;
import mpl.parser.syntactic.parts.PFunctionCall;
import mpl.parser.syntactic.parts.PGlobalVariable;
import mpl.parser.syntactic.parts.PIfStatement;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.POperand;
import mpl.parser.syntactic.parts.PPointerAssignment;
import mpl.parser.syntactic.parts.PVarAccessor;
import mpl.parser.syntactic.parts.POperator.POperatorType;
import mpl.parser.syntactic.parts.PProgram;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PRegister;
import mpl.parser.syntactic.parts.PReturnStatement;
import mpl.parser.syntactic.parts.PStackElement;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.PStructInitializer;
import mpl.parser.syntactic.parts.PUnaryExpression;
import mpl.parser.syntactic.parts.PWhileStatement;
import mpl.utils.StringHelper;

public class AssemblyCodeBuilder {
	public static boolean ADD_COMMENTS_BEFORE_ASSEMBLY_CODE = false;
	public static int ARCH = CompilerOptions.COMPILE_ARCH_32;
	private static final String nl = System.lineSeparator();
	
	private AsmProgram program;

	private String data = "";
	private String bss = "";
	private String text = "";
	
	public AssemblyCodeBuilder() {
	}
	
	public void createAssemblyCode(PProgram _program, CompilerOptions options){
		program = new AsmProgram(_program);
		ARCH = options.outputArch;
		
		// Initialize assembly commons
		AsmCommon.initialize(program);
		
		// If output format is not a library, then 
		// add global directive for main function
		/*if(options.outputFormat != CompilerOptions.COMPILE_FORMAT_LIB){
			program.symbols.addGlobal("main");
		}*/
		
		// Extern global vars from different packages
		for(String var : _program.externGlobalVars.vars){
			program.symbols.addExtern(var);
		}
		
		// Extern malloc function
		program.symbols.addExtern("malloc");
		
		// Extern free function
		program.symbols.addExtern("free");
		
		// Extern heap start and end addresses
		program.symbols.addExtern("__data_start");
		program.symbols.addExtern("_end");
		
		// Add global variables to data section
		for(PGlobalVariable var : program.program.vars){
			if(var.initialValue != null){
				// It's a definition
				data += createGlobalVariableCode(var) + nl;
			}else{
				// Declaration
				bss += createGlobalVariableCode(var) + nl;
			}
		}
		
		// Add functions to text section
		for(PFunction function : program.program.functions){
			if(!function.isDeclaration){
				// We don't create assembly code for a function
				// that is never called, except for main and library functions
				if(function.isCalled){
					text += createFunctionAssemblyCode(function) + nl + nl;
					
					// If it's a public access function
					if(function.isPublic){
						// Then add global symbol for that function
						program.symbols.addGlobal(function.nameInAssembly);
					}
				}else if(function.name.equals("main")){
					text += createFunctionAssemblyCode(function) + nl + nl;
					
					// Add main function global symbol
					program.symbols.addGlobal("main");
				}else{
					if(options.outputFormat == CompilerOptions.COMPILE_FORMAT_LIB){
						text += createFunctionAssemblyCode(function) + nl + nl;
						
						// Since output format is library, make all of
						// the functions global, so they can be called 
						// from program that uses this library
						program.symbols.addGlobal(function.nameInAssembly + ":" + "function");
					}
				}
			}else{
				if(!program.program.isFunctionDefined(function.name))
					program.symbols.addExtern(function.name);
			}
		}
	}

	private String createGlobalVariableCode(PGlobalVariable var){
		// Make label global, so other assembly modules can access it
		program.symbols.addGlobal(var.nameInAssembly);
		
		String code = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			code += "; " + var + nl;
		}
		
		// Check whether it's a variable definition or declaration
		if(var.initialValue == null){
			// It's a declaration
			code += var.nameInAssembly + ": resb " + var.getSizeInBytes();
		}else{
			// It's a definition
			
			if(var.initialValue instanceof PExpression){
				// Simple expression
				PExpression exp = (PExpression)var.initialValue;
				
				code += var.nameInAssembly + ": ";
				
				if(var.dataType.isIntegral()){
					// Add define directive
					code += var.dataType.getDefineDirective() + " ";
					
					// Add initial value
					code += ((PIntegerLiteral)exp.expression.get(0)).value;
				}else{
					// If initial value is string, then we define string as
					// constant in rodata section and point this global
					// variable to the string in rodata
					
					PStringLiteral initialValue = (PStringLiteral)exp.expression.get(0);
					addStringLiteralToRoData(initialValue);
					
					// Set global variable to point to the string in rodata
					code += "dd " + initialValue.roDataName;
				}
			}else if(var.initialValue instanceof PArrayInitializer){
				// Array initializer
				PArrayInitializer arrayInit = (PArrayInitializer)var.initialValue;
				
				code += var.nameInAssembly + ": ";
				
				// Add define directive
				code += var.dataType.getDefineDirective() + " ";
				
				// Add array elements
				for(int i=0; i<arrayInit.group.elements.size(); i++){
					if(arrayInit.group.elements.get(i) instanceof PExpression){
						PExpression el = (PExpression)arrayInit.group.elements.get(i);
						
						code += ((PIntegerLiteral)el.expression.get(0)).value;
						
						if(i < arrayInit.group.elements.size() - 1)
							code += ", ";
					}
				}
			}else if(var.initialValue instanceof PStructInitializer){
				// Struct initializer
				PStructInitializer structInit = (PStructInitializer)var.initialValue;
				
				code += var.nameInAssembly + ": ";
				
				// Add define directive
				code += var.dataType.getDefineDirective() + " ";
				
				// Add struct elements
				for(int i=0; i<structInit.assignmentSts.size(); i++){
					PExpression exp = structInit.assignmentSts.get(i).expression;
					code += ((PIntegerLiteral)exp.expression.get(0)).value;
					
					if(i < structInit.assignmentSts.size() - 1)
						code += ", ";
				}
			}
		}
		
		return code;
	}
	
	private String createFunctionAssemblyCode(PFunction function){
		String asmCode = function.nameInAssembly + ":" + nl;
		
		// Create new stack frame
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "\t; Create new stack frame" + nl;
		
		asmCode += "\tpush " + AsmRegister.EBP.getName() + nl;
		asmCode += "\tmov " + AsmRegister.EBP.getName() + ", " + AsmRegister.ESP.getName() + nl;
		
		if(!function.name.equals("main")) {
			// Save important registers
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "\t; Save important registers" + nl;
			
			asmCode += "\tpush " + AsmRegister.EBX.getName() + nl;
			asmCode += "\tpush " + AsmRegister.ECX.getName() + nl;
		}
		
		// Create assembly code for the function body
		String functionBodyCode = parseBody(function, function.body, 1);
		
		// Calculate stack space usage
		int totalUsingStackSpace = function.getTotalUsingStackSpace();
		if(totalUsingStackSpace > 0){
			asmCode += nl;
			
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "\t; Allocate space for local variables and function call arguments" + nl;

			asmCode += "\tsub esp, " + totalUsingStackSpace + nl;
		}
		asmCode += nl;
		
		// Add function body code
		asmCode += functionBodyCode;
		
		// Reset stack frame and return
		asmCode += nl;
		asmCode += ".return:" + nl;
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "\t; Free up stack space" + nl;
		
		asmCode += "\tadd " + AsmRegister.ESP.getName() + ", " + function.getTotalUsingStackSpace() + nl;
		
		if(!function.name.equals("main")) {
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "\t; Reset registers" + nl;
			
			asmCode += "\tpop " + AsmRegister.ECX.getName() + nl;
			asmCode += "\tpop " + AsmRegister.EBX.getName() + nl;
		}
		
		if(function.name.equals("main")){
			if(function.returnType == PDataType.VOID){
				asmCode += "\txor eax, eax" + nl;
			}
		}
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "\t; Reset stack frame and return" + nl;
		
		asmCode += "\tleave" + nl;
		asmCode += "\tret";
		
		return asmCode;
	}
	
	private String parseBody(PFunction fnc, PBody body, int numberOfTabs){
		String asmCode = "";
		
		if(body.statements.size() == 0)
			return asmCode;
		
		for(PProgramPart statement : body.statements){
			if(statement instanceof PAssignmentStatement){
				PAssignmentStatement assignment = (PAssignmentStatement)statement;
				String assignmentCode = createAssignmentCode(assignment);
				asmCode += StringHelper.addTabs(assignmentCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PPointerAssignment){
				PPointerAssignment ptrAssignment = (PPointerAssignment)statement;
				String ptrAssignmentCode = createPointerAssignmentCode(ptrAssignment);
				asmCode += StringHelper.addTabs(ptrAssignmentCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PFunctionCall){
				PFunctionCall call = (PFunctionCall)statement;
				String fCallCode = createFunctionCallCode(call, 0);
				asmCode += StringHelper.addTabs(fCallCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PIfStatement){
				PIfStatement ifSt = (PIfStatement)statement;
				String ifStCode = createIfStatementCode(fnc, ifSt, numberOfTabs);
				asmCode += StringHelper.addTabs(ifStCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PWhileStatement){
				PWhileStatement whileSt = (PWhileStatement)statement;
				String whileStCode = createWhileStatementCode(fnc, whileSt, numberOfTabs);
				asmCode += StringHelper.addTabs(whileStCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PDoStatement){
				PDoStatement doSt = (PDoStatement)statement;
				String whileStCode = createDoStatementCode(fnc, doSt, numberOfTabs);
				asmCode += StringHelper.addTabs(whileStCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PForStatement){
				PForStatement forSt = (PForStatement)statement;
				String forStCode = createForStatementCode(fnc, forSt, numberOfTabs);
				asmCode += StringHelper.addTabs(forStCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PForEachStatement){
				PForEachStatement forEachSt = (PForEachStatement)statement;
				String forEachStCode = createForEachStatementCode(fnc, forEachSt, numberOfTabs);
				asmCode += StringHelper.addTabs(forEachStCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PUnaryExpression){
				PUnaryExpression varOp = (PUnaryExpression)statement;
				String varOpCode = createUnaryOperationCode(varOp);
				asmCode += StringHelper.addTabs(varOpCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PReturnStatement){
				String returnCode = createReturnStatementCode((PReturnStatement)statement);
				asmCode += StringHelper.addTabs(returnCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PBreakStatement){
				String breakCode = createBreakStatementCode((PBreakStatement)statement);
				asmCode += StringHelper.addTabs(breakCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PContinueStatement){
				String continueCode = createContinueStatementCode((PContinueStatement)statement);
				asmCode += StringHelper.addTabs(continueCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PArrayInitializer){
				String arrayInitCode = createLocalArrayInitializerCode((PArrayInitializer)statement);
				asmCode += StringHelper.addTabs(arrayInitCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PStructInitializer){
				String structInitCode = createLocalStructInitializerCode((PStructInitializer)statement);
				asmCode += StringHelper.addTabs(structInitCode, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PAsmCode){
				String code = createAsmCode((PAsmCode)statement);
				asmCode += StringHelper.addTabs(code, numberOfTabs);
				asmCode += nl;
			}else if(statement instanceof PDeleteStatement){
				String code = createDeleteCode((PDeleteStatement)statement);
				asmCode += StringHelper.addTabs(code, numberOfTabs);
				asmCode += nl;
			}
		}
		
		asmCode = asmCode.substring(0, asmCode.length() - 1);
		
		return asmCode;
	}

	private String createIfStatementCode(PFunction fnc, PIfStatement statement, int numberOfTabs){
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; " + statement + " {" + nl;
		}
		
		PConditionExp condition = statement.condition;
	
		// Create assembly code for condition test code
		boolean hasElseStatement = statement.elseBody.statements.size() > 0;
		String trueLabel = statement.nameInAsm;
		String falseLabel = statement.nameInAsm + (hasElseStatement ? "_else" : "_end");
		asmCode += createConditionExpCode(fnc, condition, trueLabel, falseLabel, numberOfTabs) + nl;
		
		// Create conditional jump code
		//asmCode += createConditionalJumpInstruction(condition) + " " + jmpLabel + nl;

		// Now add body code
		asmCode += trueLabel + ":" + nl;
		asmCode += parseBody(fnc, statement.body, numberOfTabs) + nl;
		
		// Check if it has an else statement
		if(hasElseStatement){
			// Jump to the end of an if statement, since we don't
			// want to execute else-if and else statements
			asmCode += "\tjmp " + statement.nameInAsm + "_end" + nl;
			
			// Create else statement code
			asmCode += "; }else{" + nl;
			asmCode += statement.nameInAsm + "_else:" + nl;
			asmCode += parseBody(fnc, statement.elseBody, numberOfTabs) + nl;
		}
		
		// And add label where if statement ends
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; }" + nl;
		}
		asmCode += statement.nameInAsm + "_end:" + nl;
		
		return asmCode;
	}
	
	private String createWhileStatementCode(PFunction fnc, PWhileStatement statement, int numberOfTabs){
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; " + statement + " {" + nl;
		}
		
		PConditionExp condition = statement.condition;
		
		// Create assembly code for condition test code
		asmCode += statement.nameInAsm + "_condition" + ":" + nl;
		asmCode += createConditionExpCode(fnc, condition, statement.nameInAsm, statement.nameInAsm + "_done", numberOfTabs) + nl;
		
		// Create conditional jump code
		//asmCode += createConditionalJumpInstruction(condition) + " " + statement.nameInAsm + "_done" + nl;
		
		// Add while loop body code
		asmCode += statement.nameInAsm + ":" + nl;
		asmCode += parseBody(fnc, statement.body, numberOfTabs);
		
		// After reached end of the while loop body
		// jump back to the condition test
		asmCode += "jmp " + statement.nameInAsm + "_condition" + nl;
		
		// Add label for the end of the while loop
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; }" + nl;
		}
		asmCode += statement.nameInAsm + "_done:" + nl;
		
		return asmCode;
	}

	private String createDoStatementCode(PFunction fnc, PDoStatement doSt, int numberOfTabs) {
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; do {" + nl;
		}
		
		// Add label for do statement beginning
		asmCode += doSt.nameInAsm + ":" + nl;
		
		// Add do statement body code
		asmCode += parseBody(fnc, doSt.body, numberOfTabs);
		
		// Add while condition code
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; } while (" + doSt.condition + ");" + nl;
		}
		asmCode += doSt.nameInAsm + "_condition:" + nl;
	//	asmCode += createConditionExpCode(doSt.condition, doSt.nameInAsm, null) + nl;

		// Create conditional jump code
		// Jump to the beginnig of a do statement if condition is true
		/*String jmpInstruction = "";
		switch(doSt.condition.operator.type){
		case EQUALS:
			jmpInstruction = "je";
			break;
		case NOT_EQUALS:
			jmpInstruction = "jne";
			break;
		case GREATER:
			jmpInstruction = doSt.condition.operandsSwapped ? "jl" : "jg";
			break;
		case LESSER:
			jmpInstruction = doSt.condition.operandsSwapped ? "jg" : "jl";
			break;
		default:
		}
		asmCode += jmpInstruction + " " + doSt.nameInAsm + nl;*/
		
		// Create label for the end of a do statement
		asmCode += doSt.nameInAsm + "_done:" + nl;
		
		return asmCode;
	}

	private String createForStatementCode(PFunction fnc, PForStatement forSt, int numberOfTabs) {
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; " + forSt.getAstHeadCode() + " {" + nl;
		}
		
		// Create for loop initialization code
		asmCode += forSt.nameInAsm + "_init:" + nl;
		asmCode += parseBody(fnc, forSt.initBody, numberOfTabs-1);

		// Create for loop condition code
		asmCode += forSt.nameInAsm + "_condition:" + nl;
		asmCode += createConditionExpCode(fnc, forSt.condition, null, forSt.nameInAsm + "_end", 0) + nl;
		
		// Create conditional jump code
		//asmCode += createConditionalJumpInstruction(forSt.condition) + " " + forSt.nameInAsm + "_end" + nl;
		
		// Create for loop body code
		asmCode += parseBody(fnc, forSt.body, numberOfTabs);
		
		// Create for loop increment code
		asmCode += forSt.nameInAsm + "_inc:" + nl;
		asmCode += parseBody(fnc, forSt.incrementBody, numberOfTabs);
		
		// Jump back to the condition
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "\t; Jmp back to condition testing" + nl;
		asmCode += "\tjmp " + forSt.nameInAsm + "_condition" + nl;
		
		// Create label for the end of a for loop
		asmCode += forSt.nameInAsm + "_end:" + nl;
		
		return asmCode;
	}
	
	private String createForEachStatementCode(PFunction fnc, PForEachStatement forEachSt, int numberOfTabs){
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; " + forEachSt.getAstHeadCode() + nl;
		}
		
		// Store index value in register
		AsmRegister indexReg = program.registers.getFreeRegister();
		PRegister indexRegForExp = new PRegister(indexReg);
		if(indexReg == null) {
			System.out.println("Huston we have a problem!");
			System.out.println("AssemblyCodeBuilder.createForEachStatementCode(), line: " + Thread.currentThread().getStackTrace()[1].getLineNumber());
		}
		
		// Set index to 0
		asmCode += AsmCommon.movToRegister(indexReg, "0") + nl;
		
		// Create loop beginning label
		asmCode += forEachSt.nameInAsm + ":" + nl;
		
		// Compare index to array size
		//asmCode += AsmCommon.movToRegister(AsmRegister.EAX, index.toStringWithSizeDirective()) + nl;
		//asmCode += "cmp " + AsmRegister.EAX.getName() + ", " + forEachSt.array.arraySize + nl;
		asmCode += "cmp " + indexReg.getName() + ", " + forEachSt.array.arraySize + nl;
		
		// If we reached end of the array, finish this loop
		asmCode += "je " + forEachSt.nameInAsm + "_end" + nl;
		
		// Accessor for 'i' var
		PVarAccessor iteratingVarAccessor = new PVarAccessor(forEachSt.sourceFileName, forEachSt.lineInSourceCode, forEachSt.columnInSourceCode, forEachSt.body);
		iteratingVarAccessor.target = forEachSt.iteratingVar;
		
		// Accessor for 'array[i]' var
		PVarAccessor arrayAccessor = new PVarAccessor(forEachSt.sourceFileName, forEachSt.lineInSourceCode, forEachSt.columnInSourceCode, forEachSt.body);
		arrayAccessor.target = forEachSt.array;
		arrayAccessor.accessType = PVarAccessor.ACCESS_TYPE_ARRAY;
		arrayAccessor.arrayIndex = new PExpression(forEachSt.sourceFileName, forEachSt.lineInSourceCode, forEachSt.columnInSourceCode, forEachSt.body);
		//arrayAccessor.arrayIndex.expression.add(index);
		arrayAccessor.arrayIndex.expression.add(indexRegForExp);
		arrayAccessor.verify(false);
		
		// Array access expression i.e.: array[i]
		PExpression exp = new PExpression(forEachSt.sourceFileName, forEachSt.lineInSourceCode, forEachSt.columnInSourceCode, forEachSt.body);
		exp.expression.add(arrayAccessor);

		// Create assignment statement: i = array[indexReg]
		PAssignmentStatement assignment = new PAssignmentStatement(iteratingVarAccessor, exp, forEachSt.sourceFileName, forEachSt.lineInSourceCode, forEachSt.columnInSourceCode, forEachSt.body);
		assignment.checkIfVariableDefined = false;
		assignment.verify();
		
		// Create expression code
		AsmExpressionNewNew asmExp = new AsmExpressionNewNew(assignment.expression, true, program);
		asmExp.createExpCode();
		asmCode += asmExp.asmCode;
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "; " + assignment + nl;
		
		// Assign 'array[i]' expression result to the 'i'
		asmCode += asmExp.assignTo(assignment.variable) + nl;
		
		// Add for loop body
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; {" + nl;
		}
		asmCode += parseBody(fnc, forEachSt.body, numberOfTabs) + nl;
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; }" + nl;
			asmCode += "; Jmp back to beginning" + nl;
		}

		// Increase index value indexReg
		asmCode += "inc " + indexReg.getName() + nl;
		
		// Jump back to the loop beginning
		asmCode += "jmp " + forEachSt.nameInAsm + nl;
		
		// Create loop end label
		asmCode += forEachSt.nameInAsm + "_end:" + nl;
		
		return asmCode;
	}
	
	private String createUnaryOperationCode(PUnaryExpression unaryOp) {
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; " + unaryOp + nl;
		}
		
		// Load var address into eax
		if(unaryOp.variable.isPointerAccess()){
			asmCode += unaryOp.variable.movToRegister(AsmRegister.EAX, false, 0, program) + nl;
		}else{
			asmCode += unaryOp.variable.movToRegister(AsmRegister.EAX, true, 0, program) + nl;
		}
		
		// Do unary operation
		if(unaryOp.postOperator.isUnaryArithmetic()){
			if(unaryOp.postOperator.type == POperatorType.DECREASE){
				asmCode += "dec " + unaryOp.variable.getDataType().getSizeDirective() + " [eax]" + nl;
			}else if(unaryOp.postOperator.type == POperatorType.INCREASE){
				asmCode += "inc " + unaryOp.variable.getDataType().getSizeDirective() + " [eax]" + nl;
			}
		}
		
		return asmCode;
	}
	
	private String createConditionExpCode(PFunction fnc, PConditionExp conditionExp, String trueLabel, String falseLabel, int numberOfTabs){
		String code = "";
		
		if(conditionExp.expression.size() == 1){
			if(conditionExp.expression.get(0) instanceof PCondition){
				PCondition condition = (PCondition)conditionExp.expression.get(0);
				code += createConditionCode(condition) + nl;
				code += createConditionalJumpInstruction(condition, true) + " " + falseLabel;
			}else{
				// That must be parentheses condition
				PConditionExp parenthesesCondition = (PConditionExp)conditionExp.expression.get(0);
				code += createConditionExpCode(fnc, parenthesesCondition, trueLabel, falseLabel, numberOfTabs);
			}
		}else{
			PAsmCode trueJump = new PAsmCode("jmp " + trueLabel, "", -1, -1, null);
			PAsmCode falseJump = new PAsmCode("jmp " + falseLabel, "", -1, -1, null);
			
			PBody nestedConditions = (PBody)conditionExp.buildConditionIfTree(conditionExp, trueJump, falseJump, false);
			code += parseBody(fnc, nestedConditions, numberOfTabs);
		}
		
		return code;
	}
	
	
	private String createConditionCode(PCondition condition){
		String asmCode = ""; // "." + condition.name + ":" + nl;
		
		PExpression leftExp = condition.leftExp;
		PExpression rightExp = condition.rightExp;
		
		// Smaller operand should be in eax register,
		// so we find the smaller operand
		long leftSizeInBytes = leftExp.expDataType.getSizeInBytes();
		long rightSizeInBytes = rightExp.expDataType.getSizeInBytes();
		
		// Smaller operand should be in left-hand side
		if(leftSizeInBytes > rightSizeInBytes){
			leftExp = condition.rightExp;
			rightExp = condition.leftExp;
			condition.operandsSwapped = true;
		}
		
		// Create code for lhs of condition
		AsmExpressionNewNew leftAsmExp = new AsmExpressionNewNew(leftExp, true, program);
		leftAsmExp.createExpCode();
		if(leftAsmExp.asmCode.length() > 0){
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; Left-hand side of the condition" + nl;
			
			asmCode += leftAsmExp.asmCode + nl;
		}
		
		// Create code for rhs of condition
		AsmExpressionNewNew rightAsmExp = new AsmExpressionNewNew(rightExp, leftAsmExp.stackBase + leftAsmExp.stackOffset, true, program);
		rightAsmExp.createExpCode();
		if(rightAsmExp.asmCode.length() > 0){
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; Right-hand side of the condition" + nl;
			
			asmCode += rightAsmExp.asmCode + nl;
		}
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "; Condition testing (" + condition + ")" + nl;
		
		// First operand of the cmp instruction must be register,
		// so we move left operand to the eax register
		asmCode += leftAsmExp.assignTo(AsmRegister.EAX) + nl;
		// Free up lhs expression registers
		//leftAsmExp.freeUpRegisters(program.registers);

		// Compare eax to the right operand
		if(rightAsmExp.resultant instanceof PIntegerLiteral){
			asmCode += "cmp " + AsmRegister.EAX.getName() + ", " + ((PIntegerLiteral)rightAsmExp.resultant).value;
		}else if(rightAsmExp.resultant instanceof PVarAccessor){
			// Free up rhs expression registers, we will need one
			//rightAsmExp.freeUpRegisters(program.registers);
			
			PVarAccessor var = (PVarAccessor)rightAsmExp.resultant;
			
			// Get free register
			AsmRegister reg = program.registers.getFreeRegister();
			if(reg == null){
				System.out.println("Huston we have a problem!");
				System.out.println("AssemblyCodeBuilder.createConditionCode(), line: " + Thread.currentThread().getStackTrace()[1].getLineNumber());
			}
			
			
			// Assign right operand to the reg
			asmCode += AsmCommon.movToRegister(reg, var) + nl;

			// Cmp eax with reg
			asmCode += "cmp " + AsmRegister.EAX.getName() + ", " + reg.getName();
			
			// Free up rhs register
			program.registers.freeUpRegister(reg);
		}else if(rightAsmExp.resultant instanceof PRegister){
			asmCode += "cmp " + AsmRegister.EAX.getName() + ", " + rightAsmExp.resultant;
		}else if(rightAsmExp.resultant instanceof PStackElement){
			asmCode += "cmp " + AsmRegister.EAX.getName() + ", " + ((PStackElement)rightAsmExp.resultant).toStringWithSizeDirective();
		}
		
		// Free up rhs expression resultant register
		//rightAsmExp.freeUpRegisters(program.registers);
		
		return asmCode;
	}
	
	
	private String createConditionalJumpInstruction(PCondition condition, boolean inversed){
		String jmpInstruction = "";
		switch(condition.operator.type){
		case EQUALS:
			// Here we use inverse logic. If relational operator
			// is ==, then we compare if not equals
			jmpInstruction = inversed ? "jne" : "je";
			break;
		case NOT_EQUALS:
			// Here we use inverse logic. If relational operator
			// is !=, then we compare if equals
			jmpInstruction = inversed ? "je" : "jne";
			break;
		case GREATER:
			if(inversed){
				jmpInstruction = condition.operandsSwapped ? "jge" : "jle";
			}else{
				jmpInstruction = condition.operandsSwapped ? "jle" : "jg";
			}
			break;
		case LESSER:
			if(inversed){
				jmpInstruction = condition.operandsSwapped ? "jle" : "jge";
			}else{
				jmpInstruction = condition.operandsSwapped ? "jge" : "jl";
			}
			break;
		case GREATER_EQUAL:
			if(inversed){
				jmpInstruction = condition.operandsSwapped ? "jg" : "jl";
			}else{
				jmpInstruction = condition.operandsSwapped ? "jle" : "jge";
			}
			break;
		case LESSER_EQUAL:
			if(inversed){
				jmpInstruction = condition.operandsSwapped ? "jl" : "jg";
			}else{
				jmpInstruction = condition.operandsSwapped ? "jge" : "jle";
			}
			break;
		default:
		}
		
		return jmpInstruction;
	}
	
	private String createFunctionCallCode(PFunctionCall call, int argStackOffset){
		AsmFunctionCall fCall = new AsmFunctionCall(call, program);
		fCall.createFunctionCallCode(argStackOffset, false);
		
		return fCall.asmCode + nl;
	}
	
	private String createAssignmentCode(PAssignmentStatement assignment){
		String asmCode = "";
		
		if(assignment.expressionAssignment){
			// Create expression code
			AsmExpressionNewNew exp = new AsmExpressionNewNew(assignment.expression, true, program);
			exp.createExpCode();
			if(exp.asmCode.length() > 0)
				asmCode += exp.asmCode;
			
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			// Assign expression result to the variable
			asmCode += exp.assignTo(assignment.variable) + nl;
		}else{
			// Create plain assignment code
			asmCode += AsmAssignment.createAssignmentCode(assignment, program);
		}
		
		return asmCode;
	}
	
	private String createPointerAssignmentCode(PPointerAssignment ptrAssignment){
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "; " + ptrAssignment.toString() + nl;
		
		// Solve pointer address expression
		AsmExpressionNewNew ptrExp = new AsmExpressionNewNew(ptrAssignment.pointerExp, true, program);
		ptrExp.createExpCode();
		if(ptrExp.asmCode.length() > 0)
			asmCode += ptrExp.asmCode + nl;
		
		// Solve expression
		AsmExpressionNewNew exp = new AsmExpressionNewNew(ptrAssignment.exp, true, program);
		exp.createExpCode();
		if(exp.asmCode.length() > 0)
			asmCode += exp.asmCode + nl;
		
		// Free up registers
		//ptrExp.freeUpRegisters(program.registers);
		//exp.freeUpRegisters(program.registers);
		
		boolean ptrAdrressInEax = false;
		boolean resultInEdx = false;
		
		// Check if result is not in register
		if(!(exp.resultant instanceof PRegister)){
			// It's not in register, move it into edx register
			asmCode += exp.assignTo(AsmRegister.EDX) + nl;
			resultInEdx = true;
		}
		
		// Check if ptr address is not in register
		if(!(ptrExp.resultant instanceof PRegister)){
			// It's not in register, move it into eax register
			asmCode += ptrExp.assignTo(AsmRegister.EAX) + nl;
			ptrAdrressInEax = true;
		}
		
		// Move expression result into to the address
		AsmRegister targetRegister = ptrAdrressInEax ? AsmRegister.EAX : ((PRegister)ptrExp.resultant).register;
		AsmRegister resultRegister = resultInEdx ? AsmRegister.EDX : ((PRegister)exp.resultant).register;
		asmCode += AsmCommon.movToRegisterPtr(targetRegister, "" + resultRegister.getName()) + nl;
		
		return asmCode;
	}
	
	private String createLocalArrayInitializerCode(PArrayInitializer arrayInit){
		String code = "";
		
		// Create expressions and assign them to the array element
		for(PAssignmentStatement arrayAssignment : arrayInit.assignmentSts)
			code += createAssignmentCode(arrayAssignment) + nl;
		
		//code = code.substring(0, code.length() - 1);
		
		return code;
	}
	
	private String createLocalStructInitializerCode(PStructInitializer structInit) {
		String code = "";
		
		if(structInit.group == null){
			// It's a pointer struct initializer
			// Create expression and assign it to the variable
			if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				code += "; " + structInit.toString() + nl;
			
			code += createAssignmentCode(structInit.assignmentSts.get(0)) + nl;
		}else{
			// It's a plain struct initializer
			// Create expressions and assign them to the struct
			for(PAssignmentStatement value : structInit.assignmentSts)
				code += createAssignmentCode(value) + nl;
			
			// Remove last newline from code
			if(code.length() > 0)
				code = code.substring(0, code.length() - 1);
		}
		
		
		return code;
	}
	
	private String createReturnStatementCode(PReturnStatement returnSt){
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			asmCode += "; " + returnSt + nl;
		
		// If function is returning struct, create different code
		if(returnSt.function.returnType.isStruct() && !returnSt.function.returnType.isPointer()){
			// Create struct assignment to the first parameter(struct pointer)
			PAssignmentStatement assignment = new PAssignmentStatement(returnSt.sourceFileName, returnSt.lineInSourceCode, returnSt.columnInSourceCode, returnSt.parent);
			assignment.checkIfVariableDefined = false;
			
			// Find out operand, that must be first element of the return expression
			POperand operand = (POperand)returnSt.returnData.expression.get(0);
			assignment.operand = operand;
			
			// Create var access to the target struct
			PVarAccessor targetStruct = new PVarAccessor(returnSt.sourceFileName, returnSt.lineInSourceCode, returnSt.columnInSourceCode, assignment);
			targetStruct.target = returnSt.function.params.params.get(0);
			
			// Dereference pointer struct, so we can assign to it
			targetStruct.accessType = PVarAccessor.ACCESS_TYPE_POINTER;
			
			// Set assingment target variable
			assignment.variable = targetStruct;
			
			// Verify assignment
			assignment.verify();
			
			return AsmAssignment.createAssignmentCode(assignment, program);
		}
		
		// Create expression code
		AsmExpressionNewNew exp = new AsmExpressionNewNew(returnSt.returnData, true, program);
		exp.createExpCode();
		asmCode += exp.asmCode + (exp.asmCode.length() > 0 ? nl : "");
		
		// Move expression result to the eax register
		asmCode += exp.assignTo(AsmRegister.EAX) + nl;
		
		// After putting returned value into the eax register,
		// jump to the function's return label
		asmCode += "jmp .return" + nl;
		
		return asmCode;
	}

	private String createBreakStatementCode(PBreakStatement statement) {
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; break;" + nl;
		}
		
		PProgramPart flowControlStatement = statement.findParentFlowControlStatement();
		if(flowControlStatement instanceof PWhileStatement){
			asmCode += "jmp " + ((PWhileStatement)flowControlStatement).nameInAsm + "_done" + nl;
		}else if(flowControlStatement instanceof PDoStatement){
			asmCode += "jmp " + ((PDoStatement)flowControlStatement).nameInAsm + "_done" + nl;
		}else if(flowControlStatement instanceof PForStatement){
			asmCode += "jmp " + ((PForStatement)flowControlStatement).nameInAsm + "_end" + nl;
		}
		
		return asmCode;
	}
	
	private String createContinueStatementCode(PContinueStatement statement) {
		String asmCode = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; continue;" + nl;
		}
		
		PProgramPart flowControlStatement = statement.findParentFlowControlStatement();
		if(flowControlStatement instanceof PWhileStatement){
			asmCode += "jmp " + ((PWhileStatement)flowControlStatement).nameInAsm + "_condition" + nl;
		}else if(flowControlStatement instanceof PDoStatement){
			asmCode += "jmp " + ((PDoStatement)flowControlStatement).nameInAsm + "_condition" + nl;
		}else if(flowControlStatement instanceof PForStatement){
			asmCode += "jmp " + ((PForStatement)flowControlStatement).nameInAsm + "_inc" + nl;
		}
		
		return asmCode;
	}
	
	private String createAsmCode(PAsmCode asmCode) {
		String code = "";
		
		if(asmCode.operand != null){
			// Create operand expression code
			AsmExpressionNewNew opExpression = new AsmExpressionNewNew(asmCode.operand, true, program);
			opExpression.createExpCode();
			code += opExpression.asmCode;
			
			// Check if resultant is string
			if(opExpression.resultant instanceof PStringLiteral){
				// If so, add it to the rodata
				PStringLiteral string = (PStringLiteral)opExpression.resultant;
				addStringLiteralToRoData(string);
				
				// Add comment before replacing $1 with resultant
				if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
					code += "; __asm__(\"" + asmCode.code + "\", " + asmCode.operand + ")" + nl;
	
				// Replace $1 in assembly code with the resultant of the expression
				code += asmCode.code.replace("$1", string.roDataName);
				//asmCode.code = asmCode.code.replace("$1", string.roDataName);
			}else{
				// Operand used in __asm__ is variable
				
				// Check if instruction is mov
				if(asmCode.instruction.equals("mov")){
					// If so, check if lhs is register
					if(asmCode.lhs instanceof PRegister){
						// If so, mov variable into register
						PRegister reg = (PRegister)asmCode.lhs;
						code += opExpression.assignTo(reg.register);
					}else{
						// Lhs is not a register, check if it's a variable
						if(asmCode.lhs instanceof PVarAccessor){
							// It's a variable
							PVarAccessor var = (PVarAccessor)asmCode.lhs;
							
							// Check if rhs is register
							if(asmCode.rhs instanceof PRegister){
								// If so, assign register to a variable
								PRegister reg = (PRegister)asmCode.rhs;
								code += var.assignRegister(reg.register, program);
							}
						}
					}
				}else{
					// Instruction is not mov, so we don't know
					// what to do, sooo we put asm code from __asm__
					// into the assembly code, also replace $1 with operand
					code += asmCode.code.replace("$1", opExpression.resultant.toString());
				}
			}
		}else{
			// There's no operand, just add code from
			// __asm__ into assembly code
			code += asmCode;
		}
		
		return code;
	}
	
	private String createDeleteCode(PDeleteStatement statement) {
		String code = "";
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			code += "; delete " + statement.var + nl;
		
		// Move address contained in var into eax
		code += statement.var.movToRegister(AsmRegister.EAX, program) + nl;
		
		// Push eax onto the stack
		code += AsmCommon.movToAddress("[esp]", "DWORD", AsmRegister.EAX) + nl;
		
		// Call free
		code += "call free" + nl;
		
		if(ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
			code += "; Assign null to the var" + nl;
		
		code += statement.var.assignIntLiteral(0, program) + nl;
		
		return code;
	}
	
	private void addStringLiteralToRoData(PStringLiteral string){
		program.roData.addData(string.roDataName, "db " + string.getAssemblyValue());
	}
	
	
	public String getAssemblyCode(){
		String total = "";
		
		total += program.symbols.createAssemblyCode();
		
		total += nl + nl;
		
		total += "SECTION .text" + nl +
					text;
		
		total += nl;
		
		total += "SECTION .bss" + nl +
					bss;
		
		total += nl + nl;
		
		total += "SECTION .data" + nl +
					data;
		
		total += nl + nl;
		
		total += "SECTION .rodata" + nl +
				program.roData.createAssemblyCode();
		
		return total;
	}
}
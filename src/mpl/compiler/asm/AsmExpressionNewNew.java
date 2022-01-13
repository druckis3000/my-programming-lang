package mpl.compiler.asm;

import java.util.ArrayList;
import java.util.StringJoiner;

import mpl.parser.syntactic.parts.PCharLiteral;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PFunction;
import mpl.parser.syntactic.parts.PFunctionCall;
import mpl.parser.syntactic.parts.PHexLiteral;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.PLiteral;
import mpl.parser.syntactic.parts.POperator;
import mpl.parser.syntactic.parts.POperator.POperatorType;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PRegister;
import mpl.parser.syntactic.parts.PStackElement;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.PVarAccessor;

public class AsmExpressionNewNew {
	private static final String nl = System.lineSeparator();
	
	protected int stackOffset = 0;
	protected int stackBase = 0;
	
	// Used to set expression stack space usage
	private PFunction parentFunction = null;

	private AsmProgram program;
	private ArrayList<PProgramPart> infixExp = new ArrayList<PProgramPart>();
	private ArrayList<PProgramPart> postfixExp = new ArrayList<PProgramPart>();
	private boolean pointerExp = false;
	
	public String asmCode = "";
	public PProgramPart resultant = null;
	public boolean commentInAssembly = true;
	
	public AsmExpressionNewNew(PExpression exp, int stackBase, boolean parentExp, AsmProgram program) {
		this.parentFunction = exp.findParentFunction();
		this.infixExp.addAll(exp.expression);
		this.stackBase = stackBase;
		this.program = program;
		this.pointerExp = exp.pointerExpression;

		// If this is a parent expression, set stackBase to the
		// value of how much stack space is being used for function
		// call arguments, so that expression values won't be stored
		// at the beginning of stack which is used for passing arguments
		if(parentExp) {
			this.stackBase += exp.findLargestArgsSize();
		}
		
		// Evaluate function calls, and place their returned values onto the stack
		asmCode += evalFunctionCalls();
		
		// Solve parentheses
		asmCode += solveParentheses();
	}
	
	public AsmExpressionNewNew(PExpression exp, boolean parentExp, AsmProgram program){
		this(exp, 0, parentExp, program);
	}
	
	public void createExpCode(){
		createPostfixExp(infixExp, postfixExp);
		createAssemblyCodeFromPostfix();
		
		// If this is not a global variable expression
		if(parentFunction != null){
			// stackBase is actually used for offseting expression
			// variables away from the beginning of stack, which is 
			// needed for funcion call arguments. For parentheses
			// expressions stackBase is as explained above, except
			// parent expression stackOffset is added, so that
			// parentheses expression don't overwrite parent exp values
			
			// stackOffset contains size of how much stack space
			// is being used by expression itself. Since stack space
			// is already allocated for function call arguments,
			// we do not take it into calculation again.
			
			int stackUsage = /*stackBase + */stackOffset;
			parentFunction.setUsedStackSpaceByExpr(Math.max(stackUsage, parentFunction.getUsedStackSpaceByExpr()));
		}
	}
	
	private void createAssemblyCodeFromPostfix(){
		ArrayList<PProgramPart> mathStack = new ArrayList<PProgramPart>();
		
		for(int i=0; i<postfixExp.size(); i++){
			PProgramPart el = postfixExp.get(i);
			
			if(isOperand(el)){
				mathStack.add(el);
			}else if(el instanceof POperator){
				POperator operator = (POperator)el;
				
				PProgramPart bValue = mathStack.get(mathStack.size() - 1);
				mathStack.remove(mathStack.size() - 1);
				
				PProgramPart aValue = mathStack.get(mathStack.size() - 1);
				mathStack.remove(mathStack.size() - 1);
				
				switch(operator.type){
				case DIV:
					if(commentInAssembly)
						asmCode += "; " + aValue + " / " + bValue + nl;
					
					asmCode += solveDivision(aValue, bValue, mathStack) + nl;
					break;
				case MUL:
					if(commentInAssembly)
						asmCode += "; " + aValue + " * " + bValue + nl;
					
					asmCode += solveMultiplication(aValue, bValue, mathStack) + nl;
					break;
				case SUB:
					if(commentInAssembly)
						asmCode += "; " + aValue + " - " + bValue + nl;
					
					asmCode += solveSubtraction(aValue, bValue, mathStack) + nl;
					break;
				case ADD:
					if(commentInAssembly)
						asmCode += "; " + aValue + " + " + bValue + nl;
					
					asmCode += solveAddition(aValue, bValue, mathStack) + nl;
					break;
				case BITWISE_AND:
				case BITWISE_OR:
				case BITWISE_XOR:
					if(commentInAssembly)
						asmCode += "; " + aValue + " " + operator + " " + bValue + nl;
					
					asmCode += solveBitwse(operator.type, aValue, bValue, mathStack) + nl;
					break;
				default:
				}
			}
		}
		
		resultant = mathStack.get(0);
		
		if(pointerExp) {
			// Resultant is address, but we need value contained @ that address, dereference
			if(resultant instanceof PStackElement) {
				if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE) {
					asmCode += "; Dereference, referenced value" + nl;
				}
				
				// eax = resultant
				asmCode += movToRegister(AsmRegister.EAX, resultant) + nl;
				
				// eax = [eax]
				asmCode += AsmCommon.movToRegister(AsmRegister.EAX, "[eax]") + nl;
				
				// resultant = eax
				PStackElement stack = ((PStackElement)resultant);
				asmCode += AsmCommon.movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;
			}
		}
	}
	
	private String solveDivision(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> mathStack){
		String code = "";
		
		// Division requires lhs to be in eax register, thus while
		// moving rhs, we can't touch eax register, be careful
		AsmRegister regA = program.registers.getFreeRegister();
		
		if(aValue instanceof PVarAccessor){
			if(bValue instanceof PVarAccessor){
				if(((PVarAccessor) bValue).containsArrayAccess()){
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// Both contains array access
						
						AsmRegister regB = program.registers.getFreeRegister();
						
						// regB = bValue
						code += movToRegister(regB, bValue) + nl;
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = regA
						code += AsmCommon.movToRegister(AsmRegister.EAX, regA) + nl;
						
						// Sign extend eax
						code += "cdq" + nl;
						
						// eax = eax / regB
						code += "idiv " + regB.getName() + nl;
						
						program.registers.freeUpRegister(regB);
					}else{
						// Only b contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// Sign extend eax
						code += "cdq" + nl;
						
						// eax = eax / regA
						code += "idiv " + regA.getName() + nl;
					}
				}else{
					// B doesn't contain array access, check if a contains
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = regA
						code += AsmCommon.movToRegister(AsmRegister.EAX, regA) + nl;
						
						// Sign extend eax
						code += "cdq" + nl;
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = eax / regA
						code += "idiv " + regA.getName() + nl;
					}else{
						// None contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// Sign extend eax
						code += "cdq" + nl;
						
						// eax = eax / regA
						code += "idiv " + regA.getName() + nl;
					}
				}
			}else{
				// B is not a var, it can be hex/int literal or stack element
				
				if(((PVarAccessor) aValue).containsArrayAccess()){
					// A contains array access
					
					// regA = aValue
					code += movToRegister(regA, aValue) + nl;
					
					// eax = regA
					code += AsmCommon.movToRegister(AsmRegister.EAX, regA) + nl;
					
					// Sign extend eax
					code += "cdq" + nl;
					
					if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
						long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
						
						// regA = bValue
						code += AsmCommon.movToRegister(regA, "" + value) + nl;
						
						// eax = eax / regA
						code += "idiv " + regA.getName() + nl;
					}else if(bValue instanceof PStackElement){
						// eax = eax / bValue
						code += "idiv " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
					}
				}else{
					// A doesn't contain array access
					
					// eax = aValue
					code += movToRegister(AsmRegister.EAX, aValue) + nl;
					
					// Sign extend eax
					code += "cdq" + nl;
					
					if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
						long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
						
						// regA = bValue
						code += AsmCommon.movToRegister(regA, "" + value) + nl;
						
						// eax = eax / regA
						code += "idiv " + regA.getName() + nl;
					}else if(bValue instanceof PStackElement){
						// eax = eax / bValue
						code += "idiv " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
					}
				}
			}
		}else if(aValue instanceof PStackElement){
			// A is stack element
			
			if(bValue instanceof PVarAccessor){
				// B is a variable
				
				// regA = bValue
				code += movToRegister(regA, bValue) + nl;
				
				// eax = aValue
				code += movToRegister(AsmRegister.EAX, aValue) + nl;
				
				// Sign extend eax
				code += "cdq" + nl;
				
				// eax = eax / regA
				code += "idiv " + regA.getName() + nl;
			}else{
				// B is not a variable, it may be int/hex literal or stack element
				
				// eax = aValue
				code += movToRegister(AsmRegister.EAX, aValue) + nl;
				
				// Sign extend eax
				code += "cdq" + nl;
				
				if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
					long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
					
					// regA = eax
					code += AsmCommon.movToRegister(regA, "" + value) + nl;
					
					// eax = eax / regA
					code += "idiv " + regA.getName() + nl;
				}else if(bValue instanceof PStackElement){
					// eax = eax / bValue
					code += "idiv " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
				}
			}
		}else if(aValue instanceof PIntegerLiteral || aValue instanceof PHexLiteral){
			// A is a literal
			long value = aValue instanceof PIntegerLiteral ? ((PIntegerLiteral)aValue).value : ((PHexLiteral)aValue).value;
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// eax = value
			code += AsmCommon.movToRegister(AsmRegister.EAX, "" + value) + nl;
			
			// Sign extend eax
			code += "cdq" + nl;
			
			// eax = eax / regA
			code += "idiv " + regA.getName() + nl;
		}
		
		PStackElement stackForResult = null;
		if(aValue instanceof PStackElement && bValue instanceof PStackElement) {
			if(((PStackElement)aValue).stackOffset < ((PStackElement)bValue).stackOffset) {
				stackForResult = ((PStackElement)aValue);
			}else{
				stackForResult = ((PStackElement)bValue);
			}
		}else if(aValue instanceof PStackElement){
			stackForResult = ((PStackElement)aValue);
		}else if(bValue instanceof PStackElement){
			stackForResult = ((PStackElement)bValue);
		}
		code += finishResultant(stackForResult, AsmRegister.EAX, mathStack);
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveMultiplication(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> mathStack){
		String code = "";
		
		AsmRegister regA = program.registers.getFreeRegister();
		
		if(aValue instanceof PVarAccessor){
			if(bValue instanceof PVarAccessor){
				if(((PVarAccessor) bValue).containsArrayAccess()){
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// Both vars contains array access
						
						AsmRegister regB = program.registers.getFreeRegister();
						
						// regB = bValue
						code += movToRegister(regB, bValue) + nl;
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// regA = regA * regB
						code += "imul " + regA.getName() + ", " + regB.getName() + nl;
						
						program.registers.freeUpRegister(regB);
					}else{
						// Only b contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// regA = regA * eax
						code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
					}
				}else{
					// B doesn't contain array access, check if A contains
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// A contains array access
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = bValue
						code += movToRegister(AsmRegister.EAX, bValue) + nl;
						
						// regA = regA * eax
						code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
					}else{
						// None contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// regA = regA * eax
						code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
					}
				}
			}else{
				// B is not a var, it can be hex/int literal or stack element
				
				if(((PVarAccessor) aValue).containsArrayAccess()){
					// A contains array access
					
					// regA = aValue
					code += movToRegister(regA, aValue) + nl;
					
					// eax = bValue
					code += movToRegister(AsmRegister.EAX, bValue) + nl;
					
					// regA = regA * eax
					code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
				}else{
					// A doesn't contain array access
					
					// regA = aValue
					code += movToRegister(regA, aValue) + nl;
					
					// eax = bValue
					code += movToRegister(AsmRegister.EAX, bValue) + nl;
					
					// regA = regA * eax
					code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
				}
			}
		}else if(aValue instanceof PStackElement){
			// A is stack element
			
			if(bValue instanceof PVarAccessor){
				// B is a variable
				
				// regA = bValue
				code += movToRegister(regA, bValue) + nl;
				
				// eax = aValue
				code += movToRegister(AsmRegister.EAX, aValue) + nl;
				
				// regA = regA * eax
				code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
			}else{
				// B is not a variable, it may be int/hex literal or stack element
				
				// eax = bValue
				code += movToRegister(AsmRegister.EAX, bValue) + nl;
				
				// eax *= aValue
				code += "imul " + ((PStackElement)aValue).toStringWithSizeDirective() + nl;
				
				// result = eax
				program.registers.freeUpRegister(regA);
				regA = AsmRegister.EAX;
			}
		}else if(aValue instanceof PIntegerLiteral || aValue instanceof PHexLiteral){
			// A is a literal
			long value = aValue instanceof PIntegerLiteral ? ((PIntegerLiteral)aValue).value : ((PHexLiteral)aValue).value;
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// eax = value
			code += AsmCommon.movToRegister(AsmRegister.EAX, "" + value) + nl;
			
			// regA = regA * eax
			code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
		}
		
		PStackElement stackForResult = null;
		if(aValue instanceof PStackElement && bValue instanceof PStackElement) {
			if(((PStackElement)aValue).stackOffset < ((PStackElement)bValue).stackOffset) {
				stackForResult = ((PStackElement)aValue);
			}else{
				stackForResult = ((PStackElement)bValue);
			}
		}else if(aValue instanceof PStackElement){
			stackForResult = ((PStackElement)aValue);
		}else if(bValue instanceof PStackElement){
			stackForResult = ((PStackElement)bValue);
		}
		code += finishResultant(stackForResult, regA, mathStack);
		
		if(regA != AsmRegister.EAX) program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveSubtraction(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> mathStack){
		String code = "";
		
		AsmRegister regA = program.registers.getFreeRegister();
		AsmRegister resultantRegister = null;
		PStackElement resultantStack = null;
		
		if(aValue instanceof PVarAccessor){
			if(bValue instanceof PVarAccessor){
				if(((PVarAccessor) bValue).containsArrayAccess()){
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// Both contains array access
						
						AsmRegister regB = program.registers.getFreeRegister();
						
						// regB = bValue
						code += movToRegister(regB, bValue) + nl;
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// regA = regA - regB
						code += "sub " + regA.getName() + ", " + regB.getName() + nl;
						resultantRegister = regA;
						
						program.registers.freeUpRegister(regB);
					}else{
						// Only b contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax - regA
						code += "sub " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}else{
					// B doesn't contain array access, check if a contains
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// A contains array accesss
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = bValue
						code += movToRegister(AsmRegister.EAX, bValue) + nl;
						
						// regA = regA - eax
						code += "sub " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
						resultantRegister = regA;
					}else{
						// None contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax - regA
						code += "sub " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}
			}else{
				// B is not a var, it can be hex/int literal or stack element
				
				// regA = aValue
				code += movToRegister(regA, aValue) + nl;
				
				if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
					long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
					
					// regA = regA - value
					code += "sub " + regA.getName() + ", " + value + nl;
				}else if(bValue instanceof PStackElement){
					// regA = regA - stack
					code += "sub " + regA.getName() + ", " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
				}
				
				resultantRegister = regA;
			}
		}else if(aValue instanceof PStackElement){
			// A is stack element
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// aValue = aValue - regA
			code += "sub " + ((PStackElement)aValue).toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = (PStackElement)aValue;
		}else if(aValue instanceof PIntegerLiteral || aValue instanceof PHexLiteral){
			// A is a literal
			long value = aValue instanceof PIntegerLiteral ? ((PIntegerLiteral)aValue).value : ((PHexLiteral)aValue).value;
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// stack = aValue
			PStackElement stackElement = allocateStackSpace();
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), "" + value) + nl;
			
			// stack = stack - regA
			code += "sub " + stackElement.toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = stackElement;
		}
		

		/*if(resultantStack != null){
			stack.add(resultantStack);
		}else{
			PStackElement stackElement = allocateStackSpace();
			// stack = resultantRegister
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), resultantRegister) + nl;
			stack.add(stackElement);
		}*/
		code += finishResultant(resultantStack, resultantRegister, mathStack);
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveAddition(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> mathStack){
		String code = "";
		
		AsmRegister regA = program.registers.getFreeRegister();
		AsmRegister resultantRegister = null;
		PStackElement resultantStack = null;
		
		if(aValue instanceof PVarAccessor){
			if(bValue instanceof PVarAccessor){
				if(((PVarAccessor) bValue).containsArrayAccess()){
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// Both contains array access
						
						AsmRegister regB = program.registers.getFreeRegister();
						
						// regB = bValue
						code += movToRegister(regB, bValue) + nl;
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// regA = regA + regB
						code += "add " + regA.getName() + ", " + regB.getName() + nl;
						resultantRegister = regA;
						
						program.registers.freeUpRegister(regB);
					}else{
						// Only b contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax + regA
						code += "add " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}else{
					// B doesn't contain array access, check if A contains
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// A contains array accesss
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = bValue
						code += movToRegister(AsmRegister.EAX, bValue) + nl;
						
						// regA = regA + eax
						code += "add " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
						resultantRegister = regA;
					}else{
						// None contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax + regA
						code += "add " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}
			}else{
				// B is not a var, it can be hex/int literal or stack element
				
				// regA = aValue
				code += movToRegister(regA, aValue) + nl;
				
				if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
					long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
					
					// regA = regA + value
					code += "add " + regA.getName() + ", " + value + nl;
				}else if(bValue instanceof PStackElement){
					// regA = regA + stack
					code += "add " + regA.getName() + ", " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
				}
				
				resultantRegister = regA;
			}
		}else if(aValue instanceof PStackElement){
			// A is stack element
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// aValue = aValue + regA
			code += "add " + ((PStackElement)aValue).toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = (PStackElement)aValue;
		}else if(aValue instanceof PIntegerLiteral || aValue instanceof PHexLiteral){
			// A is a literal
			long value = aValue instanceof PIntegerLiteral ? ((PIntegerLiteral)aValue).value : ((PHexLiteral)aValue).value;
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// stack = stack + regA
			code += "add " + regA.getName() + ", " + value + nl;
			
			resultantRegister = regA;
		}
		
		code += finishResultant(resultantStack, resultantRegister, mathStack);
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveBitwse(POperatorType operator, PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> mathStack){
		String code = "";
		
		AsmRegister regA = program.registers.getFreeRegister();
		AsmRegister resultantRegister = null;
		PStackElement resultantStack = null;
		
		// Find out operation instruction
		String instruction = "";
		switch(operator){
		case BITWISE_AND:
			instruction = "and"; break;
		case BITWISE_OR:
			instruction = "or"; break;
		case BITWISE_XOR:
			instruction = "xor"; break;
		default:
		}
		
		// Solve bitwise operation
		if(aValue instanceof PVarAccessor){
			if(bValue instanceof PVarAccessor){
				if(((PVarAccessor) bValue).containsArrayAccess()){
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// Both contains array access
						
						AsmRegister regB = program.registers.getFreeRegister();
						
						// regB = bValue
						code += movToRegister(regB, bValue) + nl;
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// regA = regA . regB
						code += instruction + " " + regA.getName() + ", " + regB.getName() + nl;
						resultantRegister = regA;
						
						program.registers.freeUpRegister(regB);
					}else{
						// Only b contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax . regA
						code += instruction + " " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}else{
					// B doesn't contain array access, check if a contains
					if(((PVarAccessor) aValue).containsArrayAccess()){
						// A contains array accesss
						
						// regA = aValue
						code += movToRegister(regA, aValue) + nl;
						
						// eax = bValue
						code += movToRegister(AsmRegister.EAX, bValue) + nl;
						
						// regA = regA . eax
						code += instruction + " " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
						resultantRegister = regA;
					}else{
						// None contains array access
						
						// regA = bValue
						code += movToRegister(regA, bValue) + nl;
						
						// eax = aValue
						code += movToRegister(AsmRegister.EAX, aValue) + nl;
						
						// eax = eax . regA
						code += instruction + " " + AsmRegister.EAX.getName() + ", " + regA.getName() + nl;
						resultantRegister = AsmRegister.EAX;
					}
				}
			}else{
				// B is not a var, it can be hex/int literal or stack element
				
				// regA = aValue
				code += movToRegister(regA, aValue) + nl;
				
				if(bValue instanceof PIntegerLiteral || bValue instanceof PHexLiteral){
					long value = bValue instanceof PIntegerLiteral ? ((PIntegerLiteral)bValue).value : ((PHexLiteral)bValue).value;
					
					// regA = regA . value
					code += instruction + " " + regA.getName() + ", " + value + nl;
				}else if(bValue instanceof PStackElement){
					// regA = regA . stack
					code += instruction + " " + regA.getName() + ", " + ((PStackElement)bValue).toStringWithSizeDirective() + nl;
				}
				
				resultantRegister = regA;
			}
		}else if(aValue instanceof PStackElement){
			// A is stack element
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// aValue = aValue . regA
			code += instruction + " " + ((PStackElement)aValue).toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = (PStackElement)aValue;
		}else if(aValue instanceof PIntegerLiteral || aValue instanceof PHexLiteral){
			// A is a literal
			long value = aValue instanceof PIntegerLiteral ? ((PIntegerLiteral)aValue).value : ((PHexLiteral)aValue).value;
			
			// regA = bValue
			code += movToRegister(regA, bValue) + nl;
			
			// stack = aValue
			PStackElement stackElement = allocateStackSpace();
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), "" + value) + nl;
			
			// stack = stack . regA
			code += instruction + " " + stackElement.toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = stackElement;
		}
		

		/*if(resultantStack != null){
			stack.add(resultantStack);
		}else{
			PStackElement stackElement = allocateStackSpace();
			// stack = resultantRegister
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), resultantRegister) + nl;
			stack.add(stackElement);
		}*/
		code += finishResultant(resultantStack, resultantRegister, mathStack);
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String movToRegister(AsmRegister reg, PProgramPart item){
		String code = "";
		
		if(item instanceof PIntegerLiteral || item instanceof PHexLiteral){
			long value = item instanceof PIntegerLiteral ? ((PIntegerLiteral)item).value : ((PHexLiteral)item).value;
			code += AsmCommon.movToRegister(reg, "" + value);
		}else if(item instanceof PVarAccessor){
			PVarAccessor var = (PVarAccessor)item;
			code += var.movToRegister(reg, program);
		}else if(item instanceof PStackElement){
			PStackElement stack = (PStackElement)item;
			code += AsmCommon.movToRegister(reg, stack.toStringWithSizeDirective());
		}
		
		return code;
	}
	
	public String assignTo(PVarAccessor var){
		String code = "";
		
		// Null and Boolean literals are converted into
		// integer literals, thus we don't create another
		// code for them
		
		if(resultant instanceof PIntegerLiteral || resultant instanceof PHexLiteral){
			long value = resultant instanceof PIntegerLiteral ? ((PIntegerLiteral)resultant).value : ((PHexLiteral)resultant).value;
			code += var.assignIntLiteral(value, program);
		}else if(resultant instanceof PStringLiteral){
			code += var.assignStringLiteral((PStringLiteral)resultant, program);
		}else if(resultant instanceof PCharLiteral){
			code += var.assignCharLiteral((PCharLiteral)resultant, program);
		}else if(resultant instanceof PRegister){
			code += var.assignRegister(((PRegister)resultant).register, program);
		}else if(resultant instanceof PStackElement){
			code += AsmCommon.movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString()) + nl;
			code += var.assignRegister(AsmRegister.EAX, program);
		}else if(resultant instanceof PVarAccessor){
			code += var.assignVar((PVarAccessor)resultant, program);
		}
		
		return code;
	}
	
	public String assignTo(PStackElement stack){
		String code = "";
		
		// Null and Boolean literals are converted into
		// integer literals, thus we don't create another
		// code for them
		
		if(resultant instanceof PIntegerLiteral){
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + ((PIntegerLiteral)resultant).value);
		}else if(resultant instanceof PHexLiteral){
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + ((PHexLiteral)resultant).value);
		}else if(resultant instanceof PCharLiteral){
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + (int)((PCharLiteral)resultant).value);
		}else if(resultant instanceof PRegister){
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), ((PRegister)resultant).register);
		}else if(resultant instanceof PStackElement){
			code += AsmCommon.movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString()) + nl;
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
		}else if(resultant instanceof PVarAccessor){
			// eax = resultant
			code += AsmCommon.movToRegister(AsmRegister.EAX, (PVarAccessor)resultant) + nl;
			// stack = eax
			code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
		}
		
		return code;
	}
	
	public String assignTo(AsmRegister reg){
		String code = "";
		
		// Null and Boolean literals are converted into
		// integer literals, thus we don't create another
		// code for them
		
		if(resultant instanceof PIntegerLiteral){
			code += AsmCommon.movToRegister(reg, "" + ((PIntegerLiteral)resultant).value);
		}else if(resultant instanceof PHexLiteral){
			code += AsmCommon.movToRegister(reg, "" + ((PHexLiteral)resultant).value);
		}else if(resultant instanceof PCharLiteral){
			code += AsmCommon.movToRegister(reg, "" + (int)((PCharLiteral)resultant).value);
		}else if(resultant instanceof PRegister){
			code += AsmCommon.movToRegister(reg, "" + (PRegister)resultant);
		}else if(resultant instanceof PStackElement){
			code += AsmCommon.movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString());
			if(reg != AsmRegister.EAX){
				code += nl + AsmCommon.movToRegister(reg, AsmRegister.EAX);
			}
		}else if(resultant instanceof PVarAccessor){
			// reg = resultant
			code += AsmCommon.movToRegister(reg, (PVarAccessor)resultant);
		}
		
		return code;
	}
	
	private String solveParentheses() {
		String code = "";
		
		for(int i=0; i<infixExp.size(); i++) {
			PProgramPart el = infixExp.get(i);
			
			if(el instanceof PExpression) {
				// Parentheses expression
				PExpression parentheses = (PExpression)el;
				
				// Solve parentheses expression
				AsmExpressionNewNew parenAsmExp = new AsmExpressionNewNew(parentheses, stackBase + stackOffset, false, program);
				parenAsmExp.createExpCode();
				
				// Calculate stack space used by parentheses exp
				int parenStackUsage = (parenAsmExp.stackBase + parenAsmExp.stackOffset) - this.stackBase;
				
				// Update expression stack usage for the parent function
				parentFunction.setUsedStackSpaceByExpr(Math.max(parenStackUsage, parentFunction.getUsedStackSpaceByExpr()));

				// Add parentheses solution code to the begin of current expression code
				code += parenAsmExp.asmCode;
				
				// New: Doesn't matter how much stack space parentheses
				// expression used, because it's result size is no more than 4 bytes,
				// and since we move it's result to newly allocated stack space,
				// we don't need to increase stack space usage, allocateStackSpace() will do it.
				
				// Mov parentheses resultant onto current expression stack
				PProgramPart parenthesesResultant = parenAsmExp.resultant;
				if(parenthesesResultant instanceof PStackElement) {
					// Parentheses resultant as PStackElement
					PStackElement stackParenResultant = (PStackElement)parenthesesResultant;

					// If parentheses resultant is at same stack offset
					// as it would be placed here, avoid it, that would
					// waste cpu. example of the situation:
					//	mov eax, DWORD [esp + 16]
					//	mov DWORD [esp + 16], eax
					
					// But allocate stack space by any means, because
					// if parentheses resultant is on [esp + 8], this doesn't
					// ensure that parent expression stack [esp + 8] is allocated,
					// thus allocate it, but do not write unnecessary code
					
					// Allocate space on stack
					PStackElement stack = allocateStackSpace();
					
					if(nextStackOffset() != stackParenResultant.stackOffset) {
						if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE) {
							code += "; Push parentheses exp resultant onto the stack" + nl;
						}
						
						// eax = resultant
						code += AsmCommon.movToRegister(AsmRegister.EAX, ((PStackElement)parenthesesResultant).toStringWithSizeDirective()) + nl;
						
						// stack = eax
						code += AsmCommon.movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;

						code += nl;
						
						// Replace parentheses exp by it's resultant
						infixExp.set(i, stack);
					}else{
						// Replace parentheses exp by it's resultant
						infixExp.set(i, parenthesesResultant);
					}
				}else{
					// Replace parentheses exp by it's resultant
					infixExp.set(i, parenthesesResultant);
				}
			}
		}
		
	//	for(int i=0; i<sub; i++) System.out.print("\t");
	//	System.out.println("after solving: " + this.toStringInfix());
		return code;
	}

	private String finishResultant(PStackElement resultantStack, AsmRegister resultantRegister, ArrayList<PProgramPart> mathStack) {
		String code = "";
		
		if(resultantRegister != null) {
			// Free up the register, push it's value onto the stack
			PStackElement stackEl = resultantStack == null ? allocateStackSpace() : resultantStack;
			
			// stackEl = resultantRegister
			code += AsmCommon.movToAddress(stackEl.toString(), stackEl.getSizeDirective(), resultantRegister.getName()) + nl;
			
			mathStack.add(stackEl);
		}else{
			mathStack.add(resultantStack);
		}
		
		return code;
	}
	
	/** Returns postfix stack */
	private ArrayList<PProgramPart> createPostfixExp(ArrayList<PProgramPart> infixExp, ArrayList<PProgramPart> parentPostfix){
		// infixExp is Q, postfixExp is P, stack is stack
		ArrayList<PProgramPart> stack = new ArrayList<PProgramPart>();
		
		for(int i=0; i<infixExp.size(); i++){
			PProgramPart el = infixExp.get(i);
			
			if(isOperand(el)){
				// Element is an operand
				parentPostfix.add(el);
			}else if(el instanceof POperator){
				POperator operator = (POperator)el;
				
				if(stack.size() == 0){
					stack.add(operator);
				}else{
					while(stack.size() > 0){
						POperator stackOperator = (POperator)stack.get(stack.size() - 1);
						
						if(operator.type.precedence <= stackOperator.type.precedence){
							// Pop the stack and add item to the postfixExp
							PProgramPart lastItem = stack.get(stack.size() - 1);
							parentPostfix.add(lastItem);
							
							stack.remove(stack.size() - 1);
						}else{
							break;
						}
					}
					
					// Push latest operator onto the stack
					stack.add(operator);
				}
			}
		}
		
		// Pop stack items and add them to the postfixExp
		while(stack.size() > 0){
			// Pop the stack and add item to the postfixExp
			PProgramPart lastItem = stack.get(stack.size() - 1);
			parentPostfix.add(lastItem);
			
			stack.remove(stack.size() - 1);
		}
		
		return stack;
	}
	
	private static boolean isOperand(PProgramPart item){
		return item instanceof PVarAccessor || item instanceof PLiteral || item instanceof PStackElement || item instanceof PRegister || item instanceof PFunctionCall;
	}
	
	/** Evaluate function calls, and place returned value onto the stack */
	private String evalFunctionCalls(){
		String code = "";
		
		// Evaluate function calls
		for(int i=0; i<infixExp.size(); i++){
			if(infixExp.get(i) instanceof PFunctionCall){
				PFunctionCall fcall = (PFunctionCall)infixExp.get(i);
				
				// eax = function return
				code += createFunctionCall(fcall) + nl;
				
				// stack = eax
				PStackElement stack = allocateStackSpace();
				code += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
				
				// Replace function call with a stack element
				infixExp.set(i, stack);
			}
		}
		
		if (code.length() > 0) code += nl;
		return code;
	}
	
	/** Result will be in eax register */
	private String createFunctionCall(PFunctionCall call){
		AsmFunctionCall fCall = new AsmFunctionCall(call, program);
		// eax = fcall
		fCall.createFunctionCallCode(stackBase+stackOffset, true);
		
		return fCall.asmCode;
	}
	
	public PStackElement allocateStackSpace(){
		PStackElement el = new PStackElement(stackBase + stackOffset);
		stackOffset += 4;
		return el;
	}
	
	public int nextStackOffset() {
		return stackBase + stackOffset;
	}

	@Override
	public String toString(){
		StringJoiner joiner = new StringJoiner(" ");
		for(PProgramPart item : postfixExp) joiner.add(item.toString());
		return joiner.toString();
	}
	
	public String toStringInfix(){
		StringJoiner joiner = new StringJoiner(" ");
		for(PProgramPart item : infixExp) joiner.add(item.toString());
		return joiner.toString();
	}
}
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

public class AsmExpressionNew {
	private static final String nl = System.lineSeparator();
	
	protected int stackOffset = 0;
	protected int stackBase = 0;
	// Used to set expression stack space usage
	private PFunction parentFunction = null;
	private ArrayList<PProgramPart> infixExp = new ArrayList<PProgramPart>();
	private ArrayList<PProgramPart> postfixExp = new ArrayList<PProgramPart>();
	private AsmProgram program;
	
	private boolean pointerExpression = false;
	
	public String asmCode = "";
	public PProgramPart resultant = null;
	public boolean commentInAssembly = true;
	
	public AsmExpressionNew(PExpression exp, int stackBase, AsmProgram program) {
		this.parentFunction = exp.findParentFunction();
		this.infixExp.addAll(exp.expression);
		this.stackBase = stackBase;
		this.program = program;
		
		this.pointerExpression = exp.pointerExpression;
		
		// Add number bytes of stack used for functions calls, to the stack base
		this.stackBase += exp.findLargestArgsSize();
	}
	
	public AsmExpressionNew(PExpression exp, AsmProgram program){
		this(exp, 0, program);
	}
	
	public void createExpCode(){
		createPostfixExp(infixExp, postfixExp);
		createAssemblyCodeFromPostfix();
		
		if(pointerExpression) {
			System.out.println("pointer expression, postfix: " + toString());
			System.out.println("asmCode: ");
			System.out.println(asmCode);
			System.out.println();
		}
		
		// If this is not a global variable expression
		if(parentFunction != null){
			// Check if current expression stack space usage is
			// higher than currently set one
			int stackUsage = stackBase + stackOffset;
			parentFunction.setUsedStackSpaceByExpr(Math.max(stackUsage, parentFunction.getUsedStackSpaceByExpr()));
		}
	}
	
	private void createAssemblyCodeFromPostfix(){
		// Evaluate function calls, and place their returne values onto the stack
		evalFunctionCalls();
		
		ArrayList<PProgramPart> stack = new ArrayList<PProgramPart>();
		
		for(int i=0; i<postfixExp.size(); i++){
			PProgramPart el = postfixExp.get(i);
			
			if(isOperand(el)){
				stack.add(el);
			}else if(el instanceof POperator){
				POperator operator = (POperator)el;
				
				PProgramPart bValue = stack.get(stack.size() - 1);
				stack.remove(stack.size() - 1);
				
				PProgramPart aValue = stack.get(stack.size() - 1);
				stack.remove(stack.size() - 1);
				
				switch(operator.type){
				case DIV:
					if(commentInAssembly)
						asmCode += "; " + aValue + " / " + bValue + nl;
					
					asmCode += solveDivision(aValue, bValue, stack) + nl;
					break;
				case MUL:
					if(commentInAssembly)
						asmCode += "; " + aValue + " * " + bValue + nl;
					
					asmCode += solveMultiplication(aValue, bValue, stack) + nl;
					break;
				case SUB:
					if(commentInAssembly)
						asmCode += "; " + aValue + " - " + bValue + nl;
					
					asmCode += solveSubtraction(aValue, bValue, stack) + nl;
					break;
				case ADD:
					if(commentInAssembly)
						asmCode += "; " + aValue + " + " + bValue + nl;
					
					asmCode += solveAddition(aValue, bValue, stack) + nl;
					break;
				case BITWISE_AND:
				case BITWISE_OR:
				case BITWISE_XOR:
					if(commentInAssembly)
						asmCode += "; " + aValue + " " + operator + " " + bValue + nl;
					
					asmCode += solveBitwse(operator.type, aValue, bValue, stack) + nl;
					break;
				default:
				}
			}
		}
		
		resultant = stack.get(0);
		if(pointerExpression) {
			System.out.println("resultant: " + resultant);
		}
	}
	
	private String solveDivision(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> stack){
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

		PStackElement stackElement = null;
		
		if(aValue instanceof PStackElement){
			stackElement = (PStackElement)aValue;
		}else if(bValue instanceof PStackElement){
			stackElement = (PStackElement)bValue;
		}else{
			stackElement = allocateStackSpace();
		}
		
		// stack = eax
		code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), AsmRegister.EAX) + nl;
		stack.add(stackElement);
		
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveMultiplication(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> stack){
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
				
				// eax = aValue
				code += movToRegister(AsmRegister.EAX, aValue) + nl;
				
				// regA = bValue
				code += movToRegister(regA, bValue) + nl;
				
				// regA = regA * eax
				code += "imul " + regA.getName() + ", " + AsmRegister.EAX.getName() + nl;
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
		
		PStackElement stackElement = null;
		
		if(aValue instanceof PStackElement){
			stackElement = (PStackElement)aValue;
		}else if(bValue instanceof PStackElement){
			stackElement = (PStackElement)bValue;
		}else{
			stackElement = allocateStackSpace();
		}
		
		// stack = eax
		code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), regA) + nl;
		stack.add(stackElement);
		
		program.registers.freeUpRegister(regA);
		
		return code;
	}
	
	private String solveSubtraction(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> stack){
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
		
		program.registers.freeUpRegister(regA);

		if(resultantStack != null){
			stack.add(resultantStack);
		}else{
			PStackElement stackElement = allocateStackSpace();
			// stack = resultantRegister
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), resultantRegister) + nl;
			stack.add(stackElement);
		}
		
		return code;
	}
	
	private String solveAddition(PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> stack){
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
			
			// stack = aValue
			PStackElement stackElement = allocateStackSpace();
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), "" + value) + nl;
			
			// stack = stack + regA
			code += "add " + stackElement.toStringWithSizeDirective() + ", " + regA.getName() + nl;
			
			resultantStack = stackElement;
		}
		
		program.registers.freeUpRegister(regA);
		
		if(resultantStack != null){
			stack.add(resultantStack);
		}else{
			PStackElement stackElement = allocateStackSpace();
			
			if(pointerExpression) {
				// resultantRegister = [resultantRegister]
				code += AsmCommon.movToRegister(resultantRegister, "[" + resultantRegister.getName() + "]") + nl;
			}
			
			// stack = resultantRegister
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), resultantRegister) + nl;
			stack.add(stackElement);
		}
		
		return code;
	}
	
	private String solveBitwse(POperatorType operator, PProgramPart aValue, PProgramPart bValue, ArrayList<PProgramPart> stack){
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
		
		program.registers.freeUpRegister(regA);

		if(resultantStack != null){
			stack.add(resultantStack);
		}else{
			PStackElement stackElement = allocateStackSpace();
			// stack = resultantRegister
			code += AsmCommon.movToAddress(stackElement.toString(), stackElement.getSizeDirective(), resultantRegister) + nl;
			stack.add(stackElement);
		}
		
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
	
	/** Returns postfix stack */
	private static ArrayList<PProgramPart> createPostfixExp(ArrayList<PProgramPart> infixExp, ArrayList<PProgramPart> parentPostfix){
		// infixExp is Q, postfixExp is P, stack is stack
		ArrayList<PProgramPart> stack = new ArrayList<PProgramPart>();
		
		for(int i=0; i<infixExp.size(); i++){
			PProgramPart el = infixExp.get(i);
			
			if(isOperand(el)){
				// Element is an operand
				parentPostfix.add(el);
			}else if(el instanceof PExpression){
				// Parentheses expression
				PExpression parentheses = (PExpression)el;
				stack.addAll(createPostfixExp(parentheses.expression, parentPostfix));
				
				while(stack.size() > 0){
					// Pop last stack item and add it to the postfixExp
					PProgramPart lastItem = stack.get(stack.size() - 1);
					parentPostfix.add(lastItem);
					
					stack.remove(stack.size() - 1);
				}
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
	
	/** Evaluate function calls, and place returned values onto the stack */
	private void evalFunctionCalls(){
		// Evaluate function calls
		for(int i=0; i<postfixExp.size(); i++){
			if(postfixExp.get(i) instanceof PFunctionCall){
				PFunctionCall fcall = (PFunctionCall)postfixExp.get(i);
				
				// eax = function return
				asmCode += createFunctionCall(fcall) + nl;
				
				// stack = eax
				PStackElement stack = allocateStackSpace();
				asmCode += AsmCommon.movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
				
				// Replace function call with a stack element
				postfixExp.set(i, stack);
			
				asmCode += nl;
			}
		}
	}
	
	/** Result will be in eax register */
	private String createFunctionCall(PFunctionCall call){
		AsmFunctionCall fCall = new AsmFunctionCall(call, program);
		// eax = fcall
		fCall.createFunctionCallCode(stackBase+stackOffset, false);
		
		return fCall.asmCode;
	}
	
	public PStackElement allocateStackSpace(){
		PStackElement el = new PStackElement(stackBase + stackOffset);
		stackOffset += 4;
		return el;
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
package mpl.compiler.asm;

import static mpl.compiler.asm.AsmCommon.*;

import java.util.ArrayList;

import mpl.parser.syntactic.parts.PAssignmentStatement;
import mpl.parser.syntactic.parts.PCharLiteral;
import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PFunctionCall;
import mpl.parser.syntactic.parts.PHexLiteral;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.POperator;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.POperator.POperatorType;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PRegister;
import mpl.parser.syntactic.parts.PStackElement;
import mpl.parser.syntactic.parts.PVarAccessor;

public class AsmExpression {
	private static final String nl = System.lineSeparator();

	protected int stackOffset = 0;
	protected int stackBase = 0;
	private PExpression pExp;
	private ArrayList<PProgramPart> exp;
	private AsmProgram program;
	
	public String asmCode = "";
	public PProgramPart resultant = null;
	public boolean commentInAssembly = true;
	
	public AsmExpression(PExpression exp, int stackBase, AsmProgram program) {
		this.exp = new ArrayList<>();
		this.exp.addAll(exp.expression);
		this.pExp = exp;
		this.stackBase = stackBase;
		this.program = program;
	}
	
	public AsmExpression(PExpression exp, AsmProgram program){
		this(exp, 0, program);
	}
	
	public void createExpCode(){
		// Find largest number of arguments in a function call
		int argsSize = pExp.findLargestArgsSize();
		stackOffset += argsSize;
		
		// Solve all parentheses expressions
		for(int i=0; i<exp.size(); i++){
			if(exp.get(i) instanceof PExpression){
				PExpression parentheses = (PExpression)exp.get(i);
				
				// Create parentheses expression code
				AsmExpression parenthesesExp = new AsmExpression(parentheses, stackBase + stackOffset, program);
				parenthesesExp.createExpCode();
				asmCode += parenthesesExp.asmCode;
				
				// If parentheses used more stack, than this expression,
				// then set stackOffset to bigger one
				if(parenthesesExp.stackOffset > stackOffset)
					stackOffset = parenthesesExp.stackOffset;
				
				// Resultant may be changed, so we change this var
				PProgramPart resultant = parenthesesExp.resultant;
				
				// If that's a pointer expression, then get value from
				// address contained in the resultant
				if(parentheses.pointerExpression){
					if(parenthesesExp.resultant instanceof PRegister){
						AsmRegister reg = ((PRegister)parenthesesExp.resultant).register;
						asmCode += AsmCommon.movToRegister(reg, "[" + reg.getName() + "]") + nl;
					}else{
						// eax = resultant
						asmCode += parenthesesExp.assignTo(AsmRegister.EAX) + nl;
						// eax = [eax]
						asmCode += AsmCommon.movToRegister(AsmRegister.EAX, "[" + AsmRegister.EAX.getName() + "]");
						
						// If resultant was stack, store result back onto the stack
						if(resultant instanceof PStackElement){
							PStackElement stack = (PStackElement)resultant;
							asmCode += AsmCommon.movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;
						}else{
							// If resultant wasn't stack, then allocate stack space,
							// and store result onto the stack
							PStackElement stack = allocateStackSpace();
							asmCode += AsmCommon.movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;
							
							// Replace resultant aswell
							resultant = stack;
						}
					}
				}
				
				exp.set(i, resultant);
			}else if(exp.get(i) instanceof PAssignmentStatement){
				// Assignments in expression also counts as
				// a parenthesis expression, so solve assignment
				// and replace operand with the assignment target variable
				PAssignmentStatement assignment = (PAssignmentStatement)exp.get(i);
				
				// Create expression code
				AsmExpression assignmentExp = new AsmExpression(assignment.expression, program);
				assignmentExp.createExpCode();
				if(assignmentExp.asmCode.length() > 0)
					asmCode += assignmentExp.asmCode;
				
				if(commentInAssembly)
					asmCode += "; " + assignment + nl;
				
				// Assign expression result to the variable
				asmCode += assignmentExp.assignTo(assignment.variable);
				
				// Free up expression registers
				assignmentExp.freeUpRegisters(program.registers);
				
				// Set resultant to the assignment target variable
				exp.set(i, assignment.variable);
			}
		}
		
		// Solve all function calls
		for(int i=0; i<exp.size(); i++){
			if(exp.get(i) instanceof PFunctionCall){
				PFunctionCall fcall = (PFunctionCall)exp.get(i);
				
				// eax = function return
				asmCode += createFunctionCall(fcall) + nl;
				
				// stack = eax
				PStackElement stack = allocateStackSpace();
				asmCode += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
				
				// Replace function call with a stack element
				exp.set(i, stack);
			
				asmCode += nl;
			}
		}
		
		// Solve all arrays
		for(int i=0; i<exp.size(); i++){
			if(exp.get(i) instanceof PVarAccessor){
				PVarAccessor var = (PVarAccessor)exp.get(i);
				
				if(var.containsArrayAccess() && var.getGrandChild().getDataTypeUnmodified().plain() != PDataType.STRUCT){
					PStackElement stack = allocateStackSpace();
					
					if(commentInAssembly){
						asmCode += "; " + stack.toString() + " = " + var + nl;
					}
					
					// edx = var
					asmCode += movToRegister(AsmRegister.EDX, var) + nl;
					
					// stack = edx
					asmCode += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EDX);
					
					// replace operand in exp
					exp.set(i, stack);
					
					asmCode += nl;
				}
			}
		}
		
		int divOp = containsOperator(POperatorType.DIV);
		while(divOp > -1){
			asmCode += createDivisionOperationCode(exp.get(divOp-1), exp.get(divOp+1), divOp, program.registers);
			divOp = containsOperator(POperatorType.DIV);
		}

		int mulOp = containsOperator(POperatorType.MUL);
		while(mulOp > -1){
			asmCode += createMultiplicationOperationCode(exp.get(mulOp-1), exp.get(mulOp+1), mulOp, program.registers);
			mulOp = containsOperator(POperatorType.MUL);
		}
		
		int nextOp = nextOperator();
		while(nextOp > -1){
			POperator operator = (POperator)exp.get(nextOp);
			
			switch(operator.type){
			case ADD: asmCode += createAdditionOperationCode(exp.get(nextOp-1), exp.get(nextOp+1), nextOp, program.registers); break;
			case SUB: asmCode += createSubtractionOperationCode(exp.get(nextOp-1), exp.get(nextOp+1), nextOp, program.registers); break;
			case BITWISE_AND:
			case BITWISE_OR:
			case BITWISE_XOR:
				asmCode += createBitwiseOperationCode(exp.get(nextOp-1), exp.get(nextOp+1), nextOp, program.registers);
				break;
			default:
			}
			
			nextOp = nextOperator();
		}
		
		/*int subOp = containsOperator(POperatorType.SUB);
		int addOp = containsOperator(POperatorType.ADD);
		while(subOp > -1 || addOp > -1){
			if(subOp > -1){
				if(addOp > -1){
					// Check which operator is closer to the left,
					// i.e. which operation is going to be performed first
					int min = Math.min(subOp, addOp);
					if(min == subOp){
						asmCode += createSubtractionOperationCode(exp.get(subOp-1), exp.get(subOp+1), subOp, program.registers);
					}else{
						asmCode += createAdditionOperationCode(exp.get(addOp-1), exp.get(addOp+1), addOp, program.registers);
					}
				}else{
					// There is no addition in the expression, only subtraction
					asmCode += createSubtractionOperationCode(exp.get(subOp-1), exp.get(subOp+1), subOp, program.registers);
				}
			}else{
				if(addOp > -1){
					// There is no subtraction in the expression, only addition
					asmCode += createAdditionOperationCode(exp.get(addOp-1), exp.get(addOp+1), addOp, program.registers);
				}
			}
			
			subOp = containsOperator(POperatorType.SUB);
			addOp = containsOperator(POperatorType.ADD);
		}*/
		
		// Set resultant to the last one left element of the expression
		resultant = exp.get(0);
		
		// If resultant is a string literal, add it to the ro-data section
		if(resultant instanceof PStringLiteral){
			PStringLiteral str = (PStringLiteral)resultant;
			program.roData.addData(str.roDataName, "db " + str.getAssemblyValue());
		}
	}
	
	private String createDivisionOperationCode(PProgramPart leftOperand, PProgramPart rightOperand, int index, AsmSharedRegisters regs){
		String code = "";
		
		boolean constantsOnly = leftOperand instanceof PIntegerLiteral && rightOperand instanceof PIntegerLiteral;
		
		if(commentInAssembly && !constantsOnly)
			code += "; " + leftOperand + " / " + rightOperand + nl;
		
		if(leftOperand instanceof PIntegerLiteral || leftOperand instanceof PHexLiteral){
			long lhs = leftOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)leftOperand).value : ((PHexLiteral)leftOperand).value;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(intlit)
				code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
				// sign extend eax
				code += "cdq" + nl;
			}
			
			if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += rhs.movToRegister(reg.register, program) + nl;

					// eax = lhs(intlit)
					code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg.register.getName() + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += rhs.movToRegister(AsmRegister.EAX, program) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(intlit)
					code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax / rhs(reg)
				code += "idiv " + rhs + nl;
				
				// rhs(reg) = eax
				code += movToRegister(rhs.register, AsmRegister.EAX) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax / rhs(stack)
				code += "idiv " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}
		}else if(leftOperand instanceof PVarAccessor){
			PVarAccessor lhs = (PVarAccessor)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(var)
				code += movToRegister(AsmRegister.EAX, lhs) + nl;
				// sign extend eax
				code += "cdq" + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs) + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = rhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + rhs) + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toString() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += rhs.movToRegister(reg.register, program) + nl;
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg.register.getName() + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += rhs.movToRegister(AsmRegister.EAX, program) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax / rhs(reg)
				code += "idiv " + rhs + nl;
				
				// rhs(reg) = eax
				code += movToRegister(rhs.register, AsmRegister.EAX) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax / rhs(stack)
				code += "idiv " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PRegister){
			PRegister lhs = (PRegister)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(reg)
				code += movToRegister(AsmRegister.EAX, lhs.register) + nl;
				// sign extend eax
				code += "cdq" + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// lhs(reg) = rhs(intlit)
				code += movToRegister(lhs.register, "" + rhs) + nl;
				
				// eax = eax / lhs(reg)
				code += "idiv " + lhs + nl;
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// eax = rhs(var)
				code += movToRegister(lhs.register, rhs) + nl;
				
				// exchange reg(lhs) with eax(rhs) reg
				code += "xchg " + lhs.toString() + ", " + AsmRegister.EAX.getName() + nl;
				
				// Sign extend eax
				code += "cdq" + nl;
				
				// eax = eax / lhs(reg)
				code += "idiv " + lhs + nl;
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax / rhs(reg)
				code += "idiv " + rhs + nl;
				
				// Free up right register
				regs.freeUpRegister(rhs.register);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax / rhs(stack)
				code += "idiv " + rhs.toStringWithSizeDirective() + nl;
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// eax xchg lhs(reg)
				code += "xchg " + AsmRegister.EAX.getName() + ", " + lhs + nl;
				
				// eax = eax / lhs(reg)
				code += "idiv " + lhs + nl;
			}*/
			
			// lhs(reg) = eax
			code += movToRegister(lhs.register, AsmRegister.EAX) + nl;
			
			replaceOperatorAndRemoveOperands(index, lhs);
		}else if(leftOperand instanceof PStackElement){
			PStackElement lhs = (PStackElement)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(stack)
				code += movToRegister(AsmRegister.EAX, lhs.toStringWithSizeDirective()) + nl;
				// sign extend eax
				code += "cdq" + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs) + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = rhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + rhs) + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += rhs.movToRegister(reg.register, program) + nl;
					
					// eax = lhs(stack)
					code += movToRegister(AsmRegister.EAX, lhs.toStringWithSizeDirective()) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += rhs.movToRegister(AsmRegister.EAX, program) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), stack.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(stack)
					code += movToRegister(AsmRegister.EAX, lhs.toStringWithSizeDirective()) + nl;
					// sign extend eax
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), lhs.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax / rhs(reg)
				code += "idiv " + rhs + nl;
				
				// rhs(reg) = eax
				code += movToRegister(rhs.register, AsmRegister.EAX) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax / rhs(stack)
				code += "idiv " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// eax xchg lhs(stack)
				code += "xchg " + AsmRegister.EAX.getName() + ", " + lhs.toStringWithSizeDirective() + nl;
				
				// eax = eax / lhs(stack)
				code += "idiv " + lhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}*/
		}/*else if(leftOperand instanceof PFunctionCall){
			PFunctionCall lhs = (PFunctionCall)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
				// sign extend
				code += "cdq" + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral){
				PIntegerLiteral rhs = (PIntegerLiteral)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs.value) + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = rhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + rhs.value) + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax / rhs(reg)
				code += "idiv " + rhs + nl;
				
				// rhs(reg) = eax
				code += movToRegister(rhs.register, AsmRegister.EAX) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax / rhs(stack)
				code += "idiv " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// eax xchg reg
					code += "xchg " + AsmRegister.EAX.getName() + ", " + reg + nl;

					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / reg
					code += "idiv " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// eax xchg stack
					code += "xchg " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;

					// sign extend
					code += "cdq" + nl;
					
					// eax = eax / stack
					code += "idiv " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}
		}*/
		
		return code;
	}

	private String createMultiplicationOperationCode(PProgramPart leftOperand, PProgramPart rightOperand, int index, AsmSharedRegisters regs){
		String code = "";
		
		if(commentInAssembly)
			code += "; " + leftOperand + " * " + rightOperand + nl;
		
		if(leftOperand instanceof PIntegerLiteral || leftOperand instanceof PHexLiteral){
			long lhs = leftOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)leftOperand).value : ((PHexLiteral)leftOperand).value;
			
			if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = lhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + lhs) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = lhs(intlit)
				code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
				// rhs(reg) = rhs(reg) * eax
				code += "imul " + rhs + ", " + AsmRegister.EAX.getName() + "" + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs) + nl;
					
					// reg = reg * rhs(stack)
					code += "imul " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(intlit)
					code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
					
					// eax = eax * rhs(stack)
					code += "imul " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs.value) + nl;
					
					// reg = reg * eax
					code += "imul " + reg.register + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = lhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + lhs.value) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PVarAccessor){
			PVarAccessor lhs = (PVarAccessor)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs) + nl;
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = rhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + rhs) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = lhs(var)
				code += movToRegister(AsmRegister.EAX, lhs) + nl;
				
				// rhs(reg) = rhs(reg) * eax
				code += "imul " + rhs + ", " + AsmRegister.EAX.getName() + "" + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// reg = reg * rhs(stack)
					code += "imul " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// eax = eax * rhs(stack)
					code += "imul " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
				
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PRegister){
			PRegister lhs = (PRegister)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// eax = rhs(intlit)
				code += movToRegister(AsmRegister.EAX, "" + rhs) + nl;
				
				// lhs(reg) = lhs(reg) * eax
				code += "imul " + lhs + ", " + AsmRegister.EAX.getName() + "" + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// eax = rhs(var)
				code += movToRegister(AsmRegister.EAX, rhs) + nl;
				
				// lhs(reg) = lhs(reg) * eax
				code += "imul " + lhs + ", " + AsmRegister.EAX.getName() + "" + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// lhs(reg) = lhs(reg) * rhs(reg)
				code += "imul " + lhs + ", " + rhs + nl;
				
				// Free up right register
				regs.freeUpRegister(rhs.register);
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// lhs(reg) = lhs(reg) * rhs(stack)
				code += "imul " + lhs + ", " + rhs.toStringWithSizeDirective() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// lhs(reg) = lhs(reg) * eax
				code += "imul " + lhs + ", " + AsmRegister.EAX.getName() + "" + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}*/
		}else if(leftOperand instanceof PStackElement){
			PStackElement lhs = (PStackElement)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs) + nl;
				
					// reg = reg * lhs(stack)
					code += "imul " + reg + ", " + lhs.toStringWithSizeDirective();
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = rhs(intlit)
					code += movToRegister(AsmRegister.EAX, "" + rhs) + nl;
					
					// eax = eax * lhs(stack)
					code += "imul " + AsmRegister.EAX.getName() + ", " + lhs.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toString()) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// eax = eax * lhs(stack)
					code += "imul " + AsmRegister.EAX.getName() + ", " + lhs.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// rhs(reg) = rhs(reg) * lhs(stack)
				code += "imul " + rhs + ", " + lhs.toStringWithSizeDirective() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// reg = reg * rhs(stack)
					code += "imul " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(stack)
					code += movToRegister(AsmRegister.EAX, lhs.toStringWithSizeDirective()) + nl;
					
					// eax = eax * rhs(stack)
					code += "imul " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// eax = eax * lhs(stack)
				code += "imul " + AsmRegister.EAX.getName() + ", " + lhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}*/
		}/*else if(leftOperand instanceof PFunctionCall){
			PFunctionCall lhs = (PFunctionCall)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral){
				PIntegerLiteral rhs = (PIntegerLiteral)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(intlit)
					code += movToRegister(reg.register, "" + rhs.value) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = rhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + rhs.value) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// reg = reg * eax;
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + "" + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// rhs(reg) = rhs(reg) * eax
				code += "imul " + rhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax * rhs(stack)
				code += "imul " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg * eax
					code += "imul " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// eax = eax * stack
					code += "imul " + AsmRegister.EAX.getName() + ", " + stack.toStringWithSizeDirective() + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}
		}*/
		
		return code;
	}
	
	private String createAdditionOperationCode(PProgramPart leftOperand, PProgramPart rightOperand, int index, AsmSharedRegisters regs){
		String code = "";
		
		if(commentInAssembly)
			code += "; " + leftOperand + " + " + rightOperand + nl;
		
		if(leftOperand instanceof PIntegerLiteral || leftOperand instanceof PHexLiteral){
			long lhs = leftOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)leftOperand).value : ((PHexLiteral)leftOperand).value;
			
			if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// reg = reg + lhs(intlit)
					code += "add " + reg + ", " + lhs + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// eax = eax + lhs(intlit)
					code += "add " + AsmRegister.EAX.getName() + ", " + lhs + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// rhs(reg) = rhs(reg) + lhs(intlit)
				code += "add " + rhs + ", " + lhs + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs) + nl;
					
					// reg = reg + rhs(stack)
					code += "add " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = rhs(stack) + lhs(intlit)
					code += "add " + rhs.toStringWithSizeDirective() + ", " + lhs + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// eax = eax(fcall) + lhs(intlit)
				code += "add " + AsmRegister.EAX.getName() + ", " + lhs.value + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PVarAccessor){
			PVarAccessor lhs = (PVarAccessor)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// reg = reg + rhs(intlit)
					code += "add " + reg + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// eax = eax + rhs(intlit)
					code += "add " + AsmRegister.EAX.getName() + ", " + rhs + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg + eax
					code += "add " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = stack + eax
					code += "add " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = lhs(var)
				code += movToRegister(AsmRegister.EAX, lhs) + nl;
				
				// rhs(reg) = rhs(reg) + eax
				code += "add " + rhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// reg = reg + rhs(stack)
					code += "add " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// rhs(stack) = rhs(stack) + eax
					code += "add " + rhs.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
				
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg + eax
					code += "add " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// stack = stack + eax
					code += "add " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PRegister){
			PRegister lhs = (PRegister)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// lhs(reg) = lhs(reg) + rhs(intlit)
				code += "add " + lhs + ", " + rhs + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// eax = rhs(var)
				code += movToRegister(AsmRegister.EAX, rhs) + nl;
				
				// lhs(reg) = lhs(reg) + eax
				code += "add " + lhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// lhs(reg) = lhs(reg) + rhs(reg)
				code += "add " + lhs + ", " + rhs + nl;
				
				// Free up right register
				regs.freeUpRegister(rhs.register);
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// lhs(reg) = lhs(reg) + rhs(stack)
				code += "add " + lhs + ", " + rhs.toStringWithSizeDirective() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// lhs(reg) = lhs(reg) + eax
				code += "add " + lhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}*/
		}else if(leftOperand instanceof PStackElement){
			PStackElement lhs = (PStackElement)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// reg = reg + rhs(intlit)
					code += "add " + reg + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = lhs(stack) + rhs(intlit)
					code += "add " + lhs.toStringWithSizeDirective() + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// reg = reg + lhs(stack)
					code += "add " + reg + ", " + lhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// lhs(stack) = lhs(stack) + eax
					code += "add " + lhs.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// rhs(reg) = rhs(reg) + lhs(stack)
				code += "add " + rhs + ", " + lhs.toStringWithSizeDirective() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toString()) + nl;
					
					// reg = reg + rhs(stack)
					code += "add " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(stack)
					code += movToRegister(AsmRegister.EAX, lhs.toString()) + nl;
					
					// eax = eax + rhs(stack)
					code += "add " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toString()) + nl;
					
					// reg = reg + eax
					code += "add " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = lhs(stack) + eax
					code += "add " + lhs.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}*/
		}/*else if(leftOperand instanceof PFunctionCall){
			PFunctionCall lhs = (PFunctionCall)leftOperand;
			
			if(!(rightOperand instanceof PVarAccessor)){
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
			}
			
			if(rightOperand instanceof PIntegerLiteral){
				PIntegerLiteral rhs = (PIntegerLiteral)rightOperand;
				
				// eax = eax + rhs(intlit)
				code += "add " + AsmRegister.EAX.getName() + ", " + rhs.value + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// reg = reg + eax
					code += "add " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// stack = stack + eax
					code += "add " + stack.toString() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// rhs(reg) = rhs(reg) + eax
				code += "add " + rhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = eax + rhs(stack)
				code += "add " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg + eax
					code += "add " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// stack = stack + eax
					code += "add " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}
		}*/
		
		return code;
	}
	
	private String createSubtractionOperationCode(PProgramPart leftOperand, PProgramPart rightOperand, int index, AsmSharedRegisters regs){
		String code = "";
		
		if(commentInAssembly)
			code += "; " + leftOperand + " - " + rightOperand + nl;
		
		if(leftOperand instanceof PIntegerLiteral || leftOperand instanceof PHexLiteral){
			long lhs = leftOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)leftOperand).value : ((PHexLiteral)leftOperand).value;
			
			if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;

				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = lhs(intlit);
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toString() + ", " + AsmRegister.EAX.getName() + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = rhs(reg)
				code += movToRegister(AsmRegister.EAX, rhs.toString()) + nl;
				
				// rhs(reg) = lhs(intlit)
				code += movToRegister(rhs.register, "" + lhs) + nl;
				
				// rhs(reg) = rhs(reg) - eax
				code += "sub " + rhs.register + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs) + nl;
					
					// reg = reg - rhs(stack)
					code += "sub " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(intlit)
					code += movToRegister(AsmRegister.EAX, "" + lhs) + nl;
					
					// eax = eax - rhs(stack)
					code += "sub " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(intlit)
					code += movToRegister(reg.register, "" + lhs.value) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = lhs(intlit)
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + lhs.value) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PVarAccessor){
			PVarAccessor lhs = (PVarAccessor)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// reg = reg - rhs(intlit)
					code += "sub " + reg + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// eax = eax - rhs(intlit)
					code += "sub " + AsmRegister.EAX.getName() + ", " + rhs + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;

					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// Create register
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toString() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = eax - rhs(reg)
				code += "sub " + AsmRegister.EAX.getName() + ", " + rhs + nl;
				
				// rhs(reg) = eax
				code += movToRegister(rhs.register, AsmRegister.EAX) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
					
					// reg = reg - rhs(stack)
					code += "sub " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// eax = eax - rhs(stack)
					code += "sub " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(var)
					code += movToRegister(reg.register, lhs) + nl;
				
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(var)
					code += movToRegister(AsmRegister.EAX, lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
				
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}*/
		}else if(leftOperand instanceof PRegister){
			PRegister lhs = (PRegister)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				// lhs(reg) = lhs(reg) - rhs(intlit)
				code += "sub " + lhs + ", " + rhs + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				// eax = rhs(var)
				code += movToRegister(AsmRegister.EAX, rhs) + nl;
				
				// lhs(reg) = lhs(reg) - eax
				code += "sub " + lhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// lhs(reg) = lhs(reg) - rhs(reg)
				code += "sub " + lhs + ", " + rhs + nl;
				
				// Free up right register
				regs.freeUpRegister(rhs.register);
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// lhs(reg) = lhs(reg) - rhs(stack)
				code += "sub " + lhs + ", " + rhs.toStringWithSizeDirective() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				// eax = rhs(fcall)
				code += createFunctionCall(rhs) + nl;
				
				// lhs(reg) = lhs(reg) - eax
				code += "sub " + lhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, lhs);
			}*/
		}else if(leftOperand instanceof PStackElement){
			PStackElement lhs = (PStackElement)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
				long rhs = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// reg = reg - rhs(intlit)
					code += "sub " + reg + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// lhs(stack) = lhs(stack) - rhs(intlit)
					code += "sub " + lhs.toStringWithSizeDirective() + ", " + rhs + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// lhs(stack) = lhs(stack) - eax
					code += "sub " + lhs.toString() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// lhs(stack) = lhs(stack) - rhs(reg)
				code += "sub " + lhs.toStringWithSizeDirective() + ", " + rhs + nl;
				
				// rhs(reg) = lhs(stack)
				code += movToRegister(rhs.register, lhs.toStringWithSizeDirective()) + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// reg = reg - rhs(stack)
					code += "sub " + reg + ", " + rhs.toStringWithSizeDirective() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = lhs(stack)
					code += movToRegister(AsmRegister.EAX, lhs.toString()) + nl;
					
					// eax = eax - rhs(stack)
					code += "sub " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
					
					// lhs(stack) = eax
					code += movToAddress(lhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}/*else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = lhs(stack)
					code += movToRegister(reg.register, lhs.toStringWithSizeDirective()) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// lhs(stack) = lhs(stack) - eax
					code += "sub " + lhs.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, lhs);
				}
			}*/
		}/*else if(leftOperand instanceof PFunctionCall){
			PFunctionCall lhs = (PFunctionCall)leftOperand;
			
			if(rightOperand instanceof PIntegerLiteral){
				PIntegerLiteral rhs = (PIntegerLiteral)rightOperand;
				
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
				
				// eax = eax - rhs(intlit)
				code += "sub " + AsmRegister.EAX.getName() + ", " + rhs.value + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PVarAccessor){
				PVarAccessor rhs = (PVarAccessor)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = rhs(var)
					code += movToRegister(reg.register, rhs) + nl;
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// eax = eax - reg
					code += "sub " + AsmRegister.EAX.getName() + ", " + reg + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(var)
					code += movToRegister(AsmRegister.EAX, rhs) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}else if(rightOperand instanceof PRegister){
				PRegister rhs = (PRegister)rightOperand;
				
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
				
				// eax xchg rhs(reg)
				code += "xchg " + AsmRegister.EAX.getName() + ", " + rhs + nl;
				
				// reg = reg - eax
				code += "sub " + rhs + ", " + AsmRegister.EAX.getName() + nl;
				
				replaceOperatorAndRemoveOperands(index, rhs);
			}else if(rightOperand instanceof PStackElement){
				PStackElement rhs = (PStackElement)rightOperand;
				
				// eax = lhs(fcall)
				code += createFunctionCall(lhs) + nl;
				
				// eax = eax - rhs(stack)
				code += "sub " + AsmRegister.EAX.getName() + ", " + rhs.toStringWithSizeDirective() + nl;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					// rhs(stack) = eax
					code += movToAddress(rhs.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					replaceOperatorAndRemoveOperands(index, rhs);
				}
			}else if(rightOperand instanceof PFunctionCall){
				PFunctionCall rhs = (PFunctionCall)rightOperand;
				
				PRegister reg = new PRegister(regs.getFreeRegister());
				if(reg.register != null){
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// reg = eax
					code += movToRegister(reg.register, AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// reg = reg - eax
					code += "sub " + reg + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, reg);
				}else{
					PStackElement stack = allocateStackSpace();
					
					// eax = lhs(fcall)
					code += createFunctionCall(lhs) + nl;
					
					// stack = eax
					code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
					
					// eax = rhs(fcall)
					code += createFunctionCall(rhs) + nl;
					
					// stack = stack - eax
					code += "sub " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
					
					replaceOperatorAndRemoveOperands(index, stack);
				}
			}
		}*/
		
		return code;
	}
	
	private String createBitwiseOperationCode(PProgramPart leftOperand, PProgramPart rightOperand, int index, AsmSharedRegisters regs){
		String code = "";
		POperatorType opType = ((POperator)exp.get(index)).type;
		
		if(commentInAssembly)
			code += "; " + leftOperand + " " + opType.toString() + " " + rightOperand + nl;
		
		// Get free register, but if there's no
		// free register, allocate stack space
		// for left-hand side operand
		boolean lhsInRegister = true;
		AsmRegister asmReg = null;
		PRegister reg = null;
		PStackElement stack = null;
		
		if(!(leftOperand instanceof PRegister) && !(leftOperand instanceof PStackElement)){
			asmReg = regs.getFreeRegister();
			if(asmReg == null){
				lhsInRegister = false;
				stack = allocateStackSpace();
			}else{
				reg = new PRegister(asmReg);
			}
		}
		
		// Move left-hand side operand into reg/stack
		if(leftOperand instanceof PIntegerLiteral || leftOperand instanceof PHexLiteral){
			long value = leftOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)leftOperand).value : ((PHexLiteral)leftOperand).value;
			if(lhsInRegister)
				code += movToRegister(asmReg, "" + value) + nl;
			else
				code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + value) + nl;
		}else if(leftOperand instanceof PVarAccessor){
			if(lhsInRegister)
				code += ((PVarAccessor)leftOperand).movToRegister(asmReg, program) + nl;
			else {
				// eax = lhs
				code += ((PVarAccessor)leftOperand).movToRegister(AsmRegister.EAX, program) + nl;
				// stack = eax
				code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX) + nl;
			}
		}else if(leftOperand instanceof PRegister){
			reg = ((PRegister)leftOperand);
			asmReg = reg.register;
			lhsInRegister = true;
		}else if(leftOperand instanceof PStackElement){
			stack = (PStackElement)leftOperand;
			lhsInRegister = false;
		}
		
		// Find out instruction for bitwise operation
		String instruction = "";
		switch(opType){
		case BITWISE_AND: instruction = "and"; break;
		case BITWISE_OR: instruction = "or"; break;
		case BITWISE_XOR: instruction = "xor"; break;
		default:
		}
		
		// Find out right-hand side operand type,
		// and do the bitwise operation
		if(rightOperand instanceof PIntegerLiteral || rightOperand instanceof PHexLiteral){
			long value = rightOperand instanceof PIntegerLiteral ? ((PIntegerLiteral)rightOperand).value : ((PHexLiteral)rightOperand).value;

			if(lhsInRegister){
				// Bitwise operation
				code += instruction + " " + asmReg.getName() + ", " + value + nl;
			}else{
				// Bitwise operation
				code += instruction + " " + stack.toStringWithSizeDirective() + ", " + value + nl;
			}
		}else if(rightOperand instanceof PVarAccessor){
			// eax = rhs
			code += ((PVarAccessor)rightOperand).movToRegister(AsmRegister.EAX, program) + nl;

			if(lhsInRegister){
				// Bitwise operation
				code += instruction + " " + asmReg.getName() + ", " + AsmRegister.EAX.getName() + nl;
			}else{
				// Bitwise operation
				code += instruction + " " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
			}
		}else if(rightOperand instanceof PRegister){
			PRegister rhs = (PRegister)rightOperand;
			
			if(lhsInRegister){
				// Bitwise operation
				code += instruction + " " + asmReg.getName() + ", " + rhs.toString() + nl;
				
				// Free up one register
				regs.freeUpRegister(((PRegister)rightOperand).register);
			}else{
				// Bitwise operation
				code += instruction + " " + stack.toStringWithSizeDirective() + ", " + rhs.toString() + nl;
			
				// rhs = stack element
				code += movToRegister(rhs.register, stack.toStringWithSizeDirective()) + nl;
			}
		}else if(rightOperand instanceof PStackElement){
			if(lhsInRegister)
				// Bitwise operation
				code += instruction + " " + asmReg.getName() + ", " + ((PStackElement)rightOperand).toStringWithSizeDirective() + nl;
			else {
				// eax = rhs
				code += movToRegister(AsmRegister.EAX, ((PStackElement)rightOperand).toStringWithSizeDirective());
				
				// Bitwise operation
				code += instruction + " " + stack.toStringWithSizeDirective() + ", " + AsmRegister.EAX.getName() + nl;
			}
		}
		
		replaceOperatorAndRemoveOperands(index, lhsInRegister ? reg : stack);
		
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
			code += movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString()) + nl;
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
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + ((PIntegerLiteral)resultant).value);
		}else if(resultant instanceof PHexLiteral){
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + ((PHexLiteral)resultant).value);
		}else if(resultant instanceof PCharLiteral){
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), "" + (int)((PCharLiteral)resultant).value);
		}else if(resultant instanceof PRegister){
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), ((PRegister)resultant).register);
		}else if(resultant instanceof PStackElement){
			code += movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString()) + nl;
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
		}else if(resultant instanceof PVarAccessor){
			// eax = resultant
			code += movToRegister(AsmRegister.EAX, (PVarAccessor)resultant) + nl;
			// stack = eax
			code += movToAddress(stack.toString(), AsmSizes.getSizeDirective(), AsmRegister.EAX);
		}
		
		return code;
	}
	
	public String assignTo(AsmRegister reg){
		String code = "";
		
		// Null and Boolean literals are converted into
		// integer literals, thus we don't create another
		// code for them
		
		if(resultant instanceof PIntegerLiteral){
			code += movToRegister(reg, "" + ((PIntegerLiteral)resultant).value);
		}else if(resultant instanceof PHexLiteral){
			code += movToRegister(reg, "" + ((PHexLiteral)resultant).value);
		}else if(resultant instanceof PCharLiteral){
			code += movToRegister(reg, "" + (int)((PCharLiteral)resultant).value);
		}else if(resultant instanceof PRegister){
			code += movToRegister(reg, "" + (PRegister)resultant);
		}else if(resultant instanceof PStackElement){
			code += movToRegister(AsmRegister.EAX, ((PStackElement)resultant).toString());
			if(reg != AsmRegister.EAX){
				code += nl + movToRegister(reg, AsmRegister.EAX);
			}
		}else if(resultant instanceof PVarAccessor){
			// reg = resultant
			code += movToRegister(reg, (PVarAccessor)resultant);
		}
		
		return code;
	}
	
	private String createFunctionCall(PFunctionCall call){
		AsmFunctionCall fCall = new AsmFunctionCall(call, program);
		// eax = fcall
		fCall.createFunctionCallCode(stackBase+stackOffset, true);
		
		return fCall.asmCode;
	}
	
	private void replaceOperatorAndRemoveOperands(int index, PProgramPart newOperand){
		// Remove right operand from exp
		exp.remove(index+1);
		// Replace operand with resultant
		exp.set(index, newOperand);
		// Remove left operand from exp
		exp.remove(index-1);
	}
	
	private int containsOperator(POperatorType opType){
		if(exp.size() > 1){
			for(int i=0; i<exp.size(); i++){
				if(exp.get(i) instanceof POperator){
					POperator operator = (POperator)exp.get(i);
					if(operator.type == opType){
						return i;
					}
				}
			}
		}
		
		return -1;
	}
	
	private int nextOperator(){
		if(exp.size() > 1){
			for(int i=0; i<exp.size(); i++){
				if(exp.get(i) instanceof POperator)
					return i;
			}
		}
		
		return -1;
	}
	
	public PStackElement allocateStackSpace(){
		PStackElement el = new PStackElement(stackBase + stackOffset);
		stackOffset += 4;
		return el;
	}
	
	public void freeUpRegisters(AsmSharedRegisters regs){
		if(resultant instanceof PRegister){
			regs.freeUpRegister(((PRegister)resultant).register);
		}
	}
	
	public String toString() {
		String str = "";
		
		for(PProgramPart p : exp){
			if(p instanceof PExpression){
				str += "(" + p + ") ";
			}else{
				str += p + " ";
			}
		}
		
		return str;
	}
}

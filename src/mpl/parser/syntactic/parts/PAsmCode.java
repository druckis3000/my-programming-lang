package mpl.parser.syntactic.parts;

import mpl.compiler.asm.AsmRegister;
import mpl.utils.io.Console;

public class PAsmCode extends PProgramPart {

	public String code = "";
	public PExpression operand = null;
	
	// Used for determining which register is
	// being used in this assembly instruction
	public PProgramPart lhs = null;
	public PProgramPart rhs = null;
	
	public String instruction = "";
	
	public PAsmCode(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public PAsmCode(String code, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.code = code;
	}
	
	public void verify(){
		// Check if there is expression operand
		if(operand != null){
			// Make sure expression contains only one operand
			if(operand.expression.size() != 1){
				Console.throwError(Console.ERROR_NON_CONST_ASM_EXP, operand.sourceFileName, operand.lineInSourceCode, operand.columnInSourceCode);
			}
			
			// Verify expression operand
			operand.verify();
		}
		
		// Find out lhs and rhs
		instruction = code.split(" ")[0].trim();
		String firstOp = "";
		String secondOp = "";
		
		if(code.contains(",")){
			// Instruction with two operands
			
			// firstOp = code without instruction
			firstOp = code.substring(instruction.length(), code.length());
			// secondOp = split operands at ',', and take right part
			secondOp = firstOp.split(",")[1].trim();
			// firstOp = split operands at ',', and take left part
			firstOp = firstOp.split(",")[0].trim();
			
			// Find out lhs operand
			if(AsmRegister.isRegister(firstOp)){
				// If first operand is a register, then set lhs to PRegister
				lhs = PRegister.fromString(firstOp);
			}else{
				// First operand is not a register, let's find out what it is
				if(firstOp.startsWith("$")){
					// That's an expression
					lhs = operand.expression.get(0);
				}
			}
			
			// Find out rhs operand
			if(AsmRegister.isRegister(secondOp)){
				// If second operand is a register, then set rhs to PRegister
				rhs = PRegister.fromString(secondOp);
			}else{
				// Second operand is not a register, let's find out what it is
				if(secondOp.startsWith("$")){
					// That's an expression
					rhs = operand.expression.get(0);
				}
			}
		}else{
			// Instruction with only one operand
			
			// firstOp = first operand
			firstOp = code.substring(instruction.length(), code.length()).trim();
			
			if(AsmRegister.isRegister(firstOp)){
				// If first operand is a register, then set lhs to PRegister
				lhs = PRegister.fromString(firstOp);
			}else{
				// First operand is not a register, let's find out what it is
				if(firstOp.startsWith("$")){
					// That's an expression
					lhs = operand.expression.get(0);
				}
			}
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return padding + this.toString();
	}

	@Override
	public String toString(){
		return code;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PAsmCode asmCode = new PAsmCode(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			asmCode.code = code == null ? null : new String(code);
			asmCode.operand = operand == null ? null : (PExpression)operand.clone(true);
			asmCode.lhs = lhs == null ? null : lhs.clone(true);
			asmCode.rhs = rhs == null ? null : rhs.clone(true);
			asmCode.instruction = instruction == null ? null : new String(instruction);
		}else{
			asmCode.code = code;
			asmCode.operand = operand;
			asmCode.lhs = lhs;
			asmCode.rhs = rhs;
			asmCode.instruction = instruction;
		}
		
		return asmCode;
	}
}

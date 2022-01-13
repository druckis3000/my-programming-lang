package mpl.compiler.asm;

import mpl.syntactic.parts.PAssignmentStatement;
import mpl.syntactic.parts.PBooleanLiteral;
import mpl.syntactic.parts.PCharLiteral;
import mpl.syntactic.parts.PFunctionCall;
import mpl.syntactic.parts.PHexLiteral;
import mpl.syntactic.parts.PIntegerLiteral;
import mpl.syntactic.parts.PLocalVariable;
import mpl.syntactic.parts.PNewOperand;
import mpl.syntactic.parts.PNullLiteral;
import mpl.syntactic.parts.PStringLiteral;
import mpl.syntactic.parts.PStructType;
import mpl.syntactic.parts.PVarAccessor;

public class AsmAssignment {
	public static final String nl = System.lineSeparator();
	
	/* In this method we assume, that assignment
	 * statement is not an expression assignment */
	public static String createAssignmentCode(PAssignmentStatement assignment, AsmProgram program){
		String asmCode = "";
		
		PVarAccessor target = assignment.variable;
		
		// Find out type of the operand
		if(assignment.operand instanceof PIntegerLiteral || assignment.operand instanceof PHexLiteral){
			if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			long value = assignment.operand instanceof PIntegerLiteral ? ((PIntegerLiteral)assignment.operand).value :
							((PHexLiteral)assignment.operand).value;
			asmCode += target.assignIntLiteral(value, program);
		}else if(assignment.operand instanceof PStringLiteral){
			if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			PStringLiteral string = (PStringLiteral)assignment.operand;
			asmCode += target.assignStringLiteral(string, program);
			
			// Add string literal to the rodata
			program.roData.addData(string.roDataName, "db " + string.getAssemblyValue());
		}else if(assignment.operand instanceof PCharLiteral){
			if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			PCharLiteral character = (PCharLiteral)assignment.operand;
			asmCode += target.assignCharLiteral(character, program);
		}else if(assignment.operand instanceof PFunctionCall){
			PFunctionCall fCall = (PFunctionCall)assignment.operand;
			
			// Check if that's a struct to struct assignment
			if(target.getDataType().isStruct() && fCall.getDataType().isStruct() &&
					// Do the same for *struct = struct assignment, but not struct(pointer) = &struct;
					!((target.getDataTypeUnmodified().isPointer() && !target.isPointerAccess()) || fCall.getDataType().isReference())){
				if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
					asmCode += "; " + assignment + nl;
				
				asmCode += createStructFunctionToStructAssignment(fCall, program);
			}else{
				// Create function call code
				AsmFunctionCall fCallAsm = new AsmFunctionCall(fCall, program);
				fCallAsm.createFunctionCallCode(0, false);
				asmCode += fCallAsm.asmCode + nl;
				
				if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
					asmCode += "; " + assignment + nl;

				// Return value is in eax register, so move
				// eax register into the target variable
				asmCode += target.assignRegister(AsmRegister.EAX, program);
			}
		}else if(assignment.operand instanceof PVarAccessor){
			PVarAccessor var = (PVarAccessor)assignment.operand;
			
			// Check if that's a struct to struct assignment
			if(target.getDataType().isStruct() && var.getDataType().isStruct() &&
					// Do the same for *struct = struct assignment, but not struct(pointer) = &struct;
					!((target.getDataTypeUnmodified().isPointer() && !target.isPointerAccess()) || var.getDataTypeUnmodified().isReference())){
				asmCode += createStructToStructAssignment(target, var, program);
			}else{
				// Get free register
				AsmRegister reg = program.registers.getFreeRegister();
				if(reg == null){
					System.err.println("Huston we have a problem! AsmAssignment.createAssignmentCode, PVarAccessor case");
					System.exit(-1);
				}
				
				if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
					asmCode += "; " + assignment + nl;
				
				// Assign var to the register
				asmCode += var.movToRegister(reg, program) + nl;
				
				// Assign register to the var
				asmCode += target.assignRegister(reg, program);
				
				// Free up register
				program.registers.freeUpRegister(reg);
			}
		}else if(assignment.operand instanceof PNewOperand){
			// New operand is skipped, since it's converted
			// into the malloc function call in semantic analysis
		}else if(assignment.operand instanceof PNullLiteral){
			if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			// Null is just a zero
			asmCode += target.assignIntLiteral(0, program);
		}else if(assignment.operand instanceof PBooleanLiteral){
			if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE)
				asmCode += "; " + assignment + nl;
			
			PBooleanLiteral bool = (PBooleanLiteral)assignment.operand;
			asmCode += target.assignIntLiteral(bool.value ? 1 : 0, program);
		}
		
		return asmCode + nl;
	}
	
	private static String createStructFunctionToStructAssignment(PFunctionCall fCall, AsmProgram program) {
		String code = "";
		
		// Create function call code
		AsmFunctionCall fCallAsm = new AsmFunctionCall(fCall, program);
		fCallAsm.createFunctionCallCode(0, false);
		code += fCallAsm.asmCode + nl;
		
		// TODO: Wtf function returned value is not assigned?
		
		return code;
	}

	private static String createStructToStructAssignment(PVarAccessor target, PVarAccessor source, AsmProgram program){
		String asmCode = "";
		
		// Find struct type
		PStructType struct = target.getStructType();
		
		// Iterate over all struct members, and create assignments
		for(PLocalVariable member : struct.members){
			PAssignmentStatement assignment = new PAssignmentStatement(target.sourceFileName, target.lineInSourceCode, target.columnInSourceCode, null);
			assignment.expressionAssignment = false;

			PVarAccessor parentVar = null;
			
			// Create var accessor to the target member
			PVarAccessor targetMember = (PVarAccessor)target.clone(true);
			parentVar = targetMember.getGrandChild();
			targetMember.getGrandChild().child = new PVarAccessor(targetMember.sourceFileName, targetMember.lineInSourceCode, targetMember.columnInSourceCode, assignment);
			targetMember.getGrandChild().parent = parentVar;
			targetMember.getGrandChild().target = member;
			
			// If target variable is pointer, and we deference it
			// while assigning, then remove pointer access from var accessor
			if(targetMember.isPointerAccess())
				targetMember.accessType = targetMember.accessType & (~PVarAccessor.ACCESS_TYPE_POINTER);
			
			// Set assignment target variable to the targetMember
			assignment.variable = targetMember;
			
			// Create var accessor to the source member
			PVarAccessor sourceMember = (PVarAccessor)source.clone(true);
			parentVar = sourceMember.getGrandChild();
			sourceMember.getGrandChild().child = new PVarAccessor(sourceMember.sourceFileName, sourceMember.lineInSourceCode, sourceMember.columnInSourceCode, assignment);
			sourceMember.getGrandChild().parent = parentVar;
			sourceMember.getGrandChild().target = member;
			
			// If source variable is pointer, and we dereference it
			// while assigning, then remove pointer access from var accessor
			if(sourceMember.isPointerAccess())
				sourceMember.accessType = sourceMember.accessType & (~PVarAccessor.ACCESS_TYPE_POINTER);
			
			// Set assignment operand to the sourceMember
			assignment.operand = sourceMember;
			
			// Verify assignment
			assignment.verify();
			
			// Create code for that assignment
			asmCode += createAssignmentCode(assignment, program) + nl;
		}
		
		return asmCode + nl;
	}
}

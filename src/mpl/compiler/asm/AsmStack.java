package mpl.compiler.asm;

import static mpl.compiler.asm.AsmCommon.movToRegister;

import java.util.ArrayList;
import java.util.stream.Collectors;

import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.PStackElement;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.PVarAccessor;

public class AsmStack {
	private static final String nl = System.lineSeparator();
	
	private ArrayList<PDataType> stack = new ArrayList<PDataType>();
	private AsmProgram program;
	
	public AsmStack(AsmProgram program) {
		this.program = program;
	}
	
	/** Push variable on to the stack with type casting.
	 * 
	 * Type casting is used if you want to push for example
	 * char variable onto stack, as an argument, but function
	 * takes int data type parameter, so here you should cast
	 * that char into int data type. */
	/*public String push(PVarAccessor variable, PDataType typeCast){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			if(variable.isReferenceAccess()){
				asmCode += "; Push the address of variable '" + variable + "' onto the stack" + nl;
			}else{
				if(variable.getFinalDataTypeUnmodified().isPointer()){
					if(variable.isPointerAccess()){
						asmCode += "; Push the contents of memory address, contained in variable '" + variable + "' onto the stack" + nl;
					}else{
						if(variable.isArrayAccess()){
							asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
						}else{
							asmCode += "; Push the address contained in variable '" + variable + "' onto the stack" + nl;
						}
					}
				}else if(variable.getFinalDataTypeUnmodified().isArray()){
					if(variable.isArrayAccess()){
						asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
					}else if(variable.isPlainAccess()){
						asmCode += "; Push the address of variable '" + variable + "' onto the stack" + nl;
					}
				}else{
					asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
				}
			}
		}
		
		if(variable.getFinalDataTypeUnmodified().plain() == PDataType.STRUCT){
			if(variable.isReferenceAccess() || variable.getGrandChild().isPtrStructAccess()){
				// If it's a reference of a struct, then get it's
				// address and push it onto the stack
				
				// Get free register
				AsmRegister reg = program.registers.getFreeRegister();
				if(reg == null){
					System.out.println("Huston we have a problem!");
					System.out.println("AsmStack.push(PVarAccessor variable, PDataType typeCast), line: " + Thread.currentThread().getStackTrace()[1].getLineNumber());
				}
				
				// reg = struct address
				asmCode += variable.movToRegister(reg, program) + nl;
				// push reg onto stack
				asmCode += "mov " + typeCast.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() +"], " + reg.getName() + nl;
				
				// Free up register
				program.registers.freeUpRegister(reg);
				
				// Increase stack offset
				stack.add(PDataType.PTR_STRUCT);
			}else{
				// If it's a plain struct access, then push
				// all values of it's members onto the stack=
				
				for(PLocalVariable member : variable.getGrandChild().target.struct.members){
					// Create PVarAccessor for member access
					PVarAccessor memberAccess = new PVarAccessor(variable.sourceFileName, variable.lineInSourceCode, variable.columnInSourceCode, variable);
					memberAccess.target = member;
					
					// Add member access to parent accessor
					PVarAccessor structMemberAccess = variable.clone();
					memberAccess.parent = structMemberAccess.getGrandChild();
					structMemberAccess.getGrandChild().child = memberAccess;
					
					return push(structMemberAccess, typeCast);
				}
			}
		}else{
			// Check if member is of array type
			if(variable.containsMemberAccess() && variable.getFinalDataTypeUnmodified().isArray() && !variable.getGrandChild().isArrayAccess()){
				// Push array elements onto the stack
				for(int i=0; i<variable.getGrandChild().target.arraySize; i++){
					// Create array index expression
					PExpression arrayIndex = new PExpression(variable.sourceFileName, variable.lineInSourceCode, variable.columnInSourceCode, variable);
					arrayIndex.expression.add(new PIntegerLiteral(i, variable.sourceFileName, variable.lineInSourceCode, variable.columnInSourceCode, variable));
					arrayIndex.verify();
					
					// Create PVarAccessor to access array element
					PVarAccessor arrayElement = variable.clone();
					arrayElement.getGrandChild().accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
					arrayElement.getGrandChild().arrayIndex = arrayIndex;
					
					// Push array element onto stack
					asmCode += push(arrayElement, typeCast) + nl;
				}
			}else{
				// Plain variable, push it onto the stack
				asmCode += variable.movToRegister(AsmRegister.EDX, program) + nl;
				asmCode += "mov " + typeCast.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() +"], " + AsmRegister.EDX.getName() + nl;
			
				// Increase stack offset
				stack.add(typeCast);
				
				// If that's a slice, and not a reference of it,
				// then push slice length onto the stack also
				if(variable.getFinalDataTypeUnmodified().isSlice() && !variable.isReferenceAccess()){
					asmCode += variable.movToRegister(AsmRegister.EDX, PDataType.INT.getSizeInBytes(), program) + nl;
					asmCode += "mov " + PDataType.INT.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + (getStackSize() - PDataType.INT.getSizeInBytes()) +"], " + AsmRegister.EDX.getName() + nl;
				}
			}
		}
					
		return asmCode;
	}*/
	
	/** Push variable on to the stack with type casting.
	 * 
	 * Type casting is used if you want to push for example
	 * char variable onto stack, as an argument, but function
	 * takes int data type parameter, so here you should cast
	 * that char into int data type. */
	public String push(PVarAccessor variable, PDataType typeCast){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			if(variable.isReferenceAccess()){
				asmCode += "; Push the address of variable '" + variable + "' onto the stack" + nl;
			}else{
				if(variable.getGrandChild().getDataTypeUnmodified().isPointer()){
					if(variable.isPointerAccess()){
						asmCode += "; Push the contents of memory address, contained in variable '" + variable + "' onto the stack" + nl;
					}else{
						if(variable.isArrayAccess()){
							asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
						}else{
							asmCode += "; Push the address contained in variable '" + variable + "' onto the stack" + nl;
						}
					}
				}else if(variable.getGrandChild().getDataTypeUnmodified().isArray()){
					if(variable.isArrayAccess()){
						asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
					}else if(variable.isPlainAccess()){
						asmCode += "; Push the address of variable '" + variable + "' onto the stack" + nl;
					}
				}else{
					asmCode += "; Push the contents of variable '" + variable + "' onto the stack" + nl;
				}
			}
		}
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("Huston we have a problem!");
			System.out.println("AsmStack.push(PVarAccessor variable, PDataType typeCast), line: " + Thread.currentThread().getStackTrace()[1].getLineNumber());
		}
		
		// Move variable data into reg
		asmCode += variable.movToRegister(reg, program) + nl;
		
		// Push reg onto stack
		asmCode += "mov " + typeCast.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() +"], " + reg.getName() + nl;
		
		// If that's a slice variable, push it's length aswell
		if(variable.getDataTypeUnmodified().isSlice()){
			// Move slice length into reg
			asmCode += variable.movToRegister(reg, PDataType.PTR_VOID.getSizeInBytes(), program) + nl;
			
			// Push reg onto stack
			int stackOffset = getStackSize() + PDataType.PTR_VOID.getSizeInBytes();
			asmCode += "mov " + typeCast.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + stackOffset +"], " + reg.getName() + nl;
		}
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		// Increase stack offset
		stack.add(typeCast);
					
		return asmCode;
	}
	
	/** Push integer literal on to the stack with type casting.
	 * 
	 * Type casting is used if you want to push for example
	 * char-sized literal onto stack, as an argument, but function
	 * takes int data type parameter, so here you should cast
	 * that char-sized literal into int data type.*/
	public String push(PIntegerLiteral integer, PDataType typeCast){
		String asmCode = "";
		;
			
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Push " + integer.value + " onto the stack" + nl;
		}
			
		// Push integer literal onto the stack
		asmCode += "mov " + typeCast.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() + "], " + integer.value + nl;
			
		// Increase stack offset
		stack.add(typeCast);
		
		return asmCode;
	}
	
	/** Push integer literal on to the stack. */
	public String push(PIntegerLiteral integer){
		return push(integer, integer.dataType);
	}
	
	/** Push string literal onto the stack. */
	public String push(PStringLiteral string){
		String asmCode = "";
		;
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Push " + string.value + " onto the stack" + nl;
		}
		
		// Push string literal onto the stack
		asmCode += "mov " + PDataType.STRING.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() + "], " + string.roDataName + nl;
		
		// Increase stack offset
		stack.add(PDataType.STRING);
		
		return asmCode;
	}
	
	public String push(AsmRegister register){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Push " + register.getName() + " register onto the stack" + nl;
		}
		
		// Push the register onto the stack
		asmCode += "mov " + AsmSizes.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() + "], " + register.getName() + nl;
		
		// Increase stack offset
		stack.add(PDataType.INT);
		
		return asmCode;
	}
	
	public String pushMemory(String memoryAddress, PDataType dataSize){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Push data from memory at address " + memoryAddress + " onto the stack" + nl;
		}
		
		// Mov data from memory at address memoryAddress to eax register
		asmCode += movToRegister(AsmRegister.EAX, memoryAddress, dataSize) + nl;
		
		// Push eax register onto the stack
		asmCode += "mov " + AsmSizes.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() + "], " + AsmRegister.EAX.getName() + nl;
		
		// Increase stack offset
		stack.add(PDataType.INT);
		
		return asmCode;
	}
	
	public String pushMemory(PStackElement stackElement){
		return pushMemory(stackElement.toString(), PDataType.INT);
	}
	
	public String push(AsmRegister register, int stackOffset){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Push " + register.getName() + " register onto the stack" + nl;
		}
		
		// Push the register onto the stack
		asmCode += "mov " + AsmSizes.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + stackOffset + "], " + register.getName() + nl;
		
		return asmCode;
	}
	
	private int getStackSize(){
		int stackSize = 0;
		for(PDataType data : stack)
			stackSize += data.getSizeInBytes();
		return stackSize;
	}
	
	public String pop(AsmRegister register){
		String asmCode = "";
		
		if(AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE){
			asmCode += "; Pop from the stack into " + register.getName() + " register" + nl;
		}
		
		// Get last item push onto the stack
		PDataType lastStackItem = stack.get(stack.size() - 1);
		
		// Pop it into a register
		asmCode += "mov " + register.getName() + ", " + lastStackItem.getSizeDirective() + " [" + AsmRegister.ESP.getName() + " + " + getStackSize() + "]" + nl;
		
		// Remove data from the stack
		stack.remove(stack.size() - 1);
		
		return asmCode;
	}
	
	public void pop(int numberOfItems){
		// Remove items from the stack
		stack = (ArrayList<PDataType>)stack.stream().limit(stack.size() - numberOfItems).collect(Collectors.toList());
	}
	
	public void clear(){
		stack.clear();
	}
}

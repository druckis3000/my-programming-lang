package mpl.syntactic.parts;

import java.util.ArrayList;

import mpl.compiler.asm.AsmCommon;
import mpl.compiler.asm.AsmExpression;
import mpl.compiler.asm.AsmProgram;
import mpl.compiler.asm.AsmRegister;
import mpl.project.Source;
import mpl.syntactic.parts.PVariable.PScope;
import mpl.utils.io.Console;

public class PVarAccessor extends POperand {
	private static final String nl = System.lineSeparator();
	
	public static final int ACCESS_TYPE_PLAIN = 0;
	public static final int ACCESS_TYPE_POINTER = 1;
	public static final int ACCESS_TYPE_REFERENCE = 2;
	public static final int ACCESS_TYPE_ARRAY = 4;
	public static final int ACCESS_TYPE_SLICE = 8;
	
	/* Variable that we are accessing */
	public PVariable target = null;
	/* How do we acces it? */
	public int accessType = ACCESS_TYPE_PLAIN;
	
	/* Package from which we are accessing var */
	public Source pkg;
	
	/* Only used when accessType is ACCESS_TYPE_ARRAY */
	public PExpression arrayIndex = null;
	
	/* Only used when we're slicing array */
	public PSliceExp sliceExp = null;
	
	/* Only used when accessing struct member */
	public PVarAccessor parent = null;
	public PVarAccessor child = null;
	
	private boolean verified = false;
	
	public PVarAccessor(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}

	public void verify(boolean checkIfDefined){
		if(verified) return;
		else verified = true;
		
		if(checkIfDefined){
			// Only parent var is checked here
			if(parent == null){
				// Find parent body, it may be null, if that's
				// a global variables access
				PBody parentBody = findParentBody();
				
				if(pkg != null){
					// If there's package access,
					// find var in another package
					int defLine = pkg.sourceAST.getProgram().isVariableDefined(target);
					if(defLine == -1){
						// Variable is undefined
						Console.throwError(Console.ERROR_UNDEFINED_VAR, sourceFileName, lineInSourceCode, columnInSourceCode, target.name);
					}
				}else{
					// There's no other package access,
					// find var in the same package
					int defLine = -1;
					if(parentBody != null){
						// Local var
						defLine = parentBody.isVariableDefined(target, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_PARENT);
					}else{
						// Global var
						defLine = getProgram().isVariableDefined(target);
					}
					
					if(defLine == -1){
						// Variable is undefined
						// If there's child member access, let's
						// see if maybe that's a package access
						if(child != null){
							Source pkg = getProgram().findImportedSource(target.name);
							if(pkg == null){
								// Neither package not defined var
								Console.throwError(Console.ERROR_UNDEFINED_VAR, sourceFileName, lineInSourceCode, columnInSourceCode, target.name);
							}else{
								// That's a package access
								this.pkg = pkg;
								
								// Remove first member access
								this.target = child.target;
								this.accessType = child.accessType;
								this.typeCast = child.typeCast;
								this.arrayIndex = child.arrayIndex;
								
								this.parent = null;
								this.child = child.child;
								if(this.child != null)
									this.child.parent = this;
								
								this.verified = false;
								
								verify(checkIfDefined);
								
								// Stop further execution
								return;
							}
						}else{
							Console.throwError(Console.ERROR_UNDEFINED_VAR, sourceFileName, lineInSourceCode, columnInSourceCode, target.name);
						}
					}
				}
			}
		}
		
		// Find the real variable object
		if(parent == null){
			if(pkg == null){
				// Find parent body, it may be null, if that's
				// a global variables access
				PBody parentBody = findParentBody();
				
				// Variable from same package access
				if(parentBody == null){
					// Global variable
					target = getProgram().findVariable(target);
					((PGlobalVariable)target).verify();
				}else{
					// Local variable
					target = parentBody.findVariable(target, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_PARENT);
				}
			}else{
				// Variable from another package
				target = pkg.sourceAST.getProgram().findVariable(target);
				
				// If it's not public accessible, throw an error
				if(!((PGlobalVariable)target).isPublic){
					Console.throwError(Console.ERROR_UNDEFINED_VAR, sourceFileName, lineInSourceCode, columnInSourceCode, target.name);
				}
				
				// Verify global var
				((PGlobalVariable)target).verify();
				
				// Extern the variable
				getProgram().externGlobalVars.addVar(((PGlobalVariable)target).nameInAssembly);
			}
		}else{
			// That's a member access
			// target is already set in previous verify call
		}
		
		// Verify variable's struct
		target.verifyStruct();
		
		// Check if there's a member access
		if(child != null){
			// Let's find out if this var is of struct type
			if(target.struct == null){
				Console.throwError(Console.ERROR_NON_STRUCT_MEMBER_REQ, target.name, lineInSourceCode);
			}
			
			// Find member
			PLocalVariable member = target.struct.findMember(child.target.name);
			
			if(member == null){
				// Struct does not have that member!
				Console.throwError(Console.ERROR_NON_DECLARED_MEMBER_REQ, sourceFileName, lineInSourceCode, columnInSourceCode, target.struct.name, child.target.name);
			}
			
			// Verify struct of the member
			member.verifyStruct();
			
			// Set child target to it's real object
			child.target = member;
			
			// Verify child accessor
			child.verify(false);
		}
		
		if(isArrayAccess()){
			// If it's an array access, then make sure
			// that member is of array, string, or pointer type
			PDataType dataTypeUnmodified = getDataTypeUnmodified();
			if(!dataTypeUnmodified.isArray() && !dataTypeUnmodified.isSlice() && dataTypeUnmodified != PDataType.STRING && !dataTypeUnmodified.isPointer()){
				Console.throwError(Console.ERROR_NOT_ARRAY, sourceFileName, lineInSourceCode, columnInSourceCode, target.name);
			}
			
			// And verify array index expression
			arrayIndex.verify();
		}else{
			// If it's not an array access, make sure
			// that we're not accessing member of an array
			if(target.dataType.isArray()){
				if(child != null){
					Console.throwError(Console.ERROR_ARRAY_MEMBER_REQ, sourceFileName, lineInSourceCode, columnInSourceCode, getGrandParent().toString());
				}
			}
			
			// Verify slicing expression [x:y]
			if(isSliceAccess()){
				sliceExp.verify();
			}
		}
		
		if(parent == null){
			// If it's a parent variable of variable access
			// check if it's a pointer access
			if(isPointerAccess()){
				// If it's a pointer access, check if
				// final member is of pointer data type
				PDataType dt = getGrandChild().getDataTypeUnmodified();
				if(!dt.isPointer()){
					// Not a pointer, throw an error
					Console.throwError(Console.ERROR_NOT_A_POINTER, sourceFileName, lineInSourceCode, columnInSourceCode, this.toStringPlain(), lineInSourceCode);
				}
			}
		}
		
		// If it's array access without index,
		// then set access as reference
		if(parent == null){
			// TODO: Dunno yet if getGrandChild() is neccessary here...
			if(getGrandChild().getDataTypeUnmodified().isArray() && !getGrandChild().getDataTypeUnmodified().isSlice()){
				if(!getGrandChild().isArrayAccess() && !getGrandChild().isSliceAccess()){
					if(!isReferenceAccess()){
						accessType = ACCESS_TYPE_REFERENCE;
					}
				}
			}
		}
		
		if(child == null){
			// Last element, verify type cast
			if(typeCast != null){
				typeCast.verify(getGrandChild().target.dataType);
			}
		}
	}
	
	/* Assembly part - Start */
	
	/** Unlike {@link #getVarAddress()}, this function doesn't
	 * return address of a variable. Instead it mov/lea it's
	 * address into register {@code reg}. Then you can use
	 * register to manipulate this variable.
	 * 
	 * @param lea If set to true, then this method
	 * will use lea instruction to load variable address 
	 * into the {@code reg}. If set to false, then
	 * this method will use mov instruction to move value
	 * of this variable into the {@code reg}.
	 * @param reg Register where address or value of this 
	 * variable will be stored.
	 * 
	 * @return Assembly code for the operation explained above. */
	public String movToRegister(AsmRegister reg, boolean lea, int offset, AsmProgram program){
		String code = "";
		
		// Used for iterating over childs
		PVarAccessor var = this;
		
		// Iterate over all childs
		while(var != null){
			// If var is parent, use different code
			if(var == this){
				// reg = parent var address
				if(var.isArrayAccess()){
					// Calculate array index
					AsmExpression indexExp = new AsmExpression(var.arrayIndex, program);
					indexExp.createExpCode();
					
					if(indexExp.asmCode.length() > 0)
						code += indexExp.asmCode + nl;
					
					long arrayIndex = -1;
					if(indexExp.resultant instanceof PIntegerLiteral){
						// If array index is integer literal, then get value of int literal
						arrayIndex = ((PIntegerLiteral)indexExp.resultant).value;
					}else{
						// Otherwise, assing array index to the reg
						code += indexExp.assignTo(reg) + nl;
					}
					
					// Free up registers, that were used in array index expression
					indexExp.freeUpRegisters(program.registers);
					
					// Get var size
					long varSize = 0;
					if(var.target.dataType.isPointer()){
						// Var is pointer, get it's data type size
						varSize = var.target.dataType.plain().getSizeInBytes();
					}else{
						if(var.target.dataType == PDataType.STRING){
							// Var is string, it's size is char
							varSize = PDataType.CHAR.getSizeInBytes();
						}else{
							// Var is array, get it's size divided by the size of an array
							varSize = var.target.getSizeInBytes() / var.target.arraySize;
							//varSize = var.target.dataType.plain().getSizeInBytes();
						}
					}
					
					// If final data type is char, it means it's size is 1,
					// thus we skip the multiplication
					if(arrayIndex == -1 && getDataType() != PDataType.CHAR){
						// Multiply index by varSize
						code += "imul " + reg.getName() + ", " + varSize + nl;
					}else{
						// Multiply index by varSize
						arrayIndex *= varSize;
					}
					
					String arrayIndexCode = arrayIndex == -1 ? reg.getName() : "" + arrayIndex;
					
					// reg = [parent var + index]
					if(var.target.scope == PScope.LOCAL){
						int ebpOffset = Math.abs(((PLocalVariable)var.target).ebpOffset);
						
						if(var.child == null){
							// There's no more member access, this is final
							if(lea){
								// Move array element address into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp - " + ebpOffset + "]") + nl;
									// reg = [eax + index + offset]
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp - " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else{
									// reg = [var + index - offset]
									ebpOffset -= offset;
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + arrayIndexCode + " - " + ebpOffset + "]") + nl;
								}
							}else{
								// Move array element value into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp - " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp - " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else{
									// reg = var + index - offset
									ebpOffset -= offset;
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + arrayIndexCode + " - " + ebpOffset + "]", getDataType()) + nl;
								}
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access,
							// lea array element address into reg
							code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + arrayIndexCode + " - " + ebpOffset + "]") + nl;
						}
					}else if(var.target.scope == PScope.FUNCTION_PARAM){
						int ebpOffset = ((PFunctionParameter)var.target).ebpOffset;
						
						if(var.child == null){
							// There's no more member access, this is final
							if(lea){
								// Move array element address into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp + " + ebpOffset + "]") + nl;
									// reg = [eax + index + offset]
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp + " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else{
									// reg = [var + index + offset]
									ebpOffset += offset;
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + arrayIndexCode + " + " + ebpOffset + "]") + nl;
								}
							}else{
								// Move array element value into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp + " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[ebp + " + ebpOffset + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else{
									// reg = var + index + offset
									ebpOffset += offset;
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + arrayIndexCode + " + " + ebpOffset + "]", getDataType()) + nl;
								}
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access,
							// load array element address into reg
							
							// All arrays in function parameters are actually
							// pointers to the arrays, so we move pointer to reg firstly
							
							// eax = [var]
							code += AsmCommon.movToRegister(AsmRegister.EAX, "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]") + nl;
							// reg = array element address
							code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + "]") + nl;
						}
					}else if(var.target.scope == PScope.GLOBAL){
						if(var.child == null){
							// There's no more member access, this is final
							if(lea){
								// Move array element address into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
									// reg = [eax + index + offset]
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
									// reg = [eax + index + offset]
									code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}else{
									// reg = [var + index - offset]
									code += AsmCommon.leaToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + " + " + arrayIndexCode + " + " + offset + "]") + nl;
								}
							}else{
								// Move array element value into reg
								if(var.target.dataType.isPointer() || var.target.dataType.isSlice()){
									// This code is used for pointers to array, and slices
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
									// reg = eax + index + offset
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else if(var.target.dataType == PDataType.STRING){
									// This code is used for strings
									// eax = [var]
									code += AsmCommon.movToRegister(AsmRegister.EAX, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
									// reg = [eax + index + offset]
									code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EAX.getName() + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}else{
									// reg = var + index - offset
									code += AsmCommon.movToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + " + " + arrayIndexCode + " + " + offset + "]", getDataType()) + nl;
								}
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access,
							// lea array element address into reg
							code += AsmCommon.leaToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + " + " + arrayIndexCode + "]") + nl;
						}
					}
				}else{
					// reg = [parent var]
					if(var.target.scope == PScope.LOCAL){
						int ebpOffset = Math.abs(((PLocalVariable)var.target).ebpOffset);
						
						if(var.child == null){
							// There's no more member access, this is final
							ebpOffset -= offset;
							
							if(lea){
								// Move var address into reg
								code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " - " + ebpOffset + "]") + nl;
							}else{
								// Move var value into reg
								code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " - " + ebpOffset + "]", var.target.dataType) + nl;
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access
							if(var.isPtrStructAccess()){
								// And access type is pointer struct access,
								// then move struct address into reg
								code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " - " + ebpOffset + "]") + nl;
							}else{
								// And access type is not pointer struct access,
								// then move var address to the reg
								code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " - " + ebpOffset + "]") + nl;
							}
						}
					}else if(var.target.scope == PScope.FUNCTION_PARAM){
						int ebpOffset = ((PFunctionParameter)var.target).ebpOffset;
						
						if(var.child == null){
							// There's no more member access, this is final
							ebpOffset += offset;
							
							if(lea){
								// Move var address into reg
								code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]") + nl;
							}else{
								// Move var value into reg
								code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]", var.target.dataType) + nl;
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access
							if(var.isPtrStructAccess()){
								// And access type is pointer struct access,
								// then move struct address into reg
								code += AsmCommon.movToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]") + nl;
							}else{
								// And access type is not pointer struct access,
								// then move var address to the reg
								code += AsmCommon.leaToRegister(reg, "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]") + nl;
							}
						}
					}else if(var.target.scope == PScope.GLOBAL){
						if(var.child == null){
							// There's no more member access, this is final
							if(lea){
								// Move var address into reg
								code += AsmCommon.leaToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + " + " + offset + "]") + nl;
							}else{
								// Move var value into reg
								code += AsmCommon.movToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + " + " + offset + "]", var.target.dataType) + nl;
							}
							
							return code.substring(0, code.length() - 1);
						}else{
							// If there's more member access
							if(var.isPtrStructAccess()){
								// And access type is pointer struct access,
								// then move struct address into reg
								code += AsmCommon.movToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
							}else{
								// And access type is not pointer struct access,
								// then move var address to the reg
								code += AsmCommon.leaToRegister(reg, "[" + ((PGlobalVariable)var.target).nameInAssembly + "]") + nl;
							}
						}
					}
				}
			}else{
				// Add member offset to the reg
				if(var.isArrayAccess()){
					// Calculate array index
					AsmExpression indexExp = new AsmExpression(var.arrayIndex, program);
					indexExp.createExpCode();
					
					// Add array index expression code, only if there's something in it
					if(indexExp.asmCode.length() > 0)
						code += indexExp.asmCode + nl;
					
					// Get array index
					long arrayIndex = -1;
					if(indexExp.resultant instanceof PIntegerLiteral){
						arrayIndex = ((PIntegerLiteral)indexExp.resultant).value;
					}else{
						// eax = array index
						code += indexExp.assignTo(AsmRegister.EAX) + nl;
					}
					
					// Free up registers, that were used in array index expression
					indexExp.freeUpRegisters(program.registers);
					
					// Get var size (if it's an array, divide by array size)
					long varSize = 0;
					
					if(var.target.dataType.isPointer()){
						// Var is pointer, get it's data type size
						varSize = var.target.dataType.plain().getSizeInBytes();
					}else{
						if(var.target.dataType == PDataType.STRING){
							// Var is string, it's size is char
							varSize = PDataType.CHAR.getSizeInBytes();
						}else{
							// Var is array, get it's data type size
							varSize = var.target.dataType.plain().getSizeInBytes();
						}
					}
						
					if(arrayIndex == -1){
						// Multiply index by varSize
						code += "imul " + AsmRegister.EAX.getName() + ", " + varSize + nl;
					}else{
						// Multiply index by varSize
						arrayIndex *= varSize;
					}
					
					String arrayIndexRegOrValue = arrayIndex == -1 ? AsmRegister.EAX.getName() : "" + arrayIndex;

					// Find member offset
					int childOffset = var.parent.target.struct.getMemberOffset((PLocalVariable)var.target);
					
					if(var.child == null){
						// There's no more member access, this is final
						if(lea){
							if(var.target.dataType.isPointer()){
								// If final member is pointer, load address contained
								// in the member, and then load array element address into reg
								
								// reg = [var + member]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + "]") + nl;
								
								// reg = [reg + array index + offset]
								code += AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + arrayIndexRegOrValue + " + " + offset + "]") + nl;
							}else if(var.target.dataType == PDataType.STRING){
								// If final member is string, load address contained
								// in the member, and then load array element address into reg
								
								// reg = [var + member]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + "]") + nl;
								
								// reg = [reg + array index + offset]
								code += AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + arrayIndexRegOrValue + " + " + offset + "]") + nl;
							}else{
								// reg = [var + member + array index]
								code += AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + childOffset + " + " + arrayIndexRegOrValue + " + " + offset + "]") + nl;
							}
						}else{
							if(var.target.dataType.isPointer()){
								// If final member is pointer, load address contained
								// in the member, and then move array element value into reg
								
								// reg = [var + member]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + "]") + nl;
								
								// reg = [reg + array index + offset]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + arrayIndexRegOrValue + " + " + offset + "]", getDataType()) + nl;
							}else if(var.target.dataType == PDataType.STRING){
								// If final member is string, load address contained
								// in the member, and then move array element value into reg
								
								// reg = [var + member]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + "]") + nl;
								
								// reg = [reg + array index + offset]
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + arrayIndexRegOrValue + " + " + offset + "]", getDataType()) + nl;
							}else{
								// reg = member array element value (reg + member offset + array index + offset)
								code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + " + " + arrayIndexRegOrValue + " + " + offset + "]", getDataType()) + nl;
							}
						}
						
						return code.substring(0, code.length() - 1);
					}else{
						// If there is more member access, then this member
						// can only be struct, so here we don't check if target
						// is pointer, instead we check if it's a struct pointer
						if(var.isPtrStructAccess()){
							// If member is pointer to a struct, then
							// move struct address to the reg
							
							// reg = [var + member offset]
							code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + childOffset + "]") + nl;
							// reg = [reg + array index]
							code += AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + arrayIndexRegOrValue + "]") + nl;
						}else{
							// Member is not a pointer to the struct, so it's
							// a plain struct, load it's address into reg
							
							// reg = [var + member offset + array index]
							code += AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + childOffset + " + " + arrayIndexRegOrValue + "]") + nl;
						}
					}
				}else{
					// This is a member access without array index
					
					// Find member offset
					int memberOffset = var.parent.target.struct.getMemberOffset((PLocalVariable)var.target);
					
					if(var.child == null){
						// There's no more member access, this is final
						if(lea){
							// Move member address into reg, [reg + member offset + offset]
							code += (memberOffset == 0 ? "; " : "") + AsmCommon.leaToRegister(reg, "[" + reg.getName()  + " + " + memberOffset + " + " + offset + "]") + nl;
						}else{
							// Move member value into reg, [reg + member offset + offset]
							code += AsmCommon.movToRegister(reg, "[" + reg.getName()  + " + " + memberOffset + " + " + offset + "]", var.target.dataType) + nl;
						}
						
						return code.substring(0, code.length() - 1);
					}else{
						// If there's more member access
						if(var.isPtrStructAccess()){
							// And access type is pointer struct access,
							// then move member struct address into reg
							
							// reg = [reg + member offset]
							code += AsmCommon.movToRegister(reg, "[" + reg.getName() + " + " + memberOffset + "]") + nl;
						}else{
							// And access type is not pointer struct access,
							// add member offset to the reg
							
							// reg = [reg + member offset]
							code += (memberOffset == 0 ? "; " : "") + AsmCommon.leaToRegister(reg, "[" + reg.getName() + " + " + memberOffset + "]") + nl;
						}
					}
				}
			}
			
			var = var.child;
		}
		
		return code.substring(0, code.length() - 1);
	}
	
	/* Moves value of this variable to the register */
	public String movToRegister(AsmRegister reg, int offset, AsmProgram program){
		String code = "";
		
		if(isReferenceAccess()){
			// reg = address of a variable
			code += movToRegister(reg, true, offset, program);
		}else{
			// reg = value of a variable
			code += movToRegister(reg, false, offset, program);
			
			if(isPointerAccess()){
				// If it's a pointer, get value from address contained in reg
				// reg = value at address contained in reg
				code += nl + AsmCommon.movToRegister(reg, "[" + reg.getName() + "]", getDataType());
			}
		}
		
		return code;
	}
	
	public String movToRegister(AsmRegister reg, AsmProgram program){
		return movToRegister(reg, 0, program);
	}
	
	public String assignIntLiteral(long value, AsmProgram program){
		String code = "";
		
		// If target variable is of boolean type
		// value can be either 1 or 0
		if(getDataType().plain() == PDataType.BOOL && !getDataType().isPointer()){
			if(value >= 1){
				value = 1;
			}else{
				value = 0;
			}
		}
		
		// If there is no array access, then skip
		// the code below for array index computation,
		// and use much simpler code
		if(!containsArrayAccess() && !containsPointerAccess()){
			return AsmCommon.movToAddress(getVarAddress(), getDataType().getSizeDirective(), "" + value);
		}
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("PVarAccessor.assignIntLiteral: Huston we have a problem!");
			System.out.println(this + " = " + value);
		}
		
		if(isPointerAccess()){
			// reg = address contained in ptr var
			code += movToRegister(reg, false, 0, program) + nl;
		}else{
			// reg = address of a variable
			code += movToRegister(reg, true, 0, program) + nl;
		}
		
		// [reg] = operand
		code += AsmCommon.movToAddress("[" + reg.getName() + "]", getDataType().getSizeDirective(), "" + value);
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		return code;
	}
	
	public String assignStringLiteral(PStringLiteral operand, AsmProgram program){
		String code = "";
		
		// If there is no array access, then skip
		// the code below for array index computation,
		// and use much simpler code
		if(!containsArrayAccess() && !containsPointerAccess()){
			return AsmCommon.movToAddress(getVarAddress(), getDataType().getSizeDirective(), "" + operand.roDataName);
		}
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("PVarAccessor.assignIntLiteral: Huston we have a problem!");
			System.out.println(this + " = " + operand);
		}
		
		if(isPointerAccess()){
			// reg = address contained in ptr var
			code += movToRegister(reg, false, 0, program) + nl;
		}else{
			// reg = address of a variable
			code += movToRegister(reg, true, 0, program) + nl;
		}
		
		// [reg] = operand
		code += AsmCommon.movToAddress("[" + reg.getName() + "]", getDataType().getSizeDirective(), "" + operand.roDataName);
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		return code;
	}

	public String assignCharLiteral(PCharLiteral operand, AsmProgram program) {
		String code = "";
		
		// If there is no array access, then skip
		// the code below for array index computation,
		// and use much simpler code
		if(!containsArrayAccess() && !containsPointerAccess()){
			return AsmCommon.movToAddress(getVarAddress(), getDataType().getSizeDirective(), "" + (int)operand.value);
		}
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("PVarAccessor.assignCharLiteral: Huston we have a problem!");
			System.out.println(this + " = " + operand);
		}
		
		if(isPointerAccess()){
			// reg = address contained in ptr var
			code += movToRegister(reg, false, 0, program) + nl;
		}else{
			// reg = address of a variable
			code += movToRegister(reg, true, 0, program) + nl;
		}
		
		// [reg] = operand
		code += AsmCommon.movToAddress("[" + reg.getName() + "]", getDataType().getSizeDirective(), "" + (int)operand.value);
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		return code;
	}
	
	public String assignRegister(AsmRegister operand, int offset, AsmProgram program){
		String code = "";
		
		// If target variable is of boolean type
		// check if operand is greater than zero,
		// if it is, assign value 1 to the target
		if(getDataType().plain() == PDataType.BOOL && !getDataType().isPointer()){
			// If there is no array access, then skip
			// the code below for array index computation,
			// and use much simpler code
			if(!containsArrayAccess() && !containsPointerAccess()){
				// Cmp operand to 0
				code += "cmp " + operand.getName() + ", 0" + nl;
				// Set target value to 1 if cmp failed
				code += "setg " + getDataType().getSizeDirective() + " " + getVarAddress(offset);
			}else{
				// Get free register
				AsmRegister reg = program.registers.getFreeRegister();
				if(reg == null){
					System.out.println("PVarAccessor.assignRegister: Huston we have a problem!");
					System.out.println(this + " = " + operand);
				}
				
				// Mov target variable address into reg
				if(isPointerAccess()){
					// reg = var
					code += movToRegister(reg, false, offset, program) + nl;
				}else{
					// reg = [var...]
					code += movToRegister(reg, true, offset, program) + nl;
				}
				
				// Cmp operand to 0
				code += "cmp " + operand.getName() + ", 0" + nl;
				// Set target value to 1 if cmp failed
				code += "setg " + getDataType().getSizeDirective() + " [" + reg.getName() + "]";
				
				// Free up register
				program.registers.freeUpRegister(reg);
			}
			
			// Don't execute further
			return code;
		}
		
		// If there is no array access, then skip
		// the code below for array index computation,
		// and use much simpler code
		if(!containsArrayAccess() && !containsPointerAccess()){
			return AsmCommon.movToAddress(getVarAddress(offset), getDataType().getSizeDirective(), operand);
		}
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("PVarAccessor.assignRegister: Huston we have a problem!");
			System.out.println(this + " = " + operand);
		}
		
		if(isPointerAccess()){
			// reg = var
			code += movToRegister(reg, false, offset, program) + nl;
		}else{
			// reg = [var...]
			code += movToRegister(reg, true, offset, program) + nl;
		}
		
		// [reg] = operand
		code += AsmCommon.movToAddress("[" + reg.getName() + "]", getDataType().getSizeDirective(), operand);
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		return code;
	}
	
	public String assignRegister(AsmRegister operand, AsmProgram program){
		return assignRegister(operand, 0, program);
	}
	
	public String assignVar(PVarAccessor operand, AsmProgram program){
		String code = "";
		
		// Get free register
		AsmRegister reg = program.registers.getFreeRegister();
		if(reg == null){
			System.out.println("PVarAccessor.assignVar: Huston we have a problem!");
			System.out.println(this + " = " + operand);
		}
		
		// Find out if target data type is of boolean
		if(getDataType().plain() == PDataType.BOOL && !getDataType().isPointer()){
			// reg = operand
			code += operand.movToRegister(reg, program) + nl;
			
			// Check if reg value equals to zero
			code += "cmp " + reg.getName() + ", 0" + nl;
			
			// Set reg to true if not zero
			code += "setne " + reg.getNameByDataType(PDataType.BOOL);
			
			// var = reg
			code += assignRegister(reg, program) + nl;
		}
		
		// Find out if it's an integer to boolean assignment
		/*if(getDataType() == PDataType.BOOL){
			if(operand.getDataTypeUnmodified().isIntegral()){
				// Integer to boolean assignment
				
				// reg = operand
				code += operand.movToRegister(reg, program) + nl;
				
				// cmp reg with 0
				code += "cmp " + reg.getName() + ", 0" + nl;
				
				// if reg is not '0', set bool to '1'
				code += "setne " + reg.getNameByDataType(PDataType.BOOL) + nl;
				
				// var = reg
				code += assignRegister(reg, program) + nl;
				
				// Free up register
				program.registers.freeUpRegister(reg);
				
				return code;
			}
		}*/
		
		// Find out if it's a struct to struct assignment
		boolean structToStruct = false;
		if(getDataType().isStruct() && !getDataType().isPointer() && !isPointerAccess()){
			if(operand.getDataType().isStruct() && !operand.isPointerAccess() && !operand.isReferenceAccess()){
				structToStruct = true;
			}
		}
		
		if(structToStruct){
			// Struct to struct assignment
			ArrayList<PDataType> primitiveMembers = getGrandChild().target.struct.getPrimitiveMembersListRecursive();
			int offset = 0;
			
			for(PDataType member : primitiveMembers){
				// reg = operand member
				code += operand.movToRegister(reg, offset, program) + nl;
				
				// this var = reg
				code += assignRegister(reg, offset, program) + nl;
				
				// Increase offset by member size
				offset += member.getSizeInBytes();
			}
		}else{
			// reg = operand
			code += operand.movToRegister(reg, program) + nl;
						
			// var = reg
			code += assignRegister(reg, program) + nl;
		}
		
		// Free up register
		program.registers.freeUpRegister(reg);
		
		return code;
	}
	
	/* Assembly part - End */

	public boolean isPlainAccess() {
		return accessType == ACCESS_TYPE_PLAIN;
	}
	
	public boolean isPointerAccess(){
		return (accessType & ACCESS_TYPE_POINTER) == ACCESS_TYPE_POINTER;
	}
	
	public boolean isReferenceAccess(){
		return (accessType & ACCESS_TYPE_REFERENCE) == ACCESS_TYPE_REFERENCE;
	}

	public boolean isArrayAccess(){
		return (accessType & ACCESS_TYPE_ARRAY) == ACCESS_TYPE_ARRAY;
	}
	
	public boolean isSliceAccess(){
		return (accessType & ACCESS_TYPE_SLICE) == ACCESS_TYPE_SLICE;
	}
	
	public boolean isMemberAccess(){
		return child != null;
	}
	
	public boolean isPtrStructAccess(){
		return target.struct != null && target.dataType.isPointer() && (!isArrayAccess());
	}
	
	/** Iterates over all members access 
	 * and checks whther any of them has array access */
	public boolean containsArrayAccess(){
		PVarAccessor var = this;
		while(var != null){
			if(var.isArrayAccess()){
				return true;
			}
			
			var = var.child;
		}
		return false;
	}
	
	public boolean containsPointerAccess(){
		if(isPointerAccess() || isPtrStructAccess()){
			return true;
		}else{
			PVarAccessor var = this.child;
			while(var != null){
				if(var.isPtrStructAccess()){
					return true;
				}
				
				var = var.child;
			}
			return false;
		}
		
	}
	
	public boolean containsMemberAccess(){
		return child != null;
	}
	
	/** Returns unmodified data type of current member */
	/*public PDataType getDataType(){
		return target.dataType;
	}*/
	
	/** Returns modified {@link PDataType} of final member
	 * in this variable access.
	 * 
	 * It will return plain {@link PDataType} unless it's a
	 * reference access, thus it will return referenced
	 * {@link PDataType} of final member data type 
	 *  
	 * @return {@link PDataType}*/
	/*public PDataType getFinalDataType(){
		if(typeCast != null && verified){
			// If there's a type cast, return casted data type
			return typeCast.getDataType();
		}
		
		// Find grand child and it's data type
		PVarAccessor grandChild = getGrandChild();
		PDataType dt = child != null ? grandChild.target.dataType : target.dataType;
		
		if(grandChild.isPointerAccess())
			dt = dt.plain();
		
		if(grandChild.isArrayAccess())
			if(grandChild.getDataType() == PDataType.STRING)
				dt = PDataType.CHAR;
			else
				dt = dt.plain();
		
		if(grandChild.isSliceAccess())
			dt = dt.plain().asSlice();
		
		// TODO: Try to do something with this
		// If it's a reference access of array, but without index, return 
		// array data type instead of reference
		if(isReferenceAccess()){
			//if(!(grandChild.target.dataType.isArray() && !grandChild.isArrayAccess()))
				dt = dt.reference();
		}
		
		return dt;
	}*/
	
	/** Returns unmodified {@link PDataType} of final member
	 * in this variable access.
	 * 
	 * @return {@link PDataType}*/
	public PDataType getDataTypeUnmodified(){
		// TODO: May not be needed
		if(typeCast != null && verified){
			// If there's a type cast, return casted data type
			return typeCast.getDataType();
		}
		
		// TODO: When accessing child of struct array, accessor will think
		// that it's not array
		//return child != null ? getGrandChild().target.dataType : target.dataType;
		
		// TODO: Code above commented, and this is the new code, to be tested
		return target.dataType;
	}

	@Override
	protected PDataType getOperandDataType() {
		// Find grand child and it's data type
		PVarAccessor grandChild = getGrandChild();
		PDataType dt = child != null ? grandChild.target.dataType : target.dataType;
		
		// TODO: if statement below might be incorrect, if so
		// correct would be: grandChild.isPointerAccess()
		if(isPointerAccess())
			dt = dt.plain();
		
		if(grandChild.isArrayAccess())
			if(grandChild.getDataTypeUnmodified().plain() == PDataType.STRING)
				dt = PDataType.CHAR;
			else
				dt = dt.plain();
		
	//	if(grandChild.isSliceAccess())
	//		dt = dt.plain().asSlice();
		
		// TODO: Try to do something with this
		// If it's a reference access of array, but without index, return 
		// array data type instead of reference
		if(isReferenceAccess()){
			//if(!(grandChild.target.dataType.isArray() && !grandChild.isArrayAccess()))
				dt = dt.reference();
		}
		
		return dt;
	}

	@Override
	protected PStructType getOperandStructType() {
		return getGrandChild().target.struct;
	}
	
	public String getVarAddress(){
		return getVarAddress(0);
	}
	
	/** Returns variable address. This variable
	 * address is usable only if there is no array
	 * access in any member.
	 * 
	 * For accessing/modifying variable with array
	 * access, see {@link #movAddressToRegister(AsmRegister, boolean, AsmProgram)}*/
	public String getVarAddress(int offset){
		if(target instanceof PLocalVariable){
			PLocalVariable var = (PLocalVariable)target;
			int ebpWithoutMinus = Math.abs(var.ebpOffset);
			
			if(containsMemberAccess())
				ebpWithoutMinus -= getChildOffset();
			
			ebpWithoutMinus -= offset;
			
			return "[" + AsmRegister.EBP.getName() + " - " + ebpWithoutMinus + "]";
		}else if(target instanceof PFunctionParameter){
			PFunctionParameter param = (PFunctionParameter)target;
			
			int ebpOffset = param.ebpOffset;
			if(containsMemberAccess())
				ebpOffset += getChildOffset();
			
			ebpOffset += offset;
			
			return "[" + AsmRegister.EBP.getName() + " + " + ebpOffset + "]";
		}else if(target instanceof PGlobalVariable){
			if(isPointerAccess() || isReferenceAccess() || isMemberAccess()){
				String address = ((PGlobalVariable)target).nameInAssembly;
				
				int memberOffset = 0;
				if(containsMemberAccess())
					memberOffset = getChildOffset();

				memberOffset += offset;
				
				return "[" + address + (memberOffset == 0 ? "" : " + " + memberOffset) + "]";
			}else{
				return "[" + ((PGlobalVariable)target).nameInAssembly + "]";
			}
		}
		
		return "";
	}
	
	/** @return number of bytes, last access member is from parent struct */
	private int getChildOffset(){
		int offset = 0;
		PVarAccessor c = this.child;
		while(c != null){
			offset += c.parent.target.struct.getMemberOffset((PLocalVariable)c.target);
			c = c.child;
		}
		return offset;
	}
	
	/** First member in variable access chain, for ex.:
	 * Car.wheels.frontLeft would return Car var accessor */
	public PVarAccessor getGrandParent(){
		PVarAccessor var = this;
		while(var.parent != null){
			var = var.parent;
		}
		
		return var;
	}
	
	/** Last member in variable access chain, for ex.:
	 * Car.wheels.frontLeft would return frontLeft var accessor */
	public PVarAccessor getGrandChild(){
		PVarAccessor var = this;
		while(var.child != null){
			var = var.child;
		}
		
		return var;
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		if(parent == null){
			String arrayIndex = isArrayAccess() ? "[" + this.arrayIndex + "]" : "";
			String code = target.name + arrayIndex;
			
			PVarAccessor child = this.child;
			while(child != null){
				code += child.toString();
				child = child.child;
			}
			
			String accessType = isPointerAccess() ? "*" : (isReferenceAccess() ? "&" : "");
			return accessType + code;
		}else{
			String arrayIndex = isArrayAccess() ? "[" + this.arrayIndex + "]" : "";
			return "." + target.name + arrayIndex;
		}
	}
	
	public String toStringPlain(){
		if(parent == null){
			String arrayIndex = isArrayAccess() ? "[" + this.arrayIndex + "]" : "";
			String code = target.name + arrayIndex;
			
			PVarAccessor child = this.child;
			while(child != null){
				code += child.toString();
				child = child.child;
			}
			
			return code;
		}else{
			String arrayIndex = isArrayAccess() ? "[" + this.arrayIndex + "]" : "";
			return "." + target.name + arrayIndex;
		}
	}
	
	public PProgramPart clone(boolean recursive){
		PVarAccessor clone = new PVarAccessor(sourceFileName, lineInSourceCode, columnInSourceCode, super.parent);
		clone.target = target;
		clone.accessType = accessType;
		clone.arrayIndex = arrayIndex;
		clone.parent = null;
		clone.verified = verified;
		
		PVarAccessor _child = child;
		while(_child != null){
			PVarAccessor __child = new PVarAccessor(_child.sourceFileName, _child.lineInSourceCode, _child.columnInSourceCode, _child.getProgramPartParent());
			__child.target = _child.target;
			__child.accessType = _child.accessType;
			__child.arrayIndex = _child.arrayIndex;
			__child.parent = _child.parent;
			__child.verified = _child.verified;
			
			clone.getGrandChild().child = __child;
			
			_child = _child.child;
		}
		
		return clone;
	}

	@Override
	public void verify() {
	}
}

package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PVariable extends PProgramPart {
	public enum PScope {
		GLOBAL,
		LOCAL,
		FUNCTION_PARAM
	};

	public PScope scope;
	public PDataType dataType;
	public String name;
	/* Used only for struct variables, to determine struct of an variable */
	public PStructType struct = null;
	/* Used only for arrays */
	public int arraySize = 0;
	
	/* Indicates whether struct type was found in program or not */
	protected boolean structVerified = false;

	public PVariable(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}

	public void verifyStruct() {
		if(dataType.plain() != PDataType.STRUCT) return;
		if(structVerified) return;
		
		PStructType structType = null;
		String pkgName = null;
		if(struct.pkg != null){
			pkgName = struct.pkg.fileNameWithoutExtension;
			struct.pkg = getProgram().findImportedSource(pkgName);
			if(struct.pkg == null){
				// Package is not imported!
				Console.throwError(Console.ERROR_PACKAGE_NOT_IMPORTED, sourceFileName, lineInSourceCode, columnInSourceCode, pkgName);
			}else{
				// Package is imported, let's find
				// our struct type in it
				structType = struct.pkg.sourceAST.findStructType(struct.name);
			}
		}else{
			structType = getProgram().findStructType(struct.name);
		}
		
		// Let's see if struct type is defined
		if (structType == null) {
			// Undefined struct type
			if(pkgName != null){
				Console.throwError(Console.ERROR_UNDEFINED_STRUCT_TYPE_IN_PKG, sourceFileName, lineInSourceCode, columnInSourceCode, struct.name, pkgName);
			}else{
				Console.throwError(Console.ERROR_UNDEFINED_STRUCT_TYPE, sourceFileName, lineInSourceCode, columnInSourceCode, struct.name);
			}
		}else{
			// Verify struct members
			structType.verify();
			
			// Verify structs inside struct
			for(PLocalVariable member : structType.members){
				// If member struct is the same as the parent struct,
				// avoid verifying, otherwise stack overflow will occur
				if(member.dataType.isStruct()){
					PStructType realStructType = getProgram().findStructType(member.struct.name);
					if(realStructType == structType)
						continue;
				}
				
				member.verifyStruct();
			}
		}
		
		this.struct = structType;
		structVerified = true;
	}

	public boolean isIntegral() {
		return dataType.isIntegral();
	}

	/** 
	 * Returns variable size, depending on data type, in bytes. 
	 */
	public long getSizeInBytes() {
		if(struct != null){
			// If variable is pointer to the struct,
			// then return size of a pointer
			if(dataType.isPointer()) {
				// TODO: Multiplication by arraySize may not be needed
				return dataType.getSizeInBytes() * Math.max(1, arraySize);
			} else {
				// If struct is not a pointer, then
				// return size of a struct
				return struct.getSizeInBytes() * Math.max(1, arraySize);
			}
		}
		
		// If variable is not a struct,
		// return size of it's data type
		return dataType.getSizeInBytes() * Math.max(1, arraySize);
	}

	@Override
	protected String getAstCode(String padding) {
		return name;
	}

	@Override
	public String toString() {
		String code = "";
		
		if(dataType.isStruct()){
			code += dataType.toStringWithStruct(struct) + " ";
		}else{
			code += dataType + " ";
		}
		
		code += name;
		
		return code;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PVariable var = new PVariable(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		var.scope = scope;
		var.dataType = dataType;
		var.name = name;
		var.struct = struct;
		var.arraySize = arraySize;
		var.structVerified = structVerified;
		
		return var;
	}

}

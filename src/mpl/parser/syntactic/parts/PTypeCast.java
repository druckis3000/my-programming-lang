package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PTypeCast extends PProgramPart {

	public PDataType primitiveType = null;
	public PStructType structType = null;
	/* Used only along with structType */
	public boolean pointer = false;
	
	public PTypeCast(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void verify(PDataType srcType){
		// TODO: Verify struct type
		
		if(primitiveType != null){
			// Casting into primitive data type
			
			switch(primitiveType){
			case STRING:
				// Nothing can be cast into string, apart void and pointers
				if(srcType == PDataType.VOID)
					break;
				
				if(srcType.isPointer())
					break;
				
				// Neither void, nor pointer, throw an error
				Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
						srcType, primitiveType);
				
				break;
			case INT:
				// We can cast anything into pointer
				if(primitiveType.isPointer())
					break;
				
				// We can cast void to int
				if(srcType.plain() == PDataType.VOID)
					break;
				
				// We can cast pointers into int
				if(srcType.isPointer())
					break;
				
				// All integrals that are not arrays
				if(!srcType.isArray())
					if(srcType.plain() == PDataType.INT || srcType.plain() == PDataType.SHORT || srcType.plain() == PDataType.CHAR)
						break;
				
				// If source is none of above mentioned types,
				// throw an error
				Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
						srcType, primitiveType);
				
				break;
			case SHORT:
				// We can cast anything into pointer
				if(primitiveType.isPointer())
					break;
				
				// We can cast void to short
				if(srcType.plain() == PDataType.VOID)
					break;
				
				// Short ant char integrals that are not arrays
				if(!srcType.isArray()){
					if(srcType.plain() == PDataType.SHORT || srcType.plain() == PDataType.CHAR){
						break;
					}else if(srcType.plain() == PDataType.INT){
						// Lossy conversion, throw an error
						Console.throwError(Console.ERROR_LOSSY_CONVERSION, sourceFileName, lineInSourceCode, columnInSourceCode, srcType, primitiveType);
					}
				}
				
				// If source is none of above mentioned types,
				// throw an error
				Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
						srcType, primitiveType);
				
				break;
			case CHAR:
				// We can cast anything into pointer
				if(primitiveType.isPointer())
					break;
				
				// We can cast void to char
				if(srcType.plain() == PDataType.VOID)
					break;
				
				// Char that is not array
				if(!srcType.isArray()){
					if(srcType.plain() == PDataType.CHAR){
						break;
					}else if(srcType.plain() == PDataType.INT || srcType.plain() == PDataType.SHORT){
						// Lossy conversion, throw an error
						Console.throwError(Console.ERROR_LOSSY_CONVERSION, sourceFileName, lineInSourceCode, columnInSourceCode, srcType, primitiveType);
					}
				}
				
				// If source is none of above mentioned types,
				// throw an error
				Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
						srcType, primitiveType);
				
				break;
			default:
				break;
			}
		}else{
			// Casting into struct type
			
			if(pointer == false){
				// If target type is not a pointer, then
				// target can be assigned without casting,
				// thus we throw an error
				Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
						srcType, structType.name);
			}else{
				if(srcType != PDataType.VOID && srcType != PDataType.INT){
					Console.throwError(Console.ERROR_TYPE_CAST_ERROR, sourceFileName, lineInSourceCode, columnInSourceCode,
							srcType, structType.name + (pointer ? "*" : ""));
				}
			}
		}
	}
	
	public PDataType getDataType(){
		// If we have primitive data type, return it
		if(primitiveType != null)
			return primitiveType;
		
		// Otherwise, if we have struct type, check if
		// it's a pointer, if so return as pointer, else
		// return as plain struct
		if(pointer)
			return PDataType.STRUCT.asPointer();
		else
			return PDataType.STRUCT;
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}
	
	@Override
	public String toString(){
		if(primitiveType == null){
			return "(" + structType.name + (pointer ? "*" : "") + ")";
		}else{
			return "(" + primitiveType.toString() + ")";
		}
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PTypeCast typeCast = new PTypeCast(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		typeCast.primitiveType = primitiveType;
		typeCast.structType = structType;
		typeCast.pointer = pointer;
		
		return typeCast;
	}
}
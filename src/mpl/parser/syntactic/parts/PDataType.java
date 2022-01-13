package mpl.parser.syntactic.parts;

import mpl.compiler.CompilerOptions;
import mpl.compiler.asm.AsmSizes;
import mpl.compiler.asm.AssemblyCodeBuilder;

public enum PDataType {
	NULL("null"),
	
	VOID("void"),
	BOOL("bool"),
	INT("int"),
	SHORT("short"),
	CHAR("char"),
	STRING("string"),
	STRUCT("struct"),
	
	PTR_VOID("void*"),
	PTR_BOOL("bool*"),
	PTR_INT("int*"),
	PTR_SHORT("short*"),
	PTR_CHAR("char*"),
	PTR_STRING("string*"),
	PTR_STRUCT("struct*"),
	
	REF_VOID("void&"),
	REF_BOOL("bool&"),
	REF_INT("int&"),
	REF_SHORT("short&"),
	REF_CHAR("char&"),
	REF_STRING("string&"),
	REF_STRUCT("struct&"),
	
	ARRAY_VOID("void[]"),
	ARRAY_BOOL("bool[]"),
	ARRAY_INT("int[]"),
	ARRAY_SHORT("short[]"),
	ARRAY_CHAR("char[]"),
	ARRAY_STRUCT("struct[]"),
	
	SLICE_VOID("void[::]"),
	SLICE_BOOL("bool[::]"),
	SLICE_INT("int[::]"),
	SLICE_SHORT("short[::]"),
	SLICE_CHAR("char[::]"),
	SLICE_STRUCT("struct[::]");
	
	private String code;
	
	private PDataType(String code){
		this.code = code;
	}
	
	public boolean isPointer(){
		return (this == PTR_VOID ||
				this == PTR_BOOL ||
				this == PTR_INT ||
				this == PTR_SHORT ||
				this == PTR_CHAR ||
				this == PTR_STRING ||
				this == PTR_STRUCT);
	}
	
	public boolean isReference(){
		return (this == REF_VOID ||
				this == REF_BOOL ||
				this == REF_INT ||
				this == REF_SHORT ||
				this == REF_CHAR ||
				this == REF_STRING ||
				this == REF_STRUCT);
	}
	
	public boolean isArray(){
		return (this == ARRAY_VOID ||
				this == ARRAY_BOOL ||
				this == ARRAY_INT ||
				this == ARRAY_SHORT ||
				this == ARRAY_CHAR ||
				this == ARRAY_STRUCT);
	}
	
	public boolean isIntegral(){
		return (this == INT || this == SHORT || this == CHAR ||
				this.isPointer() ||
				this.isReference() ||
				this == ARRAY_INT || this == ARRAY_SHORT || this == ARRAY_CHAR ||
				this == SLICE_INT || this == SLICE_SHORT || this == SLICE_CHAR);
	}
	
	public boolean isPlain(){
		return (this == NULL || this == VOID || this == BOOL || this == INT || this == SHORT || this == CHAR || this == STRING || this == STRUCT);
	}
	
	public boolean isStruct(){
		return (this == STRUCT || this == PTR_STRUCT || this == REF_STRUCT || this == ARRAY_STRUCT);
	}
	
	public int getSizeInBytes(){
		// Slices are structs with 2 members
		if(isSlice())
			return 8;
		
		if(this.isPointer())
			return getPointerSize();
		
		if(this.plain() == CHAR || this.plain() == BOOL)
			return 1;
		else if(this.plain() == SHORT)
			return 2;
		else
			return 4;
	}
	
	public final String getSizeDirective(){
		if(isSlice())
			return "DWORD";
		
		if(this.isPointer())
			return AsmSizes.getSizeDirective();
		
		if(this.plain() == CHAR || this.plain() == BOOL)
			return "BYTE";
		else if(this.plain() == SHORT)
			return "WORD";
		else
			return "DWORD";
	}
	
	public final String getDefineDirective(){
		if(isSlice())
			return "dd";
		
		if(this.isPointer())
			return "dd";
		
		if(this.plain() == CHAR || this.plain() == BOOL)
			return "db";
		else if(this.plain() == SHORT)
			return "dw";
		else if(this.plain() == STRING)
			return "db";
		else
			return "dd";
	}
	
	public PDataType dereference(){
		if(this == PTR_VOID)
			return VOID;
		if(this == PTR_BOOL)
			return BOOL;
		if(this == PTR_INT)
			return INT;
		if(this == PTR_SHORT)
			return SHORT;
		if(this == PTR_CHAR)
			return CHAR;
		if(this == PTR_STRING)
			return STRING;
		if(this == PTR_STRUCT)
			return STRUCT;
		
		return null;
	}

	public PDataType reference() {
		if(this.plain() == VOID)
			return REF_VOID;
		if(this.plain() == BOOL)
			return REF_BOOL;
		if(this.plain() == INT)
			return REF_INT;
		if(this.plain() == SHORT)
			return REF_SHORT;
		if(this.plain() == CHAR)
			return REF_CHAR;
		if(this.plain() == STRING)
			return REF_STRING;
		if(this.plain() == STRUCT)
			return REF_STRUCT;
		
		return null;
	}

	public PDataType asPointer() {
		if(this.plain() == VOID)
			return PTR_VOID;
		if(this.plain() == BOOL)
			return PTR_BOOL;
		if(this.plain() == INT)
			return PTR_INT;
		if(this.plain() == SHORT)
			return PTR_SHORT;
		if(this.plain() == CHAR)
			return PTR_CHAR;
		if(this.plain() == STRING)
			return PTR_STRING;
		if(this.plain() == STRUCT)
			return PTR_STRUCT;
		
		return null;
	}

	public PDataType asArray() {
		if(this.plain() == VOID)
			return ARRAY_VOID;
		if(this.plain() == BOOL)
			return ARRAY_BOOL;
		if(this.plain() == INT)
			return ARRAY_INT;
		if(this.plain() == SHORT)
			return ARRAY_SHORT;
		if(this.plain() == CHAR)
			return ARRAY_CHAR;
		if(this.plain() == STRUCT)
			return ARRAY_STRUCT;
		
		return null;
	}

	public PDataType asSlice() {
		if(this.plain() == VOID)
			return SLICE_VOID;
		if(this.plain() == BOOL)
			return SLICE_BOOL;
		if(this.plain() == INT)
			return SLICE_INT;
		if(this.plain() == SHORT)
			return SLICE_SHORT;
		if(this.plain() == CHAR)
			return SLICE_CHAR;
		if(this.plain() == STRUCT)
			return SLICE_STRUCT;
		
		return null;
	}

	/** Plain means without any references/pointers/array indexing */
	public PDataType plain() {
		if(this == REF_VOID || this == PTR_VOID || this == ARRAY_VOID || this == SLICE_VOID)
			return VOID;
		if(this == REF_BOOL || this == PTR_BOOL || this == ARRAY_BOOL || this == SLICE_BOOL)
			return BOOL;
		if(this == REF_INT || this == PTR_INT || this == ARRAY_INT || this == SLICE_INT)
			return INT;
		if(this == REF_SHORT || this == PTR_SHORT || this == ARRAY_SHORT || this == SLICE_SHORT)
			return SHORT;
		if(this == REF_CHAR || this == PTR_CHAR || this == ARRAY_CHAR || this == SLICE_CHAR)
			return CHAR;
		if(this == PTR_STRUCT || this == REF_STRUCT || this == ARRAY_STRUCT || this == SLICE_STRUCT)
			return STRUCT;
		if(this == PTR_STRING || this == REF_STRING)
			return STRING;
		
		return this;
	}
	
	public static PDataType fromToken(String dataType, boolean isPtr, boolean isRef, boolean isArray, boolean isSlice){
		PDataType dt = VOID;
		
		switch(dataType){
		case "void":
			break;
		case "bool":
			dt = BOOL;
			break;
		case "int":
			dt = INT;
			break;
		case "short":
			dt = SHORT;
			break;
		case "char":
			dt = CHAR;
			break;
		case "struct":
			dt = STRUCT;
			break;
		case "string":
			dt = STRING;
			break;
		}
		
		if(isPtr)
			dt = dt.asPointer();
		else if(isRef)
			dt = dt.reference();
		else if(isSlice)
			dt = dt.asSlice();
		else if(isArray)
			dt = dt.asArray();
		
		return dt;
	}
	
	public static boolean isOperandValid(PDataType dest, PDataType src){
		switch(dest){
		/* Boolean */
		case BOOL:
			// Accept booleans
			if(dest == src)
				return true;
			
			// TODO: Accept integers aswell
			if(src.isIntegral() && !src.isArray() && !src.isPointer() && !src.isReference())
				return true;
			
			return false;
		/* Char */
		case CHAR:
			// Accepts only chars
			if(dest == src)
				return true;
			
			return false;
		/* Short */
		case SHORT:
			// Accepts chars and shorts
			if(src == SHORT || src == CHAR)
				return true;
			
			return false;
		/* Int */
		case INT:
			// Accepts only integral values
			
			if(!src.isIntegral())
				return false;
			
			if(src.isPointer() || src.isReference() || src.isArray())
				return false;
			
			return true;
		/* String */
		case STRING:
			// Accepts null
			if(src == NULL)
				return true;
			
			// And other strings
			return dest == src;
		/* Struct */
		case STRUCT:
			// Accepts only structs
			return dest == src;
		/* Pointers */
		case PTR_VOID:
			// Null is always acceptable for pointers
			if(src == NULL)
				return true;
			
			// Reference of same data type is acceptable
			if(src.isReference())
				return true;

			// Pointer of same data type is acceptable
			if(src.isPointer())
				return true;

			// Array of same data type is acceptable
			if(src.isArray())
				return true;
			
			// Slices are identified as integrals, but
			// they're not acceptable to pointers
			if(src.isSlice())
				return false;
			
			// Integrals of same data type is acceptable, since
			// pointer is an address, and address is just an integer
			if(src.isIntegral())
				return true;
			
			// Anything else is not acceptable
			return false;
		case PTR_CHAR:
		case PTR_SHORT:
		case PTR_INT:
		case PTR_STRING:
		case PTR_STRUCT:
			// If data types are not same, then
			// it can accept anything integral,
			// because address is just an integer.
			// Otherwise, if src is not integral,
			// and data types mismatch, return false.
			
			// Null is always acceptable for pointers
			if(src == NULL)
				return true;
			
			if(src.plain() != dest.plain())
				if(src.isIntegral())
					return true;
				else
					return false;
			
			// Reference of same data type is acceptable
			if(src.isReference())
				return true;

			// Pointer of same data type is acceptable
			if(src.isPointer())
				return true;

			// Array of same data type is acceptable
			if(src.isArray())
				return true;
			
			// Slices are identified as integrals, but
			// they're not acceptable to pointers
			if(src.isSlice())
				return false;
			
			// Integrals of same data type is acceptable, since
			// pointer is an address, and address is just an integer
			if(src.isIntegral())
				return true;
			
			// Anything else is not acceptable
			return false;
		/* Arrays */
		case ARRAY_CHAR:
		case ARRAY_SHORT:
		case ARRAY_INT:
		case ARRAY_STRUCT:
		case SLICE_CHAR:
		case SLICE_SHORT:
		case SLICE_INT:
		case SLICE_STRUCT:
			// Arrays and slices accept only same data type arrays
			if(dest == src)
				return true;
			
			return false;
		/* Void slices accept any type slices */
		case SLICE_VOID:
			if(src.isSlice())
				return true;
			
			return false;
		/* Void arrays accept any type arrays */
		case ARRAY_VOID:
			if(src.isArray() || src.isPointer() || src.isReference())
				return true;
			
			return false;
		default:
			return true;
		}
	}
	
	/* @return Data type symbol. */
	public String getSymbol(){
		switch(plain()){
		case CHAR:		return "c";
		case SHORT:		return "h";
		case INT:		return "d";
		case STRUCT:	return "S";
		case STRING:	return "s";
		case VOID:
		default:	return "v";
		}
	}
	
	public boolean isSlice(){
		return (this == SLICE_VOID || this == SLICE_INT || this == SLICE_SHORT || this == SLICE_CHAR || this == SLICE_STRUCT);
	}
	
	@Override
	public String toString(){
		return code;
	}
	
	public String toStringWithStruct(PStructType struct){
		String str = struct.name;
		
		if(isArray()){
			str += "[]";
		}else if(isPointer()){
			str += "*";
		}else if(isReference()){
			str = "&" + str;
		}else if(isSlice()){
			str += "[::]";
		}
		
		return str;
	}
	
	public static boolean isStructsEqual(PStructType s1, PStructType s2){
		return s1.name.equals(s2.name);
	}
	
	public static int getPointerSize(){
		return AssemblyCodeBuilder.ARCH == CompilerOptions.COMPILE_ARCH_64 ? 8 : 4;
	}
}
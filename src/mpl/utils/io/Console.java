package mpl.utils.io;

import java.util.HashMap;

public class Console {

	private static int errorCode = 0;
	
	public static final int ERROR_TYPE_MISMATCH						= errorCode++;
	public static final int ERROR_INCOMPATIBLE_OPERANDS				= errorCode++;
	public static final int ERROR_UNDEFINED_OPERATOR				= errorCode++;
	public static final int ERROR_NON_APPLICABLE_ARG				= errorCode++;
	public static final int ERROR_UNDEFINED_VAR						= errorCode++;
	public static final int ERROR_UNDEFINED_FUNCTION				= errorCode++;
	public static final int ERROR_UNEXPECTED_STATEMENT				= errorCode++;
	public static final int ERROR_UNEXPECTED_RETURN_STATEMENT		= errorCode++;
	public static final int ERROR_UNEXPECTED_RETURN_VALUE			= errorCode++;
	public static final int ERROR_NO_RETURN_RESULT					= errorCode++;
	public static final int ERROR_NOT_A_POINTER						= errorCode++;
	public static final int ERROR_FUNCTION_REDEFINITION				= errorCode++;
	public static final int ERROR_NOT_ARRAY							= errorCode++;
	public static final int ERROR_NON_CONST_INITIALIZER				= errorCode++;
	public static final int ERROR_STRUCT_REDECLARATION				= errorCode++;
	public static final int ERROR_VAR_REDECLARATION					= errorCode++;
	public static final int ERROR_UNDEFINED_STRUCT_TYPE				= errorCode++;
	public static final int ERROR_UNDEFINED_STRUCT_TYPE_IN_PKG		= errorCode++;
	public static final int ERROR_NON_STRUCT_MEMBER_REQ				= errorCode++;
	public static final int ERROR_NON_DECLARED_MEMBER_REQ			= errorCode++;
	public static final int ERROR_TOO_MANY_VALUES					= errorCode++;
	public static final int ERROR_TOO_FEW_VALUES					= errorCode++;
	public static final int ERROR_ARRAY_MEMBER_REQ					= errorCode++;
	public static final int ERROR_FUNCTION_REDECLARATION			= errorCode++;
	public static final int ERROR_UNDEFINED_STRUCT_FUNCTION			= errorCode++;
	public static final int ERROR_NON_STRUCT_FUNCTION_CALL			= errorCode++;
	public static final int ERROR_DELETING_NON_POINTER				= errorCode++;
	public static final int ERROR_TYPE_CAST_ERROR					= errorCode++;
	public static final int ERROR_UNDEFINED_MAIN_FNC				= errorCode++;
	public static final int ERROR_VAR_REDEFINITION					= errorCode++;
	public static final int ERROR_NON_CONST_ASM_EXP					= errorCode++;
	public static final int ERROR_NON_CONST_EXP						= errorCode++;
	public static final int ERROR_LOSSY_CONVERSION					= errorCode++;
	public static final int ERROR_NON_BOOL_EXP						= errorCode++;
	public static final int ERROR_PACKAGE_NOT_IMPORTED				= errorCode++;
	public static final int ERROR_FUNCTION_MUST_RETURN				= errorCode++;
	
	private static final HashMap<Integer, String> errorFormats = new HashMap<>();
	
	static {
		errorFormats.put(ERROR_TYPE_MISMATCH, "%s:%d:%d: Type mismatch. Cannot convert from '%s' to '%s'");
		errorFormats.put(ERROR_INCOMPATIBLE_OPERANDS, "%s:%d:%d: Incompatible operands '%s', '%s'");
		errorFormats.put(ERROR_UNDEFINED_OPERATOR, "%s:%d:%d: Undefined operator '%s' for data type '%s'");
		errorFormats.put(ERROR_NON_APPLICABLE_ARG, "%s:%d:%d: Function '%s' argument at index: %d is not applicable for type '%s'");
		errorFormats.put(ERROR_UNDEFINED_VAR, "%s:%d:%d: Undefined variable '%s'");
		errorFormats.put(ERROR_UNDEFINED_FUNCTION, "%s:%d:%d: Undefined function '%s'");
		errorFormats.put(ERROR_UNEXPECTED_STATEMENT, "%s:%d:%d: Unexpected statement '%s'");
		errorFormats.put(ERROR_UNEXPECTED_RETURN_STATEMENT, "%s:%d:%d: Unexpected return statement");
		errorFormats.put(ERROR_UNEXPECTED_RETURN_STATEMENT, "%s:%d:%d: Unexpected return with a value");
		errorFormats.put(ERROR_NO_RETURN_RESULT, "%s:%d:%d: Return statement must return a result of type '%s'");
		errorFormats.put(ERROR_NOT_A_POINTER, "%s:%d:%d: Variable '%s' is not a pointer!");
		errorFormats.put(ERROR_FUNCTION_REDEFINITION, "%s:%d:%d: Function '%s' redefinition, previously defined in %s:%d");
		errorFormats.put(ERROR_NOT_ARRAY, "%s:%d:%d: Variable '%s' is neither array nor pointer!");
		errorFormats.put(ERROR_NON_CONST_INITIALIZER, "%s:%d:%d: Initial value is not constant!");
		errorFormats.put(ERROR_STRUCT_REDECLARATION, "%s:%d:%d: Redeclaration of type '%s', previously declared in %s:%d");
		errorFormats.put(ERROR_VAR_REDECLARATION, "%s:%d:%d: Redeclaration of variable '%s', previously declared in %s:%d");
		errorFormats.put(ERROR_UNDEFINED_STRUCT_TYPE, "%s:%d:%d: Undefined struct type '%s'");
		errorFormats.put(ERROR_UNDEFINED_STRUCT_TYPE_IN_PKG, "%s:%d:%d: Struct type '%s' is undefined in package '%s'");
		errorFormats.put(ERROR_NON_STRUCT_MEMBER_REQ, "%s:%d:%d: Member request in variable '%s', which is not a struct!");
		errorFormats.put(ERROR_NON_DECLARED_MEMBER_REQ, "%s:%d:%d: Struct '%s' has no member named '%s'!");
		errorFormats.put(ERROR_TOO_MANY_VALUES, "%s:%d:%d: Too many %s in %s");
		errorFormats.put(ERROR_TOO_FEW_VALUES, "%s:%d:%d: Too few %s in %s");
		errorFormats.put(ERROR_ARRAY_MEMBER_REQ, "%s:%d:%d: Unexpected member request from array in variable '%s'!");
		errorFormats.put(ERROR_FUNCTION_REDECLARATION, "%s:%d:%d: Function '%s' redeclaration! Previously declared in %s:%d");
		errorFormats.put(ERROR_UNDEFINED_STRUCT_FUNCTION, "%s:%d:%d: Function '%s' is undefined for struct type: '%s'!");
		errorFormats.put(ERROR_NON_STRUCT_FUNCTION_CALL, "%s:%d:%d: Function call on non-struct type variable");
		errorFormats.put(ERROR_DELETING_NON_POINTER, "%s:%d:%d: '%s' is not a pointer");
		errorFormats.put(ERROR_TYPE_CAST_ERROR, "%s:%d:%d: Cannot cast from '%s' to '%s'");
		errorFormats.put(ERROR_UNDEFINED_MAIN_FNC, "Cannot find 'main' function definition!");
		errorFormats.put(ERROR_VAR_REDEFINITION, "%s:%d:%d: Variable '%s' redefiniton, previously defined in %s:%d");
		errorFormats.put(ERROR_NON_CONST_ASM_EXP, "%s:%d:%d: Non constant assembly expression!");
		errorFormats.put(ERROR_NON_CONST_EXP, "%s:%d:%d: Non constant expression!");
		errorFormats.put(ERROR_LOSSY_CONVERSION, "%s:%d:%d: Possible lossy conversion from '%s' to '%s'!");
		errorFormats.put(ERROR_NON_BOOL_EXP, "%s:%d:%d: Non boolean expression!");
		errorFormats.put(ERROR_PACKAGE_NOT_IMPORTED, "%s:%d:%d: Package '%s' is not imported!");
		errorFormats.put(ERROR_FUNCTION_MUST_RETURN, "%s:%d:%d: Function must return a result of type '%s'");
	}
	
	public Console() {
		
	}

	public static void printWarning(String warning){
		System.out.println("Warning: " + warning);
	}
	
	public static void throwError(String error){
		System.err.println("Error: " + error);
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		System.err.println("Called from class: " + stack[2].getClassName() + '\n' +
				"\tmethod: " + stack[2].getMethodName() + '\n' +
				"\tline: " + stack[2].getLineNumber());
		System.exit(-1);
	}
	
	public static void throwError(int errorCode, Object... additionalInfo){
		Exception ex = new Exception("Error: " + String.format(errorFormats.get(errorCode), additionalInfo));
		ex.printStackTrace();
		System.exit(-1);
	}
}

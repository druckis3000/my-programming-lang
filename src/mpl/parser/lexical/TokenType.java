package mpl.parser.lexical;

public enum TokenType {
	// Keywords
	FUNCTION_DEF("function"),		/* function */
	IF_STATEMENT("if"),				/* if */
	ELSE_STATEMENT("else"),			/* else */
	RETURN_STATEMENT("return"),		/* return */
	WHILE_STATEMENT("while"),		/* while */
	DO_STATEMENT("do"),				/* do */
	FOR_STATEMENT("for"),			/* for */
	BREAK_STATEMENT("break"),		/* break */
	CONTINUE_STATEMENT("continue"),	/* continue */
	
	// Data types
	DT_VOID("void"),			/* void */
	DT_INT("int"),				/* int */
	DT_UINT("uint"),			/* uint */
	DT_STRING("string"),		/* string */

	// Symbols
	BODY_START("{"),			/* { */
	BODY_END("}"),				/* } */
	OPEN_BRACKET("("),			/* ( */
	CLOSE_BRACKET(")"),			/* ) */
	OPEN_ARRAY_BRACKET("["),	/* [ */
	CLOSE_ARRAY_BRACKET("]"),	/* ] */
	
	// Relational operators
	COMPARE_GREATER(">"),		/* > */
	COMPARE_LESSER("<"),		/* < */
	COMPARE_EQUALS("=="),		/* == */
	COMPARE_NOT_EQUALS("!="),	/* != */
	
	// Other symbols
	ASSIGNMENT("="),			/* = */
	COMMA(","),					/* , */
	SEMICOLON(";"),				/* ; */
	
	// Binary operators
	LOGICAL_NOT("!"),			/* ! */
	
	// Arithmetic operators
	ALU_ADD("+"),				/* + */
	ALU_SUB("-"),				/* - */
	ALU_MUL("*"),				/* * */
	ALU_DIV("/"),				/* / */
	
	// Unary operators
	DECREASE("--"),				/* -- */
	INCREASE("++"),				/* ++ */
	
	// Literals...
	STRING(),							/* anystring */
	INTEGER_LITERAL("string literal"),	/* 1234567890 */
	STRING_LITERAL("integer literal");	/* "..." */
	
	private String identifier = "";

	private TokenType(){
	}
	
	private TokenType(String identifier){
		this.identifier = identifier;
	}
	
	public boolean isDataType(){
		return (this == DT_UINT || this == DT_INT);
	}
	
	public boolean isArithmeticOperator(){
		return (this == ALU_ADD || this == ALU_SUB || this == ALU_MUL || this == ALU_DIV);
	}
	
	public boolean isRelationalOperator(){
		return (this == COMPARE_EQUALS || this == COMPARE_NOT_EQUALS || this == COMPARE_GREATER || this == COMPARE_LESSER);
	}
	
	public boolean isIntegral(){
		return (this == DT_UINT || this == DT_INT);
	}
	
	@Override
	public String toString(){
		return identifier;
	}
}
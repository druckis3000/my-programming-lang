package mpl.syntactic.parts;

public class POperator extends PProgramPart {
	public enum POperatorType {
		// Arithmetic
		ADD("+", 1),
		SUB("-", 2),
		DIV("/", 4),
		MUL("*", 3),
		
		// Bitwise
		BITWISE_AND("&"),
		BITWISE_OR("|"),
		BITWISE_XOR("^"),
		BITWISE_NOT("~"),
		
		// Logical
		LOGICAL_AND("&&"),
		LOGICAL_OR("||"),
		
		// Relational
		GREATER(">"),
		LESSER("<"),
		LESSER_EQUAL("<="),
		GREATER_EQUAL(">="),
		
		// Equality
		EQUALS("=="),
		NOT_EQUALS("!="),
		
		// Unary
		INCREASE("++"),
		DECREASE("--"),
		REFERENCE("&"),
		POINTER("*"),
		
		NOT("!");
		
		private String code = "";
		public int precedence = 0;
		
		private POperatorType(String code){
			this.code = code;
		}
		
		private POperatorType(String code, int precedence){
			this.code = code;
			this.precedence = precedence;
		}

		@Override
		public String toString() {
			return code;
		}
	}
	
	public POperatorType type;
	
	public POperator(POperatorType type, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.type = type;
	}
	
	public boolean isArithmetic(){
		return (type == POperatorType.ADD || type == POperatorType.SUB || type == POperatorType.DIV || type == POperatorType.MUL);
	}
	
	public boolean isBitwise(){
		return (type == POperatorType.BITWISE_AND || type == POperatorType.BITWISE_OR || type == POperatorType.BITWISE_XOR ||
				type == POperatorType.BITWISE_NOT);
	}
	
	public boolean isUnary(){
		return (type == POperatorType.INCREASE || type == POperatorType.DECREASE ||
				type == POperatorType.REFERENCE || type == POperatorType.POINTER);
	}
	
	public boolean isUnaryArithmetic(){
		return (type == POperatorType.INCREASE || type == POperatorType.DECREASE);
	}
	
	public boolean isBooleanOperator(){
		return (type == POperatorType.GREATER || type == POperatorType.LESSER || type == POperatorType.GREATER_EQUAL ||
				type == POperatorType.LESSER_EQUAL || type == POperatorType.EQUALS || type == POperatorType.NOT_EQUALS);
	}

	@Override
	protected String getAstCode(String padding) {
		return type.toString();
	}
	
	@Override
	public String toString(){
		return type.toString();
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		return new POperator(type, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
}
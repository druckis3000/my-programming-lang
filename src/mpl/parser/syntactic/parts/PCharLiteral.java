package mpl.parser.syntactic.parts;

public class PCharLiteral extends PLiteral {
	public char value;
	
	public PCharLiteral(char value, String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		this.value = value;
	}

	@Override
	public void verify() {
		verifyTypeCast();
	}

	@Override
	protected PDataType getOperandDataType() {
		return PDataType.CHAR;
	}

	@Override
	protected PStructType getOperandStructType() {
		return null;
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}
	
	@Override
	public String toString(){
		switch(value){
		case '\b': return "\\b";
		case '\t': return "\\t";
		case '\n': return "\\n";
		case '\f': return "\\f";
		case '\r': return "\\r";
		case '\\': return "\\";
		case '\'': return "\\'";
		case '\0': return "\\0";
		}
		
		return "" + value;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PCharLiteral charLiteral = new PCharLiteral(value, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			charLiteral.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			charLiteral.structType = structType;
			charLiteral.structFound = structFound;
		}else{
			charLiteral.typeCast = typeCast;
			charLiteral.structType = structType;
			charLiteral.structFound = structFound;
		}
		
		return charLiteral;
	}
}

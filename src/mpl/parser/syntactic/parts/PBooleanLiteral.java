package mpl.parser.syntactic.parts;

public class PBooleanLiteral extends PLiteral {
	public boolean value;
	
	public PBooleanLiteral(boolean value, String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		this.value = value;
	}
	
	public PBooleanLiteral(String value, String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		this.value = Boolean.parseBoolean(value);
	}

	@Override
	public void verify() {
	}

	@Override
	protected PDataType getOperandDataType() {
		return PDataType.BOOL;
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
		return "" + value;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PBooleanLiteral booleanLiteral = new PBooleanLiteral(value, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			booleanLiteral.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			booleanLiteral.structType = structType;
			booleanLiteral.structFound = structFound;
		}else{
			booleanLiteral.typeCast = typeCast;
			booleanLiteral.structType = structType;
			booleanLiteral.structFound = structFound;
		}
		
		return booleanLiteral;
	}
}

package mpl.syntactic.parts;

public class PNullLiteral extends PLiteral {

	public PNullLiteral(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}

	@Override
	public void verify() {
		verifyTypeCast();
	}

	@Override
	protected PDataType getOperandDataType() {
		return PDataType.NULL;
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
		return "null";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PNullLiteral nullLiteral = new PNullLiteral(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			nullLiteral.structType = structType;
			nullLiteral.structFound = structFound;
			nullLiteral.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
		}else{
			nullLiteral.structType = structType;
			nullLiteral.structFound = structFound;
			nullLiteral.typeCast = typeCast;
		}
		
		return nullLiteral;
	}
}

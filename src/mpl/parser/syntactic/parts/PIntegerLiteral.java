package mpl.parser.syntactic.parts;

public class PIntegerLiteral extends PLiteral {
	public static final PIntegerLiteral ZERO = new PIntegerLiteral(0, "", -1, -1, null);
	public static final PIntegerLiteral ONE = new PIntegerLiteral(1, "", -1, -1, null);
	
	public long value = 0;
	public PDataType dataType;
	
	public PIntegerLiteral(long value, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.value = value;
		
		if(value <= 127 && value >= -128){
			dataType = PDataType.CHAR;
		}else if(value <= 32767 && value >= -32768){
			dataType = PDataType.SHORT;
		}else{
			dataType = PDataType.INT;
		}
	}
	
	@Override
	public void verify(){
		verifyTypeCast();
	}
	
	@Override
	public String getAstCode(String padding){
		return "" + value;
	}
	
	@Override
	public String toString(){
		return "" + value;
	}

	@Override
	protected PDataType getOperandDataType() {
		return dataType;
	}

	@Override
	protected PStructType getOperandStructType() {
		return null;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PIntegerLiteral intLiteral = new PIntegerLiteral(value, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			intLiteral.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			intLiteral.structType = structType;
			intLiteral.structFound = structFound;
			
			intLiteral.dataType = dataType;
		}else{
			intLiteral.typeCast = typeCast;
			intLiteral.structType = structType;
			intLiteral.structFound = structFound;
			
			intLiteral.dataType = dataType;
		}
		
		return intLiteral;
	}
}
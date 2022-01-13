package mpl.parser.syntactic.parts;

public class PHexLiteral extends PLiteral {

	public String code;
	public long value;
	public PDataType dataType;
	
	public PHexLiteral(String code, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		
		this.code = code;
		value = Long.parseLong(code.startsWith("0x") ? code.substring(2) : code, 16);
		
		if(value <= 127 && value >= -128){
			dataType = PDataType.CHAR;
		}else if(value <= 32767 && value >= -32768){
			dataType = PDataType.SHORT;
		}else{
			dataType = PDataType.INT;
		}
	}
	
	public PHexLiteral(long value, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		
		this.value = value;
		this.code = "0x" + Long.toHexString(value);
		
		if(value <= 127 && value >= -128){
			dataType = PDataType.CHAR;
		}else if(value <= 32767 && value >= -32768){
			dataType = PDataType.SHORT;
		}else{
			dataType = PDataType.INT;
		}
	}

	@Override
	public void verify() {
		verifyTypeCast();
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
	protected String getAstCode(String padding) {
		return code;
	}

	@Override
	public String toString(){
		return code;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PHexLiteral hexLiteral = new PHexLiteral(value, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			hexLiteral.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			hexLiteral.structType = structType;
			hexLiteral.structFound = structFound;
			
			hexLiteral.code = new String(code);
			hexLiteral.dataType = dataType;
		}else{
			hexLiteral.typeCast = typeCast;
			hexLiteral.structType = structType;
			hexLiteral.structFound = structFound;
			
			hexLiteral.code = code;
			hexLiteral.dataType = dataType;
		}
		
		return hexLiteral;
	}
}

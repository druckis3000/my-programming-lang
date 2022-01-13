package mpl.parser.syntactic.parts;

public class PStringLiteral extends PLiteral {
	private static int RO_DATA_INDEX = 0;
	
	public String value = "";
	public String roDataName = "";
	
	public PStringLiteral(String value, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.value = value;
		roDataName = "lc" + RO_DATA_INDEX++;
	}

	@Override
	public void verify() {
		verifyTypeCast();
	}
	
	public String getAssemblyValue(){
		String asmValue = new String(value);

		asmValue = asmValue.replace("\\r", "\", 0x0D, \"");
		asmValue = asmValue.replace("\\n", "\", 0x0A, \"");
		asmValue = asmValue.replace("\\t", "\", 0x09, \"");
		asmValue = asmValue.replace(", \"\"", "");
		
		return asmValue + ", 0";
	}

	@Override
	protected PDataType getOperandDataType() {
		return PDataType.STRING;
	}
	
	@Override
	public String getAstCode(String padding){
		return value;
	}
	
	@Override
	public String toString(){
		return value;
	}

	@Override
	protected PStructType getOperandStructType() {
		return null;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PStringLiteral string = new PStringLiteral(value, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			string.structType = structType;
			string.structFound = structFound;
			string.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			string.value = new String(value);
		}else{
			string.structType = structType;
			string.structFound = structFound;
			string.typeCast = typeCast;
		}
		
		return string;
	}
}
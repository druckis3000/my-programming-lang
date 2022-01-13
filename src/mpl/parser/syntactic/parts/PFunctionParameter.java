package mpl.parser.syntactic.parts;

public class PFunctionParameter extends PVariable {
	public int ebpOffset = 0;
	
	public PFunctionParameter(PDataType dataType, String name, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		scope = PScope.FUNCTION_PARAM;
		this.dataType = dataType;
		this.name = name;
	}
	
	@Override
	public String getAstCode(String padding){
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + this.toString();
		return total;
	}
	
	@Override
	public PProgramPart clone(boolean recursive){
		PFunctionParameter fncParam = new PFunctionParameter(dataType, name, sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			fncParam.name = new String(name);
			fncParam.struct = struct;
			fncParam.arraySize = arraySize;
			fncParam.structVerified = structVerified;
			fncParam.ebpOffset = ebpOffset;
		}else{
			fncParam.struct = struct;
			fncParam.arraySize = arraySize;
			fncParam.structVerified = structVerified;
			fncParam.ebpOffset = ebpOffset;
		}
		
		return fncParam;
	}
}
package mpl.parser.syntactic.parts;

public class PLocalVariable extends PVariable {
	public int ebpOffset;
	
	public PLocalVariable(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		scope = PScope.LOCAL;
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
		PLocalVariable var = new PLocalVariable(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			var.name = new String(name);
			var.struct = struct;
			var.arraySize = arraySize;
			var.structVerified = structVerified;
			var.ebpOffset = ebpOffset;
		}else{
			var.struct = struct;
			var.arraySize = arraySize;
			var.structVerified = structVerified;
			var.ebpOffset = ebpOffset;
		}
		
		return var;
	}
}
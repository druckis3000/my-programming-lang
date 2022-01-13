package mpl.syntactic.parts;

import java.util.ArrayList;
import java.util.List;

public class PFunctionParams extends PProgramPart {
	public List<PFunctionParameter> params = new ArrayList<PFunctionParameter>();
	
	public PFunctionParams(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void setEbpOffsets(){
		int ebpOffset = 8;
		for(int i=0; i<params.size(); i++){
			params.get(i).ebpOffset = ebpOffset;
			ebpOffset += params.get(i).getSizeInBytes();
		}
	}
	
	public PVariable findVariable(PVariable variable){
		for(PFunctionParameter param : params){
			if(param.name.equals(variable.name)){
				return param;
			}
		}
		return null;
	}
	
	public int isVariableDeclared(PVariable variable){
		for(PFunctionParameter param : params){
			if(param.name.equals(variable.name)){
				if(variable.lineInSourceCode > param.lineInSourceCode){
					return param.lineInSourceCode;
				}
			}
		}
		return -1;
	}

	@Override
	protected String getAstCode(String padding) {
		String tab = "";
		for(int i=0; i<getChildNumber(); i++) tab += '\t';
		
		String total = tab + "params:" + System.lineSeparator();
		for(int i=0; i<params.size(); i++)
			total += params.get(i).getAstCode(null) + (i + 1 < params.size() ? System.lineSeparator() : "");
		
		return total;
	}

	@Override
	public String toString(){
		String total = "params:" + System.lineSeparator();
		for(int i=0; i<params.size(); i++)
			total += params.get(i) + (i + 1 < params.size() ? System.lineSeparator() : "");
		
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PFunctionParams params = new PFunctionParams(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			for(PFunctionParameter param : this.params)
				params.params.add((PFunctionParameter)param.clone(true));
		}else{
			params.params.addAll(this.params);
		}
		
		return params;
	}
}

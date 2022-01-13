package mpl.syntactic.parts;


public class PNewOperand extends POperand {
	
	public PNewOperand(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}

	@Override
	public void verify(){
		verifyStruct();
		verifyTypeCast();
	}

	@Override
	protected PDataType getOperandDataType() {
		return PDataType.STRUCT.asPointer();
	}

	@Override
	protected PStructType getOperandStructType() {
		return structType;
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		return "new " + structType.name;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PNewOperand newOp = new PNewOperand(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			newOp.structType = structType;
			newOp.structFound = structFound;
			newOp.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
		}else{
			newOp.structType = structType;
			newOp.structFound = structFound;
			newOp.typeCast = typeCast;
		}
		
		return newOp;
	}
}

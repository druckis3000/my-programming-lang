package mpl.parser.syntactic.parts;

import mpl.utils.io.Console;

public class PSliceExp extends PProgramPart {

	public PExpression start;
	public PExpression end;
	
	public PSliceExp(String sourceFile, int lineInSource, int columnInLine, PProgramPart parent) {
		super(sourceFile, lineInSource, columnInLine, parent);
	}
	
	public void verify(){
		if(start != null){
			PDataType expType = start.verify();
			if(!expType.isIntegral())
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expType, PDataType.INT);
		}
		
		if(end != null){
			PDataType expType = end.verify();
			if(!expType.isIntegral())
				Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, lineInSourceCode, columnInSourceCode, expType, PDataType.INT);
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return toString();
	}

	@Override
	public String toString(){
		return "[" + start + ":" + end + "]";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		// TODO Auto-generated method stub
		return null;
	}
}

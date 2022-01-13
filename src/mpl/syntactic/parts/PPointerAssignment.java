package mpl.syntactic.parts;

public class PPointerAssignment extends PProgramPart {

	public PExpression pointerExp;
	public PExpression exp;
	
	public PPointerAssignment(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
	
	/** Verifies both left-hand side and right-hand side expressions */
	public void verify(){
		pointerExp.verify();
		exp.verify();
	}

	@Override
	protected String getAstCode(String padding) {
		return toString();
	}

	@Override
	public String toString(){
		return "*(" + pointerExp + ") = " + exp;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PPointerAssignment ptrAssignment = new PPointerAssignment(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			ptrAssignment.pointerExp = pointerExp == null ? null : (PExpression)pointerExp.clone(true);
			ptrAssignment.exp = exp == null ? null : (PExpression)exp.clone(true);
		}else{
			ptrAssignment.pointerExp = pointerExp;
			ptrAssignment.exp = exp;
		}
		
		return ptrAssignment;
	}
}

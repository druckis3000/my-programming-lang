package mpl.syntactic.parts;

public abstract class PLiteral extends POperand {

	public PLiteral(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
}
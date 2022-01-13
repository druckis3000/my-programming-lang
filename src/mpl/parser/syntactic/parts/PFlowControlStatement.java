package mpl.parser.syntactic.parts;

public abstract class PFlowControlStatement extends PProgramPart {
	public static final int TYPE_CHOICE = 1;
	public static final int TYPE_LOOP = 2;
	
	public final int statementType;
	
	public PBody body;
	public PConditionExp condition;
	
	public PFlowControlStatement(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent, int statementType) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		this.statementType = statementType;
	}

}

package mpl.analysis;

import mpl.parser.ParserException;
import mpl.parser.syntactic.parts.PFunction;
import mpl.parser.syntactic.parts.PProgram;

public class SemanticAnalyser {
	public PProgram program = null;
	
	public SemanticAnalyser(PProgram program){
		this.program = program;
	}
	
	public void analyze() throws ParserException {
		program.verify();
		
		// Verify functions
		for(PFunction function : program.functions){
			function.checkForLocalVariablesRedefinition();
			function.checkForUndefinedStructTypes();
			function.checkForDeadCode();
			function.verify();
		}
		
		// Process functions
		for(PFunction function : program.functions){
			function.process();
			
			function.setVariablesOffsets();
			function.calculateStackSpaceUsage();
		}
	}
}
package mpl.analysis.semantic;

import mpl.syntactic.parts.PFunction;
import mpl.syntactic.parts.PProgram;

public class SemanticAnalyser {
	public PProgram program = null;
	
	public SemanticAnalyser(PProgram program){
		this.program = program;
	}
	
	public void analyze() {
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
package mpl.compiler.asm.sections;

import java.util.ArrayList;

public class AsmSymbols extends AsmSectionData {

	private ArrayList<String> externs = new ArrayList<>();
	private ArrayList<String> globals = new ArrayList<>();
	
	public AsmSymbols() {
		super();
	}

	public void addExtern(String data){
		for(String extern : externs)
			if(extern.equals(data))
				return;
		
		externs.add(data);
	}

	public void addGlobal(String data){
		for(String global : globals)
			if(global.equals(data))
				return;
		
		globals.add(data);
	}

	@Override
	public String createAssemblyCode() {
		if(externs.size() == 0 && globals.size() == 0)
			return "";
		
		StringBuilder code = new StringBuilder();
		
		externs.forEach(extern -> code.append("EXTERN " + extern + System.lineSeparator()));
		globals.forEach(global -> code.append("GLOBAL " + global + System.lineSeparator()));
		
		// Return data without last newline
		return code.toString().substring(0, code.length() - 1);
	}
}
package mpl.parser.syntactic.parts;

import java.util.ArrayList;
import java.util.List;

import mpl.project.ProjectManager;
import mpl.project.Source;
import mpl.utils.ExternGlobalVars;
import mpl.utils.io.Console;

/** PProgram is an abstract syntax tree, individual
 * for every source file. Every source file is compiled
 * separately, and later they're linked into single
 * executable file. */
public class PProgram extends PProgramPart {
	public static final int NO_COMPILE = 0;		// Do not compile this source file
	public static final int COMPILE_FULL = 1;	// Compile to elf format
	
	// Used to import other packages
	public ProjectManager projectManager;
	// Info about this source file
	public Source source;
	// The way we gonna compile this source file
	public int compileType = COMPILE_FULL;
	
	// List of imported sources
	public List<Source> importedSources = new ArrayList<Source>();
	// List of global variables in this source file
	public List<PGlobalVariable> vars = new ArrayList<PGlobalVariable>();
	// List of functions in this source file
	public List<PFunction> functions = new ArrayList<PFunction>();
	// List of struct types defined in this source file
	public List<PStructType> structTypes = new ArrayList<PStructType>();
	
	// When we're accessing global variable from another package
	// we need to extern that global variable name, thus we collect
	// all those names here, and extern in assembly part
	public ExternGlobalVars externGlobalVars = new ExternGlobalVars();
	
	public PProgram(ProjectManager pmgr){
		super("", -1, -1, null);
		projectManager = pmgr;
	}
	
	public void verify(){
		checkForFunctionRedefinition();
		checkForGlobalVariablesRedefinition();
		checkForStructRedeclaration();
		verifyGlobalVars();
		verifyStructs();
		
		// Find out compilation type
		boolean isCalled = false;
		for(PFunction f : functions){
			if(f.isCalled){
				isCalled = true;
				break;
			}
		}
		
		// TODO: Find out if global vars were accessed from other sources
		// If there's any, we must compile this source
		
		// Determine compile type, by function usage
		if(!isCalled){
			compileType = NO_COMPILE;
		}else{
			compileType = COMPILE_FULL;
		}
	}

	private void checkForFunctionRedefinition() {
		for(PFunction i : functions){
			for(PFunction j: functions){
				if(i == j)
					continue;
				
				boolean checkForRedefinition = false;
				
				// Check for redefinition only if two functions
				// have same names
				if(i.name.equals(j.name)){
					// If both functions are struct functions,
					// check if they're same struct's functions
					if(i.isStructFunction() && j.isStructFunction()){
						// TODO: Compare structs
						if(!i.thisParam.struct.name.equals(j.thisParam.struct.name)){
							// If functions are not of same struct type,
							// check for redefinition
							checkForRedefinition = true;
						}
					}/*else{
						// If both are non-struct functions, 
						// check for redefinition
						if(!i.isStructFunction() && !j.isStructFunction()){
							checkForRedefinition = true;
						}
					}*/
				}
				
				if(checkForRedefinition){
					if(i.isDeclaration && j.isDeclaration){
						// If both functions are declaration, then it means redeclaration
						Console.throwError(Console.ERROR_FUNCTION_REDECLARATION, j.sourceFileName, j.lineInSourceCode, j.columnInSourceCode, j.name, i.sourceFileName, i.lineInSourceCode);
					}else{
						if(!i.isDeclaration && !j.isDeclaration){
							// If both functions are definitions, then it means redefinition
							Console.throwError(Console.ERROR_FUNCTION_REDEFINITION, j.sourceFileName, j.lineInSourceCode, j.columnInSourceCode, j.name, i.sourceFileName, i.lineInSourceCode);
						}
					}
				}
			}
		}
	}
	
	private void checkForStructRedeclaration(){
		for(PStructType i : structTypes){
			for(PStructType j : structTypes){
				if(i == j)
					continue;
				
				if(i.name.equals(j.name)){
					Console.throwError(Console.ERROR_STRUCT_REDECLARATION, j.sourceFileName, j.lineInSourceCode, j.columnInSourceCode, i.name, i.sourceFileName, i.lineInSourceCode);
				}
			}
		}
	}
	
	/** Make sure there are no redefinitions in global scope variables. */
	private void checkForGlobalVariablesRedefinition() {
		// Check for redefinitions between global variables
		for(PGlobalVariable var1 : vars){
			for(PGlobalVariable var2 : vars){
				
				// Don't compare against itself
				if(var1 == var2)
					continue;
				
				if(var2.name.equals(var1.name)){
					// If there's one, throw an error
					Console.throwError(Console.ERROR_VAR_REDEFINITION, var2.sourceFileName, var2.lineInSourceCode, var2.columnInSourceCode, var2.name, var1.sourceFileName, var1.lineInSourceCode);
				}
			}
		}
	}

	private void verifyGlobalVars() {
		for (PGlobalVariable var : vars) {
			var.verify();
		}
	}

	private void verifyStructs() {
		for (PStructType struct : structTypes) {
			struct.verify();
		}
	}
	
	public boolean isFunctionDeclared(String name){
		for(PFunction function : functions){
			if(function.name.equals(name))
				if(function.isDeclaration)
					return true;
		}
		return false;
	}
	
	public boolean isFunctionDefined(String name){
		for(PFunction function : functions){
			if(function.name.equals(name))
				if(!function.isDeclaration)
					return true;
		}
		return false;
	}
	
	public boolean isFunctionExisting(String name){
		for(PFunction function : functions){
			if(function.name.equals(name))
				return true;
		}
		return false;
	}
	
	public int isVariableDefined(PVariable variable){
		for(PGlobalVariable var : vars){
			if(var.name.equals(variable.name))
				if(variable.lineInSourceCode > var.lineInSourceCode)
					return var.lineInSourceCode;
		}
		return -1;
	}
	
	public PGlobalVariable findVariable(PVariable variable){
		for(PGlobalVariable var : vars){
			if(var.name.equals(variable.name))
				return var;
		}
		return null;
	}
	
	public PFunction findFunctionDef(String name){
		for(PFunction function : functions){
			if(function.name.equals(name))
				if(!function.isDeclaration)
					return function;
		}
		
		return null;
	}
	
	public PFunction findFunctionDefOrDecl(String name){
		for(PFunction function : functions){
			if(function.name.equals(name))
				return function;
		}
		
		return null;
	}
	
	/** Finds struct type in the program.
	 * 
	 *  @return PStructType for given struct type name, or null
	 *  if struct type was not defined in the program. */
	public PStructType findStructType(String name){
		for(PStructType stType : structTypes){
			if(stType.name.equals(name))
				return stType;
		}
		
		return null;
	}
	
	/** Finds imported source file
	 * 
	 *  @return Source file */
	public Source findImportedSource(String name){
		for(Source src : importedSources)
			if(src.fileNameWithoutExtension.equals(name))
				return src;
		
		return null;
	}

	@Override
	public String getAstCode(String padding) {
		String code = "";
		
		for(PGlobalVariable var : vars)
			code += var.toString() + System.lineSeparator();
		code += System.lineSeparator();
		
		for(PFunction fnc : functions)
			code += fnc.getAstCode("") + System.lineSeparator();
		
		return code;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		// TODO Auto-generated method stub
		return null;
	}
}
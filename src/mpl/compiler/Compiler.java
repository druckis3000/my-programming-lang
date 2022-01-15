package mpl.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.StringJoiner;

import mpl.analysis.semantic.SemanticAnalyser;
import mpl.compiler.asm.AssemblyCodeBuilder;
import mpl.project.Package;
import mpl.project.ProjectManager;
import mpl.project.Source;
import mpl.syntactic.parts.PFunction;
import mpl.utils.Env;
import mpl.utils.ProcessEx;
import mpl.utils.io.Console;
import mpl.utils.io.FileHelper;

public class Compiler {

	private final CompilerOptions compilerOptions;
	private final ProjectManager projectManager;
	
	// Later will be used for finding unused functions
	private PFunction mainFunction;
	
	public Compiler(CompilerOptions compilerOptions, ProjectManager projectManager) {
		this.compilerOptions = compilerOptions;
		this.projectManager = projectManager;
	}
	
	public void compileAll() {
		// Create AST
		createAbstractSyntaxTrees();
		
		// Libraries do not require	 main function, so we
		// don't perform this check if output format is library
		if(compilerOptions.outputFormat != CompilerOptions.COMPILE_FORMAT_LIB){
			boolean defined = checkIfMainIsDefined();
			
			// Throw an error if main function is not defined
			if(!defined)
				Console.throwError(Console.ERROR_UNDEFINED_MAIN_FNC);
		}
				
		// Find using functions
		findUsingFunctions();
		
		// Compile
		compileProjectFiles(projectManager.getBinaryPath(), projectManager.getExecutableName());
	}
	
	public void createAbstractSyntaxTrees() {
		for(Source src : projectManager.getSourceFiles())
			src.createAST();
	}
	
	/** Do semantic analysis for source files inside @param Package
	 * and it's sub packages */
	private void compileToAssemblyFirstStage(Package p){
		for(Source src : p.sourceFiles){
			System.out.println("Compiling: " + p.getFullName() + "/" + src.fileName);
			
			// Do semantic analysis
			SemanticAnalyser analyser = new SemanticAnalyser(src.sourceAST);
			analyser.analyze();
		}
		
		// Do semantic analysis for sub-packages
		for(Package child : p.childPackages)
			compileToAssemblyFirstStage(child);
	}
	
	/** Builds assembly code from AST for source files inside
	 * @param Package and it's sub packages */
	private void compileToAssemblySecondStage(Package p){
		for(Source src : p.sourceFiles){
			// Create assembly code
			AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE = true;
			AssemblyCodeBuilder asmBuilder = new AssemblyCodeBuilder();
			asmBuilder.createAssemblyCode(src.sourceAST, compilerOptions);
			
			// Save assembly code
			String asmFile = p.getDirectory() + File.separator + src.fileNameWithoutExtension + ".asm";
			FileHelper.writeToFile(asmFile, asmBuilder.getAssemblyCode());
		}
		
		// Create assembly code for sub-packages
		for(Package child : p.childPackages)
			compileToAssemblySecondStage(child);
	}
	
	/** Check if main function is defined and checks
	 * for it's redefinition as well */
	private boolean checkIfMainIsDefined(){
		boolean defined = false;
		for(Source src : projectManager.getSourceFiles()){
			if(!defined){
				mainFunction = src.sourceAST.findFunctionDef("main");
				if(mainFunction != null)
					defined = true;
			}else{
				PFunction anotherMainFnc = src.sourceAST.findFunctionDefOrDecl("main");
				if(anotherMainFnc != null){
					// Redefinition of main function
					Console.throwError(Console.ERROR_FUNCTION_REDEFINITION, anotherMainFnc.sourceFileName, anotherMainFnc.lineInSourceCode, anotherMainFnc.columnInSourceCode,
							mainFunction.sourceFileName, mainFunction.lineInSourceCode);
				}
			}
		}
		
		return defined;
	}
	
	private void findUsingFunctions(){
		// TODO: Finish this
	}
	
	private void printAstTrees(Package p){
		/*for(Source src : p.sourceFiles){
			System.out.println(src.sourceAST.getAstCode(""));
			System.out.println();
		}*/
		
		// Print sub-packages
		for(Package child : p.childPackages)
			printAstTrees(child);
	}
	
	/** Compiles project source files into assembly format */
	public void buildAssemblyCode(){
		// Do semantic analysis
		compileToAssemblyFirstStage(projectManager.getRootPackage());
		
		// Create actual assembly code
		compileToAssemblySecondStage(projectManager.getRootPackage());
		
		printAstTrees(projectManager.getRootPackage());
	}
	
	/** Compiles project source files into elf files
	 * 
	 * @return List of compiled files absolute paths */
	public ArrayList<String> compileSourceToElf(String binaryDirPath){
		buildAssemblyCode();
		
		// List of paths to all project elf files
		ArrayList<String> paths = new ArrayList<String>();
		
		// Compile root package and it's sub-packages
		paths.addAll(compileSourceToElf(projectManager.getRootPackage(), binaryDirPath));
		
		return paths;
	}
	
	/** Compile source files in @param Package and it's sub-packages */
	private ArrayList<String> compileSourceToElf(Package p, String binaryDirPath){
		ArrayList<String> paths = new ArrayList<String>();
		
		// Iterate over all source files in package
		for(Source src : p.sourceFiles){
			// Find the assembly file
			String asmFilePath = p.getDirectory() + File.separator + src.fileNameWithoutExtension + ".asm";
			
			// Create elf output file path
			String elfFilePath = binaryDirPath + File.separator + src.fileNameWithoutExtension + "." + Env.getElfExtension();
			paths.add(elfFilePath);
			
			// Call the nasm assembler
			String nasmFormat = "-f" + Env.getElfFormat() + (compilerOptions.outputArch == CompilerOptions.COMPILE_ARCH_64 ? "64" : "32");
			ProcessEx nasm = new ProcessEx(new String[] {"nasm", nasmFormat, asmFilePath, "-o", elfFilePath});
			
			int status = nasm.start();
			if(status != 0)
				System.exit(1);
		}
		
		// Compile sub-packages
		for(Package child : p.childPackages)
			paths.addAll(compileSourceToElf(child, binaryDirPath));
		
		return paths;
	}
	
	/** Comples project into executable file */
	public void compileProjectFiles(String binaryDirPath, String executableName){
		ArrayList<String> elfFiles = compileSourceToElf(binaryDirPath);
		
		// Save settings
		//settings.saveSettings();
		
		// Create gcc compiler parameters
		String gccFormat = "-m" + (compilerOptions.outputArch == CompilerOptions.COMPILE_ARCH_64 ? "64" : "32");
		
		// Create string of all elf files
		StringJoiner elfFilesList = new StringJoiner(" ");
		elfFiles.forEach(path -> elfFilesList.add(path));
		
		// Create executable path
		String executablePath = binaryDirPath + File.separator + executableName + Env.getExecutableExtension();
		
		// Wrap gcc command and it's arguments around elf files
		// to make gcc process command array
		elfFiles.add(0, "-g");
		elfFiles.add(0, gccFormat);
		elfFiles.add(0, "gcc");
		elfFiles.add("-o");
		elfFiles.add(executablePath);

		// Call the gcc compiler
		String[] cmdArray = elfFiles.toArray(new String[elfFiles.size()]);
		ProcessEx gcc = new ProcessEx(cmdArray);
		int status = gcc.start();
		if(status != 0)
			System.exit(1);
	}
	
	public CompilerOptions getOptions() {
		return compilerOptions;
	}
}

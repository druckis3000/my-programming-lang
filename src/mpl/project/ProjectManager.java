package mpl.project;

import java.io.File;
import java.util.ArrayList;
import java.util.StringJoiner;

import mpl.analysis.semantic.SemanticAnalyser;
import mpl.compiler.CompilerOptions;
import mpl.compiler.asm.AssemblyCodeBuilder;
import mpl.syntactic.parts.PFunction;
import mpl.utils.Env;
import mpl.utils.ProcessEx;
import mpl.utils.io.Console;
import mpl.utils.io.FileHelper;

public class ProjectManager {
	public static final int ERROR_NON_EXISTING_DIRECTORY = 1;
	public static final int ERROR_NON_DIRECTORY = 2;
	
	private CompilerOptions compilerOptions;
	
	public File srcPath;
	public File binPath;
	public SettingsFile settings;
	public String executableName;
	
	protected Package rootPackage;
	protected ArrayList<Source> allSourceFiles = new ArrayList<Source>();
	
	private PFunction mainFunction;
	
	public ProjectManager(CompilerOptions options) {
		compilerOptions = options;
	}
	
	/** Opens all project source files
	 * 
	 * @return Error code if there's any, otherwise 0 is returned */
	public int openProject(){
		File directory = compilerOptions.inputFile;
		
		// Check if project directory exists
		if(!directory.exists()){
			// Doesn't exist, print error
			System.err.println("Directory '" + directory.getAbsolutePath() +"' is not existing!");
			return ERROR_NON_EXISTING_DIRECTORY;
		}
		
		// Check if project path is directory
		if(!directory.isDirectory()){
			// If not, print an error
			System.err.println("Project path is not a directory!");
			return ERROR_NON_DIRECTORY;
		}
		
		// Set src and bin paths
		srcPath = new File(directory.getAbsolutePath() + File.separator + "src");
		binPath = new File(directory.getAbsolutePath() + File.separator + "bin");
		
		// Read settings
		settings = new SettingsFile(directory.getAbsolutePath(), this);
		
		// Set executable name
		executableName = directory.getName();
		
		// Create src and bin directories if they don't exist
		if(!srcPath.exists())
			srcPath.mkdir();
		
		if(!binPath.exists())
			binPath.mkdir();
		
		// Create root package
		rootPackage = new Package(this);
		
		// Open all source files from the root package and it's sub packages
		rootPackage.openPackageSourceFiles(srcPath, null, true);
		
		// Find all source files
		findAllSourceFiles(rootPackage);
		
		// Create abstract syntax trees for source files
		createAbstractSyntaxTrees();
		
		// Read files settings
		//settings.readSettings();
		
		// Libraries do not require main function, so we
		// don't perform this check if output format is library
		if(compilerOptions.outputFormat != CompilerOptions.COMPILE_FORMAT_LIB){
			boolean defined = checkIfMainIsDefined();
			// Throw an error if main function is not defined
			if(!defined)
				Console.throwError(Console.ERROR_UNDEFINED_MAIN_FNC);
		}
		
		// Find using functions
		findUsingFunctions();
		
		return 0;
	}
	
	public void createAbstractSyntaxTrees() {
		for(Source src : allSourceFiles)
			src.createAST();
	}
	
	/** Check if main function is defined and checks
	 * for it's redefinition as well */
	private boolean checkIfMainIsDefined(){
		boolean defined = false;
		for(Source src : allSourceFiles){
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
			String asmFile = p.directoryPath + File.separator + src.fileNameWithoutExtension + ".asm";
			FileHelper.writeToFile(asmFile, asmBuilder.getAssemblyCode());
		}
		
		// Create assembly code for sub-packages
		for(Package child : p.childPackages)
			compileToAssemblySecondStage(child);
	}
	
	/** Compiles project source files into assembly format */
	public void buildAssemblyCode(){
		// Do semantic analysis
		compileToAssemblyFirstStage(rootPackage);
		
		// Create actual assembly code
		compileToAssemblySecondStage(rootPackage);
		
		printAstTrees(rootPackage);
	}
	
	/** Compiles project source files into elf files
	 * 
	 * @return List of compiled files absolute paths */
	public ArrayList<String> compileSourceToElf(){
		buildAssemblyCode();
		
		// List of paths to all project elf files
		ArrayList<String> paths = new ArrayList<String>();
		
		// Compile root package and it's sub-packages
		paths.addAll(compileSourceToElf(rootPackage));
		
		return paths;
	}
	
	/** Compile source files in @param Package and it's sub-packages */
	private ArrayList<String> compileSourceToElf(Package p){
		ArrayList<String> paths = new ArrayList<String>();
		
		// Iterate over all source files in package
		for(Source src : p.sourceFiles){
			// Find the assembly file
			String asmFilePath = p.directoryPath + File.separator + src.fileNameWithoutExtension + ".asm";
			
			// Create elf output file path
			String elfFilePath = binPath + File.separator + src.fileNameWithoutExtension + "." + Env.getElfExtension();
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
			paths.addAll(compileSourceToElf(child));
		
		return paths;
	}
	
	/** Comples project into executable file */
	public void compileProjectFiles(){
		ArrayList<String> elfFiles = compileSourceToElf();
		
		// Save settings
		//settings.saveSettings();
		
		// Create gcc compiler parameters
		String gccFormat = "-m" + (compilerOptions.outputArch == CompilerOptions.COMPILE_ARCH_64 ? "64" : "32");
		
		// Create string of all elf files
		StringJoiner elfFilesList = new StringJoiner(" ");
		elfFiles.forEach(path -> elfFilesList.add(path));
		
		// Create executable path
		String executablePath = binPath + File.separator + executableName + Env.getExecutableExtension();
		
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
	
	/** Finds package by specified name */
	public Package findPackage(String fullName){
		// Check if it's a root package
		if(fullName.equals(""))
			return rootPackage;
		
		if(fullName.contains("/")){
			String[] splitPackages = fullName.split("/");
			
			int i = 1;
			Package nextPackage = rootPackage.findPackage(splitPackages[i]);
			while(nextPackage != null){
				rootPackage = nextPackage;
				
				i++;
				if(i == splitPackages.length)
					break;
				
				nextPackage = rootPackage.findPackage(splitPackages[i]);
			}
			
			if(nextPackage == null && i < splitPackages.length)
				return null;
		}else{
			// Package in root src folder
			Package childPackage = rootPackage.findPackage(fullName);
			return childPackage;
		}
		
		return null;
	}
	
	/** Finds all source files inside @param Package and sub packages,
	 * and puts them into the list (allSourceFiles) */
	private void findAllSourceFiles(Package p){
		for(Source src : p.sourceFiles)
			allSourceFiles.add(src);
		
		// Find in sub-packages
		for(Package child : p.childPackages)
			findAllSourceFiles(child);
	}
	
	public void printProjectTree(){
		printSubpackages(rootPackage, "");
	}
	
	private void printSubpackages(Package p, String tabs){
		System.out.println(tabs + p.getFullName());
		
		for(Source f : p.sourceFiles){
			System.out.println(tabs + "\t" + f.fileName);
		}
		
		for(Package pkg : p.childPackages){
			printSubpackages(pkg, tabs + "\t");
		}
	}
}
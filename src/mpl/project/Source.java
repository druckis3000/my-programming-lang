package mpl.project;

import java.io.File;

import mpl.analysis.syntactic.ASTCreator;
import mpl.syntactic.parts.PProgram;

/**
 * Source class that holds all necessary information about source file.
 * It also have {@link mpl.syntactic.parts.PProgram} member which references
 * to the abstract syntax tree of this source file.
 * 
 * In order to create source file, call {@link #open(File, Package)},
 * after source is opened successfully, call {@link #createAST()} to create AST
 */
public class Source {
	public static final int ERROR_NON_EXISTING_FILE = 1;
	
	protected File sourceFile;
	public String fileName;
	public String fileNameWithoutExtension;
	public Package pkg;
	public PProgram sourceAST;
	public boolean modified = false;
	public boolean compiled = false;
	public boolean containsMainFunction = false;

	public Source() {
	}

	/**
	 * @param path - Absolute path to the source file
	 * @param pkg - Package of this source file
	 * 
	 * @return Error code if there's any, 0 is returned if no errors.
	 */
	public int open(File path, Package pkg){
		// Check if file exists
		if(!path.exists()){
			// If not, print an error
			System.err.println("File '" + path.getAbsolutePath() +"' is not existing!");
			return ERROR_NON_EXISTING_FILE;
		}
		
		// Set the source file
		this.sourceFile = path;
		
		// Set the file name
		fileName = path.getName();
		
		// Set the file name without extension
		fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
		
		// Set the package
		this.pkg = pkg;
		
		return 0;
	}
	
	/**
	 * Creates abstract syntax tree for this {@link Source} file
	 */
	public void createAST() {
		String pkgFullName = pkg.getFullName();
		if(pkgFullName.length() > 0) pkgFullName += File.separator;
		
		// Read the source code
		System.out.println("Reading source code from file '" + pkgFullName + fileName + "'...");
		ASTCreator astCreator = new ASTCreator(sourceFile, pkg.projectManager);
		
		// Check if tokens can form an allowable statements
		System.out.println("Creating abstract syntax tree for file '" + pkgFullName + fileName + "'...");
		this.sourceAST = astCreator.createAstTree();
		
		// Set source object in AST
		this.sourceAST.source = this;
	}
	
	@Override
	public String toString(){
		return "source '" + pkg.getFullName() + "/" + fileNameWithoutExtension + "', abstract syntax tree created: " + (this.sourceAST != null);
	}
}

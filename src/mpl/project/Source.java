package mpl.project;

import java.io.File;

import mpl.parser.syntactic.ASTCreator;
import mpl.parser.syntactic.parts.PProgram;

/* Class that holds information about source file.
 * It also has reference to the abstract syntax tree (PProgram)
 * of this source file. */
public class Source {
	public static final int ERROR_NON_EXISTING_FILE = 1;
	
	private File sourceFile;
	public String fileName;
	public String fileNameWithoutExtension;
	public Package pkg;
	public PProgram sourceAST;
	public boolean modified = true;

	public Source() {
	}

	/** Opens source file, and creates Abstract Syntax Tree
	 * 
	 *  @return Error code if there's any, otherwise 0 is returned */
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
	
	public void createAST() {
		// Read the source code
		System.out.println("Reading source code from file '" + pkg.packageName + File.separator + fileName + "'...");
		ASTCreator astCreator = new ASTCreator(sourceFile, pkg.projectManager);
		
		// Check if tokens can form an allowable statements
		System.out.println("Creating abstract syntax tree for file '" + pkg.packageName + File.separator + fileName + "'...");
		this.sourceAST = astCreator.createAstTree();
		
		// Set source object in AST
		this.sourceAST.source = this;
	}
	
	@Override
	public String toString(){
		return "source '" + pkg.getFullName() + "/" + fileNameWithoutExtension + "', abstract syntax tree created: " + (this.sourceAST != null);
	}
}

package mpl.parser.syntactic.parts;

import java.util.ArrayList;

public abstract class PProgramPart {
	/* Used for ANTLR for finding program part by hashcode */
	public static ArrayList<PProgramPart> parts = new ArrayList<PProgramPart>();
	
	public PProgramPart parent = null;
	public String sourceFileName = "";
	public int lineInSourceCode = 0;
	public int columnInSourceCode = 0;
	public int antlrHash = 0;
	
	public PProgramPart(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		this.sourceFileName = sourceFileName;
		this.lineInSourceCode = lineInSourceCode;
		this.columnInSourceCode = columnInSourceCode;
		this.parent = parent;
		
		parts.add(this);
	}
	
	protected int getChildNumber(){
		int n = 0;
		PProgramPart currentParent = parent;
		while(currentParent != null){
			n++;
			currentParent = currentParent.parent;
		}
		return n;
	}
	
	protected PBody findParentBody(){
		PProgramPart current = this;
		while(!(current instanceof PBody)){
			current = current.parent;
			
			if(current instanceof PProgram){
				// If we reached root of the program,
				// then return null (it's probably a global var)
				return null;
			}
		}
		return (PBody)current;
	}
	
	public PFunction findParentFunction(){
		PProgramPart current = this;
		while(!(current instanceof PFunction)){
			current = current.parent;
			if(current instanceof PProgram)
				return null;
		}
		return (PFunction)current;
	}
	
	public PProgram getProgram(){
		PProgramPart current = this;
		while(!(current instanceof PProgram)){
			current = current.parent;
		}
		return (PProgram)current;
	}
	
	public PProgramPart getProgramPartParent(){
		return parent;
	}
	
	protected abstract String getAstCode(String padding);
	
	protected abstract PProgramPart clone(boolean recursive);
}
package mpl.parser.syntactic.parts;

import java.util.ArrayList;

import mpl.project.Source;
import mpl.utils.io.Console;

public class PStructType extends PProgramPart {

	public final String name;
	public Source pkg;
	public ArrayList<PLocalVariable> members = new ArrayList<PLocalVariable>();
	
	private ArrayList<PDataType> primitiveMembersOffset;
	
	private boolean verified = false;
	
	public PStructType(String name, String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
		
		this.name = name;
	}

	public void verify() {
		// Avoid multiple verifications
		if(verified) return;
		
		// Check if there is no members with same names,
		// i.e. var redeclaration
		for(PLocalVariable i : members){
			for(PLocalVariable j : members){
				if(i == j)
					continue;
				
				if(i.name.equals(j.name)){
					// If there's one, throw an error
					Console.throwError(Console.ERROR_VAR_REDECLARATION, j.sourceFileName, j.lineInSourceCode, j.columnInSourceCode, i.name, i.sourceFileName, i.lineInSourceCode);
				}
			}
		}
		
		// Mark this struct type as already verified
		verified = true;
	}
	
	public PLocalVariable findMember(String name){
		for(PLocalVariable member : members){
			if(member.name.equals(name))
				return member;
		}
		
		return null;
	}

	public PLocalVariable getMember(int i) {
		return members.get(i);
	}
	
	public ArrayList<PDataType> getPrimitiveMembersListRecursive(){
		if(primitiveMembersOffset == null){
			primitiveMembersOffset = new ArrayList<PDataType>();
			createPrimitiveMembersListRecursive();
		}
		
		return primitiveMembersOffset;
	}
	
	private void createPrimitiveMembersListRecursive(){
		for(PLocalVariable member : members){
			if(member.dataType.plain() == PDataType.STRUCT){
				if(member.dataType.isArray()){
					for(int i=0; i<member.arraySize; i++){
						primitiveMembersOffset.addAll(member.struct.getPrimitiveMembersListRecursive());
					}
				}else{
					primitiveMembersOffset.addAll(member.struct.getPrimitiveMembersListRecursive());
				}
			}else{
				if(member.dataType.isArray()){
					for(int i=0; i<member.arraySize; i++){
						primitiveMembersOffset.add(member.dataType);
					}
				}else{
					primitiveMembersOffset.add(member.dataType);
				}
			}
		}
	}
	
	public int getSizeInBytes() {
		int size = 0;
		
		for(PLocalVariable member : members){
			size += member.getSizeInBytes();
		}
	//	System.out.println("Total size of '" + this.toString() + "': " + size);
		
		return size;
	}
	
	/** Get offset (in bytes) of a member of this struct */
	public int getMemberOffset(PLocalVariable member){
		int offset = 0;
		
		for(PLocalVariable m : members){
			if(m == member)
				break;
			
			offset += m.getSizeInBytes();
		}
		
		return offset;
	}
	
	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		return "struct " + name + " (" + members.size() + " vars)";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		// Non cloneable
		return this;
	}
}
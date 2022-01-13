package mpl.syntactic.parts;

import java.util.ArrayList;

import mpl.syntactic.parts.PVariable.PScope;
import mpl.utils.io.Console;

public class PArrayInitializer extends PProgramPart {
	public PVarAccessor arrayVar = null;
	public PBraceGroup group = null;
	
	public ArrayList<PAssignmentStatement> assignmentSts = new ArrayList<PAssignmentStatement>();
	
	public PArrayInitializer(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void verify(){
		// Iterate over elements in brace group
		// and find out element's target, if it's
		// array or struct, then we expect element
		// to be instanceof PBraceGroup, if it's not,
		// throw an error. If target is neither struct nor
		// array, then we expect element to be PExpression
		// if it's not, throw an error. If everything is okay
		// create PAssignment statement, or PStruct/ArrayInitializer
		// depending on target's type
		
		for(int i=0; i<group.elements.size(); i++){
			PProgramPart element = group.elements.get(i);
			
			// Clone parent var accessor
			PVarAccessor arrayElementAccess = (PVarAccessor)arrayVar.clone(true);
			
			// Create array element index expression
			PExpression arrayIndex = new PExpression(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, arrayVar.getGrandChild());
			arrayIndex.expression.add(new PIntegerLiteral(i, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, arrayIndex));
			arrayIndex.verify();
			arrayElementAccess.getGrandChild().accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
			arrayElementAccess.getGrandChild().arrayIndex = arrayIndex;
			
			// Check if target array element type is struct
			if(arrayElementAccess.getDataType().isStruct()){
				// If it is, check if element is PBraceGroup
				if(element instanceof PBraceGroup){
					// Create PStructInitializer
					PStructInitializer structInitializer = new PStructInitializer(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
					structInitializer.structVar = (PVarAccessor)arrayElementAccess.clone(true);
					structInitializer.group = (PBraceGroup)element;
					structInitializer.verify();
					
					// Add sub-initializer assignment statements to the parent
					assignmentSts.addAll(structInitializer.assignmentSts);
				}else{
					// Element is not a PBraceGroup, throw an error
					Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, getElementDataType(element), arrayElementAccess.getDataTypeUnmodified());
				}
			}else{
				// Neither struct, nor array.
				// Check if element is PBraceGroup
				if(element instanceof PBraceGroup){
					// If it is, throw an error
					Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, "{...}", arrayElementAccess.getDataTypeUnmodified());
				}else{
					// Cast element to PExpression
					PExpression exp = (PExpression)element;
					
					// Check if struct var is in the global scope
					if(arrayVar.target.scope == PScope.GLOBAL){
						// If it is, then disallow non-constant values
						// in the global initializer
						if(exp.contains(PFunctionCall.class, PVarAccessor.class)){
							Console.throwError(Console.ERROR_NON_CONST_INITIALIZER, sourceFileName, lineInSourceCode, columnInSourceCode);
						}
					}

					// Create PAssignmentStatement
					PAssignmentStatement assignment = new PAssignmentStatement(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
					assignment.variable = (PVarAccessor)arrayElementAccess.clone(true);
					assignment.expression = exp;
					assignment.checkIfVariableDefined = false;
					assignment.verify();
					
					// Add to assignments list
					assignmentSts.add(assignment);
				}
			}
		}
		
		// Set array size for the variable
		if(arrayVar.getGrandChild().target.arraySize == 0)
			arrayVar.getGrandChild().target.arraySize = group.elements.size();
	}
	
	private PDataType getElementDataType(PProgramPart element){
		if(element instanceof PExpression){
			PExpression exp = (PExpression)element;
			return exp.verify();
		}
		
		return PDataType.VOID;
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		return arrayVar + " = " + group.toString();
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PArrayInitializer arrayInitializer = new PArrayInitializer(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			arrayInitializer.arrayVar = arrayVar == null ? null : (PVarAccessor)arrayVar.clone(true);
			arrayInitializer.group = group == null ? null : (PBraceGroup)group.clone(true);
		}else{
			arrayInitializer.arrayVar = arrayVar == null ? null : (PVarAccessor)arrayVar;
			arrayInitializer.group = group == null ? null : (PBraceGroup)group;
		}
		
		return arrayInitializer;
	}
}
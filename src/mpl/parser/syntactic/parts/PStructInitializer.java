package mpl.parser.syntactic.parts;

import java.util.ArrayList;
import java.util.StringJoiner;

import mpl.utils.io.Console;

public class PStructInitializer extends PProgramPart {
	public PVarAccessor structVar = null;
	/* If group is null, it's a pointer initialization */
	public PBraceGroup group = null;
	
	public ArrayList<PAssignmentStatement> assignmentSts = new ArrayList<PAssignmentStatement>();
	
	public PStructInitializer(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	/** This code is only for non-global variables */
	public void verify(){
		if(group == null){
			// It's a pointer initialization, e.g. Struct *s = new Struct;
			// Verify struct that we are defining
			structVar.verify(false);
			
			// Create malloc function call
			PFunctionCall fCall = new PFunctionCall(sourceFileName, lineInSourceCode, columnInSourceCode, this);
			fCall.functionName = "_c_malloc";
			PExpression arg0 = new PExpression(sourceFileName, lineInSourceCode, columnInSourceCode, fCall);
			arg0.expression.add(new PIntegerLiteral(structVar.target.struct.getSizeInBytes(), sourceFileName, lineInSourceCode, columnInSourceCode, arg0));
			fCall.arguments.add(arg0);
			
			// Create type cast
			PTypeCast typeCast = new PTypeCast(sourceFileName, lineInSourceCode, columnInSourceCode, this);
			typeCast.structType = structVar.target.struct;
			typeCast.pointer = true;
			fCall.typeCast = typeCast;
			
			// Create assignment statement
			PAssignmentStatement assign = new PAssignmentStatement(sourceFileName, lineInSourceCode, columnInSourceCode, this);
			assign.variable = structVar;
			PExpression fCallExp = new PExpression(sourceFileName, lineInSourceCode, columnInSourceCode, assign);
			fCallExp.expression.add(fCall);
			assign.expression = fCallExp;
			
			// Add assignment statement to the assignment statements list
			assign.verify();
			assignmentSts.add(assign);
		}else{
			// It's a plain struct initialization, e.g. Struct s = {...};
			
			if(group.size() > structVar.getGrandChild().target.struct.members.size()){
				Console.throwError(Console.ERROR_TOO_MANY_VALUES, sourceFileName, lineInSourceCode, columnInSourceCode, "values", "struct initializer");
			}else if(group.size() < structVar.getGrandChild().target.struct.members.size()){
				Console.throwError(Console.ERROR_TOO_FEW_VALUES, sourceFileName, lineInSourceCode, columnInSourceCode, "values", "struct initializer");
			}
			
			// Verify struct that we are defining
			structVar.verify(false);
			
			// Iterate over elements in brace group
			// and find out element's target, if it's
			// array or struct, then we expect element
			// to be instanceof brace group, if it's not
			// throw an error. If target is non struct or
			// array, then we expect element to be PExpression
			// if it's not, throw an error. If everything is okay
			// create PAssignment statement, or PStruct/ArrayInitializer
			// depending on target's type
			
			for(int i=0; i<group.elements.size(); i++){
				PProgramPart element = group.elements.get(i);
				
				// Find member of parent struct
				PLocalVariable member = structVar.getGrandChild().target.struct.getMember(i);
				
				// Clone parent struct accessor
				PVarAccessor structMemberAccess = (PVarAccessor)structVar.clone(true);
				
				// Create struct member accessor
				PVarAccessor memberAccess = new PVarAccessor(sourceFileName, lineInSourceCode, columnInSourceCode, this);
				memberAccess.target = member;
				memberAccess.parent = structMemberAccess.getGrandChild();
				structMemberAccess.getGrandChild().child = memberAccess;
				
				// Check if target member type is array
				if(member.dataType.isArray()){
					// If it is, check if element is PBraceGroup
					if(element instanceof PBraceGroup){
						// Create PArrayInitializer
						PArrayInitializer arrayInitializer = new PArrayInitializer(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
						arrayInitializer.arrayVar = (PVarAccessor)structMemberAccess.clone(true);
						arrayInitializer.group = (PBraceGroup)element;
						arrayInitializer.verify();
						
						// Add sub-initializer assignment statements to the parent
						assignmentSts.addAll(arrayInitializer.assignmentSts);
					}else{
						// Cast element to PExpression
						PExpression exp = (PExpression)element;
						PDataType expDataType = exp.verify();
						
						// Check if element expresion is of array data type
						if(expDataType.isArray()){
							// If it is, we can assign it
							// Create PAssignmentStatement
							PAssignmentStatement assignment = new PAssignmentStatement(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
							assignment.variable = (PVarAccessor)structMemberAccess.clone(true);
							assignment.expression = exp;
							assignment.verify();
							
							// Add assignment to assignments list
							assignmentSts.add(assignment);
						}else{
							// Expression is not of array type, throw an error
							Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, expDataType, structMemberAccess.getDataTypeUnmodified());
						}
					}
				}else{
					// Target memeber is not an array, check if it's a struct
					if(member.dataType.isStruct()){
						// If it is, check if element is PBraceGroup
						if(element instanceof PBraceGroup){
							// Create another PStructInitializer
							PStructInitializer structInitializer = new PStructInitializer(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
							structInitializer.structVar = (PVarAccessor)structMemberAccess.clone(true);
							structInitializer.group = (PBraceGroup)element;
							structInitializer.verify();
							
							// Add sub-initializer assignment statements to the parent
							assignmentSts.addAll(structInitializer.assignmentSts);
						}else{
							// Element is not a PBraceGroup, check if it's
							// a variable of struct type
							PExpression exp = (PExpression)element;
							PDataType expDataType = exp.verify();
							
							if(expDataType.isStruct()){
								// Create PAssignmentStatement
								PAssignmentStatement assignment = new PAssignmentStatement(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
								assignment.variable = (PVarAccessor)structMemberAccess.clone(true);
								assignment.expression = exp;
								assignment.verify();
								
								// Add assignment to assignments list
								assignmentSts.add(assignment);
							}else{
								// Expression type is not of a struct
								Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, expDataType, structMemberAccess.getDataTypeUnmodified());
							}
						}
					}else{
						// Target member is not a struct, check if
						// element is not PBraceGroup
						if(!(element instanceof PBraceGroup)){
							PExpression exp = (PExpression)element;
							
							// Create PAssignmentStatement
							PAssignmentStatement assignment = new PAssignmentStatement(sourceFileName, element.lineInSourceCode, element.columnInSourceCode, this);
							assignment.variable = (PVarAccessor)structMemberAccess.clone(true);
							assignment.expression = exp;
							assignment.verify();
							
							// Add assignment to assignments list
							assignmentSts.add(assignment);
						}else{
							// It is, throw an error
							Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, element.lineInSourceCode, element.columnInSourceCode, "{...}", structMemberAccess.getDataTypeUnmodified());
						}
					}
				}
			}
		}
	}
	
	@Override
	protected String getAstCode(String padding) {
		StringJoiner code = new StringJoiner("\n");
		
		for(PAssignmentStatement assignment : assignmentSts){
			code.add(padding + assignment);
		}
		
		return code.toString();
	}

	@Override
	public String toString() {
		if(group == null){
			return structVar.toString() + " = " +
					"new " + structVar.target.struct.name;
		}else{
			StringJoiner elements = new StringJoiner(", ");
			this.group.elements.forEach(el -> elements.add(el.toString()));
			
			return "{" + elements + "}";
		}
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PStructInitializer struct = new PStructInitializer(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			struct.structVar = structVar == null ? null : (PVarAccessor)structVar.clone(true);
			struct.group = group == null ? null : (PBraceGroup)group.clone(true);
			
			for(PAssignmentStatement assignmentSt : assignmentSts)
				struct.assignmentSts.add((PAssignmentStatement)assignmentSt.clone(true));
		}else{
			struct.structVar = structVar;
			struct.group = group;
			struct.assignmentSts.addAll(assignmentSts);
		}
		
		return struct;
	}
}

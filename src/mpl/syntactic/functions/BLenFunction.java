package mpl.syntactic.functions;

import java.util.List;

import mpl.syntactic.parts.PBody;
import mpl.syntactic.parts.PDataType;
import mpl.syntactic.parts.PExpression;
import mpl.syntactic.parts.PFunctionParameter;
import mpl.syntactic.parts.PIntegerLiteral;
import mpl.syntactic.parts.PProgramPart;
import mpl.syntactic.parts.PVarAccessor;
import mpl.utils.io.Console;

public class BLenFunction extends PBuiltInFunction {

	public BLenFunction() {
		this.name = "len";
		
		PFunctionParameter arrayParameter = new PFunctionParameter(PDataType.ARRAY_VOID, "1", sourceFileName, -1, -1, this);
		params.params.add(arrayParameter);
	}

	@Override
	public PProgramPart eval(String sourceFile, int lineInCode, int colInCode, PBody parentBody, List<PExpression> args) {
		if(args.size() > params.params.size()){
			Console.throwError(Console.ERROR_TOO_MANY_VALUES, sourceFile, lineInCode, colInCode, "arguments", "'" + name + "' function call");
		}else if(args.size() < params.params.size()){
			Console.throwError(Console.ERROR_TOO_FEW_VALUES, sourceFile, lineInCode, colInCode, "arguments", "'" + name + "' function call");
		}
		
		// Get array argument expression
		PExpression exp = args.get(0);
		PDataType dt = exp.verify();
		
		// Make sure that argument is constant
		if(exp.expression.size() > 1){
			Console.throwError(Console.ERROR_NON_CONST_EXP, sourceFile, lineInCode, colInCode);
		}
		
		// Check if data type is applicable to first param
		if(!PDataType.isOperandValid(params.params.get(0).dataType, dt)){
			Console.throwError(Console.ERROR_NON_APPLICABLE_ARG, sourceFile, lineInCode, colInCode, name, 1, dt);
		}
		
		// Evaluate function
		PVarAccessor var = (PVarAccessor)exp.expression.get(0);
		long size = var.getGrandChild().target.arraySize;
		
		PIntegerLiteral integer = new PIntegerLiteral(size, sourceFile, lineInCode, colInCode, parentBody);
		return integer;
	}
}
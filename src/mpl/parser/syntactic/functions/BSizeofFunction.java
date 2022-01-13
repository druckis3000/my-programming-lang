package mpl.parser.syntactic.functions;

import java.util.List;

import mpl.compiler.asm.AsmCommon;
import mpl.parser.syntactic.parts.PBody;
import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PFunctionParameter;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.utils.io.Console;

public class BSizeofFunction extends PBuiltInFunction {

	public BSizeofFunction() {
		this.name = "sizeof";
		
		// Parameter is void, since anything can be passed to sizeof
		PFunctionParameter voidParameter = new PFunctionParameter(PDataType.VOID, "1", sourceFileName, -1, -1, this);
		params.params.add(voidParameter);
	}

	@Override
	public PProgramPart eval(String sourceFile, int lineInCode, int colInCode, PBody parentBody, List<PExpression> args) {
		PIntegerLiteral sizeof = new PIntegerLiteral(0, sourceFile, lineInSourceCode, columnInSourceCode, parentBody);
		
		// Check if number of args is no more than 1
		if(args.size() > 1){
			Console.throwError(Console.ERROR_TOO_MANY_VALUES, sourceFile, lineInSourceCode, columnInSourceCode, "arguments", "'sizeof' function call");
		}
		
		// Check if there's at least one arg
		if(args.size() < 1){
			Console.throwError(Console.ERROR_TOO_FEW_VALUES, sourceFile, lineInSourceCode, columnInSourceCode, "arguments", "'sizeof' function call");
		}
		
		PExpression firstArg = args.get(0);
		
		// Allow only constant values in sizeof function call
		if(firstArg.expression.size() > 1){
			Console.throwError(Console.ERROR_NON_CONST_EXP, firstArg.sourceFileName, firstArg.lineInSourceCode, firstArg.columnInSourceCode);
		}
		
		// Verify first argument expression
		firstArg.verify();
		
		// Find size of the operand
		PProgramPart firstOp = firstArg.firstElement();
		long sizeInBytes = AsmCommon.findSizeInBytes(firstOp);
		sizeof.value = sizeInBytes;
		
		return sizeof;
	}
}
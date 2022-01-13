package mpl.parser.syntactic.functions;

import java.util.List;

import mpl.parser.syntactic.parts.PAsmCode;
import mpl.parser.syntactic.parts.PBody;
import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PFunctionParameter;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.utils.io.Console;

public class BAsmFunction extends PBuiltInFunction {

	public BAsmFunction() {
		this.name = "__asm__";
		
		// First param - assembly code
		PFunctionParameter stringParameter = new PFunctionParameter(PDataType.STRING, "1", sourceFileName, -1, -1, this);
		params.params.add(stringParameter);
		
		// Second param, optional - variable passed to assembly
	//	PFunctionParameter voidParameter = new PFunctionParameter(PDataType.VOID, "1", sourceFileName, -1, -1, this);
	//	params.params.add(voidParameter);
	}

	@Override
	public PProgramPart eval(String sourceFile, int lineInCode, int colInCode, PBody parentBody, List<PExpression> args) {
		PAsmCode asmCode = new PAsmCode(sourceFile, lineInSourceCode, columnInSourceCode, parentBody);
		
		// Check if number of args is no more than 2
		if(args.size() > 2){
			Console.throwError(Console.ERROR_TOO_MANY_VALUES, asmCode.sourceFileName, asmCode.lineInSourceCode, asmCode.columnInSourceCode, "arguments", "'__asm__' function call");
		}
		
		// Check if there's at least one arg
		if(args.size() < 1){
			Console.throwError(Console.ERROR_TOO_FEW_VALUES, asmCode.sourceFileName, asmCode.lineInSourceCode, asmCode.columnInSourceCode, "arguments", "'__asm__' function call");
		}
		
		// First argument must be string literal - assembly code
		PExpression firstArg = args.get(0);
		firstArg.verify();
		/*if(firstArg.verify() != PDataType.STRING){
			// If it's not, throw an error
			Console.throwError(Console.ERROR_NON_APPLICABLE_ARG, asmCode.sourceFileName, asmCode.lineInSourceCode, asmCode.columnInSourceCode, "__asm__", 1, args[0]);
		}*/
		
		// Set assembly code in PAsmCode
		asmCode.code = ((PStringLiteral)firstArg.firstElement()).value;
		
		// Unquote asm code
		asmCode.code = asmCode.code.substring(1);
		asmCode.code = asmCode.code.substring(0, asmCode.code.length() - 1);
		
		if(args.size() > 1){
			// Set operand in PAsmCode
			PExpression secondArg = args.get(1);
			asmCode.operand = secondArg;
		}
		
		// Verify assembly code
		asmCode.verify();
		
		return asmCode;
	}

}

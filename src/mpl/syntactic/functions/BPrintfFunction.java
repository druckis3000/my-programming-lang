package mpl.syntactic.functions;

import java.util.List;

import mpl.syntactic.parts.PBody;
import mpl.syntactic.parts.PDataType;
import mpl.syntactic.parts.PExpression;
import mpl.syntactic.parts.PFunctionCall;
import mpl.syntactic.parts.PProgramPart;
import mpl.utils.io.Console;

public class BPrintfFunction extends PBuiltInFunction {

	public BPrintfFunction() {
		this.name = "printf";
	}

	@Override
	public PProgramPart eval(String sourceFile, int lineInCode, int colInCode, PBody parentBody, List<PExpression> args) {
		if(args.size() == 0) {
			Console.throwError(Console.ERROR_TOO_FEW_VALUES, sourceFile, lineInCode, colInCode, "arguments", "'" + name + "' function call");
		}
		
		PExpression firstArg = args.get(0);
		PDataType firstDataType = firstArg.verify();
		if(firstArg.verify() != PDataType.STRING) {
			Console.throwError(Console.ERROR_NON_APPLICABLE_ARG, sourceFile, lineInCode, colInCode, name, 1, firstDataType);
		}
		
		// Create function call to _c_printf
		PFunctionCall fCall = new PFunctionCall(sourceFile, lineInCode, colInCode, parentBody);
		fCall.functionName = "_c_printf";
		fCall.arguments.addAll(args);
		fCall.verify();
		
		return fCall;
	}

}

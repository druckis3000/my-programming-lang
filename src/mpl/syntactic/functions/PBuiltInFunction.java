package mpl.syntactic.functions;

import java.util.ArrayList;
import java.util.List;

import mpl.syntactic.parts.PBody;
import mpl.syntactic.parts.PDataType;
import mpl.syntactic.parts.PExpression;
import mpl.syntactic.parts.PFunction;
import mpl.syntactic.parts.PFunctionParameter;
import mpl.syntactic.parts.PProgramPart;

public abstract class PBuiltInFunction extends PFunction {
	private static final ArrayList<PBuiltInFunction> builtInFunctions = new ArrayList<PBuiltInFunction>();
	
	static {
		// Initialize built in functions
		builtInFunctions.add(new BLenFunction());
		builtInFunctions.add(new BPrintfFunction());
		builtInFunctions.add(new BAsmFunction());
		builtInFunctions.add(new BSizeofFunction());
	}
	
	public PBuiltInFunction() {
		super("runtime", -1, -1, null);
	}
	
	public abstract PProgramPart eval(String sourceFile, int lineInCode, int colInCode, PBody parentBody, List<PExpression> args);
	
	public static PBuiltInFunction findBuiltInFunction(String name, List<PExpression> args){
		for(PBuiltInFunction fnc : builtInFunctions){
			// Find function that matches the name
			if(fnc.name.equals(name)){
				// Now check if args matches params
				boolean valid = true;
				
				// Difference between args and params size, skip this function
				// TODO: Ignore params vs args size check, due to optional parameters
			//	if(fnc.params.params.size() != args.size()){
			//		break;
			//	}
				
				for(int i=0; i<fnc.params.params.size(); i++){
					PFunctionParameter param = fnc.params.params.get(i);
					if(!PDataType.isOperandValid(param.dataType, args.get(i).verify())){
						valid = false;
						break;
					}
				}
				
				// If matching function is found, return it
				if(valid){
					return fnc;
				}
			}
		}
		
		// No matching function were found, return null
		return null;
	}

	public static boolean isBuiltInFunction(String functionName) {
		for(PBuiltInFunction fnc : builtInFunctions)
			if(fnc.name.equals(functionName))
				return true;
		
		return false;
	}
}

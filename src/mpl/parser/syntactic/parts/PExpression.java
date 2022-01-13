package mpl.parser.syntactic.parts;

import java.util.ArrayList;
import java.util.StringJoiner;

import mpl.parser.syntactic.functions.PBuiltInFunction;
import mpl.parser.syntactic.parts.POperator.POperatorType;
import mpl.utils.io.Console;

public class PExpression extends PProgramPart {
	public ArrayList<PProgramPart> expression = new ArrayList<PProgramPart>();
	public PDataType expDataType = null;
	public PStructType expStructType = null;
	public PTypeCast typeCast = null;
	public boolean pointerExpression = false;
	
	protected boolean verified = false;
	
	public PExpression(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent){
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}

	public PDataType verify(PDataType targetType){
		// Avoid multiple verifications of the same expression
		if(verified) return expDataType;
		
		for(int i=0; i<expression.size(); i += 2){ // Increment by two, because we are skipping operators
			PProgramPart operand = expression.get(i);
			POperator operator = expression.size() > i+1 ? (POperator)expression.get(i+1) : null;
			PDataType operandDataType = null;
			PStructType operandStructType = null;
			
			if(operand instanceof PVarAccessor){
				// Operand is variable, verify it
				PVarAccessor var = (PVarAccessor)operand;
				var.verify(true);
				
				// Set operand data type to the type of variable
				operandDataType = var.getDataType();
				// Set operand struct type to the struct of variable
				operandStructType = var.getGrandChild().target.struct;
			}else if(operand instanceof PFunctionCall){
				PFunctionCall fCall = (PFunctionCall)operand;
				
				// TODO: Finish here with built-in functions
				
				// Check if function is built-in
				if(fCall.isBuiltInFunction()){
					// If it is, 
					// Replace function call with the returned value
					PBuiltInFunction result = PBuiltInFunction.findBuiltInFunction(fCall.functionName, fCall.arguments);
					expression.set(i, result.eval(fCall.sourceFileName, fCall.lineInSourceCode, fCall.columnInSourceCode, findParentBody(), fCall.arguments));
					
					// And iterate again at same index
					i -= 2;
					continue;
				}
				
				// Verify function call
				fCall.verify();
				
				// Set operand data type to the type of returned value
				operandDataType = fCall.getReturnDataType();
				// Set operand struct type to the type of returned struct
				operandStructType = fCall.getReturnStructType();
			}else if(operand instanceof PStringLiteral){
				// Set operand data type to string
				operandDataType = PDataType.STRING;
			}else if(operand instanceof PCharLiteral){
				// Disallow arithmetic on character literals
				if(i > 0){
					Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, operator.lineInSourceCode, operator.columnInSourceCode, expression.get(i-1), PDataType.STRUCT);
				}else if(operator != null){
					Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, operator.lineInSourceCode, operator.columnInSourceCode, expression.get(i+1), PDataType.STRUCT);
				}
				
				PCharLiteral character = (PCharLiteral)operand;
				operandDataType = character.getDataType();
			}else if(operand instanceof PIntegerLiteral){
				PIntegerLiteral intLiteral = (PIntegerLiteral)operand;
				operandDataType = intLiteral.getDataType();
			}else if(operand instanceof PHexLiteral){
				PHexLiteral hexLiteral = (PHexLiteral)operand;
				operandDataType = hexLiteral.getDataType();
			}else if(operand instanceof PExpression){
				// If operand is parentheses expression, verify it
				PExpression exp = (PExpression)operand;
				exp.verify(targetType);
				
				// Set operand data type to the parentheses expression data type
				operandDataType = exp.expDataType;
				// Set operand struct type to the parentheses expression struct type
				operandStructType = exp.expStructType;
			}else if(operand instanceof PNewOperand){
				// Verify new struct type
				PNewOperand newOp = (PNewOperand)operand;
				newOp.verify();
				
				// Set operand data type to struct pointer
				operandDataType = PDataType.STRUCT.asPointer();
				// Set operand struct type to new struct type
				operandStructType = newOp.structType;
			}else if(operand instanceof PNullLiteral){
				// Set operand data type to null
				operandDataType = PDataType.NULL;
			}else if(operand instanceof PBooleanLiteral){
				// TODO: Do not replace boolean literal with integer literal,
				// since it doesn't work in conditions, when comparing bool var
				// to integer
				// Replace boolean operand with an integer with value either one or zero
				PBooleanLiteral bool = (PBooleanLiteral)operand;
				expression.set(i, bool.value ? PIntegerLiteral.ONE : PIntegerLiteral.ZERO);
				
				// Reiterate
				i -= 2;
				continue;
			}else if(operand instanceof PAssignmentStatement){
				// This is assignment inside expression
				
				// Verify assignment statement
				PAssignmentStatement assignment = (PAssignmentStatement)operand;
				assignment.verify();
				
				// Set operand data type to the type of assignment variable
				operandDataType = assignment.variable.getDataType();
				// Set operand struct type to the struct type of assignment variable
				operandStructType = assignment.variable.getGrandChild().target.struct;
			}else if(operand instanceof PRegister){
				// Register will be 32-bit integer
				operandDataType = PDataType.INT;
			}else if(operand instanceof PStackElement){
				// Stack element will be 32-bit integer
				operandDataType = PDataType.INT;
			}else{
				System.err.println("Invalid operand! " + operand);
				new Exception().printStackTrace();
				System.exit(-1);
			}
			
			// Disallow arithmetic on structs, strings, booleans, nulls, arrays, slices
			// except pointers, since you can add to address
			if(!operandDataType.isPointer()) {
				if(operandDataType.isStruct() || operandDataType == PDataType.STRING || operandDataType == PDataType.BOOL || operandDataType == PDataType.NULL
						|| operandDataType.isArray() || operandDataType.isSlice()){
					if(i > 0){
						Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, operator.lineInSourceCode, operator.columnInSourceCode, expression.get(i-1), operandDataType);
					}else if(operator != null){
						Console.throwError(Console.ERROR_UNDEFINED_OPERATOR, sourceFileName, operator.lineInSourceCode, operator.columnInSourceCode, expression.get(i+1), operandDataType);
					}
				}
			}
			
			if(i == 0){
				// If that's a first iteration, then set the exp data type
				expDataType = operandDataType;
				// And struct type aswell
				expStructType = operandStructType;
				
				// If exp data type is integral and smaller than target
				// type, then replace exp data type with target type
				if(targetType != null){
					if(targetType.isPlain() && expDataType.isPlain()){
						if(targetType.isIntegral() && expDataType.isIntegral()){
							if(targetType.getSizeInBytes() > expDataType.getSizeInBytes()){
								expDataType = targetType;
							}
						}
					}
				}
				
				// If this is a pointer expression, i.e. *(...)
				// then it's data type will equal to first operand
				// data type, without any pointers
				if(pointerExpression) {
					if(expDataType.plain().equals(PDataType.STRUCT)) {
						// Struct pointer is just a 4-byte integer
						expDataType = PDataType.INT;
					}else{
						expDataType = expDataType.plain();
					}
				}
				
				// If there's a type cast, verify it
				if(typeCast != null){
					typeCast.verify(expDataType);
					// If verification was succcessful, set
					// expDataType to the typeCast
					expDataType = typeCast.getDataType();
				}
			}else{
				// If that's not a first iteration, then
				// make sure that operand is valid for the expression
				if(!PDataType.isOperandValid(expDataType, operandDataType)){
					Console.throwError(Console.ERROR_TYPE_MISMATCH, sourceFileName, operand.lineInSourceCode, operand.columnInSourceCode, operandDataType, expDataType);
				}
			}
		}
		
		// Optimise expression
		optimise();
		
		verified = true;
		return expDataType;
	}
	
	public PDataType verify(){
		return verify(null);
	}
	
	private void optimise(){
		for(int i=0; i<expression.size(); i++){
			PProgramPart op = expression.get(i);
			
			if(op instanceof PExpression){
				// It's a parentheses expression, optimise it
				PExpression paren = (PExpression)op;
				paren.optimise();
				
				// If there's only one operand left in parentheses exp,
				// and without a type cast, move it out from parentheses
				if(paren.expression.size() == 1){
					if(paren.expression.get(0) instanceof POperand){
						POperand operand = (POperand)paren.expression.get(0);
						
						if(operand.typeCast == null){
							// Operand doesn't have type cast, thus we can move it out
							// form parentheses, but if parentheses have type cast, then
							// add that type cast to the operand
							if(paren.typeCast != null){
								operand.typeCast = paren.typeCast;
							}

							// Replace parentheses exp with it's first operand
							expression.set(i, operand);
							
							// And iterate at previous operator, if there's one
							if(i > 1){
								i -= 2;
								continue;
							}
						}else{
							// Operand have type cast. We can move it out only if
							// parentheses expression doesn't have type cast
							if(paren.typeCast == null){
								// Parentheses doesn't have type cast, let's move it out
								expression.set(i, operand);
								
								// And iterate at previous operator, if there's one
								if(i > 1){
									i -= 2;
									continue;
								}
							}
						}
					}
				}
			}else if(op instanceof POperator){
				POperator operator = (POperator)op;
				POperator previousOperator = i > 1 ? (POperator)expression.get(i-2) : null;
				
				if(previousOperator != null){
					if(operator.type.precedence <= previousOperator.type.precedence){
						// Current operator precedence is lower than previous, thus
						// we can't evaluate this arithmetic
						continue;
					}
				}
				
				// If lhs and rhs of the operator is int or hex literals, evaluate them
				PProgramPart lhs = expression.get(i - 1);
				PProgramPart rhs = expression.get(i + 1);
				
				if(lhs instanceof PIntegerLiteral || lhs instanceof PHexLiteral){
					if(rhs instanceof PIntegerLiteral || rhs instanceof PHexLiteral){
						long lhsValue = lhs instanceof PIntegerLiteral ? ((PIntegerLiteral)lhs).value : ((PHexLiteral)lhs).value;
						long rhsValue = rhs instanceof PIntegerLiteral ? ((PIntegerLiteral)rhs).value : ((PHexLiteral)rhs).value;
						long result = eval(lhsValue, operator.type, rhsValue);
						
						// Create new literal
						PIntegerLiteral resultLiteral = new PIntegerLiteral(result, sourceFileName, operator.lineInSourceCode, operator.columnInSourceCode, this);
						
						// And replace it in the expression
						expression.remove(i + 1);
						expression.set(i, resultLiteral);
						expression.remove(i - 1);
						
						// And iterate again from previous operator, if there was one
						if(previousOperator != null){
							// If there's an operator before current operator,
							// iterate again from the previous operator
							i -= 3;
							continue;
						}else{
							// There's no previous operator, thus we continue iteration
							i -= 1;
							continue;
						}
					}
				}
			}
		}
	}

	/** Recursive */
	public ArrayList<PExpression> getAllExpressions(ArrayList<PExpression> list) {
		if(list == null)
			list = new ArrayList<PExpression>();
		
		list.add(this);
		
		for(PProgramPart operand : expression){
			if(operand instanceof PFunctionCall){
				PFunctionCall fCall = (PFunctionCall)operand;
				for(PExpression arg : fCall.arguments){
					arg.getAllExpressions(list);
				}
			}else if(operand instanceof PExpression){
				PExpression exp = (PExpression)operand;
				exp.getAllExpressions(list);
			}else if(operand instanceof PAssignmentStatement){
				// TODO: Finish this
				PAssignmentStatement assignment = (PAssignmentStatement)operand;
				assignment.expression.getAllExpressions(list);
				
				if(assignment.variable.isArrayAccess()) {
					assignment.variable.arrayIndex.getAllExpressions(list);
				}
			}else if(operand instanceof PVarAccessor){
				// TODO: Finish this
				PVarAccessor var = (PVarAccessor)operand;
				if(var.isArrayAccess()){
					var.arrayIndex.getAllExpressions(list);
				}
			}
		}
		
		return list;
	}

	/** Non recursive */
	public ArrayList<PFunctionCall> findFunctionCalls(ArrayList<PFunctionCall> list) {
		if(list == null)
			list = new ArrayList<PFunctionCall>();
		
		for(PProgramPart operand : expression){
			if(operand instanceof PFunctionCall)
				list.add((PFunctionCall)operand);
		}
		
		return list;
	}

	public int getTotalOperands() {
		int numOfOperands = 0;
		
		for(PExpression exp : getAllExpressions(null)){
			numOfOperands += (exp.expression.size() + 1) / 2;
		}
		
		return numOfOperands;
	}
	/* Finds out largest stack space usage in one
	 * function call. This function is recursive and 
	 * iterates over all parenthesis expressions,
	 * and looks in expressions in array indexes aswell. */
	public int findLargestArgsSize(){
		int argsSize = 0;
		
		for(PProgramPart operand : expression){
			if(operand instanceof PFunctionCall){
				PFunctionCall fcall = (PFunctionCall)operand;
				argsSize = Math.max(argsSize, fcall.getTotalUsingStackSpaceRec());
			}else if(operand instanceof PVarAccessor){
				PVarAccessor var = (PVarAccessor)operand;
				if(var.isArrayAccess()){
					argsSize = Math.max(argsSize, var.arrayIndex.findLargestArgsSize());
				}
			}else if(operand instanceof PExpression){
				PExpression exp = (PExpression)operand;
				argsSize = Math.max(argsSize, exp.findLargestArgsSize());
			}else if(operand instanceof PAssignmentStatement){
				PAssignmentStatement assignment = (PAssignmentStatement)operand;
				argsSize = Math.max(argsSize, assignment.expression.findLargestArgsSize());
			}
		}
		
		return argsSize;
	}
	
	private static long eval(long left, POperatorType op, long right){
		long sum = left;
		
		switch(op){
		case ADD: sum = sum + right; break;
		case SUB: sum = sum - right; break;
		case MUL: sum = sum * right; break;
		case DIV: sum = sum / right; break;
		case BITWISE_AND: sum = sum & right; break;
		case BITWISE_OR: sum = sum | right; break;
		case BITWISE_XOR: sum = sum ^ right; break;
		default:
			break;
		}
		
		return sum;
	}
	
	public boolean contains(Object type){
		for(PProgramPart op : expression)
			if(op.getClass().isInstance(type))
				return true;
		return false;
	}

	public boolean contains(Object... type) {
		for(PProgramPart op : expression){
			for(Object c : type){
				if(op.getClass().equals(c))
					return true;
			}
		}
		
		return false;
	}
	
	public PProgramPart firstElement(){
		return expression.get(0);
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(" ");
		
		for(PProgramPart el : expression){
			if(el instanceof PExpression){
				if(((PExpression)el).pointerExpression) joiner.add("*(" + el.toString() + ")");
				else joiner.add("(" + el.toString() + ")");
			}else{
				joiner.add(el.toString());
			}
		}
		
		return (typeCast != null ? "(" + typeCast.toString() + ")" : "") + joiner.toString();
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PExpression exp = new PExpression(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			for(PProgramPart el : expression)
				exp.expression.add(el.clone(true));
			
			exp.expDataType = expDataType;
			exp.expStructType = expStructType == null ? null : (PStructType)expStructType.clone(true);
			exp.typeCast = typeCast == null ? null : (PTypeCast)typeCast.clone(true);
			exp.pointerExpression = pointerExpression;
			exp.verified = verified;
		}else{
			exp.expression.addAll(expression);
			exp.expDataType = expDataType;
			exp.expStructType = expStructType;
			exp.typeCast = typeCast;
			exp.pointerExpression = pointerExpression;
			exp.verified = verified;
		}
		
		return exp;
	}
}

package mpl.syntactic.parts;

import java.util.ArrayList;
import java.util.List;

import mpl.syntactic.functions.PBuiltInFunction;
import mpl.utils.io.Console;

public class PBody extends PProgramPart {
	
	public static final int LOOK_IN_PARENT = 1;
	public static final int LOOK_IN_CHILD = 2;
	public static final int LOOK_IN_CURRENT = 4;
	
	public List<PProgramPart> statements = new ArrayList<PProgramPart>();
	public List<PLocalVariable> vars = new ArrayList<PLocalVariable>();
	
	public PBody(String sourceFile, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFile, lineInSourceCode, columnInSourceCode, parent);
	}
	
	/** Verify all statements in the body and it's children bodies */
	public void processBody() {
		for(int i=0; i<statements.size(); i++){
			PProgramPart part = statements.get(i);
			
			if(part instanceof PAssignmentStatement){
				PAssignmentStatement assignment = (PAssignmentStatement)part;
				assignment.verify();
			}else if(part instanceof PPointerAssignment){
				PPointerAssignment ptrAssignment = (PPointerAssignment)part;
				ptrAssignment.verify();
			}else if(part instanceof PFunctionCall){
				PFunctionCall call = (PFunctionCall)part;
				if(call.isBuiltInFunction()){
					// TODO: Finish here with built-in functions
					PBuiltInFunction result = PBuiltInFunction.findBuiltInFunction(call.functionName, call.arguments);
					statements.set(i, result.eval(call.sourceFileName, call.lineInSourceCode, call.columnInSourceCode, this, call.arguments));
					
				//	PProgramPart result = PBuiltinFunctionEval.evalBuiltInFunction(call.functionName, (ArrayList<PExpression>)call.arguments,
				//														call.sourceFileName, call.lineInSourceCode, call.columnInSourceCode);
				//	statements.set(i, result);
				}else{
					call.verify();
				}
			}else if(part instanceof PIfStatement){
				PIfStatement ifSt = (PIfStatement)part;
				ifSt.verify();
			}else if(part instanceof PElseStatement){
				PElseStatement elseSt = (PElseStatement)part;
				if(i == 0){
					Console.throwError("Unexpected else statement in line: " + elseSt.lineInSourceCode);
				}
				elseSt.verify(statements.get(i-1));
			}else if(part instanceof PElseIfStatement){
				PElseIfStatement elseIfSt = (PElseIfStatement)part;
				if(i == 0){
					Console.throwError("Unexpected else-if statement in line: " + elseIfSt.lineInSourceCode);
				}
				elseIfSt.verify(statements.get(i-1));
			}else if(part instanceof PWhileStatement){
				PWhileStatement whileSt = (PWhileStatement)part;
				whileSt.verify();
			}else if(part instanceof PDoStatement){
				PDoStatement doSt = (PDoStatement)part;
				doSt.verify();
			}else if(part instanceof PForStatement){
				PForStatement forSt = (PForStatement)part;
				forSt.verify();
			}else if(part instanceof PForEachStatement){
				PForEachStatement forEachSt = (PForEachStatement)part;
				forEachSt.verify();
			}else if(part instanceof PUnaryExpression){
				PUnaryExpression varOp = (PUnaryExpression)part;
				varOp.verify();
			}else if(part instanceof PReturnStatement){
				PReturnStatement returnSt = (PReturnStatement)part;
				returnSt.verify();
			}else if(part instanceof PBreakStatement){
				PBreakStatement breakSt = (PBreakStatement)part;
				breakSt.verify();
			}else if(part instanceof PContinueStatement){
				PContinueStatement continueSt = (PContinueStatement)part;
				continueSt.verify();
			}else if(part instanceof PArrayInitializer){
				PArrayInitializer arrayInit = (PArrayInitializer)part;
				arrayInit.verify();
			}else if(part instanceof PStructInitializer){
				PStructInitializer structInit = (PStructInitializer)part;
				structInit.verify();
			}else if(part instanceof PDeleteStatement){
				PDeleteStatement deleteSt = (PDeleteStatement)part;
				deleteSt.verify();
			}
		}
		
		buildIfStatementsTrees();
	}
	
	/** Translates else-if statements into if statements
	 * and puts them into the deepest else branch of an if statement */
	private void buildIfStatementsTrees() {
		/* Here if statements gets converted from:
		 if(...){
		 }else if{
		 }else if{
		 }else{
		 }
		 
		 To:
		 
		 if(...){
		 }else{
		 	if(){
		 	// else if
		 	}else{
		 		if(){
		 		// else if
		 		}else{
		 		// else
		 		}
			}
		 }
		 */
		
		for(int i=0; i<statements.size(); i++){
			PProgramPart part = statements.get(i);
			
			if(part instanceof PElseStatement){
				PElseStatement elseSt = (PElseStatement)part;
				// Find if statement
				PProgramPart parent = statements.get(i - 1);
				
				// Move else statement into deepest else body
				// of if statement
				PBody deepestElse = ((PIfStatement)parent).findDeepestElseBody();
				deepestElse.vars.addAll(elseSt.body.vars);
				deepestElse.statements.addAll(elseSt.body.statements);
				
				// Remove else statement
				statements.remove(i--);
			}else if(part instanceof PElseIfStatement){
				PElseIfStatement elseIfSt = (PElseIfStatement)part;
				// Find if statement
				PProgramPart parent = statements.get(i - 1);
				
				// Create new if-else statement
				PIfStatement ifStatement = new PIfStatement("", -1, -1, ((PIfStatement)parent).findDeepestElseBody());
				ifStatement.condition = elseIfSt.condition;
				ifStatement.body.vars.addAll(elseIfSt.body.vars);
				ifStatement.body.statements.addAll(elseIfSt.body.statements);
				
				// Move if-else statement into deepest else body
				// of if statement
				((PIfStatement)parent).findDeepestElseBody().statements.add(ifStatement);
				
				// Remove else-if statement
				statements.remove(i--);
			}
		}
	}

	/** Checks for variable redefinition in current body only.
	 * This function is not recursive! */
	public void checkForVariablesRedefinition() {
		// Check for redefinitions between local variables
		/*for(PVariable var : vars){
			int redefinitionLine = isVariableDefined(var, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_CHILD, var.lineInSourceCode);
			if(redefinitionLine != -1){
				PVariable varRedefinition = findVariable(var, PBody.LOOK_IN_CURRENT | PBody.LOOK_IN_CHILD, var.lineInSourceCode);
				if(varRedefinition != var){
					// If there's one, throw an error
					Console.throwError(Console.ERROR_VAR_REDEFINITION, varRedefinition.sourceFileName, varRedefinition.lineInSourceCode, varRedefinition.columnInSourceCode, varRedefinition.name,
							var.sourceFileName, var.lineInSourceCode);
				}
			}
		}*/
		
		for(PVariable var : vars){
			PVariable redefVar = findVariable(var);
			if(redefVar != null){
				if(redefVar.lineInSourceCode > var.lineInSourceCode){
					Console.throwError(Console.ERROR_VAR_REDEFINITION, redefVar.sourceFileName, redefVar.lineInSourceCode, redefVar.columnInSourceCode, redefVar.name,
							var.sourceFileName, var.lineInSourceCode);
				}
			}
			
			// Check for var redefinition in child bodies
			for(PBody body : findChildrenBodies(false, true)){
				redefVar = body.findVariable(var);
				if(redefVar != null){
					if(redefVar.lineInSourceCode > var.lineInSourceCode){
						Console.throwError(Console.ERROR_VAR_REDEFINITION, redefVar.sourceFileName, redefVar.lineInSourceCode, redefVar.columnInSourceCode, redefVar.name,
								var.sourceFileName, var.lineInSourceCode);
					}
				}
			}
			
			// Perform same checking for children bodies
			for(PBody body : findChildrenBodies(false, false)){
				body.checkForVariablesRedefinition();
			}
		}
	}
	
	/** Check if there is no dead code,
	 * i.e. code after return statement */
	public void checkForDeadCode() {
		ArrayList<PReturnStatement> returnStatements = findReturnStatements(null);
		
		// If there is no return statements, it could mean
		// that function return type is void
		if(returnStatements.size() > 0){
			for(PProgramPart statement : statements){
				if(statement.lineInSourceCode > returnStatements.get(0).lineInSourceCode){
					Console.printWarning("Dead code! Line: " + statement.lineInSourceCode);
				}
			}
		}
	}
	
	public PVariable findVariable(PVariable variable){
		for(PVariable var : vars){
			if(var == variable)
				continue;
			
			if(var.name.equals(variable.name))
				return var;
		}
		
		return null;
	}
	
	public PVariable findVariable(PVariable variable, int searchType){
		if((searchType & LOOK_IN_CURRENT) == LOOK_IN_CURRENT){
			// Look for definition in current body
			for(PLocalVariable var : vars){
				if(var.name.equals(variable.name))
						return var;
			}
		}
		
		if((searchType & LOOK_IN_CHILD) == LOOK_IN_CHILD){
			// Look for definition in child
			ArrayList<PBody> bodies = findChildrenBodies(false, false);
			if(bodies.size() > 0){
				for(PBody body : bodies){
					return body.findVariable(variable, LOOK_IN_CURRENT | LOOK_IN_CHILD);
				}
			}
		}else if((searchType & LOOK_IN_PARENT) == LOOK_IN_PARENT){
			PProgramPart p = parent;

			// Look for definition in parent bodies
			while(!(p instanceof PProgram)){
				if(p instanceof PBody)
					return ((PBody)p).findVariable(variable, LOOK_IN_CURRENT | LOOK_IN_PARENT);
				p = p.parent;
			}
			
			// Reset p variable
			p = parent;
			
			// Try to find the variable in function parameters
			while(!(p instanceof PFunction))
				p = p.parent;
				
			PVariable var = ((PFunction)p).params.findVariable(variable);
			if(var != null)
				return var;
				
			// Try to find the variable in global variables
			return getProgram().findVariable(variable);
		}
		
		return null;
	}
	
	
	/** Returns line that contains declaration of a given variable.
	 * 	If variable is undeclared in this body, then it will return -1 */
	public int isVariableDefined(PVariable variable, int searchType){
		int line = -1;
		
		if((searchType & LOOK_IN_CURRENT) == LOOK_IN_CURRENT){
			// Check for declaration in current body
			int varDeclarationLine = -1;
			for(PVariable var : vars){
				if(var.name.equals(variable.name) && var != variable){
					varDeclarationLine = var.lineInSourceCode;
					break;
				}
			}
			
			if((line = varDeclarationLine) != -1){
				return line;
			}
		}
		
		if((searchType & LOOK_IN_CHILD) == LOOK_IN_CHILD){
			// Check for declaration in child bodies
			ArrayList<PBody> bodies = findChildrenBodies(false, false);
			if(bodies.size() > 0){
				int defLineInBody = -1;
				for(PBody body : bodies){
					defLineInBody = body.isVariableDefined(variable, LOOK_IN_CURRENT | LOOK_IN_CHILD);
					if(defLineInBody != -1)
						return defLineInBody;
				}
			}
		}else if((searchType & LOOK_IN_PARENT) == LOOK_IN_PARENT){
			// Check for declaration in parent bodies
			if(parent instanceof PBody){
				return ((PBody)parent).isVariableDefined(variable, LOOK_IN_CURRENT | LOOK_IN_PARENT);
			}else{
				PProgramPart p = parent;
				
				while(!(p instanceof PProgram)){
					if(p instanceof PBody)
						return ((PBody)p).isVariableDefined(variable, LOOK_IN_CURRENT | LOOK_IN_PARENT);
					p = p.parent;
				}
				
				// Reset p variable
				p = parent;
				
				// Try to find the variable in function parameters
				while(!(p instanceof PFunction))
					p = p.parent;
					
				int varLine = ((PFunction)p).params.isVariableDeclared(variable);
				if(varLine != -1)
					return varLine;
					
				// Try to find the variable in global variables
				return getProgram().isVariableDefined(variable);
			}
		}
		
		return line;
	}
	
	public ArrayList<PBody> findChildrenBodies(boolean includeSelf, boolean recursive){
		ArrayList<PBody> bodies = new ArrayList<PBody>();
		
		if(includeSelf)
			bodies.add(this);
		
		for(PProgramPart part : statements){
			if(part instanceof PBody){
				PBody childBody = (PBody)part;
				if(recursive){
					bodies.addAll(childBody.findChildrenBodies(true, true));
				}else{
					bodies.add(childBody);
				}
			}else if(part instanceof PFlowControlStatement){
				PBody childBody = ((PFlowControlStatement)part).body;
				if(recursive){
					bodies.addAll(childBody.findChildrenBodies(true, true));
					
					// If that's an if statement, get it's else-if and else bodies
					if(part instanceof PIfStatement){
						PIfStatement ifst = (PIfStatement)part;
						bodies.addAll(ifst.elseBody.findChildrenBodies(true, true));
					}
				}else{
					bodies.add(childBody);
					
					// If that's an if statement, get it's else-if and else bodies
					if(part instanceof PIfStatement){
						PIfStatement ifst = (PIfStatement)part;
						bodies.add(ifst.elseBody);
					}
				}
			}
		}
		
		return bodies;
	}
	
	/** Returns list of all function calls in this body only.
	 * Function is not recursive!*/
	public ArrayList<PFunctionCall> findFunctionCalls(ArrayList<PFunctionCall> listOfFunctionCalls, boolean includeExpressions){
		if(listOfFunctionCalls == null)
			listOfFunctionCalls = new ArrayList<PFunctionCall>();
		
		// Find function calls in current body statements
		for(PProgramPart statement : statements) {
			if(statement instanceof PFunctionCall) {
				//listOfFunctionCalls.addAll(((PFunctionCall)statement).findFunctionCallsInArgs(null, true));
				//listOfFunctionCalls.add((PFunctionCall)statement);
				
				((PFunctionCall)statement).findFunctionCallsInArgs(listOfFunctionCalls, true);
			}
		}
				
		// Find function calls in current body expressions
		if(includeExpressions) {
			for(PExpression exp : findExpressions(null)) {
				exp.findFunctionCalls(listOfFunctionCalls);
			}
		}
		
		return listOfFunctionCalls;
	}
	
	/** Returns list of all return statements in this body only.
	 * Function is not recursive!*/
	public ArrayList<PReturnStatement> findReturnStatements(ArrayList<PReturnStatement> listOfReturns){
		if(listOfReturns == null)
			listOfReturns = new ArrayList<PReturnStatement>();
		
		// Find return statements in current body
		for(PProgramPart part : statements){
			if(part instanceof PReturnStatement){
				listOfReturns.add((PReturnStatement)part);
			}
		}
		
		return listOfReturns;
	}
	
	/** Returns list of all expressions in this body only.
	 * Function is not recursive!*/
	public ArrayList<PExpression> findExpressions(ArrayList<PExpression> listOfExpressions){
		if(listOfExpressions == null)
			listOfExpressions = new ArrayList<PExpression>();
		
		// Find all of the expressions in current body
		for(PProgramPart statement : statements){
			if(statement instanceof PAssignmentStatement){
				PAssignmentStatement assignment = (PAssignmentStatement)statement;
				if(assignment.expressionAssignment)
					listOfExpressions.addAll(assignment.expression.getAllExpressions(null));
			}else if(statement instanceof PPointerAssignment){
				listOfExpressions.addAll(((PPointerAssignment)statement).pointerExp.getAllExpressions(null));
				listOfExpressions.addAll(((PPointerAssignment)statement).exp.getAllExpressions(null));
			}else if(statement instanceof PFunctionCall){
				for(PExpression arg : ((PFunctionCall)statement).arguments)
					listOfExpressions.addAll(arg.getAllExpressions(null));
			}else if(statement instanceof PReturnStatement){
				PReturnStatement st = (PReturnStatement)statement;
				if(st.returnData != null)
					listOfExpressions.addAll(st.returnData.getAllExpressions(null));
			}else if(statement instanceof PFlowControlStatement){
				PFlowControlStatement flowControlSt = (PFlowControlStatement)statement;
				if(flowControlSt.condition != null){
					flowControlSt.condition.getAllExpressions(listOfExpressions);
				//	listOfExpressions.addAll(flowControlSt.condition.leftExp.getAllExpressions(null));
				//	listOfExpressions.addAll(flowControlSt.condition.rightExp.getAllExpressions(null));
				}
			}
		}
				
		return listOfExpressions;
	}
	
	/** Calculates the total size of stack space being used by local variables.
	 * This function is not recursive! */
	public int getSizeOfLocalVars(){
		int totalUsingSpace = 0;
		
		for(PLocalVariable var : vars)
			totalUsingSpace += var.getSizeInBytes();
		
		return totalUsingSpace;
	}
	
	/** Calculates the highest stack space usage by arguments.
	 * This function is not recursive! */
	public int getHighestStackUsageByArgs(){
		int biggestStackSpaceUsage = 0;
		
		// Iterate over all function calls inside this body
		for(PFunctionCall fcall : findFunctionCalls(null, true)) {
			biggestStackSpaceUsage = Math.max(biggestStackSpaceUsage, fcall.getTotalUsingStackSpace());
		}
		
		return biggestStackSpaceUsage;
	}
	
	/** Calculates the total size of stack space being used by expressions.
	 * This function is not recursive! */
	public int getSizeOfExpr() {
		int totalUsingSpace = 0;
		
		for(PExpression exp : findExpressions(null))
			totalUsingSpace = Math.max(totalUsingSpace, exp.getTotalOperands());

		// 'For each' statements use 4 bytes of stack space index variable
		for(PProgramPart p : parts)
			if(p instanceof PForEachStatement)
				totalUsingSpace++;
		
		// We assume that any operand in any expression is 4-bytes in size,
		// so we multiply number of operands by 4
		totalUsingSpace *= 4;
		
		return totalUsingSpace;
	}
	
	public String getAst(){
		return getAstCode("");
	}

	@Override
	protected String getAstCode(String padding) {
		String code = "";
		
		for(int i=0; i<vars.size(); i++){
			PLocalVariable var = vars.get(i);
			code += padding + var.toString();
			
			if(i + 1 < vars.size())
				code += System.lineSeparator();
		}
		
		if(statements.size() > 0 && code.length() > 0){
			code += System.lineSeparator() + System.lineSeparator();
		}
		
		for(int i=0; i<statements.size(); i++){
			PProgramPart statement = statements.get(i);
			code += statement.getAstCode(padding);
			
			if(i + 1 < statements.size())
				code += System.lineSeparator() + System.lineSeparator();
		}
		
		return code;
	}
	
	@Override
	public String toString(){
		String total = "vars:" + System.lineSeparator();
		for(int i=0; i<vars.size(); i++)
			total += vars.get(i).getAstCode(null) + (i + 1 < vars.size() ? System.lineSeparator() : "");
		
		total += System.lineSeparator();
		
		total += "body:" + System.lineSeparator();
		for(int i=0; i<statements.size(); i++)
			total += statements.get(i).getAstCode(null) + (i + 1 < statements.size() ? System.lineSeparator() : "");
		
		return total;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PBody body = new PBody(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			for(PProgramPart statement : statements)
				body.statements.add(statement.clone(true));
			
			for(PLocalVariable var : vars)
				body.vars.add((PLocalVariable)var.clone(true));
		}else{
			for(PProgramPart statement : statements)
				body.statements.add(statement);
			
			for(PLocalVariable var : vars)
				body.vars.add(var);
		}
		
		return body;
	}
}
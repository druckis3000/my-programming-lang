package mpl.syntactic.parts;

import java.util.ArrayList;

import mpl.syntactic.parts.POperator.POperatorType;

public class PConditionExp extends PProgramPart {
	public ArrayList<PProgramPart> expression = new ArrayList<PProgramPart>();
	
	public PConditionExp(String sourceFileName, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public void verify(){
		// Increase i by two, since we're skipping operators
		for(int i=0; i<expression.size(); i+=2){
			if(expression.get(i) instanceof PCondition){
				// Verify condition
				((PCondition)expression.get(i)).verify();
			}
			
			if(expression.get(i) instanceof PConditionExp){
				// Verify parentheses condition
				((PConditionExp)expression.get(i)).verify();
			}
		}
	}
	
	public PProgramPart buildConditionIfTree(PConditionExp conditionExp, PAsmCode trueJump, PAsmCode falseJump, boolean isChild){
		PBody root = new PBody("", -1, -1, conditionExp.parent);
		PBody lastParentheses = null;
		
		PIfStatement lastIf = null;
		
		ArrayList<PProgramPart> clonedExp = new ArrayList<>(conditionExp.expression);
		
		// Increase by two, since we're skipping operator
		for(int i=0; i<clonedExp.size(); i+=2){
			PProgramPart el = clonedExp.get(i);
			POperator previousOperator = i > 1 ? (POperator)clonedExp.get(i-1) : null;
			
			if(el instanceof PConditionExp){
				// Found parentheses condition
				PBody childExp = (PBody)buildConditionIfTree((PConditionExp)el, trueJump, falseJump, true);
	//			System.out.println("Created parentheses condition for: " + el.toString());
				
				if(previousOperator != null){
					if(previousOperator.type == POperatorType.LOGICAL_AND){
	//					System.out.println("\tOperator is &&");
						
						if(lastIf != null){
	//						System.out.println("\tPut it inside lastIf: " + lastIf.toString());
							
							// Last element was non-parentheses condition
							// Put child exp into last if statement
							lastIf.body.statements.addAll(childExp.statements);
							lastIf = null;
							lastParentheses = childExp;
						}else{
	//						System.out.println("\tPut it inside previous parentheses condition");
							
							// Last element was parentheses condition
							// Put child exp into last parentheses condition, deepest or if statements
							insertIntoDeepestIfs(lastParentheses, childExp);
							lastParentheses = childExp;
						}
					}else{
	//					System.out.println("\tOperator is ||");
	//					System.out.println("\tPut it inside root body");
						
						// Put child exp into root
						root.statements.addAll(childExp.statements);
						lastIf = null;
						lastParentheses = childExp;
					}
				}else{
	//				System.out.println("\tOperator is null");
	//				System.out.println("\tPut it inside root body");
					
					// Put child exp into root
					root.statements.addAll(childExp.statements);
					lastIf = null;
					lastParentheses = childExp;
				}
			}else{
				// Found plain condition
			
				// Create child if statement
				PIfStatement childIf = new PIfStatement("", -1, -1, el.parent);
				PConditionExp cExp = new PConditionExp("", -1, -1, conditionExp.parent);
				cExp.expression.add(el);
				childIf.condition = cExp;
				
	//			System.out.println("Created child if: " + childIf.toString());
				
				if(previousOperator != null){
					if(previousOperator.type == POperatorType.LOGICAL_AND){
	//					System.out.println("\tOperator is &&");
						
						if(lastIf != null){
	//						System.out.println("\tPut it inside this if: " + lastIf.toString());
							
							// Last element was non-parentheses condition
							// Put child exp into last if statement
							lastIf.body.statements.add(childIf);
							lastIf = childIf;
						}else{
	//						System.out.println("\tPut it inside last parentheses condition");
							
							// Last element was parentheses condition
							// Put child exp into last parentheses condition, deepest or if statements
							insertIntoDeepestIfs(lastParentheses, childIf);
							lastIf = null;
							
							// Remove current condition from the expression
							clonedExp.remove(i);
							// Remove previous operator aswell
							clonedExp.remove(i-1);
							// And iterate again at same index
							i -= 2;
							continue;
						}
					}else{
	//					System.out.println("\tOperator is ||");
	//					System.out.println("\tPut it inside root body");
						
						// Put new condition into root and set lastIf to childIf
						root.statements.add(childIf);
						lastIf = childIf;
					}
				}else{
	//				System.out.println("\tOperator is null");
	//				System.out.println("\tPut it inside root body");
					
					// Put new condition into root and set lastIf to childIf
					root.statements.add(childIf);
					lastIf = childIf;
				}
			}
		}
		
		if(!isChild){
			insertIntoDeepestIfs(root, trueJump);
			root.statements.add(falseJump);
		}
		
		return root;
	}
	
	private void insertIntoDeepestIfs(PBody aExp, PBody bExp){
		// Find deepest if statements in aExp
		for(PProgramPart st : aExp.statements){
			if(st instanceof PIfStatement){
				PIfStatement orIf = (PIfStatement)st;
				
				// Find deepest if statement, and put bExp statements inside it
				if(orIf.body.statements.size() > 0){
					insertIntoDeepestIfs(orIf.body, bExp);
				}else{
					orIf.body.statements.addAll(((PBody)bExp.clone(true)).statements);
				}
			}
		}
	}
	
	private void insertIntoDeepestIfs(PBody aExp, PProgramPart b){
		// Find deepest if statements in aExp
		for(PProgramPart st : aExp.statements){
			if(st instanceof PIfStatement){
				PIfStatement orIf = (PIfStatement)st;
				
				// Find deepest if statement, and put bExp statements inside it
				if(orIf.body.statements.size() > 0){
					insertIntoDeepestIfs(orIf.body, b);
				}else{
					orIf.body.statements.add(b.clone(true));
				}
			}
		}
	}
	
	/*public PProgramPart buildConditionIfTree(PConditionExp conditionExp, PAsmCode trueJump, PAsmCode falseJump, boolean isChild){
		PBody root = new PBody("", -1, -1, conditionExp.parent);
		
		PIfStatement rootIf = null;
		PIfStatement lastIf = null;
		
		// Increase i by two, since we're skipping operators
		for(int i=0; i<conditionExp.expression.size(); i+=2){
			// Check if we're at the end of expression
			if(i == conditionExp.expression.size() - 1){
				// Last condition expression element
				
				// Create condition for last if
				PCondition condition = (PCondition)conditionExp.expression.get(i);
				PConditionExp cExp = new PConditionExp("", -1, -1, conditionExp.parent);
				cExp.expression.add(condition);
				
				if(rootIf == null){
					// Initialize root if
					rootIf = new PIfStatement("", -1, -1, conditionExp.parent);
					rootIf.condition = cExp;
				}else{
					// Find out previous condition operator
					POperator prevOperator = (POperator)conditionExp.expression.get(i-1);
					
					// Create child if
					PIfStatement childIf = new PIfStatement("", -1, -1, lastIf);
					childIf.condition = cExp;
					
					if(prevOperator.type == POperatorType.LOGICAL_AND){
						// If previous operator was and, put child if statement
						// to the last if statement
						lastIf.body.statements.add(childIf);
					}else{
						// If previous operator was or, put child if statement
						// to the rootAnd
						root.statements.add(childIf);
					}
				}
			}else{
				// Not last condition expression element
				
				// Create condition for last if
				if(conditionExp.expression.get(i) instanceof PConditionExp){
					PBody child = (PBody)buildConditionIfTree((PConditionExp)conditionExp.expression.get(i), trueJump, falseJump, true);
					
					if(rootIf == null){
						// Initialize root if
						// TODO: Finish this, first element is parentheses expression
						//rootIf = childIf;
						
						//lastIf = rootIf;
					}else{
						// Find out previous condition operator
						POperator prevOperator = (POperator)conditionExp.expression.get(i-1);
						
						if(prevOperator.type == POperatorType.LOGICAL_AND){
							// If previous operator was and, put child if statement
							// to the last if statement
							lastIf.body.statements.addAll(child.statements);
						}else{
							// If previous operator was or, put child if statement
							// to the rootAnd
							root.statements.addAll(child.statements);
						}
						
						// Set last if to the deepest if statement inside childIf
						lastIf = ((PIfStatement)child.statements.get(0)).findDeepestIf();
					}
				}else{
					PCondition condition = (PCondition)conditionExp.expression.get(i);
					PConditionExp cExp = new PConditionExp("", -1, -1, conditionExp.parent);
					cExp.expression.add(condition);
					
					if(rootIf == null){
						// Initialize root if
						rootIf = new PIfStatement("", -1, -1, conditionExp);
						rootIf.condition = cExp;
						
						lastIf = rootIf;
					}else{
						// Find out previous condition operator
						POperator prevOperator = (POperator)conditionExp.expression.get(i-1);
						
						// Create child if
						PIfStatement childIf = new PIfStatement("", -1, -1, lastIf);
						childIf.condition = cExp;
						
						if(prevOperator.type == POperatorType.LOGICAL_AND){
							// If previous operator was and, put child if statement
							// to the last if statement
							lastIf.body.statements.add(childIf);
						}else{
							// If previous operator was or, put child if statement
							// to the rootAnd
							root.statements.add(childIf);
						}
						
						// Set last if to the child if
						lastIf = childIf;
					}
				}
			}
		}
		
		//System.out.println(conditionExp + ":");
		root.statements.add(0, rootIf);
		//System.out.println(root.getAst());
		
		// TODO: When calling this method recursively, return root body, instead of rootIf if statement
		
		if(isChild)
			return root;
		else
			return rootIf;
	}*/
	
	public void getAllExpressions(ArrayList<PExpression> listOfExpressions){
		if(listOfExpressions == null)
			listOfExpressions = new ArrayList<PExpression>();
		
		// Increase i by two, since we're skipping operators
		for(int i=0; i<expression.size(); i+=2){
			if(expression.get(i) instanceof PCondition){
				PCondition condition = (PCondition)expression.get(i);
				listOfExpressions.add(condition.leftExp);
				listOfExpressions.add(condition.rightExp);
			}
			
			if(expression.get(i) instanceof PConditionExp){
				// Verify parentheses condition
				((PConditionExp)expression.get(i)).getAllExpressions(listOfExpressions);
			}
		}
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString(){
		String code = "";
		
		for(int i=0; i<expression.size(); i++){
			if(expression.get(i) instanceof PConditionExp){
				code += "(" + expression.get(i) + ") ";
			}else{
				code += expression.get(i) + " ";
			}
		}
		
		return code.length() > 0 ? code.substring(0, code.length()-1) : code;
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PConditionExp conditionExp = new PConditionExp(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			for(PProgramPart el : expression)
				conditionExp.expression.add(el.clone(true));
		}else{
			for(PProgramPart el : expression)
				conditionExp.expression.add(el);
		}
		
		return conditionExp;
	}
}
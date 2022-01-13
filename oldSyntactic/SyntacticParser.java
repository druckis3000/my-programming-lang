package spl.parser.syntactic;

import java.util.ArrayList;

import spl.parser.ParserException;
import spl.parser.lexical.Token;
import spl.parser.lexical.TokenType;
import spl.parser.syntactic.parts.PBody;
import spl.parser.syntactic.parts.PBreakStatement;
import spl.parser.syntactic.parts.PCondition;
import spl.parser.syntactic.parts.PContinueStatement;
import spl.parser.syntactic.parts.PDataType;
import spl.parser.syntactic.parts.PDoStatement;
import spl.parser.syntactic.parts.PElseIfStatement;
import spl.parser.syntactic.parts.PElseStatement;
import spl.parser.syntactic.parts.PForStatement;
import spl.parser.syntactic.parts.PFunction;
import spl.parser.syntactic.parts.PFunctionParameter;
import spl.parser.syntactic.parts.PFunctionParams;
import spl.parser.syntactic.parts.PGlobalVariable;
import spl.parser.syntactic.parts.PIfStatement;
import spl.parser.syntactic.parts.PProgram;
import spl.parser.syntactic.parts.PProgramPart;
import spl.parser.syntactic.parts.PStatement;
import spl.parser.syntactic.parts.PWhileStatement;
import spl.utils.io.Console;

public class SyntacticParser {
	private TokenReader reader;
	private PProgram program;
	
	public SyntacticParser(){
		program = new PProgram();
	}
	
	public PProgram parse(ArrayList<Token> tokens) throws ParserException {
		reader = new TokenReader(tokens);
		
		while(reader.hasMoreTokens()){
			Token firstTok = reader.nextToken();
			
			switch(firstTok.type){
			case FUNCTION_DEF:
				parseFunction();
				break;
			// Data types
			case DT_INT:
			case DT_UINT:
			case DT_STRING:
				parseGlobalVarDefinition(firstTok);
				break;
			default:
				
			}
		}
		
		return program;
	}
	
	private void parseGlobalVarDefinition(Token dataType) throws ParserException {
		// 2nd token after data type must be a variable name
		Token varName = reader.eat(TokenType.STRING);
		
		// 3rd token after var name must be assignment or semicolon symbol
		Token equalsOrSemicolon = reader.eat(TokenType.ASSIGNMENT, TokenType.SEMICOLON);
		
		// Create global variable
		PGlobalVariable var = new PGlobalVariable(varName.lineInSourceCode, program);
		var.dataType = PDataType.fromTokenType(dataType.type);
		var.name = varName.data;
		var.nameInAsm = varName.data;

		if(equalsOrSemicolon.type == TokenType.ASSIGNMENT){
			// If 3rd token was assignment symbol, then it's a variable definition
			// Now whats left - must be an initial value operand
			PProgramPart initialValue = SyntacticUtils.parseIdentifierFromTokens(reader.readUntilSemicolon(), var);
			var.initialValue = initialValue;
		}
		
		// Add variable to the program
		program.vars.add(var);
	}
	
	private void parseFunction() throws ParserException {
		// 2nd token after keyword 'function' must be a function return type
		Token returnType = reader.eat(TokenType.DT_VOID, TokenType.DT_INT, TokenType.DT_UINT, TokenType.DT_STRING);
		// 3rd token after function return type must be a function name
		Token funcName = reader.eat(TokenType.STRING);
		
		// Create function object
		PFunction function = new PFunction(PDataType.fromTokenType(returnType.type), funcName.data, funcName.lineInSourceCode, program);
		
		// 4th token after function name must be beginning of function params '(' or function start '{'
		Token paramsOrBody = reader.eat(TokenType.OPEN_BRACKET, TokenType.BODY_START);
		
		// If fourth token was open bracket '(', then parse function params
		if(paramsOrBody.type == TokenType.OPEN_BRACKET)
			parseFunctionParams(function.params);
		
		// Then parse function body
		parseBody(function.body);
		
		// Add function to the program
		program.functions.add(function);
	}
	
	private void parseBody(PBody parent) throws ParserException {
		Token next = reader.nextToken();
		
		// Empty body
		if(next.type == TokenType.BODY_END){
			Console.printWarning("Empty body in line: " + next.lineInSourceCode);
			return;
		}
		
		parent.lineInSourceCode = next.lineInSourceCode;
		
		// Body is not empty, let's parse it
		do {
			if(next.type == TokenType.IF_STATEMENT){
				// Create if statement
				PIfStatement ifs = new PIfStatement(next.lineInSourceCode, parent);
				
				// Parse if statement condition
				ifs.condition = parseCondition(ifs, next.lineInSourceCode);
				
				// Now, next token must be body start '{'
				reader.eat(TokenType.BODY_START);
				
				// Parse if statement body
				parseBody(ifs.body);
				
				// Add if statement to the parent body statements
				parent.statements.add(ifs);
			}else if(next.type == TokenType.ELSE_STATEMENT){
				// Next token must be body start '{', or if statement
				next = reader.eat(TokenType.BODY_START, TokenType.IF_STATEMENT);
				
				if(next.type == TokenType.BODY_START){
					// This is an else statement
					
					// Create an else statement
					PElseStatement elseSt = new PElseStatement(next.lineInSourceCode, parent);
					
					// Parse the body of an else statement
					parseBody(elseSt.body);
					
					// Add else statement to the parent body
					parent.statements.add(elseSt);
				}else{
					// This is an else-if statement
					
					// Create an else-if statement
					PElseIfStatement elseIfSt = new PElseIfStatement(next.lineInSourceCode, parent);
					
					// Parse the condition
					elseIfSt.condition = parseCondition(elseIfSt, elseIfSt.lineInSourceCode);
					
					// Skip one token '{'
					reader.eat(TokenType.BODY_START);
					
					// Parse the body of an else-if statement
					parseBody(elseIfSt.body);
					
					// Add else-if statement to the parent body
					parent.statements.add(elseIfSt);
				}
			}else if(next.type == TokenType.WHILE_STATEMENT){
				// Create while statement
				PWhileStatement whileSt = new PWhileStatement(next.lineInSourceCode, parent);
				
				// Parse while statement condition
				whileSt.condition = parseCondition(whileSt, whileSt.lineInSourceCode);
				
				// Now, next token must be body start '{'
				reader.eat(TokenType.BODY_START);
				
				// Parse while statement body
				parseBody(whileSt.body);
				
				// Add while statement to the parent body statements
				parent.statements.add(whileSt);
			}else if(next.type == TokenType.DO_STATEMENT){
				// Create do statement
				PDoStatement doSt = new PDoStatement(next.lineInSourceCode, parent);
				
				// Parse do statement body
				reader.eat(TokenType.BODY_START);
				parseBody(doSt.body);
				
				// Parse while condition
				Token whileSt = reader.eat(TokenType.WHILE_STATEMENT);
				doSt.condition = parseCondition(parent, whileSt.lineInSourceCode);
				
				// After condition there must be a semicolon
				reader.eat(TokenType.SEMICOLON);
				
				// Add do statement to the parent body statements
				parent.statements.add(doSt);
			}else if(next.type == TokenType.FOR_STATEMENT){
				// Create for statement
				PForStatement forSt = new PForStatement(next.lineInSourceCode, parent);
				forSt.parseForStatement(reader.readBetweenBrackets(false));
				
				// Skip body start token
				reader.eat(TokenType.BODY_START);
				
				// Read the loop body
				parseBody(forSt.body);
				
				// Add for statement to the parent body statements
				parent.statements.add(forSt);
			}else if(next.type == TokenType.BREAK_STATEMENT){
				PBreakStatement breakSt = new PBreakStatement(next.lineInSourceCode, parent);
				parent.statements.add(breakSt);
			}else if(next.type == TokenType.CONTINUE_STATEMENT){
				PContinueStatement continueSt = new PContinueStatement(next.lineInSourceCode, parent);
				parent.statements.add(continueSt);
			}else{
				// It's just a plain statement, let's parse it
				reader.decreaseIndex();
				PStatement statement = new PStatement(next.lineInSourceCode, parent);
				
				// Read statement tokens until semicolon
				reader.readUntilSemicolon(statement.tokens);
				
				statement.perceiveStatementAction();
				statement.parseStatement(parent);
			}
			
			// Loop until body end token is found 
		} while((next = reader.nextToken()).type != TokenType.BODY_END);
	}
	
	private void parseFunctionParams(PFunctionParams params) throws ParserException {
		Token firstToken = null;
		
		// Read params until we reach close bracket ')' symbol
		do {
			// 1st token must be parameter data type
			firstToken = reader.eat(TokenType.DT_INT, TokenType.DT_UINT, TokenType.DT_STRING);
			
			// 2nd token must be parameter name, alphabetic string
			Token secondToken = reader.eat(TokenType.STRING);
			
			// If everything is ok, create function parameter
			PFunctionParameter param = new PFunctionParameter(PDataType.fromTokenType(firstToken.type), secondToken.data, firstToken.lineInSourceCode, params);
			
			// And add it to the function params list
			params.params.add(param);
			
		} while(reader.nextToken().type != TokenType.CLOSE_BRACKET);
		
		// Then skip one token (function start '{')
		reader.nextToken();
	}
	
	private PCondition parseCondition(PProgramPart parent, int line) throws ParserException {
		// Read tokens in between condition brackets
		ArrayList<Token> tokens = reader.readBetweenBrackets(false);
		
		if(tokens.size() == 0){
			Console.throwError("Empty condition in line: " + line);
		}
		
		// Create and return the condition
		PCondition condition = new PCondition(tokens, tokens.get(0).lineInSourceCode, parent);
		return condition;
	}
	
	public PProgram getProgram(){
		return program;
	}
}
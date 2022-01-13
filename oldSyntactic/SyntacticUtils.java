package spl.parser.syntactic;

import java.util.ArrayList;
import java.util.stream.Collectors;

import spl.parser.ParserException;
import spl.parser.lexical.Token;
import spl.parser.lexical.TokenType;
import spl.parser.syntactic.parts.PFunctionCall;
import spl.parser.syntactic.parts.PIntegerLiteral;
import spl.parser.syntactic.parts.PProgramPart;
import spl.parser.syntactic.parts.PStringLiteral;
import spl.parser.syntactic.parts.PVariable;
import spl.utils.io.Console;

public class SyntacticUtils {

	public SyntacticUtils() {
	}

	public static PProgramPart parseIdentifierFromTokens(ArrayList<Token> tokens, PProgramPart parent) throws ParserException {
		Token firstToken = tokens.get(0);
			
		switch(firstToken.type){
		case INTEGER_LITERAL:
			return new PIntegerLiteral(Integer.parseInt(firstToken.data), firstToken.lineInSourceCode, parent);
		case STRING_LITERAL:
			return new PStringLiteral(firstToken.data, firstToken.lineInSourceCode, parent);
		case STRING:
			// Check if it's a variable or function call
			if(tokens.size() > 1){
				if(tokens.get(1).type == TokenType.OPEN_BRACKET && tokens.get(tokens.size()-1).type == TokenType.CLOSE_BRACKET){
					// It's a function call
					if(tokens.size() > 3){
						return new PFunctionCall(firstToken.data, (ArrayList<Token>)tokens.stream().skip(2).limit(tokens.size() - 3).collect(Collectors.toList()), firstToken.lineInSourceCode, parent);
					}else{
						return new PFunctionCall(firstToken.data, firstToken.lineInSourceCode, parent);
					}
				}else{
					// Check if it's an array element
					if(tokens.get(1).type == TokenType.OPEN_ARRAY_BRACKET && tokens.get(tokens.size()-1).type == TokenType.CLOSE_ARRAY_BRACKET){
						// yup it's an array
					}else{
						Console.throwError("Unexpected token '" + firstToken.data + "' in line: " + firstToken.lineInSourceCode);
						return null;
					}
				}
			}else{
				// It's a variable
				PVariable var = new PVariable(firstToken.lineInSourceCode, parent);
				var.name = firstToken.data;
				return var;
			}
		default:
			Console.throwError("Unexpected operand '" + firstToken.data + "' in line: " + firstToken.lineInSourceCode);
			return null;
		}
	}
}
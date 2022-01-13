package spl.parser.syntactic;

import java.util.ArrayList;

import spl.parser.ParserException;
import spl.parser.lexical.Token;
import spl.parser.lexical.TokenType;

public class TokenReader {
	public static boolean DEBUG = false;
	
	private ArrayList<Token> tokens;
	private int index = 0;
	
	public TokenReader(ArrayList<Token> tokens){
		this.tokens = tokens;
	}
	
	public Token nextToken() throws ParserException {
		if(!hasMoreTokens())
			throw new ParserException("Reached end of the tokens stream!");
		
		index++;
		
		if(DEBUG){
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			StackTraceElement caller = stackTrace[2];
			
			String callerInfo = caller.getClassName().substring(caller.getClassName().lastIndexOf(".")+1, caller.getClassName().length()).trim() + "." +
								caller.getMethodName() + ":" + caller.getLineNumber();
			
			System.out.println("Reading token " + (index - 1) + " (cls.: " + callerInfo + "): " + tokens.get(index - 1));
		}
		
		return tokens.get(index - 1);
	}
	
	public ArrayList<Token> readBetweenBrackets(boolean skipOpenBracket) throws ParserException {
		if(!skipOpenBracket)
			eat(TokenType.OPEN_BRACKET);
		
		int openBrackets = 1;

		ArrayList<Token> tokens = new ArrayList<>();
		
		while(openBrackets > 0){
			Token t = nextToken();
			
			if(t.type == TokenType.OPEN_BRACKET)
				openBrackets++;
			else if(t.type == TokenType.CLOSE_BRACKET)
				openBrackets--;
			
			if(openBrackets > 0)
				tokens.add(t);
		}
		
		return tokens;
	}
	
	public ArrayList<Token> readUntilEnd() throws ParserException {
		ArrayList<Token> tokens = new ArrayList<>();
		while (hasMoreTokens()) {
			tokens.add(nextToken());
		}
		return tokens;
	}
	
	public ArrayList<Token> readUntilSemicolon() throws ParserException {
		ArrayList<Token> tokens = new ArrayList<>();
		readUntilSemicolon(tokens);
		return tokens;
	}
	
	public void readUntilSemicolon(ArrayList<Token> list) throws ParserException {
		Token t = nextToken();
		
		while(t.type != TokenType.SEMICOLON){
			list.add(t);
			t = nextToken();
		}
	}
	
	public void readUntilToken(TokenType tokenType, ArrayList<Token> list) throws ParserException {
		Token t = nextToken();
		
		while(t.type != tokenType){
			list.add(t);
			t = nextToken();
		}
	}
	
	public Token eat(TokenType type) throws ParserException {
		index++;
		Token temp = tokens.get(index - 1);
		
		if(DEBUG){
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			StackTraceElement caller = stackTrace[2];
			String callerInfo = caller.getClassName().substring(caller.getClassName().lastIndexOf("."), caller.getClassName().length()).trim() + "." +
					caller.getMethodName() + ":" + caller.getLineNumber();
			System.out.println("Eating token " + (index - 1) + " (cls.: " + callerInfo + "): " + tokens.get(index - 1));
		}
		
		if(temp.type != type)
			throw new ParserException("Invalid token found: '" + temp + "' line: " + temp.lineInSourceCode + "\nExpected token: " + type);
		return temp;
	}
	
	public Token eat(TokenType ...types) throws ParserException {
		index++;
		Token temp = tokens.get(index - 1);
		for(int i=0; i<types.length; i++){
			if(temp.type == types[i])
				return temp;
		}
		
		String expectedTypes = types[0].toString();
		for(int i=1; i<types.length; i++)
			expectedTypes += ", " + types[i];
		throw new ParserException("Invalid token found in line " + temp.lineInSourceCode + ": " + temp + "\nExpected: " + expectedTypes);
	}
	
	public void decreaseIndex(){
		index = index - 1;
	}
	
	public int getCurrentIndex(){
		return index - 1;
	}
	
	public boolean hasMoreTokens(){
		return index < tokens.size();
	}
	
	public int getTotalTokens(){
		return tokens.size();
	}
}
package mpl.parser.lexical;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpl.utils.io.Console;

public class LexicalAnalyzer {
	
	private ArrayList<TokenInfo> tokenInfos;
	private ArrayList<Token> tokens;
	
	public LexicalAnalyzer(){
		tokenInfos = new ArrayList<TokenInfo>();
		tokens = new ArrayList<Token>();
		
		addTokenInfo("function", TokenType.FUNCTION_DEF);
		addTokenInfo("if", TokenType.IF_STATEMENT);
		addTokenInfo("else", TokenType.ELSE_STATEMENT);
		addTokenInfo("return", TokenType.RETURN_STATEMENT);
		addTokenInfo("while", TokenType.WHILE_STATEMENT);
		addTokenInfo("do", TokenType.DO_STATEMENT);
		addTokenInfo("for", TokenType.FOR_STATEMENT);
		addTokenInfo("break", TokenType.BREAK_STATEMENT);
		addTokenInfo("continue", TokenType.CONTINUE_STATEMENT);
		
		addTokenInfo("void", TokenType.DT_VOID);
		addTokenInfo("int", TokenType.DT_INT);
		addTokenInfo("uint", TokenType.DT_UINT);
		addTokenInfo("string", TokenType.DT_STRING);
		
		addTokenInfo("\\--", TokenType.DECREASE);
		addTokenInfo("\\++", TokenType.INCREASE);
		
		addTokenInfo("\\-?[0-9]+", TokenType.INTEGER_LITERAL); // \\+?
		addTokenInfo("\"(.*?)\"", TokenType.STRING_LITERAL);
		addTokenInfo("\\w+", TokenType.STRING);

		addTokenInfo("\\{", TokenType.BODY_START);
		addTokenInfo("\\}", TokenType.BODY_END);
		addTokenInfo("\\(", TokenType.OPEN_BRACKET);
		addTokenInfo("\\)", TokenType.CLOSE_BRACKET);
		addTokenInfo("\\[", TokenType.OPEN_ARRAY_BRACKET);
		addTokenInfo("\\]", TokenType.CLOSE_ARRAY_BRACKET);

		addTokenInfo(">", TokenType.COMPARE_GREATER);
		addTokenInfo("<", TokenType.COMPARE_LESSER);
		addTokenInfo("==", TokenType.COMPARE_EQUALS);
		addTokenInfo("!=", TokenType.COMPARE_NOT_EQUALS);
		
		addTokenInfo("=", TokenType.ASSIGNMENT);
		addTokenInfo(",", TokenType.COMMA);
		addTokenInfo(";", TokenType.SEMICOLON);

		addTokenInfo("\\!", TokenType.LOGICAL_NOT);
		
		addTokenInfo("\\+", TokenType.ALU_ADD);
		addTokenInfo("\\-", TokenType.ALU_SUB);
		addTokenInfo("\\*", TokenType.ALU_MUL);
		addTokenInfo("\\/", TokenType.ALU_DIV);
	}
	
	public void analyze(String source) {
		int currentLine = 0;
		
		String[] lines = source.split("\n");
		String line = null;
		
		while(currentLine < lines.length){
			//line = lines[currentLine].replaceAll("\t", "");
			line = lines[currentLine].trim();
			
			if(line.contains("//"))
				line = line.substring(0, line.indexOf("//"));
			
			while(!line.equals("")){
				boolean match = false;
				
				for(TokenInfo info : tokenInfos){
					Matcher matcher = info.regex.matcher(line);
					
					if(matcher.find()){
						match = true;
						
						String tok = matcher.group().trim();
						Token t = new Token(info.type, tok);
						t.lineInSourceCode = currentLine + 1;
						/*if((Debug.getDebugMode() & Debug.DEBUG_TOKENIZE_SOURCE) == Debug.DEBUG_TOKENIZE_SOURCE)
							System.out.println(tokens.size() + ": " + t);*/
						tokens.add(t);
						
						line = matcher.replaceFirst("");
						line = line.replaceAll("^\\s+", "");
						break;
					}
				}
				
				if(!match)
					Console.throwError("Unexpected token '" + line + "' in line: " + (currentLine + 1));
			}
			
			currentLine++;
		}
	}
	
	public ArrayList<Token> getTokens(){
		return tokens;
	}
	
	private void addTokenInfo(String regex, TokenType type){
		tokenInfos.add(new TokenInfo(Pattern.compile("^(" + regex + ")", Pattern.UNIX_LINES), type));
	}
}
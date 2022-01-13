package mpl.parser.lexical;

public class Token {
	public int lineInSourceCode = 0;
	public TokenType type;
	public String data;
	
	public Token(TokenType t, String text){
		this.type = t;
		this.data = text;
	}
	
	public Token(){
	}
	
	@Override
	public String toString(){
		return data;
	}
}
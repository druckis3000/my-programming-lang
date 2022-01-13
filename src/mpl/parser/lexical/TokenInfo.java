package mpl.parser.lexical;

import java.util.regex.Pattern;

public class TokenInfo {
	public final Pattern regex;
	public final TokenType type;
	
	public TokenInfo(Pattern regex, TokenType type){
		this.regex = regex;
		this.type = type;
	}
}
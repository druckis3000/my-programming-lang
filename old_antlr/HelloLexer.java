// Generated from Hello.g4 by ANTLR 4.6

    package spl.parser.syntactic;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HelloLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.6", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, FUNC=9, 
		IF=10, ELSE=11, WHILE=12, DO=13, FOR=14, BREAK=15, CONTINUE=16, RETURN=17, 
		TYPEDEF=18, AS=19, IN=20, STRUCT=21, NEW=22, DELETE=23, IMPORT=24, IntegerLiteral=25, 
		CharLiteral=26, HexLiteral=27, StringLiteral=28, NullLiteral=29, BooleanLiteral=30, 
		LPAREN=31, RPAREN=32, LBRACE=33, RBRACE=34, LBRACK=35, RBRACK=36, SEMI=37, 
		COMMA=38, DOT=39, COLON=40, STAR=41, DIV=42, ADD=43, SUB=44, POW=45, LOGICAL_AND=46, 
		LOGICAL_OR=47, BITWISE_AND=48, BITWISE_OR=49, BITWISE_XOR=50, BITWISE_NOT=51, 
		LESSER=52, GREATER=53, LESSER_EQUAL=54, GREATER_EQUAL=55, EQUAL=56, NOTEQUAL=57, 
		INC=58, DEC=59, NOT=60, Identifier=61, WS=62, COMMENT=63, LINE_COMMENT=64;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "FUNC", 
		"IF", "ELSE", "WHILE", "DO", "FOR", "BREAK", "CONTINUE", "RETURN", "TYPEDEF", 
		"AS", "IN", "STRUCT", "NEW", "DELETE", "IMPORT", "IntegerLiteral", "CharLiteral", 
		"HexLiteral", "HexNumeral", "HexDigits", "HexDigit", "SingleCharacter", 
		"StringLiteral", "StringCharacters", "StringCharacter", "EscapeSequence", 
		"LetterOrDigit", "NullLiteral", "BooleanLiteral", "LPAREN", "RPAREN", 
		"LBRACE", "RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", "COLON", 
		"STAR", "DIV", "ADD", "SUB", "POW", "LOGICAL_AND", "LOGICAL_OR", "BITWISE_AND", 
		"BITWISE_OR", "BITWISE_XOR", "BITWISE_NOT", "LESSER", "GREATER", "LESSER_EQUAL", 
		"GREATER_EQUAL", "EQUAL", "NOTEQUAL", "INC", "DEC", "NOT", "Identifier", 
		"WS", "COMMENT", "LINE_COMMENT"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'void'", "'bool'", "'string'", "'int'", "'short'", "'char'", "'_c_'", 
		"'='", "'function'", "'if'", "'else'", "'while'", "'do'", "'for'", "'break'", 
		"'continue'", "'return'", "'typedef'", "'as'", "'in'", "'struct'", "'new'", 
		"'delete'", "'import'", null, null, null, null, "'null'", null, "'('", 
		"')'", "'{'", "'}'", "'['", "']'", "';'", "','", "'.'", "':'", "'*'", 
		"'/'", "'+'", "'-'", null, "'&&'", "'||'", "'&'", "'|'", null, "'~'", 
		"'<'", "'>'", "'<='", "'>='", "'=='", "'!='", "'++'", "'--'", "'!'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, "FUNC", "IF", "ELSE", 
		"WHILE", "DO", "FOR", "BREAK", "CONTINUE", "RETURN", "TYPEDEF", "AS", 
		"IN", "STRUCT", "NEW", "DELETE", "IMPORT", "IntegerLiteral", "CharLiteral", 
		"HexLiteral", "StringLiteral", "NullLiteral", "BooleanLiteral", "LPAREN", 
		"RPAREN", "LBRACE", "RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", 
		"COLON", "STAR", "DIV", "ADD", "SUB", "POW", "LOGICAL_AND", "LOGICAL_OR", 
		"BITWISE_AND", "BITWISE_OR", "BITWISE_XOR", "BITWISE_NOT", "LESSER", "GREATER", 
		"LESSER_EQUAL", "GREATER_EQUAL", "EQUAL", "NOTEQUAL", "INC", "DEC", "NOT", 
		"Identifier", "WS", "COMMENT", "LINE_COMMENT"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public HelloLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Hello.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2B\u01c1\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3"+
		"\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b"+
		"\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3"+
		"\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3"+
		"\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3"+
		"\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3"+
		"\23\3\23\3\23\3\23\3\24\3\24\3\24\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3"+
		"\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3"+
		"\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\5\32\u0116\n\32\3\32\6\32\u0119"+
		"\n\32\r\32\16\32\u011a\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\5\33\u0125"+
		"\n\33\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\5\36\u012f\n\36\3\37\3\37"+
		"\3 \3 \3!\3!\5!\u0137\n!\3!\3!\3\"\6\"\u013c\n\"\r\"\16\"\u013d\3#\3#"+
		"\5#\u0142\n#\3$\3$\3$\3%\3%\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3\'\3\'"+
		"\3\'\3\'\5\'\u0157\n\'\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3"+
		"/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66"+
		"\3\67\3\67\3\67\38\38\38\39\39\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3?\3?"+
		"\3@\3@\3@\3A\3A\3A\3B\3B\3B\3C\3C\3C\3D\3D\3D\3E\3E\3F\6F\u019e\nF\rF"+
		"\16F\u019f\3G\6G\u01a3\nG\rG\16G\u01a4\3G\3G\3H\3H\3H\3H\7H\u01ad\nH\f"+
		"H\16H\u01b0\13H\3H\3H\3H\3H\3H\3I\3I\3I\3I\7I\u01bb\nI\fI\16I\u01be\13"+
		"I\3I\3I\3\u01ae\2J\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r"+
		"\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
		"\65\34\67\359\2;\2=\2?\2A\36C\2E\2G\2I\2K\37M O!Q\"S#U$W%Y&[\'](_)a*c"+
		"+e,g-i.k/m\60o\61q\62s\63u\64w\65y\66{\67}8\1779\u0081:\u0083;\u0085<"+
		"\u0087=\u0089>\u008b?\u008d@\u008fA\u0091B\3\2\13\3\2\62;\4\2ZZzz\5\2"+
		"\62;CHch\6\2\f\f\17\17))^^\4\2$$^^\13\2$$))\62\62^^ddhhppttvv\7\2&&\62"+
		";C\\aac|\5\2\13\f\17\17\"\"\4\2\f\f\17\17\u01c4\2\3\3\2\2\2\2\5\3\2\2"+
		"\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2"+
		"\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3"+
		"\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3"+
		"\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\2A\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3"+
		"\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2"+
		"\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2"+
		"i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3"+
		"\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081"+
		"\3\2\2\2\2\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2"+
		"\2\2\u008b\3\2\2\2\2\u008d\3\2\2\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\3\u0093"+
		"\3\2\2\2\5\u0098\3\2\2\2\7\u009d\3\2\2\2\t\u00a4\3\2\2\2\13\u00a8\3\2"+
		"\2\2\r\u00ae\3\2\2\2\17\u00b3\3\2\2\2\21\u00b7\3\2\2\2\23\u00b9\3\2\2"+
		"\2\25\u00c2\3\2\2\2\27\u00c5\3\2\2\2\31\u00ca\3\2\2\2\33\u00d0\3\2\2\2"+
		"\35\u00d3\3\2\2\2\37\u00d7\3\2\2\2!\u00dd\3\2\2\2#\u00e6\3\2\2\2%\u00ed"+
		"\3\2\2\2\'\u00f5\3\2\2\2)\u00f8\3\2\2\2+\u00fb\3\2\2\2-\u0102\3\2\2\2"+
		"/\u0106\3\2\2\2\61\u010d\3\2\2\2\63\u0115\3\2\2\2\65\u0124\3\2\2\2\67"+
		"\u0126\3\2\2\29\u0128\3\2\2\2;\u012c\3\2\2\2=\u0130\3\2\2\2?\u0132\3\2"+
		"\2\2A\u0134\3\2\2\2C\u013b\3\2\2\2E\u0141\3\2\2\2G\u0143\3\2\2\2I\u0146"+
		"\3\2\2\2K\u0148\3\2\2\2M\u0156\3\2\2\2O\u0158\3\2\2\2Q\u015a\3\2\2\2S"+
		"\u015c\3\2\2\2U\u015e\3\2\2\2W\u0160\3\2\2\2Y\u0162\3\2\2\2[\u0164\3\2"+
		"\2\2]\u0166\3\2\2\2_\u0168\3\2\2\2a\u016a\3\2\2\2c\u016c\3\2\2\2e\u016e"+
		"\3\2\2\2g\u0170\3\2\2\2i\u0172\3\2\2\2k\u0174\3\2\2\2m\u0176\3\2\2\2o"+
		"\u0179\3\2\2\2q\u017c\3\2\2\2s\u017e\3\2\2\2u\u0180\3\2\2\2w\u0182\3\2"+
		"\2\2y\u0184\3\2\2\2{\u0186\3\2\2\2}\u0188\3\2\2\2\177\u018b\3\2\2\2\u0081"+
		"\u018e\3\2\2\2\u0083\u0191\3\2\2\2\u0085\u0194\3\2\2\2\u0087\u0197\3\2"+
		"\2\2\u0089\u019a\3\2\2\2\u008b\u019d\3\2\2\2\u008d\u01a2\3\2\2\2\u008f"+
		"\u01a8\3\2\2\2\u0091\u01b6\3\2\2\2\u0093\u0094\7x\2\2\u0094\u0095\7q\2"+
		"\2\u0095\u0096\7k\2\2\u0096\u0097\7f\2\2\u0097\4\3\2\2\2\u0098\u0099\7"+
		"d\2\2\u0099\u009a\7q\2\2\u009a\u009b\7q\2\2\u009b\u009c\7n\2\2\u009c\6"+
		"\3\2\2\2\u009d\u009e\7u\2\2\u009e\u009f\7v\2\2\u009f\u00a0\7t\2\2\u00a0"+
		"\u00a1\7k\2\2\u00a1\u00a2\7p\2\2\u00a2\u00a3\7i\2\2\u00a3\b\3\2\2\2\u00a4"+
		"\u00a5\7k\2\2\u00a5\u00a6\7p\2\2\u00a6\u00a7\7v\2\2\u00a7\n\3\2\2\2\u00a8"+
		"\u00a9\7u\2\2\u00a9\u00aa\7j\2\2\u00aa\u00ab\7q\2\2\u00ab\u00ac\7t\2\2"+
		"\u00ac\u00ad\7v\2\2\u00ad\f\3\2\2\2\u00ae\u00af\7e\2\2\u00af\u00b0\7j"+
		"\2\2\u00b0\u00b1\7c\2\2\u00b1\u00b2\7t\2\2\u00b2\16\3\2\2\2\u00b3\u00b4"+
		"\7a\2\2\u00b4\u00b5\7e\2\2\u00b5\u00b6\7a\2\2\u00b6\20\3\2\2\2\u00b7\u00b8"+
		"\7?\2\2\u00b8\22\3\2\2\2\u00b9\u00ba\7h\2\2\u00ba\u00bb\7w\2\2\u00bb\u00bc"+
		"\7p\2\2\u00bc\u00bd\7e\2\2\u00bd\u00be\7v\2\2\u00be\u00bf\7k\2\2\u00bf"+
		"\u00c0\7q\2\2\u00c0\u00c1\7p\2\2\u00c1\24\3\2\2\2\u00c2\u00c3\7k\2\2\u00c3"+
		"\u00c4\7h\2\2\u00c4\26\3\2\2\2\u00c5\u00c6\7g\2\2\u00c6\u00c7\7n\2\2\u00c7"+
		"\u00c8\7u\2\2\u00c8\u00c9\7g\2\2\u00c9\30\3\2\2\2\u00ca\u00cb\7y\2\2\u00cb"+
		"\u00cc\7j\2\2\u00cc\u00cd\7k\2\2\u00cd\u00ce\7n\2\2\u00ce\u00cf\7g\2\2"+
		"\u00cf\32\3\2\2\2\u00d0\u00d1\7f\2\2\u00d1\u00d2\7q\2\2\u00d2\34\3\2\2"+
		"\2\u00d3\u00d4\7h\2\2\u00d4\u00d5\7q\2\2\u00d5\u00d6\7t\2\2\u00d6\36\3"+
		"\2\2\2\u00d7\u00d8\7d\2\2\u00d8\u00d9\7t\2\2\u00d9\u00da\7g\2\2\u00da"+
		"\u00db\7c\2\2\u00db\u00dc\7m\2\2\u00dc \3\2\2\2\u00dd\u00de\7e\2\2\u00de"+
		"\u00df\7q\2\2\u00df\u00e0\7p\2\2\u00e0\u00e1\7v\2\2\u00e1\u00e2\7k\2\2"+
		"\u00e2\u00e3\7p\2\2\u00e3\u00e4\7w\2\2\u00e4\u00e5\7g\2\2\u00e5\"\3\2"+
		"\2\2\u00e6\u00e7\7t\2\2\u00e7\u00e8\7g\2\2\u00e8\u00e9\7v\2\2\u00e9\u00ea"+
		"\7w\2\2\u00ea\u00eb\7t\2\2\u00eb\u00ec\7p\2\2\u00ec$\3\2\2\2\u00ed\u00ee"+
		"\7v\2\2\u00ee\u00ef\7{\2\2\u00ef\u00f0\7r\2\2\u00f0\u00f1\7g\2\2\u00f1"+
		"\u00f2\7f\2\2\u00f2\u00f3\7g\2\2\u00f3\u00f4\7h\2\2\u00f4&\3\2\2\2\u00f5"+
		"\u00f6\7c\2\2\u00f6\u00f7\7u\2\2\u00f7(\3\2\2\2\u00f8\u00f9\7k\2\2\u00f9"+
		"\u00fa\7p\2\2\u00fa*\3\2\2\2\u00fb\u00fc\7u\2\2\u00fc\u00fd\7v\2\2\u00fd"+
		"\u00fe\7t\2\2\u00fe\u00ff\7w\2\2\u00ff\u0100\7e\2\2\u0100\u0101\7v\2\2"+
		"\u0101,\3\2\2\2\u0102\u0103\7p\2\2\u0103\u0104\7g\2\2\u0104\u0105\7y\2"+
		"\2\u0105.\3\2\2\2\u0106\u0107\7f\2\2\u0107\u0108\7g\2\2\u0108\u0109\7"+
		"n\2\2\u0109\u010a\7g\2\2\u010a\u010b\7v\2\2\u010b\u010c\7g\2\2\u010c\60"+
		"\3\2\2\2\u010d\u010e\7k\2\2\u010e\u010f\7o\2\2\u010f\u0110\7r\2\2\u0110"+
		"\u0111\7q\2\2\u0111\u0112\7t\2\2\u0112\u0113\7v\2\2\u0113\62\3\2\2\2\u0114"+
		"\u0116\7/\2\2\u0115\u0114\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0118\3\2"+
		"\2\2\u0117\u0119\t\2\2\2\u0118\u0117\3\2\2\2\u0119\u011a\3\2\2\2\u011a"+
		"\u0118\3\2\2\2\u011a\u011b\3\2\2\2\u011b\64\3\2\2\2\u011c\u011d\7)\2\2"+
		"\u011d\u011e\5? \2\u011e\u011f\7)\2\2\u011f\u0125\3\2\2\2\u0120\u0121"+
		"\7)\2\2\u0121\u0122\5G$\2\u0122\u0123\7)\2\2\u0123\u0125\3\2\2\2\u0124"+
		"\u011c\3\2\2\2\u0124\u0120\3\2\2\2\u0125\66\3\2\2\2\u0126\u0127\59\35"+
		"\2\u01278\3\2\2\2\u0128\u0129\7\62\2\2\u0129\u012a\t\3\2\2\u012a\u012b"+
		"\5;\36\2\u012b:\3\2\2\2\u012c\u012e\5=\37\2\u012d\u012f\5;\36\2\u012e"+
		"\u012d\3\2\2\2\u012e\u012f\3\2\2\2\u012f<\3\2\2\2\u0130\u0131\t\4\2\2"+
		"\u0131>\3\2\2\2\u0132\u0133\n\5\2\2\u0133@\3\2\2\2\u0134\u0136\7$\2\2"+
		"\u0135\u0137\5C\"\2\u0136\u0135\3\2\2\2\u0136\u0137\3\2\2\2\u0137\u0138"+
		"\3\2\2\2\u0138\u0139\7$\2\2\u0139B\3\2\2\2\u013a\u013c\5E#\2\u013b\u013a"+
		"\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013b\3\2\2\2\u013d\u013e\3\2\2\2\u013e"+
		"D\3\2\2\2\u013f\u0142\n\6\2\2\u0140\u0142\5G$\2\u0141\u013f\3\2\2\2\u0141"+
		"\u0140\3\2\2\2\u0142F\3\2\2\2\u0143\u0144\7^\2\2\u0144\u0145\t\7\2\2\u0145"+
		"H\3\2\2\2\u0146\u0147\t\b\2\2\u0147J\3\2\2\2\u0148\u0149\7p\2\2\u0149"+
		"\u014a\7w\2\2\u014a\u014b\7n\2\2\u014b\u014c\7n\2\2\u014cL\3\2\2\2\u014d"+
		"\u014e\7v\2\2\u014e\u014f\7t\2\2\u014f\u0150\7w\2\2\u0150\u0157\7g\2\2"+
		"\u0151\u0152\7h\2\2\u0152\u0153\7c\2\2\u0153\u0154\7n\2\2\u0154\u0155"+
		"\7u\2\2\u0155\u0157\7g\2\2\u0156\u014d\3\2\2\2\u0156\u0151\3\2\2\2\u0157"+
		"N\3\2\2\2\u0158\u0159\7*\2\2\u0159P\3\2\2\2\u015a\u015b\7+\2\2\u015bR"+
		"\3\2\2\2\u015c\u015d\7}\2\2\u015dT\3\2\2\2\u015e\u015f\7\177\2\2\u015f"+
		"V\3\2\2\2\u0160\u0161\7]\2\2\u0161X\3\2\2\2\u0162\u0163\7_\2\2\u0163Z"+
		"\3\2\2\2\u0164\u0165\7=\2\2\u0165\\\3\2\2\2\u0166\u0167\7.\2\2\u0167^"+
		"\3\2\2\2\u0168\u0169\7\60\2\2\u0169`\3\2\2\2\u016a\u016b\7<\2\2\u016b"+
		"b\3\2\2\2\u016c\u016d\7,\2\2\u016dd\3\2\2\2\u016e\u016f\7\61\2\2\u016f"+
		"f\3\2\2\2\u0170\u0171\7-\2\2\u0171h\3\2\2\2\u0172\u0173\7/\2\2\u0173j"+
		"\3\2\2\2\u0174\u0175\7`\2\2\u0175l\3\2\2\2\u0176\u0177\7(\2\2\u0177\u0178"+
		"\7(\2\2\u0178n\3\2\2\2\u0179\u017a\7~\2\2\u017a\u017b\7~\2\2\u017bp\3"+
		"\2\2\2\u017c\u017d\7(\2\2\u017dr\3\2\2\2\u017e\u017f\7~\2\2\u017ft\3\2"+
		"\2\2\u0180\u0181\7`\2\2\u0181v\3\2\2\2\u0182\u0183\7\u0080\2\2\u0183x"+
		"\3\2\2\2\u0184\u0185\7>\2\2\u0185z\3\2\2\2\u0186\u0187\7@\2\2\u0187|\3"+
		"\2\2\2\u0188\u0189\7>\2\2\u0189\u018a\7?\2\2\u018a~\3\2\2\2\u018b\u018c"+
		"\7@\2\2\u018c\u018d\7?\2\2\u018d\u0080\3\2\2\2\u018e\u018f\7?\2\2\u018f"+
		"\u0190\7?\2\2\u0190\u0082\3\2\2\2\u0191\u0192\7#\2\2\u0192\u0193\7?\2"+
		"\2\u0193\u0084\3\2\2\2\u0194\u0195\7-\2\2\u0195\u0196\7-\2\2\u0196\u0086"+
		"\3\2\2\2\u0197\u0198\7/\2\2\u0198\u0199\7/\2\2\u0199\u0088\3\2\2\2\u019a"+
		"\u019b\7#\2\2\u019b\u008a\3\2\2\2\u019c\u019e\5I%\2\u019d\u019c\3\2\2"+
		"\2\u019e\u019f\3\2\2\2\u019f\u019d\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0\u008c"+
		"\3\2\2\2\u01a1\u01a3\t\t\2\2\u01a2\u01a1\3\2\2\2\u01a3\u01a4\3\2\2\2\u01a4"+
		"\u01a2\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\u01a7\bG"+
		"\2\2\u01a7\u008e\3\2\2\2\u01a8\u01a9\7\61\2\2\u01a9\u01aa\7,\2\2\u01aa"+
		"\u01ae\3\2\2\2\u01ab\u01ad\13\2\2\2\u01ac\u01ab\3\2\2\2\u01ad\u01b0\3"+
		"\2\2\2\u01ae\u01af\3\2\2\2\u01ae\u01ac\3\2\2\2\u01af\u01b1\3\2\2\2\u01b0"+
		"\u01ae\3\2\2\2\u01b1\u01b2\7,\2\2\u01b2\u01b3\7\61\2\2\u01b3\u01b4\3\2"+
		"\2\2\u01b4\u01b5\bH\2\2\u01b5\u0090\3\2\2\2\u01b6\u01b7\7\61\2\2\u01b7"+
		"\u01b8\7\61\2\2\u01b8\u01bc\3\2\2\2\u01b9\u01bb\n\n\2\2\u01ba\u01b9\3"+
		"\2\2\2\u01bb\u01be\3\2\2\2\u01bc\u01ba\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd"+
		"\u01bf\3\2\2\2\u01be\u01bc\3\2\2\2\u01bf\u01c0\bI\2\2\u01c0\u0092\3\2"+
		"\2\2\17\2\u0115\u011a\u0124\u012e\u0136\u013d\u0141\u0156\u019f\u01a4"+
		"\u01ae\u01bc\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
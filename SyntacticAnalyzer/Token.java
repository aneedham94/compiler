package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenType type;
	public String spelling;
	public Token(TokenType type, String spelling){
		this.type = type;
		this.spelling = spelling;
	}
}

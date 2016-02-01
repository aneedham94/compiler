package miniJava.SyntacticAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;

public class Scanner {
	private BufferedReader reader;
	private boolean eot;
	private char current;
	private String spelling;
	public Scanner(BufferedReader reader) throws IOException{
		this.reader = reader;
		eot = false;
		spelling = "";
		readChar();
	}
	
	public Token scan() throws IOException{
		spelling = "";
		skipWhite();
		TokenType type = findType();
		while(type == TokenType.COMMENT){
			spelling = "";
			skipWhite();
			type = findType();
		}
		return new Token(type, spelling);
	}
	
	private TokenType findType() throws IOException{
		if(eot) return TokenType.EOT;
		switch(current){
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			while(Character.isDigit(current)) add();
			return TokenType.NUM;
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': 
		case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v':
		case 'w': case 'x': case 'y': case 'z': case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
		case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
		case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
			while((current >= 48 && current <= 57) || (current >= 65 && current <= 90) || (current >= 97 && current <= 122)) add();
			if(spelling.equals("class")) return TokenType.CLASS;
			if(spelling.equals("void")) return TokenType.VOID;
			if(spelling.equals("public")) return TokenType.PUBLIC;
			if(spelling.equals("private")) return TokenType.PRIVATE;
			if(spelling.equals("static")) return TokenType.STATIC;
			if(spelling.equals("int")) return TokenType.INT;
			if(spelling.equals("boolean")) return TokenType.BOOLEAN;
			if(spelling.equals("this")) return TokenType.THIS;
			if(spelling.equals("return")) return TokenType.RETURN;
			if(spelling.equals("if")) return TokenType.IF;
			if(spelling.equals("else")) return TokenType.ELSE;
			if(spelling.equals("while")) return TokenType.WHILE;
			if(spelling.equals("true")) return TokenType.TRUE;
			if(spelling.equals("false")) return TokenType.FALSE;
			if(spelling.equals("new")) return TokenType.NEW;
			return TokenType.ID;
		case '{':
			add();
			return TokenType.LCURL;
		case '}':
			add();
			return TokenType.RCURL;
		case ';':
			add();
			return TokenType.SEMI;
		case '(':
			add();
			return TokenType.LPAREN;
		case ')':
			add();
			return TokenType.RPAREN;
		case '[':
			add();
			return TokenType.LBRACK;
		case ']':
			add();
			return TokenType.RBRACK;
		case ',':
			add();
			return TokenType.COMMA;
		case '.':
			add();
			return TokenType.DOT;
		case '=':
			add();
			if(current == '='){
				add();
				return TokenType.EQEQ;
			}
			else return TokenType.EQ;
		case '<':
			add();
			if(current == '='){
				add();
				return TokenType.LTE;
			}
			else return TokenType.LT;
		case '>':
			add();
			if(current == '='){
				add();
				return TokenType.GTE;
			}
			else return TokenType.GT;
		case '!':
			add();
			if(current == '+'){
				add();
				return TokenType.NEQ;
			}
			else return TokenType.NOT;
		case '&':
			add();
			if(current == '&'){
				add();
				return TokenType.AND;
			}
			else throw new IOException("" + current);
		case '|':
			add();
			if(current == '|'){
				add();
				return TokenType.OR;
			}
			else throw new IOException("" + current);
		case '+':
			add();
			return TokenType.PLUS;
		case '-':
			add();
			return TokenType.MINUS;
		case '*':
			add();
			return TokenType.TIMES;
		case '/':
			add();
			if(current == '/'){
				skip();
				while(current != '\n') skip();
				skip();
				return TokenType.COMMENT;
			}
			else if(current == '*'){
				skip();
				boolean comment = true;
				while(comment){
					while(current != '*') skip();
					skip();
					if(current == '/') comment = false;
					skip();
				}
				return TokenType.COMMENT;
			}
			return TokenType.DIV;
		default:
			throw new IOException("" + current);
		}
	}
	
	private void add() throws IOException{
		spelling = spelling + current;
		nextChar();
	}
	
	private void skip() throws IOException{
		
		System.out.println("Skipping character " + visible(current));
		nextChar();
	}
	
	private void skipWhite() throws IOException{
		while(current == ' ' || current == '\n' || current == '\r' || current == '\t'){
			nextChar();
		}
	}
	
	private void nextChar() throws IOException{
		if(!eot) readChar();
	}
	
	private void readChar() throws IOException{
		int next = reader.read();
		if(next != -1) current = (char)next;
		else eot = true;
	}
	
	/**
	 * Utility method to help with debugging what characters are being dealt with if they are whitespace characters.
	 * @param c
	 * @return
	 */
	public static String visible(char c){
		if(c == '\n'){
			return "\\n";
		}
		else if(c == '\r'){
			return "\\r";
		}
		else if(c == '\t'){
			return "\\t";
		}
		else if(c == ' '){
			return "\\sp";
		}
		else return "" + c;
	}
}

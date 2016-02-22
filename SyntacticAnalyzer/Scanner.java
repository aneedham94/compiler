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
		TokenKind type = findType();
		while(type == TokenKind.COMMENT){
			spelling = "";
			skipWhite();
			type = findType();
		}
		return new Token(type, spelling, null);
	}
	
	private TokenKind findType() throws IOException{
		if(eot) return TokenKind.EOT;
		switch(current){
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			while(Character.isDigit(current)) add();
			return TokenKind.NUM;
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': 
		case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v':
		case 'w': case 'x': case 'y': case 'z': case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
		case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
		case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
			while((current >= 48 && current <= 57) || (current >= 65 && current <= 90) || (current >= 97 && current <= 122) || current=='_') add();
			if(spelling.equals("class")) return TokenKind.CLASS;
			if(spelling.equals("void")) return TokenKind.VOID;
			if(spelling.equals("public")) return TokenKind.PUBLIC;
			if(spelling.equals("private")) return TokenKind.PRIVATE;
			if(spelling.equals("static")) return TokenKind.STATIC;
			if(spelling.equals("int")) return TokenKind.INT;
			if(spelling.equals("boolean")) return TokenKind.BOOLEAN;
			if(spelling.equals("this")) return TokenKind.THIS;
			if(spelling.equals("return")) return TokenKind.RETURN;
			if(spelling.equals("if")) return TokenKind.IF;
			if(spelling.equals("else")) return TokenKind.ELSE;
			if(spelling.equals("while")) return TokenKind.WHILE;
			if(spelling.equals("true")) return TokenKind.TRUE;
			if(spelling.equals("false")) return TokenKind.FALSE;
			if(spelling.equals("new")) return TokenKind.NEW;
			return TokenKind.ID;
		case '{':
			add();
			return TokenKind.LCURL;
		case '}':
			add();
			return TokenKind.RCURL;
		case ';':
			add();
			return TokenKind.SEMI;
		case '(':
			add();
			return TokenKind.LPAREN;
		case ')':
			add();
			return TokenKind.RPAREN;
		case '[':
			add();
			return TokenKind.LBRACK;
		case ']':
			add();
			return TokenKind.RBRACK;
		case ',':
			add();
			return TokenKind.COMMA;
		case '.':
			add();
			return TokenKind.DOT;
		case '=':
			add();
			if(current == '='){
				add();
				return TokenKind.EQEQ;
			}
			else return TokenKind.EQ;
		case '<':
			add();
			if(current == '='){
				add();
				return TokenKind.LTE;
			}
			else return TokenKind.LT;
		case '>':
			add();
			if(current == '='){
				add();
				return TokenKind.GTE;
			}
			else return TokenKind.GT;
		case '!':
			add();
			if(current == '='){
				add();
				return TokenKind.NEQ;
			}
			else return TokenKind.NOT;
		case '&':
			add();
			if(current == '&'){
				add();
				return TokenKind.AND;
			}
			else return TokenKind.ERROR;
		case '|':
			add();
			if(current == '|'){
				add();
				return TokenKind.OR;
			}
			else return TokenKind.ERROR;
		case '+':
			add();
			return TokenKind.PLUS;
		case '-':
			add();
			if(current == '-'){
				add();
				return TokenKind.ERROR;
			}
			return TokenKind.MINUS;
		case '*':
			add();
			return TokenKind.TIMES;
		case '/':
			add();
			if(current == '/' && !eot){
				skip();
				while(current != '\n' && current != '\r'){
					if(eot) break;
					skip();
				}
				skip();
				return TokenKind.COMMENT;
			}
			
			else if(current == '*' && !eot){
				skip();
				boolean comment = true;
				while(comment){
					while(current != '*'){
						if(eot) throw new IOException("Unterminated comment");
						skip();
					}
					if(eot) throw new IOException("Unterminated comment");
					skip();
					if(current == '/'){
						skip();
						comment = false;
					}
				}
				return TokenKind.COMMENT;
			}
			return TokenKind.DIV;
		default:
			return TokenKind.ERROR;
		}
	}
	
	private void add() throws IOException{
		spelling = spelling + current;
		nextChar();
	}
	
	private void skip() throws IOException{
		nextChar();
	}
	
	private void skipWhite() throws IOException{
		while(current == ' ' || current == '\n' || current == '\r' || current == '\t'){
			if(eot) break;
			nextChar();
		}
	}
	
	private void nextChar() throws IOException{
		if(!eot) readChar();
	}
	
	private void readChar() throws IOException{
		int next = reader.read();
		if(next != -1) current = (char)next;
		else{
			eot = true;
		}
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

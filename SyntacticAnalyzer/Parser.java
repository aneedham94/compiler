package miniJava.SyntacticAnalyzer;

import miniJava.SyntaxException;

public class Parser {
	private Scanner scanner;
	private Token token;
	
	public Parser(Scanner scanner){
		this.scanner = scanner;
	}
	
	//GTG
	public void parse() throws SyntaxException{
		token = scanner.scan();
		parseProgram();
	}
	
	//GTG
	private void parseProgram() throws SyntaxException{
		while(token.type != TokenType.EOT){
			parseClassDeclaration();
		}
		accept(TokenType.EOT);
	}
	
	//GTG
	private void parseClassDeclaration() throws SyntaxException{
		accept(TokenType.CLASS);
		accept(TokenType.ID);
		accept(TokenType.LCURL);
		while(token.type != TokenType.RCURL){
			parseVisibility();
			parseAccess();
			if(token.type == TokenType.VOID){
				acceptIt();
				accept(TokenType.ID);
				parseMethodDeclaration();
			}
			else{
				parseType();
				accept(TokenType.ID);
				if(token.type == TokenType.SEMI) acceptIt();
				else parseMethodDeclaration();
			}
		}
		accept(TokenType.RCURL);
	}
	
	//GTG
	private void parseMethodDeclaration() throws SyntaxException{
		accept(TokenType.LPAREN);
		if(token.type != TokenType.RPAREN){
			parseParameterList();
		}
		accept(TokenType.RPAREN);
		accept(TokenType.LCURL);
		while(token.type != TokenType.RCURL){
			parseStatement();
		}
		accept(TokenType.RCURL);
	}
	
	//GTG
	private void parseVisibility(){
		switch(token.type){
		case PUBLIC:
			acceptIt();
			break;
		case PRIVATE:
			acceptIt();
			break;
		default:
			break;
		}
	}
	
	//GTG
	private void parseAccess(){
		switch(token.type){
		case STATIC:
			acceptIt();
			break;
		default:
			break;
		}
	}
	
	//GTG
	private void parseType() throws SyntaxException{
		switch(token.type){
		case INT:
			acceptIt();
			if(token.type == TokenType.LBRACK){
				acceptIt();
				accept(TokenType.RBRACK);
			}
			break;
		case BOOLEAN:
			acceptIt();
			break;
		case ID:
			acceptIt();
			if(token.type == TokenType.LBRACK){
				acceptIt();
				accept(TokenType.RBRACK);
			}
			break;
		default:
			parseException("Expecting token of types INT, BOOLEAN, or ID, but got token of type " + token.type + ".");
			break;
		}
	}
	
	//GTG
	private void parseParameterList() throws SyntaxException{
		parseType();
		accept(TokenType.ID);
		while(token.type == TokenType.COMMA){
			acceptIt();
			parseType();
			accept(TokenType.ID);
		}
	}
	
	//GTG
	private void parseArgumentList() throws SyntaxException{
		parseExpression();
		while(token.type == TokenType.COMMA){
			acceptIt();
			parseExpression();
		}
	}
	
	//GTG
	private void parseReference() throws SyntaxException{
		switch(token.type){
		case THIS:
			acceptIt();
			break;
		case ID:
			acceptIt();
			break;
		default:
			parseException("Expecting token of types THIS or ID but got token of type " + token.type + ".");
			break;
		}
		while(token.type == TokenType.DOT){
			acceptIt();
			accept(TokenType.ID);
		}
	}
	
	private void parseArrayReference() throws SyntaxException{
		accept(TokenType.ID);
		accept(TokenType.LBRACK);
		parseExpression();
		accept(TokenType.RBRACK);
	}
	
	//GTG
	private void parseStatement() throws SyntaxException{
		switch(token.type){
		case LCURL:
			acceptIt();
			while(token.type != TokenType.RCURL){
				parseStatement();
			}
			accept(TokenType.RCURL);
			break;
		case INT:
			parseType();
			accept(TokenType.ID);
			accept(TokenType.EQ);
			parseExpression();
			accept(TokenType.SEMI);
			break;
		case BOOLEAN:
			parseType();
			accept(TokenType.ID);
			accept(TokenType.EQ);
			parseExpression();
			accept(TokenType.SEMI);
			break;
		case THIS:
			parseReference();
			if(token.type == TokenType.EQ){
				acceptIt();
				parseExpression();
				accept(TokenType.SEMI);
			}
			else if(token.type == TokenType.LPAREN){
				acceptIt();
				if(token.type != TokenType.RPAREN) parseArgumentList();
				accept(TokenType.RPAREN);
				accept(TokenType.SEMI);
			}
			break;
		case RETURN:
			acceptIt();
			if(token.type != TokenType.SEMI) parseExpression();
			accept(TokenType.SEMI);
			break;
		case IF:
			acceptIt();
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
			parseStatement();
			if(token.type == TokenType.ELSE){
				acceptIt();
				parseStatement();
			}
			break;
		case WHILE:
			acceptIt();
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
			parseStatement();
			break;
		case ID:
			acceptIt();
			if(token.type == TokenType.EQ){
				acceptIt();
				parseExpression();
				accept(TokenType.SEMI);
			}
			else if(token.type == TokenType.LPAREN){
				acceptIt();
				if(token.type != TokenType.RPAREN) parseArgumentList();
				accept(TokenType.RPAREN);
				accept(TokenType.SEMI);
			}
			else if(token.type == TokenType.LBRACK){
				acceptIt();
				if(token.type != TokenType.RBRACK){
					parseExpression();
					accept(TokenType.RBRACK);
					accept(TokenType.EQ);
					parseExpression();
					accept(TokenType.SEMI);
				}
				else{
					acceptIt();
					accept(TokenType.ID);
					accept(TokenType.EQ);
					parseExpression();
					accept(TokenType.SEMI);
				}
			}
			else if(token.type == TokenType.ID){
				acceptIt();
				accept(TokenType.EQ);
				parseExpression();
				accept(TokenType.SEMI);
			}
			else parseException("Expecting token of types EQ, LPAREN, ID or LBRACK but got token of type " + token.type + ".");
			break;
		default:
			parseException("Expecting token of types LCURL, INT, BOOLEAN, THIS, RETURN, IF, WHILE, or ID but got token of type " + token.type + ".");
			break;
		}
	}
	
	//GTG
	private void parseExpression() throws SyntaxException{
		switch(token.type){
		case THIS:
			parseReference();
			if(token.type == TokenType.LPAREN){
				acceptIt();
				parseArgumentList();
				accept(TokenType.RPAREN);
			}
			break;
		case UNOP:
			acceptIt();
			parseExpression();
			break;
		case LPAREN:
			acceptIt();
			parseExpression();
			accept(TokenType.RPAREN);
			break;
		case NUM:
			acceptIt();
			break;
		case TRUE:
			acceptIt();
			break;
		case FALSE:
			acceptIt();
			break;
		case NEW:
			acceptIt();
			if(token.type == TokenType.ID){
				acceptIt();
				if(token.type == TokenType.LPAREN){
					acceptIt();
					accept(TokenType.RPAREN);
				}
				else if(token.type == TokenType.LBRACK){
					acceptIt();
					parseExpression();
					accept(TokenType.RBRACK);
				}
				else parseException("Expecting token of type LPAREN or RPAREN but got token of type " + token.type + ".");
			}
			else if(token.type == TokenType.INT){
				acceptIt();
				accept(TokenType.LBRACK);
				parseExpression();
				accept(TokenType.RBRACK);
			}
			else parseException("Expecting token of type ID or INT but got token of type " + token.type + ".");
			break;
		case ID:
			acceptIt();
			if(token.type == TokenType.LBRACK){
				acceptIt();
				parseExpression();
				accept(TokenType.RBRACK);
			}
			else if(token.type == TokenType.DOT){
				while(token.type == TokenType.DOT){
					acceptIt();
					accept(TokenType.ID);
				}
				if(token.type == TokenType.LPAREN){
					acceptIt();
					parseArgumentList();
					accept(TokenType.RPAREN);
				}
			}
			break;
		default:
			parseException("Expecting token of type THIS, UNOP, LPAREN, NUM, TRUE, FALSE, NEW, or ID but got token of type " + token.type + ".");
			break;
		}
	}
	
	private void acceptIt(){
		try{
			accept(token.type);
		} catch(SyntaxException e){}
	}
	
	private void accept(TokenType type)throws SyntaxException{
		if(token.type == type) token = scanner.scan();
		else parseException("Expecting token of type " + type + " but got token of type " + token.type + ".");
	}
	
	private void parseException(String message) throws SyntaxException{
		throw new SyntaxException(message);
	}
}

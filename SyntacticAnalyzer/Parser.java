package miniJava.SyntacticAnalyzer;

import miniJava.SyntaxError;
import miniJava.SyntaxException;

public class Parser {
	private Scanner scanner;
	private Token token;
	
	public Parser(Scanner scanner){
		this.scanner = scanner;
	}
	
	public void parse(){
		token = scanner.scan();
		parseProgram();
	}
	
	private void parseProgram(){
		while(token.type != TokenType.EOT){
			parseClassDeclaration();
		}
		accept(TokenType.EOT);
	}
	
	private void parseClassDeclaration(){
		accept(TokenType.CLASS);
		accept(TokenType.ID);
		accept(TokenType.LCURL);
		while(token.type != TokenType.RCURL){
			//parse Field and if that fails parse Method
		}
		accept(TokenType.RCURL);
	}
	
	private void parseFieldDeclaration(){
		parseVisibility();
		parseAccess();
		parseType();
		accept(TokenType.ID);
		accept(TokenType.SEMI);
	}
	
	private void parseMethodDeclaration(){
		parseVisibility();
		parseAccess();
		//parse between type and void
		accept(TokenType.ID);
		accept(TokenType.LPAREN);
		//parse ParameterList?
		accept(TokenType.RPAREN);
		accept(TokenType.LCURL);
		//parse Statement*
		accept(TokenType.RCURL);
	}
	
	private void parseVisibility(){
		switch(token.type){
		case PUBLIC:
			acceptIt();
		case PRIVATE:
			acceptIt();
		default:
			
		}
	}
	
	private void parseAccess(){
		switch(token.type){
		case STATIC:
			acceptIt();
		default:
			
		}
	}
	
	private void parseType(){
		switch(token.type){
		case INT:
			
		case BOOLEAN:
			
		case ID:
			
		default:
			//error
		}
			
	}
	
	private void parseParameterList(){
		parseType();
		accept(TokenType.ID);
		while(token.type == TokenType.COMMA){
			acceptIt();
			parseType();
			accept(TokenType.ID);
		}
	}
	
	private void parseArgumentList(){
		parseExpression();
		while(token.type == TokenType.COMMA){
			acceptIt();
			parseExpression();
		}
	}
	
	private void parseReference(){
		switch(token.type){
		case THIS:
			acceptIt();
		case ID:
			acceptIt();
		default:
			//error
		}
		while(token.type == TokenType.DOT){
			acceptIt();
			accept(TokenType.ID);
		}
	}
	
	private void parseArrayReference(){
		accept(TokenType.ID);
		accept(TokenType.LBRACK);
		parseExpression();
		accept(TokenType.RBRACK);
	}
	
	private void parseStatement(){
		
	}
	
	private void parseExpression(){
		
	}
	
	private void acceptIt(){
		accept(token.type);
	}
	
	private void accept(TokenType type){
		if(token.type == type) scanner.scan();
		//else error
	}
	
	private void parseException(String message) throws SyntaxException{
		throw new SyntaxException(message);
	}
	
	private void parseError(String message) throws SyntaxError{
		throw new SyntaxError(message);
	}
}

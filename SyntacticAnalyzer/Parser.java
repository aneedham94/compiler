package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import java.io.IOException;
import java.util.LinkedList;

public class Parser {
	private Scanner scanner;
	private Token token;
	//This is just used for readability when passing null as source position
	private SourcePosition posn;
	
	public Parser(Scanner scanner){
		this.scanner = scanner;
	}
	
	public AST parse() throws SyntaxException, IOException{
		token = scanner.scan();
		return parseProgram();
	}
	
	private Package parseProgram() throws SyntaxException, IOException{
		posn = token.posn;
		ClassDeclList classes = new ClassDeclList();
		while(token.kind != TokenKind.EOT){
			classes.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		return new Package(classes, posn);
	}
	
	private ClassDecl parseClassDeclaration() throws SyntaxException, IOException{
		accept(TokenKind.CLASS);
		String name = token.spelling;
		posn = token.posn;
		accept(TokenKind.ID);
		accept(TokenKind.LCURL);
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		while(token.kind != TokenKind.RCURL){
			MemberDecl member = parseMethodOrField();
			if(member instanceof MethodDecl) methods.add((MethodDecl)member);
			else fields.add((FieldDecl)member);
		}
		accept(TokenKind.RCURL);
		return new ClassDecl(name, fields, methods, posn);
	}
	
	private MemberDecl parseMethodOrField() throws SyntaxException, IOException{
		boolean isPriv = parseVisibility();
		boolean isStat = parseAccess();
		Type t;
		if(token.kind == TokenKind.VOID){
			t = new BaseType(TypeKind.VOID, posn);
			acceptIt();
			String name = token.spelling;
			posn = token.posn;
			accept(TokenKind.ID);
			return parseMethodDeclaration(isPriv, isStat, t, name, posn);
		}
		else{
			t = parseType();
			String name = token.spelling;
			posn = token.posn;
			accept(TokenKind.ID);
			if(token.kind == TokenKind.SEMI){
				acceptIt();
				return new FieldDecl(isPriv, isStat, t, name, posn);
			}
			else{
				return parseMethodDeclaration(isPriv, isStat, t, name, posn);
			}
		}
	}
	
	private MethodDecl parseMethodDeclaration(boolean isPriv, boolean isStat, Type t, String name, SourcePosition posn) throws SyntaxException, IOException{
		ParameterDeclList parameters = new ParameterDeclList();
		accept(TokenKind.LPAREN);
		if(token.kind != TokenKind.RPAREN){
			parameters = parseParameterList();
		}
		accept(TokenKind.RPAREN);
		accept(TokenKind.LCURL);
		StatementList statements = new StatementList();
		while(token.kind != TokenKind.RCURL){
			statements.add(parseStatement());
		}
		accept(TokenKind.RCURL);
		return new MethodDecl(new FieldDecl(isPriv, isStat, t, name, posn), parameters, statements, posn);
	}
	
	/**
	 * @return True if and only if the token is PRIVATE, False if and only if the token is PUBLIC or none
	 * @throws IOException
	 */
	private boolean parseVisibility() throws IOException{
		switch(token.kind){
		case PUBLIC:
			acceptIt();
			return false;
		case PRIVATE:
			acceptIt();
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * @return True if and only if the token is STATIC
	 * @throws IOException
	 */
	private boolean parseAccess() throws IOException{
		switch(token.kind){
		case STATIC:
			acceptIt();
			return true;
		default:
			return false;
		}
	}
	
	private Type parseType() throws SyntaxException, IOException{
		posn = token.posn;
		switch(token.kind){
		case INT:
			acceptIt();
			if(token.kind == TokenKind.LBRACK){
				acceptIt();
				accept(TokenKind.RBRACK);
				return new ArrayType(new BaseType(TypeKind.INT, posn), posn);
			}
			return new BaseType(TypeKind.INT, posn);
		case BOOLEAN:
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN, posn);
		case ID:
			Identifier id = new Identifier(token);
			acceptIt();
			if(token.kind == TokenKind.LBRACK){
				acceptIt();
				accept(TokenKind.RBRACK);
				return new ArrayType(new ClassType(id, posn), posn);
			}
			return new ClassType(id, posn);
		default:
			parseException("Expecting token of types INT, BOOLEAN, or ID, but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
			return new BaseType(TypeKind.UNSUPPORTED, posn);
		}
	}
	
	private ParameterDeclList parseParameterList() throws SyntaxException, IOException{
		ParameterDeclList parameters = new ParameterDeclList();
		Type t = parseType();
		String name = token.spelling;
		posn = token.posn;
		accept(TokenKind.ID);
		parameters.add(new ParameterDecl(t, name, posn));
		
		while(token.kind == TokenKind.COMMA){
			acceptIt();
			t = parseType();
			name = token.spelling;
			posn = token.posn;
			accept(TokenKind.ID);
			parameters.add(new ParameterDecl(t, name, posn));
		}
		return parameters;
	}
	
	private ExprList parseExpressionList() throws SyntaxException, IOException{
		ExprList expressions = new ExprList();
		if(token.kind != TokenKind.RPAREN){
			expressions.add(parseExpression());
			while(token.kind == TokenKind.COMMA){
				acceptIt();
				expressions.add(parseExpression());
			}
		}
		return expressions;
	}
	
	private Reference parseReference() throws SyntaxException, IOException{
		posn = token.posn;
		Reference ref;
		switch(token.kind){
		case THIS:
			ref = new ThisRef(posn);
			acceptIt();
			break;
		case ID:
			ref = new IdRef(new Identifier(token), posn);
			acceptIt();
			break;
		default:
			parseException("Expecting token of types THIS or ID but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
			ref = null;
			break;
		}
		while(token.kind == TokenKind.DOT){
			acceptIt();
			posn = token.posn;
			ref = new QualifiedRef(ref, new Identifier(token), posn);
			accept(TokenKind.ID);
		}
		return ref;
	}
	
	private Statement parseStatement() throws SyntaxException, IOException{
		posn = token.posn;
		switch(token.kind){
		case LCURL:
			{
				StatementList statements = new StatementList();
				acceptIt();
				while(token.kind != TokenKind.RCURL){
					statements.add(parseStatement());
				}
				accept(TokenKind.RCURL);
				return new BlockStmt(statements, posn);
			}
		case INT: case BOOLEAN:
			{
				SourcePosition startposn = posn;
				Type t = parseType();
				String name = token.spelling;
				posn = token.posn;
				VarDecl vd = new VarDecl(t, name, posn);
				accept(TokenKind.ID);
				accept(TokenKind.EQ);
				Expression e = parseExpression();
				accept(TokenKind.SEMI);
				return new VarDeclStmt(vd, e, startposn);
			}
		case THIS:
			{
				Reference ref = parseReference();
				if(token.kind == TokenKind.EQ){
					acceptIt();
					Expression e = parseExpression();
					accept(TokenKind.SEMI);
					return new AssignStmt(ref, e, posn);
				}
				else if(token.kind == TokenKind.LPAREN){
					acceptIt();
					ExprList expressions = parseExpressionList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMI);
					return new CallStmt(ref, expressions, posn);
				}
			}
		case RETURN:
			{
				acceptIt();
				Expression returnExpression = null;
				if(token.kind != TokenKind.SEMI) returnExpression = parseExpression();
				accept(TokenKind.SEMI);
				return new ReturnStmt(returnExpression, posn);
			}
		case IF:
			{
				acceptIt();
				accept(TokenKind.LPAREN);
				Expression partIf = parseExpression();
				accept(TokenKind.RPAREN);
				Statement partThen = parseStatement();
				Statement partElse = null;
				if(token.kind == TokenKind.ELSE){
					acceptIt();
					partElse = parseStatement();
				}
				return new IfStmt(partIf, partThen, partElse, posn);
			}
		case WHILE:
			{
				acceptIt();
				accept(TokenKind.LPAREN);
				Expression partWhile = parseExpression();
				accept(TokenKind.RPAREN);
				Statement partBody = parseStatement();
				return new WhileStmt(partWhile, partBody, posn);
			}
		case ID:
			{//Begin ID block
				Identifier id = new Identifier(token);
				Type t = new ClassType(id, posn);
				acceptIt();
				if(token.kind == TokenKind.EQ){
					acceptIt();
					Expression e = parseExpression();
					accept(TokenKind.SEMI);
					return new AssignStmt(new IdRef(id, posn), e, posn);
				}
				else if(token.kind == TokenKind.LPAREN){
					ExprList expressions = new ExprList();
					acceptIt();
					if(token.kind != TokenKind.RPAREN) expressions = parseExpressionList();
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMI);
					return new CallStmt(new IdRef(id, posn), expressions, posn);
				}
				else if(token.kind == TokenKind.LBRACK){
					acceptIt();
					if(token.kind != TokenKind.RBRACK){
						Expression inner = parseExpression();
						accept(TokenKind.RBRACK);
						accept(TokenKind.EQ);
						Expression outer = parseExpression();
						accept(TokenKind.SEMI);
						return new IxAssignStmt(new IndexedRef(new IdRef(id, posn), inner, posn), outer, posn);
					}
					else{
						acceptIt();
						String name = token.spelling;
						VarDecl vd = new VarDecl(new ArrayType(t, posn), name, posn);
						accept(TokenKind.ID);
						accept(TokenKind.EQ);
						Expression e = parseExpression();
						accept(TokenKind.SEMI);
						return new VarDeclStmt(vd, e, posn);
					}
				}
				else if(token.kind == TokenKind.ID){
					String name = token.spelling;
					VarDecl vd = new VarDecl(t, name, posn);
					acceptIt();
					accept(TokenKind.EQ);
					Expression e = parseExpression();
					accept(TokenKind.SEMI);
					return new VarDeclStmt(vd, e, posn);
				}
				else if(token.kind == TokenKind.DOT){
					acceptIt();
					Reference ref = new IdRef(id, posn);
					ref = new QualifiedRef(ref, new Identifier(token), posn);
					accept(TokenKind.ID);
					while(token.kind == TokenKind.DOT){
						acceptIt();
						ref = new QualifiedRef(ref, new Identifier(token), posn);
						accept(TokenKind.ID);
					}
					if(token.kind == TokenKind.EQ){
						acceptIt();
						Expression e = parseExpression();
						accept(TokenKind.SEMI);
						return new AssignStmt(ref, e, posn);
					}
					else if(token.kind == TokenKind.LPAREN){
						acceptIt();
						ExprList expressions = parseExpressionList();
						accept(TokenKind.RPAREN);
						accept(TokenKind.SEMI);
						return new CallStmt(ref, expressions, posn);
					}
				}
				else parseException("Expecting token of types EQ, LPAREN, ID or LBRACK but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
				return null;
			}//End ID block
		default:
			parseException("Expecting token of types LCURL, INT, BOOLEAN, THIS, RETURN, IF, WHILE, or ID but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
			return null;
		}
	}
	
	private Expression parseExpression() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseAND();
		while(token.kind == TokenKind.OR){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseAND(), posn);
		}
		return e1;
	}
	
	private Expression parseAND() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseEquality();
		while(token.kind == TokenKind.AND){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseEquality(), posn);
		}
		return e1;
	}
	
	private Expression parseEquality() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseRelational();
		while(token.kind == TokenKind.EQEQ || token.kind == TokenKind.NEQ){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseRelational(), posn);
		}
		return e1;
	}
	
	private Expression parseRelational() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseTerm();
		while(token.kind == TokenKind.GT || token.kind == TokenKind.LT || token.kind == TokenKind.GTE || token.kind == TokenKind.LTE){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseTerm(), posn);
		}
		return e1;
	}
	
	private Expression parseTerm() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseFactor();
		while(token.kind == TokenKind.PLUS || token.kind == TokenKind.MINUS){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseFactor(), posn);
		}
		return e1;
	}
	
	private Expression parseFactor() throws IOException, SyntaxException{
		posn = token.posn;
		Expression e1 = parseUnary();
		while(token.kind == TokenKind.TIMES || token.kind == TokenKind.DIV){
			Operator op = new Operator(token);
			acceptIt();
			e1 = new BinaryExpr(op, e1, parseUnary(), posn);
		}
		return e1;
	}
	
	private Expression parseUnary() throws IOException, SyntaxException{
		posn = token.posn;
		LinkedList<Operator> operators = new LinkedList<Operator>();
		while(token.kind == TokenKind.MINUS || token.kind == TokenKind.NOT){
			operators.push(new Operator(token));
			acceptIt();
		}
		int numop = operators.size();
		Expression e;
		if(token.kind == TokenKind.LPAREN){
			acceptIt();
			e = parseExpression();
			accept(TokenKind.RPAREN);
		}
		else{
			e = parseExprLit();
		}
		for(int i = 0; i < numop; i++){
			e = new UnaryExpr(operators.pop(), e, posn);
		}
		return e;
	}
	
	private Expression parseExprLit() throws IOException, SyntaxException{
		posn = token.posn;
		Expression returnExpression = null;
		switch(token.kind){
		case THIS:
		{
			Reference ref = parseReference();
			if(token.kind == TokenKind.LPAREN){
				acceptIt();
				ExprList expressions = parseExpressionList();
				accept(TokenKind.RPAREN);
				returnExpression = new CallExpr(ref, expressions, posn);
			}
			else returnExpression = new RefExpr(ref, posn);
			break;
		}
			
		case NUM:
			{
				returnExpression = new LiteralExpr(new IntLiteral(token), posn);
				acceptIt();
				break;
			}
		case TRUE: case FALSE:
			{
				returnExpression = new LiteralExpr(new BooleanLiteral(token), posn);
				acceptIt();
				break;
			}
		case NEW:
			{
				acceptIt();
				if(token.kind == TokenKind.ID){
					Identifier id = new Identifier(token);
					ClassType t = new ClassType(id, posn);
					acceptIt();
					if(token.kind == TokenKind.LPAREN){
						acceptIt();
						accept(TokenKind.RPAREN);
						returnExpression = new NewObjectExpr(t, posn);
					}
					else if(token.kind == TokenKind.LBRACK){
						acceptIt();
						Expression e = parseExpression();
						accept(TokenKind.RBRACK);
						returnExpression = new NewArrayExpr(t, e, posn);
					}
					else parseException("Expecting token of type LPAREN or RPAREN but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
				}
				else if(token.kind == TokenKind.INT){
					Type t = new BaseType(TypeKind.INT, posn);
					acceptIt();
					accept(TokenKind.LBRACK);
					Expression e = parseExpression();
					accept(TokenKind.RBRACK);
					returnExpression = new NewArrayExpr(t, e, posn);
				}
				else parseException("Expecting token of type ID or INT but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
				break;
			}
		case ID:
			{
				Identifier id = new Identifier(token);
				acceptIt();
				if(token.kind == TokenKind.LBRACK){
					acceptIt();
					Expression e = parseExpression();
					accept(TokenKind.RBRACK);
					returnExpression = new RefExpr(new IndexedRef(new IdRef(id, posn), e, posn), posn);
				}
				else if(token.kind == TokenKind.DOT){
					Reference ref = new IdRef(id, posn);
					acceptIt();
					ref = new QualifiedRef(ref, new Identifier(token), posn);
					accept(TokenKind.ID);
					while(token.kind == TokenKind.DOT){
						acceptIt();
						ref = new QualifiedRef(ref, new Identifier(token), posn);
						accept(TokenKind.ID);
					}
					if(token.kind == TokenKind.LPAREN){
						acceptIt();
						ExprList expressions = parseExpressionList();
						accept(TokenKind.RPAREN);
						returnExpression = new CallExpr(ref, expressions, posn);
					}
					else returnExpression = new RefExpr(ref, posn);
				}
				else returnExpression = new RefExpr(new IdRef(id, posn), posn);
				break;
			}
		case NULL:
			{
				Identifier id = new Identifier(token);
				acceptIt();
				returnExpression = new RefExpr(new IdRef(id, posn), posn);
				break;
			}
		default:
			parseException("Expecting token of type THIS, NUM, TRUE, FALSE, NEW, or ID but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
			break;
		}
		return returnExpression;
	}
	
	private void acceptIt() throws IOException{
		try{
			accept(token.kind);
		} catch(SyntaxException e){}
	}
	
	private void accept(TokenKind type)throws SyntaxException, IOException{
		if(token.kind == type){
			token = scanner.scan();
		}
		else parseException("Expecting token of type " + type + " but got token of type " + token.kind + " with spelling " + token.spelling + " at position " + token.posn);
	}
	
	private void parseException(String message) throws SyntaxException{
		throw new SyntaxException(message);
	}
}

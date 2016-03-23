package miniJava.AbstractSyntaxTrees;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ASTTypecheck implements Visitor {
	
	private BaseType VOID;
	private BaseType INT;
	private BaseType BOOLEAN;
	private BaseType ERROR;
	private ClassType NULL;
	private ErrorReporter reporter;
	public ASTTypecheck(ErrorReporter reporter){
		this.reporter = reporter;
		VOID = new BaseType(TypeKind.VOID, null);
		INT = new BaseType(TypeKind.INT, null);
		BOOLEAN = new BaseType(TypeKind.BOOLEAN, null);
		ERROR = new BaseType(TypeKind.ERROR, null);
		NULL = new ClassType(new Identifier(new Token(TokenKind.CLASS, "null", null)), null);
	}
	
	public void check(AST program){
		program.visit(this, null);
	}
	
	private boolean equality(Type t1, Type t2){
		if(t1.typeKind == TypeKind.ERROR || t2.typeKind == TypeKind.ERROR) return true;
		else if(t1.typeKind == TypeKind.UNSUPPORTED || t2.typeKind == TypeKind.UNSUPPORTED) return false;
		else if(t1.typeKind == TypeKind.CLASS && t2.typeKind == TypeKind.CLASS){
			if(((ClassType)t1).className.spelling.equals("String") || ((ClassType)t2).className.spelling.equals("String")) return false;
			if(((ClassType)t1).className.spelling.equals(((ClassType)t2).className.spelling)) return true;
			if(t2 == NULL) return true;
		}
		else if(t1.typeKind == TypeKind.ARRAY && t2.typeKind == TypeKind.ARRAY){
			if(equality(((ArrayType)t1).eltType, ((ArrayType)t2).eltType)) return true;
		}
		else if(t1.typeKind == t2.typeKind) return true;
		return false;
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		for(ClassDecl cd : prog.classDeclList){
			cd.visit(this, null);
		}
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for(FieldDecl fd : cd.fieldDeclList){
			fd.visit(this, null);
		}
		for(MethodDecl md : cd.methodDeclList){
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.checkedType = fd.type;
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		md.checkedType = md.type;
		for(ParameterDecl pd : md.parameterDeclList){
			pd.visit(this, null);
		}
		for(Statement s : md.statementList){
			s.visit(this, null);
			if(s instanceof ReturnStmt){
				if(!equality(s.checkedType, md.checkedType)){
					if(s.checkedType instanceof ClassType){
						if(md.checkedType instanceof ClassType){
							reporter.log("***Type mismatch.  Method \"" + md.name + "\" has a return statement at program location " + s.posn + " of return type " + ((ClassType)s.checkedType).className.spelling + ", which conflicts with the method's stated return type of " + ((ClassType)md.checkedType).className.spelling);
						}
						else reporter.log("***Type mismatch.  Method \"" + md.name + "\" has a return statement at program location " + s.posn + " of return type " + ((ClassType)s.checkedType).className.spelling + ", which conflicts with the method's stated return type of " + md.checkedType.typeKind);
					}
					else reporter.log("***Type mismatch.  Method \"" + md.name + "\" has a return statement at program location " + s.posn + " of return type " + s.checkedType.typeKind + ", which conflicts with the method's stated return type of " + md.checkedType.typeKind);
				}
			}
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.checkedType = pd.type;
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.checkedType = decl.type;
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//TYPES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		type.checkedType = type;
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		type.checkedType = type;
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.checkedType = type;
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		for(Statement s : stmt.sl){
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.initExp.visit(this, null);
		stmt.varDecl.visit(this, null);
		if(!equality(stmt.varDecl.checkedType, stmt.initExp.checkedType)){
			if(stmt.varDecl.checkedType instanceof ClassType){
				if(stmt.initExp.checkedType instanceof ClassType){
					reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + ((ClassType)stmt.initExp.checkedType).className.spelling + " to variable \"" + stmt.varDecl.name + "\" of type " + ((ClassType)stmt.varDecl.checkedType).className.spelling);
				}
				else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.initExp.checkedType.typeKind + " to variable \"" + stmt.varDecl.name + "\" of type " + ((ClassType)stmt.varDecl.checkedType).className.spelling);
			}
			else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.initExp.checkedType.typeKind + " to variable \"" + stmt.varDecl.name + "\" of type " + stmt.varDecl.checkedType);
			stmt.checkedType = ERROR;
		}
		else stmt.checkedType = VOID;
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		if(!equality(stmt.ref.checkedType, stmt.val.checkedType)){
			if(stmt.ref.checkedType instanceof ClassType){
				if(stmt.val.checkedType instanceof ClassType){
					reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + ((ClassType)stmt.val.checkedType).className.spelling + " to variable \"" + stmt.ref.decl.name + "\" of type " + ((ClassType)stmt.ref.checkedType).className.spelling);
				}
				else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.val.checkedType.typeKind + " to variable \"" + stmt.ref.decl.name + "\" of type " + ((ClassType)stmt.ref.checkedType).className.spelling);
			}
			else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.val.checkedType.typeKind + " to variable \"" + stmt.ref.decl.name + "\" of type " + stmt.ref.checkedType);
			stmt.checkedType = ERROR;
		}
		else stmt.checkedType = VOID;
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ixRef.visit(this, null);
		stmt.val.visit(this, null);
		if(!equality(stmt.ixRef.checkedType, stmt.val.checkedType)){
			if(stmt.ixRef.checkedType instanceof ClassType){
				if(stmt.val.checkedType instanceof ClassType){
					reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + ((ClassType)stmt.val.checkedType).className.spelling + " to variable \"" + stmt.ixRef.decl.name + "\" of type " + ((ClassType)stmt.ixRef.checkedType).className.spelling);
				}
				else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.val.checkedType.typeKind + " to variable \"" + stmt.ixRef.decl.name + "\" of type " + ((ClassType)stmt.ixRef.checkedType).className.spelling);
			}
			else reporter.log("***Type mismatch at program location " + stmt.posn + ". Cannot assign value of type " + stmt.val.checkedType.typeKind + " to variable \"" + stmt.ixRef.decl.name + "\" of type " + stmt.ixRef.checkedType);
			stmt.checkedType = ERROR;
		}
		else stmt.checkedType = VOID;
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		int size = stmt.argList.size();
		int initsize = ((MethodDecl)stmt.methodRef.decl).parameterDeclList.size();
		int i;
		for(i = 0; i < size; i++){
			try{
				ParameterDecl pd = ((MethodDecl)stmt.methodRef.decl).parameterDeclList.get(i);
				Expression e = stmt.argList.get(i);
				pd.visit(this, null);
				e.visit(this, null);
				if(!equality(pd.checkedType, e.checkedType)){
					reporter.log("***Parameter passed to method \"" + stmt.methodRef.decl.name + "\" at program location " + e.posn + " does not match declared parameter type");
					stmt.checkedType = ERROR;
				}
			} catch(IndexOutOfBoundsException e){
				reporter.log("***Too many arguments passed to method \"" + stmt.methodRef.decl.name + "\" at program location " + stmt.posn);
				stmt.checkedType = ERROR;
			}
		}
		if(i < initsize){
			reporter.log("***Too few arguments passed to method \"" + stmt.methodRef.decl.name + "\" at program location " + stmt.posn);
			stmt.checkedType = ERROR;
		}
		stmt.checkedType = stmt.methodRef.checkedType;
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if(stmt.returnExpr != null){
			stmt.returnExpr.visit(this, null);
			stmt.checkedType = stmt.returnExpr.checkedType;
		}
		else stmt.checkedType = VOID;
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		if(equality(stmt.cond.checkedType, BOOLEAN)){
			stmt.checkedType = VOID;
		}
		else{
			reporter.log("***Conditional in branch at program location " + stmt.cond.posn + " must evaluate to a boolean value");
			stmt.checkedType = ERROR;
		}
		stmt.thenStmt.visit(this, null);
		if(stmt.elseStmt != null) stmt.elseStmt.visit(this, null);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		if(equality(stmt.cond.checkedType, BOOLEAN)){
			stmt.checkedType = VOID;
		}
		else{
			reporter.log("***Conditional in loop at program location " + stmt.cond.posn + " must evaluate to a boolean value");
			stmt.checkedType = ERROR;
		}
		stmt.body.visit(this, null);
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.operator.visit(this, null);
		expr.expr.visit(this, null);
		if(expr.operator.spelling.equals("!")){
			if(equality(BOOLEAN, expr.expr.checkedType)) expr.checkedType = BOOLEAN;
			else{
				reporter.log("***Cannot negate non-boolean type at program location " + expr.expr.posn);
				expr.checkedType = ERROR;
			}
		}
		else{
			if(equality(INT, expr.expr.checkedType)) expr.checkedType = INT;
			else{
				reporter.log("***Cannot take the negative of non-integer type at program location " + expr.expr.posn);
				expr.checkedType = ERROR;
			}
		}
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		if(expr.operator.spelling.equals("+") || expr.operator.spelling.equals("-") || expr.operator.spelling.equals("*") || expr.operator.spelling.equals("/")){
			if(equality(expr.left.checkedType, expr.right.checkedType)){
				expr.checkedType = INT;
			}
			else{
				reporter.log("***Cannot apply arithmetic operator " + expr.operator.spelling + " to non-integer values at program location " + expr.posn);
				expr.checkedType = ERROR;
			}
		}
		else if(expr.operator.spelling.equals(">") || expr.operator.spelling.equals("<") || expr.operator.spelling.equals(">=") || expr.operator.spelling.equals("<=")){
			if(equality(expr.left.checkedType, expr.right.checkedType)){
				expr.checkedType = BOOLEAN;
			}
			else{
				reporter.log("***Cannot apply arithmetic operator " + expr.operator.spelling + " to non-integer values at program location " + expr.posn);
				expr.checkedType = ERROR;
			}
		}
		else if(expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||")){
			if(equality(expr.left.checkedType, expr.right.checkedType)){
				expr.checkedType = BOOLEAN;
			}
			else{
				reporter.log("***Cannot apply boolean operator " + expr.operator.spelling + " to non-boolean values at program location " + expr.posn);
				expr.checkedType = ERROR;
			}
		}
		else{
			if(equality(expr.left.checkedType, expr.right.checkedType)){
				expr.checkedType = expr.left.checkedType;
			}
			else{
				reporter.log("***Incompatible boolean and integer values in expression at program location " + expr.posn);
				expr.checkedType = ERROR;
			}
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.checkedType = expr.ref.checkedType;
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, null);
		for(Expression e : expr.argList){
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		expr.checkedType = expr.lit.checkedType;
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, null);
		expr.checkedType = expr.classtype.checkedType;
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		if(equality(expr.sizeExpr.checkedType, INT)){
			expr.checkedType = new ArrayType(expr.eltType, expr.posn);
		}
		else{
			reporter.log("***Cannot initialize array at program location " + expr.sizeExpr.posn + " with non-integer values");
			expr.checkedType = ERROR;
		}
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////
	//
	//REFERENCES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		ref.id.visit(this, null);
		ref.checkedType = ref.id.checkedType;
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Object arg) {
		ref.idRef.visit(this, null);
		ref.indexExpr.visit(this, null);
		if(!equality(ref.indexExpr.checkedType, INT)){
			reporter.log("***Cannot use a non-integer value as an index into array at program location " + ref.indexExpr.posn);
			ref.checkedType = ERROR;
		}
		else ref.checkedType = ((ArrayType)ref.idRef.id.decl.type).eltType;
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		ref.id.visit(this, null);
		ref.checkedType = ref.id.checkedType;
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		ref.checkedType = ref.decl.type;
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//TERMINALS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		id.checkedType = id.decl.type;
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		op.checkedType = VOID;
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		num.checkedType = INT;
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		bool.checkedType = BOOLEAN;
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		nul.checkedType = NULL;
		return null;
	}

}

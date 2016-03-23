package miniJava.AbstractSyntaxTrees;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ASTIdentification implements Visitor {
	private static final int numIDs = 100;
	private static final SourcePosition zero = new SourcePosition(0,0);
	public ScopedTable ScopeID;
	private ErrorReporter reporter;
	
	public ASTIdentification(ErrorReporter e){
		reporter = e;
		ScopeID = new ScopedTable(numIDs);
		predefine();
	}
	
	private void predefine(){
		//Making System class
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		FieldDecl out = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenKind.CLASS, "_PrintStream", zero)), zero), "out", zero);
		fields.add(out);
		ClassDecl system = new ClassDecl("System", fields, methods, zero);
		
		
		//Making _Printstream class
		fields = new FieldDeclList();
		methods = new MethodDeclList();
		ParameterDeclList parameters = new ParameterDeclList();
		parameters.add(new ParameterDecl(new BaseType(TypeKind.INT, zero), "a", zero));
		StatementList statements = new StatementList();
		MethodDecl println = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, zero), "println", zero), parameters, statements, zero);
		methods.add(println);
		ClassDecl printstream = new ClassDecl("_PrintStream", fields, methods, zero);
		
		//Making String class
		fields = new FieldDeclList();
		methods = new MethodDeclList();
		ClassDecl string = new ClassDecl("String", fields, methods, zero);
		
		ScopeID.openScope();
		ScopeID.predefine(system.name, null, system);
		ScopeID.predefine(printstream.name, null, printstream);
		ScopeID.predefine(string.name, null, string);
		ScopeID.openScope();
		
		ScopeID.predefine(system.name, out.name, out);
		ScopeID.predefine(printstream.name, println.name, println);
		
		ScopeID.closeScope();
		ScopeID.closeScope();
	}
	
	public void identify(AST prog){
		prog.visit(this, null);
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		//Preliminary logging pass over classes and their members for later use
		{
			ScopeID.openScope();
			for(ClassDecl cd : prog.classDeclList){
				boolean cdsuccess = ScopeID.skim(cd.name, cd);
				if(!cdsuccess) reporter.log("***Duplicate declaration of class \"" + cd.name + "\" at program location " + cd.posn);
				ScopeID.openScope();
				for(FieldDecl fd : cd.fieldDeclList){
					boolean fdsuccess = ScopeID.skim(fd.name, fd);
					if(!fdsuccess) reporter.log("***Duplicate declaration of field \"" + fd.name + "\" at program location " + fd.posn);
				}
				for(MethodDecl md : cd.methodDeclList){
					boolean mdsuccess = ScopeID.skim(md.name, md);
					if(!mdsuccess) reporter.log("***Duplicate declaration of method \"" + md.name + "\" at program location " + md.posn);
				}
				ScopeID.closeScope();
			}
			ScopeID.closeScope();
		}
		
		ScopeID.openScope();
		for(ClassDecl cd : prog.classDeclList){
			cd.visit(this, null);
		}
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		ScopeID.put(cd.name, cd);
		ScopeID.openScope();
		for(FieldDecl f : cd.fieldDeclList){
			f.visit(this, null);
		}
		for(MethodDecl m : cd.methodDeclList){
			m.visit(this, null);
		}
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		if(fd.type instanceof ClassType){
			fd.type.visit(this, null);
			if(((ClassType)fd.type).className.decl == null){
				reporter.log("***Field \"" + fd.name + "\" at program location " + fd.posn + " is of type \"" + ((ClassType)fd.type).className.spelling + "\", which cannot be resolved to a type");
			}
		}
		boolean success = ScopeID.put(fd.name, fd);
		if(!success) reporter.log("***Duplicate declaration of field \"" + fd.name + "\" at program location " + fd.posn);
		return null;
	}
	
	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		if(md.type instanceof ClassType){
			md.type.visit(this, null);
			if(((ClassType)md.type).className.decl == null){
				reporter.log("***Method \"" + md.name + "\" at program location " + md.posn + " is of type \"" + ((ClassType)md.type).className.spelling + "\", which cannot be resolved to a type");
			}
		}
		boolean success = ScopeID.put(md.name, md);
		if(!success) reporter.log("***Duplicate declaration of method \"" + md.name + "\" at program location " + md.posn);
		ScopeID.openScope();
		for(ParameterDecl pd : md.parameterDeclList){
			pd.visit(this, null);
		}
		ScopeID.openScope();
		for(Statement s : md.statementList){
			s.visit(this, null);
		}
		ScopeID.closeScope();
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		if(pd.type instanceof ClassType){
			pd.type.visit(this, null);
			if(((ClassType)pd.type).className.decl == null){
				reporter.log("***Parameter \"" + pd.name + "\" at program location " + pd.posn + " is of type \"" + ((ClassType)pd.type).className.spelling + "\", which cannot be resolved to a type");
			}
		}
		boolean success = ScopeID.put(pd.name, pd);
		if(!success) reporter.log("***Duplicate declaration of parameter \"" + pd.name + "\" at program location " + pd.posn);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		if(decl.type instanceof ClassType){
			decl.type.visit(this, null);
			if(((ClassType)decl.type).className.decl == null){
				reporter.log("***Local variable \"" + decl.name + "\" at program location " + decl.posn + " is of type \"" + ((ClassType)decl.type).className.spelling + "\", which cannot be resolved to a type");
			}
		}
		boolean success = ScopeID.put(decl.name, decl);
		if(!success) reporter.log("***Duplicate declaration of local variable \"" + decl.name + "\" at program location " + decl.posn);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		type.className.decl = ScopeID.get(type.className.spelling);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		ScopeID.openScope();
		if(stmt.sl.size() == 1){
			if(stmt.sl.get(0) instanceof VarDeclStmt){
				reporter.log("***Declaration of local variable in block statement at program location " + (((VarDeclStmt)stmt.sl.get(0)).varDecl).posn + " is the only statement in the block.  This is illegal.");
			}
		}
		for(Statement s : stmt.sl){
			s.visit(this, null);
		}
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ixRef.visit(this, null);
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.methodRef.visit(this, null);
		for(Expression e : stmt.argList){
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if(stmt.body != null) stmt.body.visit(this, null);
		if(stmt.returnExpr != null) stmt.returnExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		ScopeID.openScope();
		if(stmt.thenStmt instanceof VarDeclStmt) reporter.log("***Declaration of local variable in branch at program location " + stmt.posn + " is the only statement in the block.  This is illegal.");
		stmt.thenStmt.visit(this, null);
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		ScopeID.openScope();
		if(stmt.body instanceof VarDeclStmt) reporter.log("***Declaration of local variable in loop at program location " + stmt.posn + " is the only statement in the block.  This is illegal.");
		stmt.body.visit(this, null);
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
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
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		ref.ref.visit(this, null);
		if(ref.ref.decl == null){
			ref.ref.decl = null;
			ref.decl = null;
			ref.id.decl = null;
			return null;
		}
		if(ref.ref.decl.type instanceof ClassType){
			ref.id.decl = ScopeID.getMemberDecl(((ClassType)ref.ref.decl.type).className.spelling, ref.id.spelling);
			ref.decl = ref.id.decl;
			if(ref.id.decl == null){
				String parent = "";
				if(ref.ref instanceof ThisRef){
					parent = ScopeID.currentClass.name;
				}
				else if(ref.ref.decl.type instanceof ClassType){
					parent = ((ClassType)ref.ref.decl.type).className.spelling;
				}
				reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + parent);
			}
			else if(((MemberDecl)ref.id.decl).isPrivate){
				reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((ClassType)ref.ref.decl.type).className.spelling);
			}
		}
		else if(ref.ref instanceof ThisRef){
			ref.id.decl = ScopeID.getMemberDecl(ScopeID.currentClass.name, ref.id.spelling);
			ref.decl = ref.id.decl;
			if(ref.id.decl == null){
				String parent = "";
				if(ref.ref instanceof ThisRef){
					parent = ScopeID.currentClass.name;
				}
				else if(ref.ref.decl.type instanceof ClassType){
					parent = ((ClassType)ref.ref.decl.type).className.spelling;
				}
				reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + parent);
			}
		}
		//Static access
		else if(ref.ref.decl.type == null){
			ref.id.decl = ScopeID.getMemberDecl(ref.ref.decl.name, ref.id.spelling);
			if(ref.id.decl == null){
				reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + ref.ref.decl.name);
			}
			else{
				if(!((MemberDecl)ref.id.decl).isStatic){
					reporter.log("***Cannot reference non-static member \"" + ref.id.decl.name + "\" from the static type \"" + ref.ref.decl.name + "\" at program location " + ref.id.decl.posn);
				}
				else if(((MemberDecl)ref.id.decl).isPrivate){
					reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ref.ref.decl.name);
				}
				else ref.decl = ref.id.decl;
			}

		}
		else{
			reporter.log("***Identifier \"" + ((IdRef)ref.ref).id.spelling + "\" at program location " + ((IdRef)ref.ref).id.posn + " is not a class type, trying to reference members of \"" + ((IdRef)ref.ref).id.spelling + "\" is illegal");
			ref.ref.decl = null;
		}
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Object arg) {
		ref.idRef.visit(this, null);
		ref.decl = ref.idRef.decl;
		ref.indexExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		ref.id.visit(this, null);
		ref.decl = ref.id.decl;
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		ref.decl = ScopeID.currentClass;
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		id.decl = ScopeID.get(id.spelling);
		if(id.decl == null){
			reporter.log("***Identifier \"" + id.spelling + "\" at program location " + id.posn + " has not been declared");
		}
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

}

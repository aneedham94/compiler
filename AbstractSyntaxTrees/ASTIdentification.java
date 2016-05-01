package miniJava.AbstractSyntaxTrees;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class ASTIdentification implements Visitor<Object, Object> {
	private static final int numIDs = 100;
	private static final SourcePosition nullPosn = null;
	public ScopedTable ScopeID;
	private ErrorReporter reporter;
	private boolean initializing = false;
	private Declaration initDecl = null;
	private boolean staticContext = false;
	private FieldDecl ARRAY_LEN = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
	
	public ASTIdentification(ErrorReporter e){
		reporter = e;
		ScopeID = new ScopedTable(numIDs);
		predefine();
	}
	
	private void predefine(){
		//Making System class
		FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
		FieldDecl out = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenKind.CLASS, "_PrintStream", nullPosn)), nullPosn), "out", nullPosn);
		fields.add(out);
		ClassDecl system = new ClassDecl("System", fields, methods, nullPosn);
		
		
		//Making _Printstream class
		fields = new FieldDeclList();
		methods = new MethodDeclList();
		ParameterDeclList parameters = new ParameterDeclList();
		parameters.add(new ParameterDecl(new BaseType(TypeKind.INT, nullPosn), "a", nullPosn));
		StatementList statements = new StatementList();
		MethodDecl println = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, nullPosn), "println", nullPosn), parameters, statements, nullPosn);
		methods.add(println);
		ClassDecl printstream = new ClassDecl("_PrintStream", fields, methods, nullPosn);
		
		//Making String class
		fields = new FieldDeclList();
		methods = new MethodDeclList();
		ClassDecl string = new ClassDecl("String", fields, methods, nullPosn);
		
		
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
		
		//Checking for unique main method
		boolean found = false;
		for(ClassDecl cd : prog.classDeclList){
			for(MethodDecl md : cd.methodDeclList){
				if(md.name.equals("main")){
					if(!found){
						if(md.isPrivate) reporter.log("***Main method at program location " + md.posn + " is private, and must be public.");
						if(!md.isStatic) reporter.log("***Main method at program location " + md.posn + " is not static, and must be static.");
						if(md.type.typeKind != TypeKind.VOID) reporter.log("***Main method at program location " + md.posn + " has non-void return type, and must have a void return type.");
						if(md.parameterDeclList.size() != 1) reporter.log("***Main method at program location " + md.posn + " has either too few or too many parameters, it must have exactly 1.");
						else{
							if(md.parameterDeclList.get(0).type instanceof ArrayType){
								if(((ArrayType)md.parameterDeclList.get(0).type).eltType instanceof ClassType){
									if(((ClassType)((ArrayType)md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) md.isMain = true;
									else reporter.log("***Main method at program location " + md.posn + " must have String[] as the type of its one parameter.");
								}
								else reporter.log("***Main method at program location " + md.posn + " must have String[] as the type of its one parameter.");
							}
							else reporter.log("***Main method at program location " + md.posn + " must have String[] as the type of its one parameter.");
						}
					}
					else reporter.log("***Duplicate main method found at program location " + md.posn + ".");
					found = true;
				}
			}
		}
		if(!found){
			reporter.log("***No main method could be found.  Aborting code generation.");
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
		if(md.isStatic) staticContext = true;
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
		staticContext = false;
		
		//Checking for return statements
		if(md.type.typeKind != TypeKind.VOID){
			if(md.statementList.size() > 0){
				if(!(md.statementList.get(md.statementList.size()-1) instanceof ReturnStmt)){
					reporter.log("***Method " + md.name + " at program location " + md.posn + " must have a return statement of type " + md.type.typeKind);
				}
			}
			else reporter.log("***Method " + md.name + " at program location " + md.posn + " must have a return statement of type " + md.type.typeKind);
		}
		else{
			if(md.statementList.size() > 0){
				if(!(md.statementList.get(md.statementList.size()-1) instanceof ReturnStmt)){
					md.statementList.add(new ReturnStmt(null, null));
				}
			}
			else{
				md.statementList.add(new ReturnStmt(null, null));
			}
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		if(pd.type instanceof ClassType){
			pd.type.visit(this, null);
			if(((ClassType)pd.type).className.decl == null || !(((ClassType)pd.type).className.decl instanceof ClassDecl)){
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

	
	/////////////////////////////////////////////////////////////////////
	//
	//TYPES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		type.className.decl = ScopeID.getClass(type.className.spelling);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		ScopeID.openScope();
		for(Statement s : stmt.sl){
			s.visit(this, null);
		}
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		initDecl = stmt.varDecl;
		initializing = true;
		stmt.initExp.visit(this, null);
		initializing = false;
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		if(stmt.ref instanceof QualifiedRef){
			if(((QualifiedRef)stmt.ref).id.spelling.equals("length")){
				if(((QualifiedRef)stmt.ref).ref.decl.type instanceof ArrayType){
					reporter.log("***Assignment to the length field of array at program location " + stmt.ref.posn + " is illegal.");
				}
			}
		}
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
		if(stmt.methodRef instanceof QualifiedRef){
			for(Expression e : stmt.argList){
				e.visit(this, null);
			}
			if(!(((QualifiedRef)stmt.methodRef).id.decl instanceof MethodDecl)){
				if(((QualifiedRef)stmt.methodRef).id.decl != null){
					String name = ((QualifiedRef)stmt.methodRef).id.decl.name;
					if(name == ScopeID.currentClass.name) name = "this";
					reporter.log("***Identifier " + name + " at program location " + stmt.methodRef.posn + " is not a method.");
				}
			}
		}
		else{
			for(Expression e : stmt.argList){
				e.visit(this, null);
			}
			if(!(stmt.methodRef.decl instanceof MethodDecl)){
				if(stmt.methodRef.decl != null){
					String name = stmt.methodRef.decl.name;
					if(name == ScopeID.currentClass.name) name = "this";
					reporter.log("***Identifier " + name + " at program location " + stmt.methodRef.posn + " is not a method.");
				}
			}
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
		if(stmt.thenStmt instanceof VarDeclStmt) reporter.log("***Declaration of local variable in branch at program location " + stmt.thenStmt.posn + " is the only statement in the block.  This is illegal.");
		stmt.cond.visit(this, null);
		stmt.thenStmt.visit(this, null);
		if(stmt.elseStmt != null) stmt.elseStmt.visit(this, null);
		if(stmt.elseStmt instanceof VarDeclStmt) reporter.log("***Declaration of local variable in branch at program location " + stmt.elseStmt.posn + " is the only statement in the block.  This is illegal.");
		ScopeID.closeScope();
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		ScopeID.openScope();
		if(stmt.body instanceof VarDeclStmt) reporter.log("***Declaration of local variable in loop at program location " + stmt.posn + " is the only statement in the block.  This is illegal.");
		stmt.cond.visit(this, null);
		stmt.body.visit(this, null);
		ScopeID.closeScope();
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
		if(initializing){
			if(expr.ref.decl == initDecl){
				reporter.log("***Local variable " + expr.ref.decl.name + " at program location " + expr.ref.posn + " cannot be used in its own declaration and initialization.");
			}
		}
		if(expr.ref instanceof IdRef){
			if(((IdRef)expr.ref).id.decl instanceof ClassDecl){
				if(((ClassDecl)((IdRef)expr.ref).id.decl).name.equals(((IdRef)expr.ref).id.spelling)){
					reporter.log("***Identifier " + ((IdRef)expr.ref).id.spelling + " at program location " + ((IdRef)expr.ref).id.posn + " has not been declared.");
				}
			}
			else if(((IdRef)expr.ref).id.decl instanceof MethodDecl){
				reporter.log("***Identifier " + ((IdRef)expr.ref).id.spelling + " at program location " + ((IdRef)expr.ref).id.posn + " has not been declared.");
			}
		}
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

	/////////////////////////////////////////////////////////////////////
	//
	//REFERENCES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		ref.ref.visit(this, null);
		if(ref.ref instanceof QualifiedRef){
			if(staticContext){
				if(((QualifiedRef)ref.ref).id.decl == null) return null;
				if(((QualifiedRef)ref.ref).id.decl.type instanceof ClassType && !(((QualifiedRef)ref.ref).id.decl instanceof MethodDecl)){
					ref.id.decl = ScopeID.getMember(((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling, ref.id.spelling);
					ref.decl = ref.ref.decl;
					if(ref.id.decl == null){
						String parent = "";
						if(ref.ref instanceof ThisRef){
							parent = ScopeID.currentClass.name;
						}
						else if(((QualifiedRef)ref.ref).id.decl.type instanceof ClassType){
							parent = ((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling;
						}
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + parent);
					}
					else if(((MemberDecl)ref.id.decl).isPrivate){
						if(!((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling.equals(ScopeID.currentClass.name)){
							reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling);
						}
					}
				}
				else if(((QualifiedRef)ref.ref).id.decl instanceof MethodDecl){
					reporter.log("***Cannot qualify method reference to " + ((QualifiedRef)ref.ref).id.decl.name + " at program location " + ((QualifiedRef)ref.ref).id.posn + ".");
				}
				else if(ref.ref instanceof ThisRef){
					reporter.log("***Cannot make use of \"this\" reference (" + ((QualifiedRef)ref.ref).id.posn + ") in a static context.");
				}
				else if(((QualifiedRef)ref.ref).id.decl.type instanceof ArrayType){
					if(ref.id.spelling.equals("length")){
						ref.id.decl = ARRAY_LEN;
						//ref.decl = ARRAY_LEN;
					}
					else{
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member of the Array class.");
					}
				}
				//CLASS access
				else if(((QualifiedRef)ref.ref).id.decl.type == null){
					ref.id.decl = ScopeID.getMember(((QualifiedRef)ref.ref).id.decl.name, ref.id.spelling);
					if(ref.id.decl == null){
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + ((QualifiedRef)ref.ref).id.decl.name);
					}
					else{
						if(!((MemberDecl)ref.id.decl).isStatic){
							reporter.log("***Cannot reference non-static member \"" + ref.id.decl.name + "\" from the static type \"" + ((QualifiedRef)ref.ref).id.decl.name + "\" at program location " + ref.id.decl.posn);
						}
						if(((MemberDecl)ref.id.decl).isPrivate){
							if(!(((ClassDecl)((QualifiedRef)ref.ref).id.decl).name.equals(ScopeID.currentClass.name))){
								reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((QualifiedRef)ref.ref).id.decl.name);
							}
						}
						//else ref.decl = ref.id.decl;
					}

				}
				else{
					reporter.log("***Identifier \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is not a class type, trying to reference members of \"" + ref.id.spelling + "\" is illegal");
					ref.ref.decl = null;
				}
			}
			else{
				if(((QualifiedRef)ref.ref).id.decl == null) return null;
				if(((QualifiedRef)ref.ref).id.decl.type instanceof ClassType && !(((QualifiedRef)ref.ref).id.decl instanceof MethodDecl)){
					ref.id.decl = ScopeID.getMember(((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling, ref.id.spelling);
					ref.decl = ref.ref.decl;
					if(ref.id.decl == null){
						String parent = "";
						if(ref.ref instanceof ThisRef){
							parent = ScopeID.currentClass.name;
						}
						else if(((QualifiedRef)ref.ref).id.decl.type instanceof ClassType){
							parent = ((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling;
						}
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + parent);
					}
					else if(((MemberDecl)ref.id.decl).isPrivate){
						if(!((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling.equals(ScopeID.currentClass.name)){
							reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling);
						}				}
				}
				else if(((QualifiedRef)ref.ref).id.decl instanceof MethodDecl){
					reporter.log("***Cannot qualify method reference to " + ((QualifiedRef)ref.ref).id.decl.name + " at program location " + ((QualifiedRef)ref.ref).id.posn + ".");
				}
				else if(ref.ref instanceof ThisRef){
					ref.id.decl = ScopeID.getMember(ScopeID.currentClass.name, ref.id.spelling);
					//ref.decl = ref.id.decl;
					if(ref.id.decl == null){
						String parent = "";
						if(ref.ref instanceof ThisRef){
							parent = ScopeID.currentClass.name;
						}
						else if(((QualifiedRef)ref.ref).id.decl.type instanceof ClassType){
							parent = ((ClassType)((QualifiedRef)ref.ref).id.decl.type).className.spelling;
						}
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + parent);
					}
				}
				else if(((QualifiedRef)ref.ref).id.decl.type instanceof ArrayType){
					if(ref.id.spelling.equals("length")){
						ref.id.decl = ARRAY_LEN;
						//ref.decl = ARRAY_LEN;
					}
					else{
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member of the Array class.");
					}
				}
				//CLASS access
				else if(((QualifiedRef)ref.ref).id.decl.type == null){
					ref.id.decl = ScopeID.getMember(((QualifiedRef)ref.ref).id.decl.name, ref.id.spelling);
					if(ref.id.decl == null){
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + ((QualifiedRef)ref.ref).id.decl.name);
					}
					else{
						if(!((MemberDecl)ref.id.decl).isStatic){
							reporter.log("***Cannot reference non-static member \"" + ref.id.decl.name + "\" from the static type \"" + ((QualifiedRef)ref.ref).id.decl.name + "\" at program location " + ref.id.decl.posn);
						}
						else if(((MemberDecl)ref.id.decl).isPrivate){
							if(!(((ClassDecl)((QualifiedRef)ref.ref).id.decl).name.equals(ScopeID.currentClass.name))){
								reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((QualifiedRef)ref.ref).id.decl.name);
							}					}
						//else ref.decl = ref.id.decl;
					}

				}
				else{
					reporter.log("***Identifier \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is not a class type, trying to reference members of \"" + ref.id.spelling + "\" is illegal");
					((QualifiedRef)ref.ref).id.decl = null;
				}
			}
		}
		else{
			if(ref.ref.decl == null){
				return null;
			}
			if(staticContext){
				if(ref.ref.decl.type instanceof ClassType && !(ref.ref.decl instanceof MethodDecl)){
					ref.id.decl = ScopeID.getMember(((ClassType)ref.ref.decl.type).className.spelling, ref.id.spelling);
					ref.decl = ref.ref.decl;
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
						if(!((ClassType)ref.ref.decl.type).className.spelling.equals(ScopeID.currentClass.name)){
							reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((ClassType)ref.ref.decl.type).className.spelling);
						}
					}
				}
				else if(ref.ref.decl instanceof MethodDecl){
					reporter.log("***Cannot qualify method reference to " + ref.ref.decl.name + " at program location " + ref.ref.posn + ".");
				}
				else if(ref.ref instanceof ThisRef){
					reporter.log("***Cannot make use of \"this\" reference (" + ref.ref.posn + ") in a static context.");
				}
				else if(ref.ref.decl.type instanceof ArrayType){
					if(ref.id.spelling.equals("length")){
						ref.id.decl = ARRAY_LEN;
						//ref.decl = ARRAY_LEN;
					}
					else{
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member of the Array class.");
					}
				}
				//CLASS access
				else if(ref.ref.decl.type == null){
					ref.id.decl = ScopeID.getMember(ref.ref.decl.name, ref.id.spelling);
					if(ref.id.decl == null){
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + ref.ref.decl.name);
					}
					else{
						if(!((MemberDecl)ref.id.decl).isStatic){
							reporter.log("***Cannot reference non-static member \"" + ref.id.decl.name + "\" from the static type \"" + ref.ref.decl.name + "\" at program location " + ref.id.decl.posn);
						}
						if(((MemberDecl)ref.id.decl).isPrivate){
							if(!(((ClassDecl)ref.ref.decl).name.equals(ScopeID.currentClass.name))){
								reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ref.ref.decl.name);
							}
						}
						//else ref.decl = ref.id.decl;
					}

				}
				else{
					reporter.log("***Identifier \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is not a class type, trying to reference members of \"" + ref.id.spelling + "\" is illegal");
					ref.ref.decl = null;
				}
			}
			else{
				if(ref.ref.decl == null){
					return null;
				}
				if(ref.ref.decl.type instanceof ClassType && !(ref.ref.decl instanceof MethodDecl)){
					ref.id.decl = ScopeID.getMember(((ClassType)ref.ref.decl.type).className.spelling, ref.id.spelling);
					ref.decl = ref.ref.decl;
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
						if(!((ClassType)ref.ref.decl.type).className.spelling.equals(ScopeID.currentClass.name)){
							reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ((ClassType)ref.ref.decl.type).className.spelling);
						}				}
				}
				else if(ref.ref.decl instanceof MethodDecl){
					reporter.log("***Cannot qualify method reference to " + ref.ref.decl.name + " at program location " + ref.ref.posn + ".");
				}
				else if(ref.ref instanceof ThisRef){
					ref.id.decl = ScopeID.getMember(ScopeID.currentClass.name, ref.id.spelling);
					//ref.decl = ref.id.decl;
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
				else if(ref.ref.decl.type instanceof ArrayType){
					if(ref.id.spelling.equals("length")){
						ref.id.decl = ARRAY_LEN;
						//ref.decl = ARRAY_LEN;
					}
					else{
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member of the Array class.");
					}
				}
				//CLASS access
				else if(ref.ref.decl.type == null){
					ref.id.decl = ScopeID.getMember(ref.ref.decl.name, ref.id.spelling);
					if(ref.id.decl == null){
						reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " could not be resolved to a member in the class " + ref.ref.decl.name);
					}
					else{
						if(!((MemberDecl)ref.id.decl).isStatic){
							reporter.log("***Cannot reference non-static member \"" + ref.id.decl.name + "\" from the static type \"" + ref.ref.decl.name + "\" at program location " + ref.id.decl.posn);
						}
						else if(((MemberDecl)ref.id.decl).isPrivate){
							if(!(((ClassDecl)ref.ref.decl).name.equals(ScopeID.currentClass.name))){
								reporter.log("***Member \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is a private field and cannot be accesed outside of the class " + ref.ref.decl.name);
							}					}
						//else ref.decl = ref.id.decl;
					}

				}
				else{
					reporter.log("***Identifier \"" + ref.id.spelling + "\" at program location " + ref.id.posn + " is not a class type, trying to reference members of \"" + ref.id.spelling + "\" is illegal");
					ref.ref.decl = null;
				}
			}
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
		if(ref.id.decl instanceof MemberDecl){
			if(staticContext){
				if(!((MemberDecl)ref.id.decl).isStatic){
					reporter.log("***Cannot access non-static member " + ref.id.spelling + " in a static context at program location " + ref.posn + ".");
				}
			}
		}
		ref.decl = ref.id.decl;
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		if(staticContext){
			reporter.log("***Cannot use a \"this\" reference in a static context.");
		}
		else ref.decl = ScopeID.currentClass;
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//TERMINALS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		id.decl = ScopeID.get(id.spelling);
		if(id.decl == null) id.decl = ScopeID.getMember(ScopeID.currentClass.name, id.spelling);
		if(id.decl == null) id.decl = ScopeID.getClass(id.spelling);
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

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		return null;
	}

}

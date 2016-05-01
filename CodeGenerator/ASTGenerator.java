package miniJava.CodeGenerator;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

import java.util.Hashtable;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.ObjectFile;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;

public class ASTGenerator implements Visitor<Object, Object> {
	private Hashtable<Declaration, Integer> patchAddr;
	private Hashtable<Statement, Integer> jumpAddr;
	private int fieldNum = 0;
	private int staticFieldNum = 0;
	private int paramNum = 0;
	private int varNum = 0;
	private String progName;
	private MethodDecl MAIN;
	
	public ASTGenerator(String progName){
		patchAddr = new Hashtable<Declaration, Integer>();
		jumpAddr = new Hashtable<Statement, Integer>();
		this.progName = progName;
		Machine.initCodeGen();
	}
	
	public void genCode(AST program){
		program.visit(this, null);
		//writing code to object file
		String objectCodeFileName = progName + ".mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		if (objF.write()) {
			System.exit(-4);
		}
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		for(ClassDecl cd : prog.classDeclList){
			fieldNum = 0;
			for(MethodDecl md : cd.methodDeclList){
				if(md.isMain) MAIN = md;
			}
			for(FieldDecl fd : cd.fieldDeclList){
				if(!fd.isStatic){
					fd.disp = fieldNum;
					fieldNum++;
				}
				else{
					fd.disp = staticFieldNum;
					staticFieldNum++;
					Machine.emit(Op.LOADL, 0);
				}
			}
			cd.size = fieldNum;
		}
		Machine.emit(Op.LOADL, 0);
		Machine.emit(Prim.newarr);
		patchAddr.put(MAIN, Machine.nextInstrAddr());
		//let program know main needs to be patched
		Machine.emit(Op.CALL, Reg.CB, -1);
		Machine.emit(Op.HALT, 0, 0, 0);
		for(ClassDecl cd : prog.classDeclList){
			cd.visit(this, null);
		}
		//Patching addresses
		for(Declaration d : patchAddr.keySet()){
			Machine.patch(patchAddr.get(d), d.disp);
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
		for(MethodDecl md : cd.methodDeclList){
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		md.disp = Machine.nextInstrAddr();
		varNum = 0;
		paramNum = 0;
		for(int i = 0; i < md.parameterDeclList.size(); i++){
			paramNum++;
			md.parameterDeclList.get(i).disp = -(md.parameterDeclList.size()-i);
		}
		for(Statement s : md.statementList){
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.disp = varNum;
		varNum++;
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
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		for(Statement s : stmt.sl){
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		if(stmt.ref instanceof IdRef){
			if(((IdRef)stmt.ref).id.decl instanceof FieldDecl){
				if(((FieldDecl)((IdRef)stmt.ref).id.decl).isStatic){
					stmt.val.visit(this, null);
					Machine.emit(Op.STORE, Reg.SB, ((FieldDecl)((IdRef)stmt.ref).id.decl).disp + 1);
				}
				else{
					Machine.emit(Op.LOADA, Reg.OB, 0);
					Machine.emit(Op.LOADL, stmt.ref.decl.disp);
					stmt.val.visit(this, null);
					Machine.emit(Prim.fieldupd);
					//Machine.emit(Op.STORE, Reg.OB, stmt.ref.decl.disp);
				}
			}
			else if(((IdRef)stmt.ref).id.decl instanceof VarDecl){
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.LB, stmt.ref.decl.disp + 3);
			}
		}
		else if(stmt.ref instanceof QualifiedRef){
			if(((FieldDecl)((QualifiedRef)stmt.ref).id.decl).isStatic){
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.SB, ((FieldDecl)((QualifiedRef)stmt.ref).id.decl).disp + 1);
			}
			else{
				assignQualifiedRef((QualifiedRef)stmt.ref, null);
				stmt.val.visit(this, null);
				Machine.emit(Prim.fieldupd);
			}
		}
		else{
			Machine.emit(Op.STORE, Reg.LB, stmt.ref.decl.disp + 3);
		}
		return null;
	}
	
	private Object assignQualifiedRef(QualifiedRef ref, Object arg){
		if(ref.id.decl instanceof FieldDecl){
			if(((FieldDecl)ref.id.decl).isStatic){
				//Don't know how to handle this
			}
			else{
				if(ref.ref instanceof QualifiedRef){
					assignQualifiedRef((QualifiedRef)ref.ref, null);
					Machine.emit(Prim.fieldref);
					Machine.emit(Op.LOADL, ref.id.decl.disp);
				}
				else if(ref.ref instanceof ThisRef){
					Machine.emit(Op.LOADA, Reg.OB, 0);
					Machine.emit(Op.LOADL, ref.id.decl.disp);
				}
				//must be an IDref
				else{
					if(((IdRef)ref.ref).id.decl instanceof VarDecl){
						Machine.emit(Op.LOAD, Reg.LB, ref.ref.decl.disp + 3);
						Machine.emit(Op.LOADL, ref.id.decl.disp);
					}
					else if(((IdRef)ref.ref).id.decl instanceof FieldDecl){
						Machine.emit(Op.LOADA, Reg.OB, 0);
						Machine.emit(Op.LOADL, ((IdRef)ref.ref).id.decl.disp);
						Machine.emit(Prim.fieldref);
						Machine.emit(Op.LOADL, ref.id.decl.disp);
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		assignIxReference((IndexedRef)stmt.ixRef, null);
		stmt.val.visit(this, null);
		Machine.emit(Prim.arrayupd);
		return null;
	}
	
	private Object assignIxReference(IndexedRef ref, Object arg){
		if(ref.idRef.decl instanceof FieldDecl){
			Machine.emit(Op.LOADA, Reg.OB, 0);
			Machine.emit(Op.LOADL, ref.idRef.decl.disp);
			Machine.emit(Prim.fieldref);
		}
		else if(ref.idRef.decl instanceof ParameterDecl){
			Machine.emit(Op.LOAD, Reg.LB, ref.idRef.decl.disp);
		}
		else{
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.disp + 3);
		}
		ref.indexExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		if(stmt.argList.size() != 0){
			for(Expression e : stmt.argList){
				e.visit(this, null);
			}
		}
		if(stmt.methodRef instanceof QualifiedRef){
			if(((QualifiedRef)stmt.methodRef).id.decl.posn == null){
				Machine.emit(Prim.putintnl);
			}
			else{
				if(((MethodDecl)((QualifiedRef)stmt.methodRef).id.decl).isStatic){
					if(((QualifiedRef)stmt.methodRef).id.decl.disp == -1){
						patchAddr.put(((QualifiedRef)stmt.methodRef).id.decl, Machine.nextInstrAddr());
					}
					Machine.emit(Op.CALL, Reg.CB, ((QualifiedRef)stmt.methodRef).id.decl.disp);
				}
				else{
					((QualifiedRef)stmt.methodRef).visit(this, null);
					if(((QualifiedRef)stmt.methodRef).id.decl.disp == -1){
						patchAddr.put(((QualifiedRef)stmt.methodRef).id.decl, Machine.nextInstrAddr());
					}
					Machine.emit(Op.CALLI, Reg.CB, ((QualifiedRef)stmt.methodRef).id.decl.disp);
				}
			}
			if(((MethodDecl)((QualifiedRef)stmt.methodRef).id.decl).type.typeKind != TypeKind.VOID){
				Machine.emit(Op.POP, 1);
			}
		}
		else if(stmt.methodRef instanceof IdRef){
			stmt.methodRef.visit(this, null);
			if(((MethodDecl)((IdRef)stmt.methodRef).id.decl).type.typeKind != TypeKind.VOID){
				Machine.emit(Op.POP, 1);
			}
		}
		
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		if(stmt.returnExpr == null){
			Machine.emit(Op.RETURN, 0, 0, paramNum);
		}
		else{
			stmt.returnExpr.visit(this, null);
			Machine.emit(Op.RETURN, 1, 0, paramNum);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.disp = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);
		jumpAddr.put(stmt.thenStmt, Machine.nextInstrAddr());
		Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, stmt.thenStmt.disp);
		int branch_end = -1;
		int after_if = -1;
		if(stmt.elseStmt != null){
			jumpAddr.put(stmt.elseStmt, Machine.nextInstrAddr());
			Machine.emit(Op.JUMP, Reg.CB, stmt.elseStmt.disp);
		}
		else{
			after_if = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, branch_end);
		}
		stmt.thenStmt.visit(this, null);
		
		int jump_end = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, branch_end);
		if(stmt.elseStmt != null){
			stmt.elseStmt.visit(this, null);
		}
		branch_end = Machine.nextInstrAddr();
		Machine.patch(jumpAddr.get(stmt.thenStmt), stmt.thenStmt.disp);
		if(stmt.elseStmt != null) Machine.patch(jumpAddr.get(stmt.elseStmt), stmt.elseStmt.disp);
		else Machine.patch(after_if, jump_end);
		Machine.patch(jump_end, branch_end);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int count = 0;
		if(stmt.body instanceof BlockStmt){
			for(Statement s : ((BlockStmt)stmt.body).sl){
				if(s instanceof VarDeclStmt) count++;
			}
		}
		stmt.disp = Machine.nextInstrAddr();
		int start = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);
		jumpAddr.put(stmt.body, Machine.nextInstrAddr());
		Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, stmt.body.disp);
		int loop_end = -1;
		int jump_end = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, loop_end);
		stmt.body.visit(this, null);
		Machine.emit(Op.POP, count);
		Machine.emit(Op.JUMP, Reg.CB, start);
		loop_end = Machine.nextInstrAddr();
		Machine.patch(jumpAddr.get(stmt.body), stmt.body.disp);
		Machine.patch(jump_end, loop_end);
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		if(expr.operator.spelling.equals("!")) Machine.emit(Prim.not);
		else if(expr.operator.spelling.equals("-")) Machine.emit(Prim.neg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		Object left = expr.left.visit(this, null);
		if(left instanceof Boolean){
			if(expr.operator.spelling.equals("&&")){
				if(((Boolean)left).booleanValue() == false){
					return new Boolean(false);
				}
			}
			else if(expr.operator.spelling.equals("||")){
				if(((Boolean)left).booleanValue() == true){
					return new Boolean(true);
				}
			}
		}
		expr.right.visit(this, null);
		if(expr.operator.spelling.equals("+")) Machine.emit(Prim.add);
		else if(expr.operator.spelling.equals("-")) Machine.emit(Prim.sub);
		else if(expr.operator.spelling.equals("*")) Machine.emit(Prim.mult);
		else if(expr.operator.spelling.equals("/")) Machine.emit(Prim.div);
		else if(expr.operator.spelling.equals("||")) Machine.emit(Prim.or);
		else if(expr.operator.spelling.equals("&&")) Machine.emit(Prim.and);
		else if(expr.operator.spelling.equals("==")) Machine.emit(Prim.eq);
		else if(expr.operator.spelling.equals("!=")) Machine.emit(Prim.ne);
		else if(expr.operator.spelling.equals("<=")) Machine.emit(Prim.le);
		else if(expr.operator.spelling.equals(">=")) Machine.emit(Prim.ge);
		else if(expr.operator.spelling.equals("<")) Machine.emit(Prim.lt);
		else if(expr.operator.spelling.equals(">")) Machine.emit(Prim.gt);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		for(Expression e : expr.argList){
			e.visit(this, null);
		}
		if(expr.functionRef instanceof IdRef){
			Machine.emit(Op.LOADA, Reg.OB, 0);
			if(((IdRef)expr.functionRef).id.decl.disp == -1){
				patchAddr.put(((IdRef)expr.functionRef).id.decl, Machine.nextInstrAddr());
			}
			Machine.emit(Op.CALLI, Reg.CB, ((IdRef)expr.functionRef).id.decl.disp);
		}
		else if(expr.functionRef instanceof QualifiedRef){
			expr.functionRef.visit(this, null);
			if(((QualifiedRef)expr.functionRef).id.decl.disp == -1){
				patchAddr.put(((QualifiedRef)expr.functionRef).id.decl, Machine.nextInstrAddr());
			}
			Machine.emit(Op.CALLI, Reg.CB, ((QualifiedRef)expr.functionRef).id.decl.disp);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		Object ex = expr.lit.visit(this, null);
		return ex;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.size);
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//REFERENCES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		if(ref.id.spelling.equals("length")){
			if(ref.ref.decl instanceof FieldDecl){
				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, ref.ref.decl.disp);
				Machine.emit(Prim.fieldref);
				Machine.emit(Prim.arraylen);
			}
			else{
				Machine.emit(Op.LOAD, Reg.LB, ref.ref.decl.disp + 3);
				Machine.emit(Prim.arraylen);
			}
		}
		else{
			
			ref.ref.visit(this, null);
			if(ref.id.decl instanceof FieldDecl){
				if(((FieldDecl)ref.id.decl).isStatic){
					Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.disp + 1);
				}
				else{
					Machine.emit(Op.LOADL, ref.id.decl.disp);
					Machine.emit(Prim.fieldref);
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Object arg) {
		if(ref.idRef.decl instanceof FieldDecl){
			Machine.emit(Op.LOADA, Reg.OB, 0);
			Machine.emit(Op.LOADL, ref.idRef.decl.disp);
			Machine.emit(Prim.fieldref);
		}
		else if(ref.idRef.decl instanceof ParameterDecl){
			Machine.emit(Op.LOAD, Reg.LB, ref.idRef.decl.disp);
		}
		else{
			Machine.emit(Op.LOAD, Reg.LB, ref.idRef.decl.disp + 3);
		}
		ref.indexExpr.visit(this, null);
		Machine.emit(Prim.arrayref);
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		ref.id.visit(this, null);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//TERMINALS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		if(id.decl instanceof FieldDecl){
			if(((FieldDecl)id.decl).isStatic){
				Machine.emit(Op.LOAD, Reg.SB, id.decl.disp + 1);
			}
			else{
				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, id.decl.disp);
				Machine.emit(Prim.fieldref);
			}
		}
		else if(id.decl instanceof MethodDecl){
			Machine.emit(Op.LOADA, Reg.OB, 0);
			if(id.decl.disp == -1){
				patchAddr.put(id.decl, Machine.nextInstrAddr());
			}
			Machine.emit(Op.CALLI, Reg.CB, id.decl.disp);
		}
		else if(id.decl instanceof ClassDecl){
			
		}
		else if(id.decl instanceof ParameterDecl){
			Machine.emit(Op.LOAD, Reg.LB, id.decl.disp);
		}
		else{
			Machine.emit(Op.LOAD, Reg.LB, id.decl.disp+3);
		}
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if(bool.spelling.equals("true")){
			Machine.emit(Op.LOADL, Machine.trueRep);
			return new Boolean(true);
		}
		else{
			Machine.emit(Op.LOADL, Machine.falseRep);
			return new Boolean(false);
		}
	}

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		Machine.emit(Op.LOADL, Machine.nullRep);
		return null;
	}

}

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
	private int fieldNum = 0;
	private int paramNum = 0;
	private int varNum = 0;
	private String progName;
	private MethodDecl MAIN;
	
	public ASTGenerator(String progName){
		patchAddr = new Hashtable<Declaration, Integer>();
		this.progName = progName;
		Machine.initCodeGen();
	}
	
	public void genCode(AST program){
		program.visit(this, null);
		//writing code to object file
		String objectCodeFileName = progName + ".mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
		}
		else
			System.out.println("SUCCEEDED");	
		/* ONLY FOR USE IN TESTING, DELETE AFTERWARDS
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 */
		//creating asm file
		String asmCodeFileName = progName + ".asm";
		System.out.print("Writing assembly file ... ");
		Disassembler d = new Disassembler(objectCodeFileName);
		if (d.disassemble()) {
			System.out.println("FAILED!");
		}
		else
			System.out.println("SUCCEEDED");

		System.out.println("Running code ... ");
		Interpreter.debug(objectCodeFileName, asmCodeFileName);
		
		System.out.println("*** mJAM execution completed");
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		for(ClassDecl cd : prog.classDeclList){
			for(MethodDecl md : cd.methodDeclList){
				if(md.isMain) MAIN = md;
			}
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
//		for(MethodDecl md : codeAddr.keySet()){
//			if(patchAddr.get(md.name) != null){
//				Machine.patch(patchAddr.get(md.name), md.disp);
//			}
//		}
//		for(String s : patchAddr.keySet()){
//			Machine.patch(patchAddr.get(s), codeAddr.get(s));
//		}
		
		
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		for(FieldDecl fd : cd.fieldDeclList){
			fd.visit(this, null);
			if(!fd.isStatic){
				fieldNum++;
			}
		}
		for(MethodDecl md : cd.methodDeclList){
			md.visit(this, null);
		}
		cd.size = fieldNum;
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		fd.disp = fieldNum;
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		for(Statement s : stmt.sl){
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.PUSH, 1);
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		if(stmt.initExp instanceof NewObjectExpr){
			
		}
		else{
			Machine.emit(Op.STORE, Reg.LB, stmt.varDecl.disp + 3);
		}

		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.val.visit(this, null);
		Machine.emit(Op.STORE, Reg.OB, stmt.ref.decl.disp);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		for(Expression e : stmt.argList){
			e.visit(this, null);
		}
		stmt.methodRef.visit(this, null);
		//Need to check if CALL or CALLI should be used
		//Machine.emit(Op.CALLI, Reg.CB, stmt.methodRef.decl.disp);
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.left.visit(this, null);
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
		// TODO Auto-generated method stub
		for(int i = 0; i < expr.argList.size(); i++){
			expr.argList.get(i).visit(this, null);
		}
		if(((MethodDecl)expr.functionRef.decl).isStatic){
			if(expr.functionRef.decl.disp == -1){
				patchAddr.put(expr.functionRef.decl, Machine.nextInstrAddr());
			}
			Machine.emit(Op.CALL, Reg.CB, expr.functionRef.decl.disp);
		}
		else{
			
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
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.size);
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//REFERENCES
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref.ref.visit(this, null);
		ref.id.visit(this, null);
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		if(ref.decl instanceof FieldDecl){
			//pretending they're all 'this' references right now
			Machine.emit(Op.LOAD, Reg.OB, ref.decl.disp);
			System.out.println("FieldDecl " + ref.decl.name + " referenced");
		}
		else if(ref.decl instanceof MethodDecl){
			System.out.println("MethodDecl " + ref.decl.name + " referenced");
		}
		else if(ref.decl instanceof ClassDecl){
			System.out.println("ClassDecl " + ref.decl.name + " referenced");
		}
		else if(ref.decl instanceof ParameterDecl){
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.disp);
			System.out.println("ParameterDecl " + ref.decl.name + " referenced");
		}
		else{
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.disp+3);
			System.out.println("VarDecl " + ref.decl.name + " referenced");
		}
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	//
	//TERMINALS
	//
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		//May not be right
		if(op.spelling.equals("+")) Machine.emit(Prim.add);
		else if(op.spelling.equals("-")) Machine.emit(Prim.sub);
		else if(op.spelling.equals("*")) Machine.emit(Prim.mult);
		else if(op.spelling.equals("/")) Machine.emit(Prim.div);
		else if(op.spelling.equals("||")) Machine.emit(Prim.or);
		else if(op.spelling.equals("&&")) Machine.emit(Prim.and);
		else if(op.spelling.equals("==")) Machine.emit(Prim.eq);
		else if(op.spelling.equals("!=")) Machine.emit(Prim.ne);
		else if(op.spelling.equals("<=")) Machine.emit(Prim.le);
		else if(op.spelling.equals(">=")) Machine.emit(Prim.ge);
		else if(op.spelling.equals("<")) Machine.emit(Prim.lt);
		else if(op.spelling.equals(">")) Machine.emit(Prim.gt);
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if(bool.spelling.equals("true")) Machine.emit(Op.LOADL, Machine.trueRep);
		else Machine.emit(Op.LOADL, Machine.falseRep);
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		Machine.emit(Op.LOADL, Machine.nullRep);
		return null;
	}

}

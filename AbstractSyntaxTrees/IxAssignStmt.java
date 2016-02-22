/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IxAssignStmt extends Statement
{
    public IxAssignStmt(IndexedRef ir, Expression e, SourcePosition posn){
        super(posn);
        ixRef = ir;
        val = e;
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitIxAssignStmt(this, o);
    }
    
    public IndexedRef ixRef;
    public Expression val;
}

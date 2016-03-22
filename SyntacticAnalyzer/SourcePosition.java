package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int line, col;
	public SourcePosition(int line, int col){
		this.line = line;
		this.col = col;
	}
	
	public String toString(){
		return "Line: " + line + ", Character: " + col;
	}
}

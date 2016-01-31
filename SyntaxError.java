package miniJava;

public class SyntaxError extends Error {
	private static final long serialVersionUID = 1L;
	private String message;
	public SyntaxError(){
		message = null;
	}
	
	public SyntaxError(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return message;
	}
}

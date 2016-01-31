package miniJava;

public class SyntaxException extends Exception {
	private static final long serialVersionUID = 1L;
	private String message;
	public SyntaxException(){
		message = null;
	}
	public SyntaxException(String message){
		this.message = message;
	}
	public String getMessage(){
		return message;
	}
}

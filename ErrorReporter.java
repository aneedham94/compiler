package miniJava;
import java.util.ArrayList;

public class ErrorReporter {
	private ArrayList<String> errors;
	public ErrorReporter(){
		errors = new ArrayList<String>();
	}
	
	public void log(String message){
		errors.add(message);
	}
	
	public int report(){
		if(errors.size() > 0){
			for(String error: errors){
				System.out.println(error);
			}
			return errors.size();
		}
		return 0;
	}
}

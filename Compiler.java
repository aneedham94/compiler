package miniJava;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Parser;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

public class Compiler {
	public static final int parseSuccess = 0;
	public static final int parseFail = 4;
	public static void main(String[] args){
		if(args.length > 0){
			if(args[0].substring(args[0].length()-5, args[0].length()).equals(".java") || args[0].substring(args[0].length()-6, args[0].length()).equals(".mjava")){
				BufferedReader reader = null;
				try{
					reader = new BufferedReader(new FileReader(new File(System.getProperty("user.dir") + System.getProperty("file.separator") + args[0])));
				} catch(FileNotFoundException e){
					System.out.println("Could not find file " + args[0] + "in current working directory.");
					System.exit(parseFail);
				}
				Scanner scanner = new Scanner(reader);
				Parser parser = new Parser(scanner);
				parser.parse();
			}
			else{
				System.out.println("Source file must be of type .java or .mjava.");
				System.exit(parseFail);
			}
		}
		else{
			System.out.println("No source file specified.  Please run the program as \"java Compiler <filename>\".");
			System.exit(parseFail);
		}
	}
}
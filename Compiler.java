package miniJava;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.ASTIdentification;
import miniJava.AbstractSyntaxTrees.ASTTypecheck;
import miniJava.CodeGenerator.ASTGenerator;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SyntaxException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;


public class Compiler {
	public static final int parseSuccess = 0;
	public static final int parseFail = 4;
	public static void main(String[] args){
		String name = null;
		if(args.length > 0){
			if(args[0].substring(args[0].length()-5, args[0].length()).equals(".java") || args[0].substring(args[0].length()-6, args[0].length()).equals(".mjava")){
				if(args[0].substring(args[0].length()-5, args[0].length()).equals(".java")){
					name = args[0].substring(0, args[0].length()-5);
				}
				else{
					name = args[0].substring(0, args[0].length()-6);
				}
				BufferedReader reader = null;
				try{
					reader = new BufferedReader(new FileReader(new File(args[0])));
				} catch(FileNotFoundException e){
					System.out.println("Could not find file " + args[0]);
					System.exit(parseFail);
				}
				Scanner scanner = null;
				try{
					scanner = new Scanner(reader);
				} catch(IOException e){
					System.out.println("Failed to read the first character from source file while initializing scanner.  Aborting parse.");
					System.exit(parseFail);
				}
				Parser parser = new Parser(scanner);
				AST program = null;
				try{
					program = parser.parse();
				} catch(SyntaxException e){
					System.out.println("Parse failed.  " + args[0] + " is not a valid miniJava program.");
					System.out.println(e.getMessage());
					System.exit(parseFail);
				} catch(IOException e){
					if(e.getMessage().length() == 1) System.out.println("Illegal token \'" + Scanner.visible(e.getMessage().charAt(0)) + "\'.  Aborting parse.");
					else System.out.println("Scanning error.  Aborting parse.");
					System.exit(parseFail);
				}
				try {
					reader.close();
				} catch (IOException e) {
					System.out.println("Failed to close the input file reader.");
					System.exit(parseFail);
				}
				ErrorReporter reporter = new ErrorReporter();
				ASTIdentification identifier = new ASTIdentification(reporter);
				identifier.identify(program);
				int numContextErrors = 0;
				numContextErrors = reporter.report();
				//AST display code
//				try{
//				 	ASTDisplay visitor = new ASTDisplay();
//					visitor.showTree(program);
//				} catch(NullPointerException e){
//					System.out.println("Null pointer exception while reading AST");
//				}
				//AST display code
				if(numContextErrors > 0) System.exit(parseFail);
				else{
					ErrorReporter typeErrors = new ErrorReporter();
					ASTTypecheck checker = new ASTTypecheck(typeErrors);
					checker.check(program);
					int numTypeErrors =  0;
					numTypeErrors = typeErrors.report();
					if(numTypeErrors > 0) System.exit(parseFail);
					else{
						ASTGenerator generator = new ASTGenerator(name);
						generator.genCode(program);
						System.exit(parseSuccess);
					}
//					System.exit(parseSuccess);
				}
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
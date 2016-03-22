package miniJava.AbstractSyntaxTrees;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Set;

public class ScopedTable {
	private Hashtable<String, ArrayList<Declaration>> scope;
	private Hashtable<String, Declaration> predefClasses;
	private Hashtable<String, Hashtable<String,Declaration>> predefMembers;
	private Hashtable<String, Declaration> classes;
	private Hashtable<String, Hashtable<String,Declaration>> members;
	private int scopeLevel = 0;
	public Declaration currentClass;
	public ScopedTable(){
		scope = new Hashtable<String, ArrayList<Declaration>>();
		predefClasses = new Hashtable<String, Declaration>();
		predefMembers = new Hashtable<String, Hashtable<String, Declaration>>();
		classes = new Hashtable<String, Declaration>();
		members = new Hashtable<String, Hashtable<String, Declaration>>();
	}
	
	public ScopedTable(int initialCapacity){
		scope = new Hashtable<String, ArrayList<Declaration>>(initialCapacity);
		predefClasses = new Hashtable<String, Declaration>(3);
		predefMembers = new Hashtable<String, Hashtable<String, Declaration>>(3);
		classes = new Hashtable<String, Declaration>(initialCapacity);
		members = new Hashtable<String, Hashtable<String, Declaration>>(initialCapacity);
	}
	
	public void openScope(){
		scopeLevel++;
		Set<String> keys = scope.keySet();
		for(String key : keys){
			scope.get(key).add(scopeLevel, null);
		}
	}
	
	public void closeScope(){
		Set<String> keys = scope.keySet();
		for(String key : keys){
			scope.get(key).add(scopeLevel, null);
		}
		scopeLevel--;
	}
	
	public boolean put(String key, Declaration value){
		if(scope.get(key) == null){
			ArrayList<Declaration> nlist = new ArrayList<Declaration>();
			for(int i = 0; i < scopeLevel; i++){
				nlist.add(null);
			}
			nlist.add(value);
			scope.put(key, nlist);
			if(scopeLevel == 1){
				currentClass = value;
			}
			return true;
		}
		else if(scope.get(key).get(scopeLevel) == null){
			scope.get(key).add(scopeLevel, value);
			if(scopeLevel == 1){
				currentClass = value;
			}
			return true;
		}
		return false;
	}
	
	public boolean skim(String key, Declaration value){
		if(scopeLevel == 1){
			currentClass = value;
			if(members.get(key) == null){
				currentClass = value;
				members.put(key, new Hashtable<String, Declaration>());
				classes.put(key, value);
				return true;
			}
			else return false;
		}
		else if(scopeLevel == 2){
			if(getMemberDecl(currentClass.name, key) == null){
				members.get(currentClass.name).put(key,  value);
				return true;
			}
			else return false;
		}
		return false;
	}
	
	public boolean predefine(String classname, String membername, Declaration value){
		if(scopeLevel == 1){
			currentClass = value;
			predefClasses.put(classname, value);
			if(predefMembers.get(classname) == null){
				predefMembers.put(classname, new Hashtable<String, Declaration>());
				return true;
			}
			else return false;
		}
		else if(scopeLevel == 2){
			if(predefMembers.get(classname).get(membername) == null){
				predefMembers.get(classname).put(membername,  value);
				return true;
			}
			else return false;
		}
		return false;
	}
	
	public Declaration get(String key){
		Declaration rval = null;
		//Search current scope
		if(scope.get(key) == null);
		else{
			for(int i = scopeLevel; i >= 0; i--){
				rval = scope.get(key).get(i);
				if(rval != null) return rval;
			}
		}
		
		//Search user-defined classes and their members
		Set<String> keys = members.keySet();
		for(String k : keys){
			rval = getMemberDecl(k, key);
			if(rval != null) return rval;
		}
		rval = getClassDecl(key);
		if(rval != null) return rval;
		
		//Search System classes and their members
		Set<String> preKeys = predefMembers.keySet();
		for(String k : preKeys){
			rval = getPredefMemberDecl(k, key);
			if(rval != null) return rval;
		}
		rval = getPredefClassDecl(key);
		
		return rval;
	}
	
	private Declaration getPredefMemberDecl(String classname, String membername){
		if(predefMembers.get(classname) == null) return null;
		return predefMembers.get(classname).get(membername);
	}
	
	private Declaration getPredefClassDecl(String classname){
		return predefClasses.get(classname);
	}
	
	public Declaration getMemberDecl(String classname, String membername){
		if(members.get(classname) == null && predefMembers.get(classname) == null) return null;
		Declaration rval = null;
		if(members.get(classname) != null) rval = members.get(classname).get(membername);
		if(rval == null && predefMembers.get(classname) != null) rval = predefMembers.get(classname).get(membername);
		return rval;
	}
	
	public Declaration getClassDecl(String classname){
		Declaration rval = classes.get(classname);
		if(rval == null) rval = predefClasses.get(classname);
		return rval;
	}
	
	public static void test(){
		
	}
}

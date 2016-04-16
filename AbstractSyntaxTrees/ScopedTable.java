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
		else{
			if(scopeLevel >= 4){
				for(int i = scopeLevel; i >= 3; i--){
					if(scope.get(key).get(i) != null) return false;
				}
				scope.get(key).add(scopeLevel, value);
				return true;
			}
			else{
				if(scope.get(key).get(scopeLevel) == null){
					scope.get(key).add(scopeLevel, value);
					if(scopeLevel == 1){
						currentClass = value;
					}
					return true;
				}
			}
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
			if(members.get(currentClass.name).get(key) == null){
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
	
	public Declaration getClass(String className){
		Declaration rval = null;
		rval = classes.get(className);
		if(rval == null){
			rval = predefClasses.get(className);
		}
		return rval;
	}
	
	public Declaration getMember(String className, String member){
		Declaration rval = null;
		if(classes.get(className) != null){
			rval = members.get(className).get(member);
//			if(rval == null){
//				if(predefClasses.get(className) != null){
//					rval = predefMembers.get(className).get(member);
//				}
//			}
		}
		else if(predefClasses.get(className) != null){
			rval = predefMembers.get(className).get(member);
		}
		return rval;
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
		return rval;
	}
	
	public Declaration searchAll(String key){
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
			rval = members.get(k).get(key);
			if(rval != null) return rval;
		}
		rval = getClass(key);
		if(rval != null) return rval;
		
		//Search System classes and their members
		Set<String> preKeys = predefMembers.keySet();
		for(String k : preKeys){
			rval = predefMembers.get(k).get(key);
			if(rval != null) return rval;
		}
		rval = predefClasses.get(key);
		return rval;
	}
	
	public static void test(){
		
	}
}

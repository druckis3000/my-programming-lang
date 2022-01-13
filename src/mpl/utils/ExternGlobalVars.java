package mpl.utils;

import java.util.ArrayList;

public class ExternGlobalVars {
	public ArrayList<String> vars = new ArrayList<>();
	
	public ExternGlobalVars() {
	}
	
	public void addVar(String name){
		for(String str : vars){
			if(str.equals(name))
				return;
		}
		
		vars.add(name);
	}
}
package mpl.compiler.asm.sections;

import java.util.HashMap;

public class AsmSectionData {
	
	/* Id, Data */
	private HashMap<String, String> dataList = new HashMap<String, String>();
	
	public AsmSectionData() {
	}
	
	public void addData(String id, String data){
		dataList.put(id, data);
	}
	
	public boolean containsData(String data){
		for(String key : dataList.keySet())
			if(dataList.get(key).equals(data))
				return true;
		
		return false;
	}
	
	public String createAssemblyCode(){
		if(dataList.isEmpty())
			return "";
		
		StringBuilder code = new StringBuilder();
		
		// Put rodata in 'code'
		dataList.forEach((id, data) -> code.append(id + ": " + data + System.lineSeparator()));
		
		// Return rodata without last line
		return code.toString().substring(0, code.length() - 1);
	}
}
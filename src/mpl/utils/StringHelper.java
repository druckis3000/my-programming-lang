package mpl.utils;

public class StringHelper {

	public static String addTabs(String string, int numberOfTabs){
		String newString = "";
		String tabs = createTabs(numberOfTabs);
		String[] lines = string.split(System.lineSeparator());
		boolean endsWithNewLine = string.endsWith(System.lineSeparator());
		
		for(int i=0; i<lines.length; i++){
			newString += tabs + lines[i];
			
			if(i+1 == lines.length){
				if(endsWithNewLine){
					newString += System.lineSeparator();
				}
			}else{
				newString += System.lineSeparator();
			}
		}
		
		return newString;
	}
	
	private static String createTabs(int numberOfTabs){
		String tabs = "";
		for(int i=0; i<numberOfTabs; i++)
			tabs += "\t";
		return tabs;
	}
	
	public static String findLine(String haystack, String needle){
		String[] lines = haystack.split(System.lineSeparator());
		for(String currentLine : lines){
			if(currentLine.contains(needle))
				return currentLine;
		}
		return "";
	}
}

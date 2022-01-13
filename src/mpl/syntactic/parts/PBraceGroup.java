package mpl.syntactic.parts;

import java.util.ArrayList;
import java.util.StringJoiner;

public class PBraceGroup extends PProgramPart {
	public ArrayList<PProgramPart> elements = new ArrayList<>();
	
	public PBraceGroup(String file, int lineInSourceCode, int columnInSourceCode, PProgramPart parent) {
		super(file, lineInSourceCode, columnInSourceCode, parent);
	}
	
	public int size(){
		return elements.size();
	}

	@Override
	protected String getAstCode(String padding) {
		return this.toString();
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");
		elements.forEach(element -> joiner.add(element.toString()));
		return "{" + joiner.toString() + "}";
	}

	@Override
	protected PProgramPart clone(boolean recursive) {
		PBraceGroup braceGroup = new PBraceGroup(sourceFileName, lineInSourceCode, columnInSourceCode, parent);
		
		if(recursive){
			for(PProgramPart el : elements)
				braceGroup.elements.add(el.clone(true));
		}else{
			for(PProgramPart el : elements)
				braceGroup.elements.add(el);
		}
		
		return null;
	}
}

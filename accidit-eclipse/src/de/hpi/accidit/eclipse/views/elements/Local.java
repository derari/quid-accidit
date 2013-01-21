package de.hpi.accidit.eclipse.views.elements;

public class Local {
	
	public int id;
	public String name;
	public int arg;
	
	public long valueId;
	public char primType;

	public int methodId;
	public int step;
	
	/**
	 * Returns the local's value depending on its primType.
	 * 
	 * @return
	 */
	public String getValue() {
		// TODO finish
		
		switch(primType) {
		case 'L': return "";
		default: return String.valueOf(valueId);
		}
	}
	
	public boolean isObject() {
		return primType == 'L';
	}
}

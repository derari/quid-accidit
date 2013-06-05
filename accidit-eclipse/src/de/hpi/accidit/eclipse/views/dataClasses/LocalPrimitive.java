package de.hpi.accidit.eclipse.views.dataClasses;


public class LocalPrimitive extends LocalBase {

	public String value;
	
	public LocalPrimitive(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return String.format("\"%s\"", value);
	}
	
	public boolean isObject() {
		return false;
	}
}

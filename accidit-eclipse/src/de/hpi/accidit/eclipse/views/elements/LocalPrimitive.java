package de.hpi.accidit.eclipse.views.elements;

public class LocalPrimitive extends LocalBase {

	public String value;
	
	public LocalPrimitive(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public boolean isObject() {
		return false;
	}
}

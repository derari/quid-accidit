package de.hpi.accidit.eclipse.views.elements;

public class LocalObject extends LocalBase {
	
	public long objectId;
	
	public LocalObject(long objectId) {
		this.objectId = objectId;
	}

	@Override
	public String getValue() {
		return type;
	}

	@Override
	public boolean isObject() {
		return true;
	}

}

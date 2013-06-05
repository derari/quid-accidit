package de.hpi.accidit.eclipse.views.dataClasses;


public class LocalObject extends LocalBase {
	
	public long objectId;
	
	public LocalObject(long objectId) {
		this.objectId = objectId;
	}

	@Override
	public String getValue() {
		String result = type.substring(type.lastIndexOf(".") + 1);
		return result.substring(result.lastIndexOf("$") + 1);
	}

	@Override
	public boolean isObject() {
		return true;
	}

}

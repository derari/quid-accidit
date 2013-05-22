package de.hpi.accidit.eclipse.model;

public class FieldValue extends NamedValue {
	
	private long thisId;
	private boolean isPut, hasGet, getIsCurrent;
	
	public long getThisId() {
		return thisId;
	}
	
}
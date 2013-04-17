package de.hpi.accidit.eclipse.views.dataClasses;

public abstract class LocalBase {
	
	public abstract boolean isObject();
	public abstract String getValue();

	public int id;
	public String name;
	public int typeId;
	public String type;

	public int step;
	
	public int methodId;
	
	public boolean isLocalVariable;

	public String getName() {
		return name;
	}

	public String getStep() {
		return String.valueOf(step);
	}

	public String getType() {
		return type;
	}

}

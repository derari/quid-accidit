package de.hpi.accidit.eclipse.model;

public class Field implements NamedEntity {

	
	private int id;
	private String name;
	
	public Field() {
	}
	
	public Field(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
}

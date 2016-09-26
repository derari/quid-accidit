package de.hpi.accidit.eclipse.model;

public class Variable implements NamedEntity {

	private int id;
	private String name;
	
	public Variable() {
	}
	
	public Variable(int id, String name) {
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

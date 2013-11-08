package de.hpi.accidit.eclipse.model;

import org.cthul.miro.dsl.View;

import de.hpi.accidit.eclipse.model.db.VariableDao;
import de.hpi.accidit.eclipse.model.db.VariableDao.Query;

public class Variable implements NamedEntity {

	public static final View<Query> VIEW = VariableDao.VIEW;
	
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

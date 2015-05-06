package de.hpi.accidit.eclipse.model;

import org.cthul.miro.view.ViewR;

import de.hpi.accidit.eclipse.model.db.FieldDao;
import de.hpi.accidit.eclipse.model.db.FieldDao.Query;

public class Field implements NamedEntity {

	public static final ViewR<Query> VIEW = FieldDao.VIEW;
	
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

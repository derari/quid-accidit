package de.hpi.accidit.eclipse.model;

import org.cthul.miro.view.ViewR;

import de.hpi.accidit.eclipse.model.db.MethodDao;
import de.hpi.accidit.eclipse.model.db.MethodDao.Query;

public class Method {
	
	public static final ViewR<Query> VIEW = MethodDao.VIEW;
	
	public int id;
	public String type;
	public String name;
	public String signature;

}

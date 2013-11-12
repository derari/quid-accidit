package de.hpi.accidit.eclipse.model;

import org.cthul.miro.dsl.View;

import de.hpi.accidit.eclipse.model.db.FieldEventDao;
import de.hpi.accidit.eclipse.model.db.FieldEventDao.PutQuery;


public class FieldEvent extends TraceElement {
	
	public static final View<PutQuery> PUT_VIEW = FieldEventDao.PUT;
}

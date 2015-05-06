package de.hpi.accidit.eclipse.model;

import org.cthul.miro.view.ViewR;

import de.hpi.accidit.eclipse.model.db.FieldEventDao;
import de.hpi.accidit.eclipse.model.db.FieldEventDao.PutQuery;


public class FieldEvent extends TraceElement {
	
	public static final ViewR<PutQuery> PUT_VIEW = FieldEventDao.PUT;
}

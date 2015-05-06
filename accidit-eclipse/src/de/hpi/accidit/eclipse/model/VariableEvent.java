package de.hpi.accidit.eclipse.model;

import org.cthul.miro.view.ViewR;

import de.hpi.accidit.eclipse.model.db.VariableEventDao;
import de.hpi.accidit.eclipse.model.db.VariableEventDao.PutQuery;

public class VariableEvent extends TraceElement {

	public static final ViewR<PutQuery> PUT_VIEW = VariableEventDao.PUT;
}

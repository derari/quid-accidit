package de.hpi.accidit.eclipse.model;

import org.cthul.miro.dsl.View;

import de.hpi.accidit.eclipse.model.db.ExceptionEventDao;
import de.hpi.accidit.eclipse.model.db.ExceptionEventDao.CatchQuery;
import de.hpi.accidit.eclipse.model.db.ExceptionEventDao.ThrowQuery;


public class ExceptionEvent extends TraceElement {
	
	public static View<ThrowQuery> THROW_VIEW = ExceptionEventDao.THROW;
	public static View<CatchQuery> CATCH_VIEW = ExceptionEventDao.CATCH;

	public boolean isThrow;
	
	public ExceptionEvent() {
	}
	
	@Override
	public String getImage() {
		return isThrow ? "trace_throw.png" : "trace_catch.png";
	}
	
	@Override
	public String getShortText() {
		return isThrow ? "throw" : "catch";
	}
}

package de.hpi.accidit.eclipse.model;

public class ExceptionEvent extends TraceElement {
	
//	public static ViewR<ThrowQuery> THROW_VIEW = ExceptionEventDao.THROW;
//	public static ViewR<CatchQuery> CATCH_VIEW = ExceptionEventDao.CATCH;

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

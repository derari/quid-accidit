package de.hpi.accidit.eclipse.views.util;

import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public abstract class DoInUiThread<T> implements OFutureAction<OFuture<T>, Void>, Runnable {

	
	private T value;
	private Throwable error;

	@Override
	public Void call(OFuture<T> param) throws Exception {
		value = param.getResult();
		error = param.getException();
		
		// TODO rm
		System.out.println("DoInUiThread<T> #call (async)");
		
		Display.getDefault().asyncExec(this);
		return null;
	}

	@Override
	public void run() {
		run(value, error);
	}
	
	protected abstract void run(T value, Throwable error);

}

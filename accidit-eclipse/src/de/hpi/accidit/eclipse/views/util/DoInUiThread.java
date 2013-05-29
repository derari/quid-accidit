package de.hpi.accidit.eclipse.views.util;

import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.eclipse.swt.widgets.Display;

public abstract class DoInUiThread<T> implements MiFutureAction<MiFuture<T>, Void> {

	@Override
	public Void call(MiFuture<T> param) throws Exception {
		T value = param.getResult();
		Throwable error = param.getException();
		Display.getDefault().asyncExec(new UiThreadRunnable(value, error));
		return null;
	}
	
	protected abstract void run(T value, Throwable error);
	
	private class UiThreadRunnable implements Runnable {
		
		private T value;
		private Throwable error;
		
		public UiThreadRunnable(T value, Throwable error) {
			this.value = value;
			this.error = error;
		}
		
		@Override
		public void run() {
			DoInUiThread.this.run(value, error);
		}
		
	}

}

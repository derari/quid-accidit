package de.hpi.accidit.eclipse.views.util;

import java.util.function.BiConsumer;

import org.cthul.miro.function.MiFunction;
import org.cthul.miro.futures.MiFuture;
import org.eclipse.swt.widgets.Display;

public abstract class DoInUiThread<T> implements MiFunction<MiFuture<? extends T>, Void> {

	public static void run(Runnable r) {
		Display.getDefault().asyncExec(r);
	}
	
	public static <T> DoInUiThread<T> run(BiConsumer<T, ? super Throwable> action) {
		return new DoInUiThread<T>() {
			@Override
			protected void run(T value, Throwable error) {
				action.accept(value, error);
			}
		};
	}
	
	@Override
	public Void call(MiFuture<? extends T> param) throws Exception {
		T value = param.getResult();
		Throwable error = param.getException();
		DoInUiThread.run(new UiThreadRunnable(value, error));
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

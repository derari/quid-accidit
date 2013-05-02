package de.hpi.accidit.orm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public abstract class OFutureBase<V> implements OFuture<V> {
	
	private final Object lock = new Object();
	private final Future<?> cancelDelegate;
	private boolean done = false;
	private V value = null;
	private Throwable exception = null;
	
	private OnComplete<? super OFuture<V>, ?> onCompleteListener = null;
	private List<OnComplete<? super OFuture<V>, ?>> onCompleteListeners = null;

	public OFutureBase(Future<?> cancelDelegate) {
		this.cancelDelegate = cancelDelegate;
	}

	protected Future<?> getCancelDelegate() {
		return cancelDelegate;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return cancelDelegate.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return cancelDelegate.isCancelled();
	}

	@Override
	public boolean isDone() {
		if (done) return true;
		synchronized (lock) {
			return done;
		}
	}

	protected void setValue(V value) {
		synchronized (lock) {
			if (done) throw new IllegalStateException("Already done");
			this.value = value;
			done = true;
			lock.notifyAll();
		}
		triggerAllListeners();
	}
	
	protected void setException(Throwable exception) {
		synchronized (lock) {
			if (done) throw new IllegalStateException("Already done");
			this.exception = exception;
			done = true;
			lock.notifyAll();
		}
		triggerAllListeners();
	}
	
	private void triggerAllListeners() {
		assert isDone();
		if (onCompleteListener != null) {
			trigger(onCompleteListener);
		}
		if (onCompleteListeners != null) {
			for (OnComplete<? super OFuture<V>, ?> l: onCompleteListeners) {
				trigger(l);
			}
		}
	}
	
	protected void trigger(OnComplete<? super OFuture<V>, ?> listener) {
		listener.call(this);
	}
	
	@Override
	public boolean _waitUntilDone() {
		try {
			waitUntilDone();
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	@Override
	public V _get() {
		try {
			return get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		waitUntilDone();
		return __get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!waitUntilDone(timeout, unit)) {
			throw new TimeoutException();
		}
		return __get();
	}
	
	private V __get() throws ExecutionException {
		assert done;
		if (exception != null) throw new ExecutionException(exception);
		return value;
	}
	
	@Override
	public void waitUntilDone() throws InterruptedException {
		if (done) return;
		synchronized (lock) {
			if (!done) lock.wait();
		}
	}
	
	@Override
	public boolean waitUntilDone(long timeout, TimeUnit unit) throws InterruptedException {
		if (done) return true;
		synchronized (lock) {
			if (!done) lock.wait(unit.toMillis(timeout));
		}
		return done;
	}
	
	protected void ensureDone() {
		if (!isDone()) {
			throw new IllegalStateException("Execution not yet complete");
		}
	}
	
	@Override
	public boolean hasResult() {
		ensureDone();
		return exception == null;
	}
	
	@Override
	public boolean hasFailed() {
		return !hasResult();
	}
	
	@Override
	public V getResult() {
		ensureDone();
		return value;
	}
	
	@Override
	public Throwable getException() {
		ensureDone();
		return exception;
	}

	@Override
	public <R> OFuture<R> onComplete(OFutureAction<? super OFuture<V>, R> action) {
		OnComplete<? super OFuture<V>, R> listener = new OnComplete<>(cancelDelegate, action);
		if (done) {
			trigger(listener);
			return listener;
		}
		synchronized (lock) {
			if (done) {
				trigger(listener);
				return listener;
			}
			if (onCompleteListener == null) {
				onCompleteListener = listener;
			} else {
				if (onCompleteListeners == null) {
					onCompleteListeners = new ArrayList<>();
				}
				onCompleteListeners.add(listener);
			}
		}
		return listener;
	}
	
	@Override
	public <R> OFuture<R> onComplete(
					final OFutureAction<? super V, R> onSuccess, 
					final OFutureAction<? super Throwable, R> onFailure) {
		return onComplete(new OFutureAction<OFuture<V>, R>() {
			@Override
			public R call(OFuture<V> f) throws Exception {
				if (f.hasResult()) {
					return onSuccess.call(f.getResult());
				} else {
					return onFailure.call(f.getException());
				}
			}
		});
	}
	
	@Override
	public <R> OFuture<R> onSuccess(final OFutureAction<? super V, R> action) {
		return onComplete(new OFutureAction<OFuture<V>, R>() {
			@Override
			public R call(OFuture<V> f) throws Exception {
				return action.call(f.get());
			}
		});
	}
	
	@Override
	public <R> OFuture<R> onFailure(final OFutureAction<? super Throwable, R> action) {
		return onComplete(new OFutureAction<OFuture<V>, R>() {
			@Override
			public R call(OFuture<V> f) throws Exception {
				if (f.hasResult()) {
					return null;
				} else {
					return action.call(f.getException());
				}
			}
		});	
	}
	
	protected static class OnComplete<Param, Result> extends OFutureBase<Result> {
		
		private final OFutureAction<Param, Result> action;

		public OnComplete(Future<?> cancelDelegate, OFutureAction<Param, Result> action) {
			super(cancelDelegate);
			this.action = action;
		}
		
		protected void call(Param param) {
			final Result result;
			try {
				result = action.call(param);
			} catch (Throwable t) {
				setException(t);
				if (t instanceof Error) {
					throw (Error) t;
				}
				if (t instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				return;
			}
			setValue(result);
		}
		
	}

}

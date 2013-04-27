package de.hpi.accidit.orm.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public class OFutureDelegator<V> implements OFuture<V> {
	
	private final OFuture<V> delegatee;
	
	public OFutureDelegator(OFuture<V> delegatee) {
		this.delegatee = delegatee;
	}
	
	protected OFuture<V> getDelegatee() {
		return delegatee;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return getDelegatee().cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return getDelegatee().isCancelled();
	}

	@Override
	public boolean isDone() {
		return getDelegatee().isDone();
	}
	
	@Override
	public boolean _waitUntilDone() {
		return getDelegatee()._waitUntilDone();
	}
	
	@Override
	public V _get() {
		return getDelegatee()._get();
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		return getDelegatee().get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return getDelegatee().get(timeout, unit);
	}

	@Override
	public void waitUntilDone() throws InterruptedException {
		getDelegatee().waitUntilDone();
	}

	@Override
	public boolean waitUntilDone(long timeout, TimeUnit unit) throws InterruptedException {
		return getDelegatee().waitUntilDone(timeout, unit);
	}

	@Override
	public boolean hasResult() {
		return getDelegatee().hasResult();
	}

	@Override
	public boolean hasFailed() {
		return getDelegatee().hasFailed();
	}

	@Override
	public V getResult() {
		return getDelegatee().getResult();
	}

	@Override
	public Throwable getException() {
		return getDelegatee().getException();
	}

	@Override
	public <R> OFuture<R> onComplete(OFutureAction<? super OFuture<V>, R> action) {
		return getDelegatee().onComplete(action);
	}
	
	@Override
	public <R> OFuture<R> onComplete(OFutureAction<? super V, R> onSuccess, OFutureAction<? super Throwable, R> onFailure) {
		return getDelegatee().onComplete(onSuccess, onFailure);
	}
	
	@Override
	public <R> OFuture<R> onSuccess(OFutureAction<? super V, R> action) {
		return getDelegatee().onSuccess(action);
	}

	@Override
	public <R> OFuture<R> onFailure(OFutureAction<? super Throwable, R> action) {
		return getDelegatee().onFailure(action);
	}
	
}

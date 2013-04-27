package de.hpi.accidit.orm;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface OFuture<V> extends Future<V> {
	
	void waitUntilDone() throws InterruptedException;
	
	boolean waitUntilDone(long timeout, TimeUnit unit) throws InterruptedException;
	
	boolean hasResult();
	
	boolean hasFailed();
	
	V getResult();
	
	Throwable getException();
	
	boolean _waitUntilDone();
	
	V _get();

	<R> OFuture<R> onComplete(OFutureAction<? super OFuture<V>, R> action);
	
	<R> OFuture<R> onComplete(OFutureAction<? super V, R> onSuccess, OFutureAction<? super Throwable, R> onFailure);
	
	<R> OFuture<R> onSuccess(OFutureAction<? super V, R> action);
	
	<R> OFuture<R> onFailure(OFutureAction<? super Throwable, R> action);
	
}

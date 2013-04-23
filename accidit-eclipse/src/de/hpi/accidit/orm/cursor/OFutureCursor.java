package de.hpi.accidit.orm.cursor;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public interface OFutureCursor<V> extends OFuture<ResultCursor<V>>, AutoCloseable {

	@Override
	public <R> OFuture<R> onComplete(OFutureAction<? super OFuture<ResultCursor<V>>, R> action);
	
	public <R> OFuture<R> onCompleteC(OFutureAction<? super OFutureCursor<V>, R> action);
	
}

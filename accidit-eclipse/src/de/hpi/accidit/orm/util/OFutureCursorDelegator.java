package de.hpi.accidit.orm.util;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.cursor.OFutureCursor;
import de.hpi.accidit.orm.cursor.ResultCursor;

public class OFutureCursorDelegator<V> extends OFutureDelegator<ResultCursor<V>> implements OFutureCursor<V> {

	public OFutureCursorDelegator(OFuture<ResultCursor<V>> delegatee) {
		super(delegatee);
	}
	
	@Override
	public <R> OFuture<R> onCompleteC(final OFutureAction<? super OFutureCursor<V>, R> action) {
		return onComplete(new OFutureAction<Object, R>() {
			@Override
			public R call(Object param) throws Exception {
				return action.call(OFutureCursorDelegator.this);
			}
		});
	}
	
	@Override
	public void close() throws Exception {
		onComplete(CLOSE_ACTION);
	}
	
	protected static OFutureAction<OFuture<? extends AutoCloseable>, Void> CLOSE_ACTION = new OFutureAction<OFuture<? extends AutoCloseable>, Void>() {
		@Override
		public Void call(OFuture<? extends AutoCloseable> f) throws Exception {
			if (f.hasResult()) f.getResult().close();
			return null;
		}
	};
	
}

package de.hpi.accidit.orm.util;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public abstract class OLazyFuture<V> extends OFutureDelegator<V> {

	private OFuture<V> value;
	
	public OLazyFuture() {
		super(null);
	}
	
	@Override
	protected OFuture<V> getDelegatee() {
		if (value == null) {
			initValue();
		}
		return value;
	}

	private synchronized void initValue() {
		if (value == null) {
			try {
				value = (OFuture) initialize();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			value.onComplete(new OFutureAction<OFuture<V>, Void>() {
				@Override
				public Void call(OFuture<V> param) throws Exception {
					if (param.hasResult()) {
						value = new OFinalFuture<>(param.getResult());
					} else {
						value = new OFinalFuture<>(param.getException());
					}
					return null;
				}
			});
		}
	}
	
	protected abstract OFuture<? extends V> initialize() throws Exception;

}

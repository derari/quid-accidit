package de.hpi.accidit.orm.util;

public class OFinalFuture<V> extends OFutureBase<V> {

	public OFinalFuture(V value) {
		super(null);
		setValue(value);
	}

	public OFinalFuture(Throwable error) {
		super(null);
		setException(error);
	}
	
}

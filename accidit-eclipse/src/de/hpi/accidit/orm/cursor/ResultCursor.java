package de.hpi.accidit.orm.cursor;

import java.util.Iterator;

public interface ResultCursor<V> extends Iterable<V>, Iterator<V>, AutoCloseable {

	V getValue();
	
	@Override
	boolean hasNext();
	
	@Override
	V next();
	
	V getFixCopy();
	
	@Override
	Iterator<V> iterator();
	
}

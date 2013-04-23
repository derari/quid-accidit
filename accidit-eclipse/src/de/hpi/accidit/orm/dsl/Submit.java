package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.cursor.OFutureCursor;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.util.OFutureCursorDelegator;
import de.hpi.accidit.orm.util.OFutureDelegator;

public class Submit<Type> extends OFutureDelegator<ResultSet> {

	private final Mapping<Type> mapping;
	private final ValueAdapter<Type> va;
	
	public Submit(OFuture<ResultSet> delegatee, Mapping<Type> mapping, ValueAdapter<Type> va) {
		super(delegatee);
		this.mapping = mapping;
		this.va = va;
	}

	public <V> OFuture<V> as(final ResultBuilder<V, Type> ra) throws SQLException {
		return onComplete(new OFutureAction<OFuture<ResultSet>, V>() {
			@Override
			public V call(OFuture<ResultSet> resultSet) throws Exception {
				return ra.adapt(resultSet.get(), mapping.getEntityFactory(), va);
			}
		});
	}
	
	public OFuture<Type[]> asArray() throws SQLException {
		return as(mapping.asArray());
	}
	
	public OFuture<List<Type>> asList() throws SQLException {
		return as(mapping.asList());
	}
	
	public OFutureCursor<Type> asCursor() throws SQLException {
		return new OFutureCursorDelegator<>(as(mapping.asCursor()));
	}
	
	public OFuture<Type> getSingle() throws SQLException {
		return as(mapping.getSingle());
	}
	
	public OFuture<Type> getFirst() throws SQLException {
		return as(mapping.getFirst());
	}
	
}

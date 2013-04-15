package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.cursor.OFutureCursor;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultAdapter;
import de.hpi.accidit.orm.util.OFutureCursorDelegator;
import de.hpi.accidit.orm.util.OFutureDelegator;

public class Submit<Type> extends OFutureDelegator<ResultSet> {

	private final List<String> selectedFields;
	private final Mapping<Type> mapping;
	
	public Submit(OFuture<ResultSet> delegatee, List<String> selectedFields, Mapping<Type> mapping) {
		super(delegatee);
		this.selectedFields = selectedFields;
		this.mapping = mapping;
	}

	public <V> OFuture<V> as(final ResultAdapter<V> ra) throws SQLException {
		return onComplete(new OFutureAction<OFuture<ResultSet>, V>() {
			@Override
			public V call(OFuture<ResultSet> resultSet) throws Exception {
				return ra.adapt(resultSet.get(), selectedFields);
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
	
}

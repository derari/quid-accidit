package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;

public class Run<Type> {

	private final ResultSet rs;
	private final Mapping<Type> mapping;
	private final ValueAdapter<Type> va;

	public Run(ResultSet rs, Mapping<Type> mapping, ValueAdapter<Type> va) {
		this.rs = rs;
		this.mapping = mapping;
		this.va = va;
	}

	public ResultSet result() {
		return rs;
	}

	public <V> V as(final ResultBuilder<V, Type> ra) throws SQLException {
		return ra.adapt(rs, mapping.getEntityFactory(), va);
	}
	
	public Type[] asArray() throws SQLException {
		return as(mapping.asArray());
	}
	
	public List<Type> asList() throws SQLException {
		return as(mapping.asList());
	}
	
	public ResultCursor<Type> asCursor() throws SQLException {
		return as(mapping.asCursor());
	}
	
	public Type getSingle() throws SQLException {
		return as(mapping.getSingle());
	}
	
	public Type getFirst() throws SQLException {
		return as(mapping.getFirst());
	}
	
}

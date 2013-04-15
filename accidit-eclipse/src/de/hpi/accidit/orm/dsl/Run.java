package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultAdapter;

public class Run<Type> {

	private final ResultSet rs;
	private final List<String> selectedFields;
	private final Mapping<Type> mapping;
	
	public Run(ResultSet rs, List<String> selectedFields, Mapping<Type> mapping) {
		this.rs = rs;
		this.selectedFields = selectedFields;
		this.mapping = mapping;
	}
	
	public ResultSet result() {
		return rs;
	}

	public <V> V as(final ResultAdapter<V> ra) throws SQLException {
		return ra.adapt(rs, selectedFields);
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
	
}

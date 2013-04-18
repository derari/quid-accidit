package de.hpi.accidit.orm.map;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;

public abstract class ValueAdapterBase<Entity> implements ValueAdapter<Entity> {

	protected int[] getFieldIndices(ResultSet rs, String[] fields) throws SQLException {
		final int [] indices = new int[fields.length];
		for (int i = 0; i < indices.length; i++) {
			indices[i] = rs.findColumn(fields[i]);
		}
		return indices;
	}
	
	protected int[] getFieldIndices(ResultSet rs, List<String> fields) throws SQLException {
		final int [] indices = new int[fields.size()];
		for (int i = 0; i < indices.length; i++) {
			indices[i] = rs.findColumn(fields.get(i));
		}
		return indices;
	}
	
}

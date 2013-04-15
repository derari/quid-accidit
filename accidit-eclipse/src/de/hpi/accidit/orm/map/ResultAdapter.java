package de.hpi.accidit.orm.map;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultAdapter<T> {

	T adapt(ResultSet rs, List<String> fields) throws SQLException;
	
}

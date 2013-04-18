package de.hpi.accidit.orm.map;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.cursor.ResultCursor;

public interface ResultBuilder<R, E> {

	R adapt(ResultSet rs, EntityFactory<E> ef, ValueAdapter<E> va) throws SQLException;
	
	static interface ValueAdapter<E> {
		
		// Call Pattern:
		// ( initialize (apply* complete)* )?

		void initialize(ResultSet rs) throws SQLException;
		
		void apply(E entity) throws SQLException;
		
		void complete() throws SQLException;
		
	}

	static interface EntityFactory<E> {
		
		E newEntity();
		
		E newCursorValue(ResultCursor<E> rc);
		
		E copy(E e);
		
	}
	
}

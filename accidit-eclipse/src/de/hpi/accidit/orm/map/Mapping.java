package de.hpi.accidit.orm.map;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.cursor.ResultCursorBase;


public abstract class Mapping<Type> {

	private final Class<Type> recordClass;
	private ResultAdapter<List<Type>> listResult = null;
	private ResultAdapter<Type[]> arrayResult = null;
	private ResultAdapter<ResultCursor<Type>> cursorResult = null;
	
	public Mapping(Class<Type> recordClass) {
		this.recordClass = recordClass;
	}
	
	protected abstract Type newRecord();
	
	protected abstract Type newCursorValue(ResultCursor<Type> cursor);
	
	protected abstract void setField(Type record, ResultSet rs, int i, String field) throws SQLException;
	
	protected void setFields(Type record, ResultSet rs, List<String> fields) throws SQLException {
		final int len = fields.size();
		for (int i = 0; i < len; i++) {
			setField(record, rs, i+1, fields.get(i));
		}
	}
	
	public ResultAdapter<List<Type>> asList() {
		if (listResult == null) {
			listResult = new ListResult();
		}
		return listResult;
	}
	
	public ResultAdapter<Type[]> asArray() {
		if (arrayResult == null) {
			arrayResult = new ArrayResult();
		}
		return arrayResult;
	}
	
	public ResultAdapter<ResultCursor<Type>> asCursor() {
		if (cursorResult == null) {
			cursorResult = new CursorResult();
		}
		return cursorResult;
	}
	
	protected class ArrayResult implements ResultAdapter<Type[]> {

		@Override
		@SuppressWarnings("unchecked")
		public Type[] adapt(ResultSet rs, List<String> fields) throws SQLException {
			List<Type> result = asList().adapt(rs, fields);
			return result.toArray((Type[]) Array.newInstance(recordClass, result.size()));
		}
		
	}
	
	protected class ListResult implements ResultAdapter<List<Type>> {

		@Override
		public List<Type> adapt(ResultSet rs, List<String> fields) throws SQLException {
			final List<Type> result = new ArrayList<>();
			while (rs.next()) {
				final Type record = newRecord();
				setFields(record, rs, fields);
				result.add(record);
			}
			return result;
		}
		
	}
	
	protected class CursorResult implements ResultAdapter<ResultCursor<Type>> {

		@Override
		public ResultCursor<Type> adapt(ResultSet rs, List<String> fields) throws SQLException {
			return new MappedResultCursor(rs, fields);
		}
		
	}
	
	protected class MappedResultCursor extends ResultCursorBase<Type> {

		private final ResultSet rs;
		private final List<String> fields;
		
		private boolean nextIsExpected = true;
		private boolean isAtNext = false;
		
		public MappedResultCursor(ResultSet rs, List<String> fields) {
			this.rs = rs;
			this.fields = fields;
			setCursorValue(newCursorValue(this));
		}

		@Override
		public boolean hasNext() {
			try {
				if (nextIsExpected) {
					if (!isAtNext) {
						nextIsExpected = rs.next();
						isAtNext = nextIsExpected;
					}
				}
				return nextIsExpected;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Type getFixCopy() {
			try {
				if (isAtNext) {
					rs.previous();
				}
				Type record = newRecord();
				setFields(record, rs, fields);
				return record;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected void makeNext() {
			if (!isAtNext) {
				hasNext();
				if (!isAtNext) {
					throw new IllegalStateException("No next element");
				}
			}
			isAtNext = false;
			try {
				setFields(cursor, rs, fields);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void close() throws Exception {
			rs.close();
		}
	}
	
}

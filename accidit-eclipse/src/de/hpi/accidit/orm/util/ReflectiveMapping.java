package de.hpi.accidit.orm.util;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.map.Mapping;

public class ReflectiveMapping<Type> extends Mapping<Type> {
	
	private final Constructor<Type> newRecord;
	private final Constructor<Type> newCursor;

	public ReflectiveMapping(Class<Type> recordClass) {
		this(recordClass, null);
	}
	
	public ReflectiveMapping(Class<Type> recordClass, Class<? extends Type> cursorClass) {
		super(recordClass);
		try {
			newRecord = recordClass.getConstructor();
			if (cursorClass != null) {
				newCursor = recordClass.getConstructor(ResultCursor.class);
			} else {
				newCursor = null;
			}
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Type newRecord() {
		try {
			return newRecord.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Type newCursorValue(ResultCursor<Type> cursor) {
		try {
			if (newCursor != null) {
				return newCursor.newInstance(cursor);
			}
			return newRecord.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void setField(Type record, String field, ResultSet rs, int i) throws SQLException {
		injectField(record, field, rs, i);
	}
	
	@Override
	public void setField(Type record, String field, Object value) throws SQLException {
		injectField(record, field, value);
	}
}

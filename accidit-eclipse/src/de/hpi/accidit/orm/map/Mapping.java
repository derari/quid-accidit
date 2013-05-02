package de.hpi.accidit.orm.map;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.map.ResultBuilder.EntityFactory;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;


public abstract class Mapping<Type> {

	private final Class<Type> recordClass;
	private ResultBuilder<Type[], Type> arrayResult = null;
	
	private final EntityFactory<Type> ef = new EntityFactory<Type>() {
		@Override
		public Type newEntity() {
			return Mapping.this.newRecord();
		}
		@Override
		public Type newCursorValue(ResultCursor<Type> rc) {
			return Mapping.this.newCursorValue(rc);
		}
		@Override
		public Type copy(Type e) {
			return Mapping.this.copy(e);
		}
	};
	
	public Mapping(Class<Type> recordClass) {
		this.recordClass = recordClass;
	}
	
	protected Type newRecord() {
		throw new UnsupportedOperationException("Records not supported");
	}
	
	protected Type newCursorValue(ResultCursor<Type> cursor) {
		throw new UnsupportedOperationException("Cursor not supported");
	}
	
	protected Type copy(Type e) {
		throw new UnsupportedOperationException("Copy not supported");
	}
	
	protected void setField(Type record, String field, ResultSet rs, int i) throws SQLException {
		throw new IllegalArgumentException(
				"Cannot set field " + field + " of " + recordClass.getSimpleName());
	}
	
	protected void injectField(Type record, String field, ResultSet rs, int i) throws SQLException {
		injectField(record, field, rs.getObject(i));
	}
	
	protected void injectField(Type record, String field, Object value) throws SQLException {
		try {
			Field f = null;
			Class<?> clazz = record.getClass();
			while (clazz != null && f == null) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field df: fields) {
					if (df.getName().equals(field)) {
						f = df;
						break;
					}
				}
				clazz = clazz.getSuperclass();
			}
			if (f == null) {
				throw new NoSuchFieldException(field);
			}
			f.setAccessible(true);
			f.set(record, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setField(Type record, String field, Object value) throws SQLException {
		throw new IllegalArgumentException(
				"Cannot set field " + field + " of " + recordClass.getSimpleName());
	}
	
	protected void setFields(Type record, ResultSet rs, List<String> fields) throws SQLException {
		final int len = fields.size();
		for (int i = 0; i < len; i++) {
			setField(record, fields.get(i), rs, i+1);
		}
	}
	
	public ValueAdapter<Type> getValueAdapter(final List<String> fields) {
		return new ValueAdapterBase<Type>() {
			private ResultSet rs;
			private int[] fieldIndices;
			
			@Override
			public void initialize(ResultSet rs) throws SQLException {
				this.rs = rs;
				this.fieldIndices = getFieldIndices(rs, fields);
			}

			@Override
			public void apply(Type entity) throws SQLException {
				int len = fieldIndices.length;
				for (int i = 0; i < len; i++) {
					setField(entity, fields.get(i), rs, fieldIndices[i]);
				}
			}

			@Override
			public void complete() throws SQLException { }
		};
	}
	
	public EntityFactory<Type> getEntityFactory() {
		return ef;
	}
	
	public ResultBuilder<List<Type>, Type> asList() {
		return ResultBuilders.getListResult();
	}
	
	public ResultBuilder<Type[], Type> asArray() {
		if (arrayResult == null) {
			arrayResult = ResultBuilders.getArrayResult(recordClass);
		}
		return arrayResult;
	}
	
	public ResultBuilder<ResultCursor<Type>, Type> asCursor() {
		return ResultBuilders.getCursorResult();
	}
	
	public ResultBuilder<Type, Type> getSingle() {
		return ResultBuilders.getSingleResult();
	}
	
	public ResultBuilder<Type, Type> getFirst() {
		return ResultBuilders.getFirstResult();
	}
	
}

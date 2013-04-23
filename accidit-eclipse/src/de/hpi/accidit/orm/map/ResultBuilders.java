package de.hpi.accidit.orm.map;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.cursor.ResultCursorBase;
import de.hpi.accidit.orm.map.ResultBuilder.EntityFactory;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;

public class ResultBuilders {
	
	protected ResultBuilders() {}
	
	@SuppressWarnings("rawtypes")
	private static ListResult LIST_RESULT = null;
	
	@SuppressWarnings("unchecked")
	public static <Entity> ListResult<Entity> getListResult() {
		if (LIST_RESULT == null) {
			LIST_RESULT = new ListResult<>();
		}
		return LIST_RESULT;
	}
	
	public static <Entity> ListResult<Entity> getListResult(Class<Entity> clazz) {
		return getListResult();
	}

	public static class ListResult<Entity> implements ResultBuilder<List<Entity>, Entity> {

		@Override
		public List<Entity> adapt(ResultSet rs, EntityFactory<Entity> ef, ValueAdapter<Entity> va) throws SQLException {
			final List<Entity> result = new ArrayList<>();
			va.initialize(rs);
			while (rs.next()) {
				final Entity record = ef.newEntity();
				va.apply(record);
				result.add(record);
			}
			va.complete();
			rs.close();
			return result;
		}
		
	}
	
	public static <Entity> ArrayResult<Entity> getArrayResult(Class<Entity> clazz) {
		return new ArrayResult<>(clazz);
	}

	public static class ArrayResult<Entity> implements ResultBuilder<Entity[], Entity> {
		
		private final Class<Entity> entityClass;
		
		public ArrayResult(Class<Entity> entityClass) {
			this.entityClass = entityClass;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Entity[] adapt(ResultSet rs, EntityFactory<Entity> ef, ValueAdapter<Entity> va) throws SQLException {
			List<Entity> result = getListResult(entityClass).adapt(rs, ef, va);
			return result.toArray((Entity[]) Array.newInstance(entityClass, result.size()));
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	private static CursorResult CURSOR_RESULT = null;
	
	@SuppressWarnings("unchecked")
	public static <Entity> CursorResult<Entity> getCursorResult() {
		if (CURSOR_RESULT == null) {
			CURSOR_RESULT = new CursorResult<>();
		}
		return CURSOR_RESULT;
	}
	
	public static <Entity> CursorResult<Entity> getCursorResult(Class<Entity> clazz) {
		return getCursorResult();
	}
	
	public static class CursorResult<Entity> implements ResultBuilder<ResultCursor<Entity>, Entity> {

		@Override
		public ResultCursor<Entity> adapt(ResultSet rs, EntityFactory<Entity> ef, ValueAdapter<Entity> va) throws SQLException {
			return new MappedResultCursor<>(rs, ef, va);
		}
		
	}
	
	protected static class MappedResultCursor<Entity> extends ResultCursorBase<Entity> {

		private final ResultSet rs;
		private final EntityFactory<Entity> ef;
		private final ValueAdapter<Entity> va;
		
		private boolean nextIsExpected = true;
		private boolean isAtNext = false;
		
		public MappedResultCursor(ResultSet rs, EntityFactory<Entity> ef, ValueAdapter<Entity> va) throws SQLException {
			this.rs = rs;
			this.ef = ef;
			this.va = va;
			va.initialize(rs);
			setCursorValue(ef.newCursorValue(this));
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
		public Entity getFixCopy() {
			return ef.copy(cursor);
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
				va.apply(cursor);
				va.complete();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void close() throws Exception {
			rs.close();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static SingleResult SINGLE_RESULT = null;
	
	@SuppressWarnings("unchecked")
	public static <Entity> SingleResult<Entity> getSingleResult() {
		if (SINGLE_RESULT == null) {
			SINGLE_RESULT = new SingleResult<>(true);
		}
		return SINGLE_RESULT;
	}
	
	public static <Entity> SingleResult<Entity> getSingleResult(Class<Entity> clazz) {
		return getSingleResult();
	}

	@SuppressWarnings("rawtypes")
	private static SingleResult FIRST_RESULT = null;
	
	@SuppressWarnings("unchecked")
	public static <Entity> SingleResult<Entity> getFirstResult() {
		if (FIRST_RESULT == null) {
			FIRST_RESULT = new SingleResult<>(false);
		}
		return FIRST_RESULT;
	}
	
	public static <Entity> SingleResult<Entity> getFirstResult(Class<Entity> clazz) {
		return getSingleResult();
	}
	
	public static class SingleResult<Entity> implements ResultBuilder<Entity, Entity> {
		
		private final boolean forceSingle;
		
		public SingleResult(boolean forceSingle) {
			this.forceSingle = forceSingle;
		}

		@Override
		public Entity adapt(ResultSet rs, EntityFactory<Entity> ef, ValueAdapter<Entity> va) throws SQLException {
			if (!rs.next()) return null;
			va.initialize(rs);
			final Entity record = ef.newEntity();
			va.apply(record);
			if (forceSingle && rs.next()) throw new IllegalArgumentException("Result not unique");
			va.complete();
			rs.close();
			return record;
		}
	}
	
}

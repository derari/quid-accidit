package de.hpi.accidit.orm.cursor;

public class Cursor {
	
	public static boolean hasNext(ResultCursor<?> cursor) {
		return cursor.hasNext();
	}
	
	public static boolean hasNext(CursorValue cursor) {
		return hasNext(cursor.getResultCursor());
	}
	
	public static boolean hasNext(Object cursor) {
		return hasNext((CursorValue) cursor);
	}
	
	public static <V> V next(ResultCursor<V> cursor) {
		return cursor.next();
	}
	
	@SuppressWarnings("unchecked")
	public static <V> V next(CursorValue cursor) {
		return (V) next(cursor.getResultCursor());
	}

	public static <V> V next(V cursor) {
		return next((CursorValue) cursor);
	}
	
	public static <V> V fixedCopy(ResultCursor<V> cursor) {
		return cursor.getFixCopy();
	}
	
	@SuppressWarnings("unchecked")
	public static <V> V fixedCopy(CursorValue cursor) {
		return (V) fixedCopy(cursor.getResultCursor());
	}

	public static <V> V fixedCopy(V cursor) {
		return fixedCopy((CursorValue) cursor);
	}
	
}

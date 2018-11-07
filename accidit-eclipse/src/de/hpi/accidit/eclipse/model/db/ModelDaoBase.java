package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.graph.TypeBuilder;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.template.TemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.SqlEntitySet;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.ModelBase;

public abstract class ModelDaoBase<Entity extends ModelBase, This extends ModelDaoBase<Entity, This>> extends SqlEntitySet<Entity, This> {
	
	protected static void init(TypeBuilder<?,?,?> t) {
		t.as("db").optional("-no-column-").field("db");
	}
	
	public ModelDaoBase(MiConnection cnn, TemplateLayer<MappedQuery<Entity, SelectQuery>> queryLayer) {
		super(cnn, queryLayer);
	}

	protected ModelDaoBase(ModelDaoBase<Entity, This> source) {
		super(source);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		TraceDB db = DatabaseConnector.getTraceDB();
		setUp(MappingKey.SET, "db", db);
	}
	
	protected This where(String sql, Object... args) {
		return sql(s -> s.where(sql, args));
	}
	
//	protected static <E> EntityInitializer<E> injectField(String field, Object value) {
//		return injectField(Object.class, field, value);
//	}
//	
//	protected static <E> EntityInitializer<E> injectField(Class<?> filter, String field, Object value) {
//		return e -> {
//			if (!filter.isInstance(e)) return;
//			Class<?> c = e.getClass();
//			Field f = null;
//			while (f == null && c != null) {
//				try {
//					f = c.getDeclaredField(field);
//				} catch (NoSuchFieldException ex) {
//					c = c.getSuperclass();
//				}
//			}
//			if (f == null) {
//				throw new IllegalArgumentException(e.getClass().getSimpleName() + "." + field);
//			}
//			try {
//				f.setAccessible(true);
//				f.set(e, value);
//			} catch (ReflectiveOperationException ex) {
//				throw new MiException(ex);
//			}
//		};
//	}
}

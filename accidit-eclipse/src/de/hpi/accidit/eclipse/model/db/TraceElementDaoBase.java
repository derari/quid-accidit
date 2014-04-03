package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.Impl;
import org.cthul.miro.at.MiQuery;
import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Where;
import org.cthul.miro.map.MappedInternalQueryBuilder;
import org.cthul.miro.result.QueryWithResult;
import org.cthul.miro.result.Results;
import org.cthul.miro.util.CfgSetField;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.TraceElement;

public class TraceElementDaoBase extends ModelDaoBase {
	
	@MiQuery(
	attributes = "e.`testId`, e.`line`, e.`step`",
	where = @Where(key="callStep_EQ", value="e.`callStep` = ?")
	)
	public static interface QueryAttributes extends ModelDaoBase.Query {
	}
	
	public static interface Query<E extends TraceElement, This extends Query<E, This>> extends QueryWithResult<Results<E>>, ModelDaoBase.Query, QueryAttributes {
		
		This where();
		
		@Impl(QueryImpl.class)
		This inInvocation(Invocation inv);
		
		This orderBy();
		
		@OrderBy("step")
		This step_asc(); 
	}
	
	static class QueryImpl {
		
		public static void inInvocation(MappedInternalQueryBuilder query, Invocation inv) {
			query.configure(CfgSetField.newInstance("parent", inv));
			query.put("testId =", inv.getTestId());
			query.put("callStep_EQ", inv.getStep());
		}
	}
//	
//	protected static class InitParent 
//					implements EntityInitializer<TraceElement> {
//		
//		private final Invocation parent;
//		
//		public InitParent(Invocation parent) {
//			this.parent = parent;
//		}
//
//		@Override
//		public void apply(TraceElement entity) throws SQLException {
//			entity.parent = parent;
//		}
//
//		@Override
//		public void complete() throws SQLException { }
//
//		@Override
//		public void close() throws SQLException { }
//	}
}

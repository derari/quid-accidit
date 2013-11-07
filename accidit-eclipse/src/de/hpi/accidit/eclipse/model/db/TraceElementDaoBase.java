package de.hpi.accidit.eclipse.model.db;

import java.sql.SQLException;

import org.cthul.miro.at.AnnotatedMappedStatement;
import org.cthul.miro.at.AnnotatedQueryHandler;
import org.cthul.miro.at.Impl;
import org.cthul.miro.at.MiQuery;
import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Put;
import org.cthul.miro.at.Select;
import org.cthul.miro.at.Where;
import org.cthul.miro.result.EntityInitializer;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.TraceElement;

public class TraceElementDaoBase extends ModelDaoBase {
	
	@MiQuery(
	select = @Select("e.`line`, e.`step`"),
	where = {@Where(key="testId_EQ", value="e.`testId` = ?"),
			 @Where(key="callStep_EQ", value="e.`callStep` = ?")},
	orderBy = @OrderBy(key="asc_step", value="e.`step`"))
	public static interface QueryAttributes extends ModelDaoBase.Query {
	}
	
	public static interface Query<E extends TraceElement, This extends Query<E, This>> extends AnnotatedMappedStatement<E>, ModelDaoBase.Query, QueryAttributes {
		
		This where();
		
		@Impl(QueryImpl.class)
		This inInvocation(Invocation inv);
		
		This orderBy();
		
		@Put("asc_step")
		This step_asc(); 
	}
	
	static class QueryImpl {
		
		public static void inInvocation(AnnotatedQueryHandler<? extends TraceElement> query, Invocation inv) {
			query.configure(new InitParent(inv));
			query.put("testId_EQ", inv.testId);
			query.put("callStep_EQ", inv.step);
		}
	}
	
	protected static class InitParent 
					implements EntityInitializer<TraceElement> {
		
		private final Invocation parent;
		
		public InitParent(Invocation parent) {
			this.parent = parent;
		}

		@Override
		public void apply(TraceElement entity) throws SQLException {
			entity.parent = parent;
		}

		@Override
		public void complete() throws SQLException { }

		@Override
		public void close() throws SQLException { }
	}
}

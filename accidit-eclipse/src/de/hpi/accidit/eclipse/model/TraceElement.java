package de.hpi.accidit.eclipse.model;

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


public class TraceElement extends ModelBase implements Comparable<TraceElement> {

	public Invocation parent;
	public int line;
	public long step;
	
	@Override
	public int compareTo(TraceElement o) {
		int c = Long.compare(step, o.step);
		if (c != 0) return c;
		return deepCompare(o);
	}

	protected int deepCompare(TraceElement o) {
		return 0;
	}
	
	public String getImage() {
		return "";
	}
	
	public String getShortText() {
		return "";
	}

	@MiQuery(
	select = @Select("e.`line`, e.`step`"),
	where = {@Where(key="testId_EQ", value="e.`testId` = ?"),
			 @Where(key="callStep_EQ", value="e.`callStep` = ?")},
	orderBy = @OrderBy(key="asc_step", value="e.`step`"))
	public static interface Query<E extends TraceElement, This extends Query<E, This>> extends AnnotatedMappedStatement<E> {
		
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

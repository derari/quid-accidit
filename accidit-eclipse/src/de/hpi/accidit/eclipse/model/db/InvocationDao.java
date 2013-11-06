package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedQueryHandler;
import org.cthul.miro.at.AnnotatedQueryTemplate;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.Impl;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.Invocation;

public class InvocationDao extends TraceElementDaoBase {
	
	private static final Mapping<Invocation> MAPPING = new ReflectiveMapping<Invocation>(Invocation.class) {
		protected void injectField(Invocation record, String field, java.sql.ResultSet rs, int i) throws java.sql.SQLException {
			if (field.equals("returned")) {
				injectField(record, field, rs.getInt(i) == 1);
				return;
			}
			super.injectField(record, field, rs, i);
		};
	};
	
	private static final AnnotatedQueryTemplate<Invocation> TEMPLATE = new AnnotatedQueryTemplate<Invocation>(){{
		select("e.`testId`, e.`depth`", 
			   "x.`step` AS `exitStep`, x.`returned`, x.`line` AS `exitLine`",
			   "m.`name` AS `method`, t.`name` AS `type`");
		from("`CallTrace` e");
		join("LEFT OUTER JOIN `ExitTrace` x ON e.`testId` = x.`testId` AND e.`step` = x.`callStep`");
		join("`Method` m ON e.`methodId` = m.`id`");
		using("m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
		
		where("test_EQ", "e.`testId` = ?",
			  "depth_EQ", "e.`depth` = ?",
			  "step_BETWEEN", "e.`step` > ? AND e.`step` < ?");
	}};
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING, TEMPLATE);
	
	@Impl(QueryImpl.class)
	public static interface Query extends TraceElementDaoBase.Query<Invocation, Query> {
		
		Query inInvocation(Invocation inv);
		
		Query rootOfTest(int i);
	}
	
	static class QueryImpl {
		
		public static void inInvocation(AnnotatedQueryHandler<Invocation> query, Invocation inv) {
			query.configure(new InitParent(inv));
			query.put("testId_EQ", inv.testId);
			query.put("depth_EQ", inv.depth+1);
			query.put("step_BETWEEN", inv.step, inv.exitStep);
			query.put("asc_step");
		}
		
		public static void rootOfTest(AnnotatedQueryHandler<Invocation> query, int testId) {
			query.put("testId_EQ", testId);
			query.put("depth_EQ", 0);
		}
	}

}

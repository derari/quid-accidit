package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.Impl;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedInternalQueryBuilder;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.util.CfgSetField;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

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
	
	private static final MappedTemplateProvider<Invocation> TEMPLATE = new MappedDataQueryTemplateProvider<Invocation>(MAPPING){{
		attributes("e.`testId`, e.`depth`", 
			   "x.`step` AS `exitStep`, x.`returned`, x.`line` AS `exitLine`",
			   "m.`name` AS `method`, t.`name` AS `type`");
		table("`CallTrace` e");
		join("LEFT OUTER JOIN `ExitTrace` x ON e.`testId` = x.`testId` AND e.`step` = x.`callStep`");
		join("`Method` m ON e.`methodId` = m.`id`");
		using("m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
		
		where("step_BETWEEN", "e.`step` > ? AND e.`step` < ?");
	}};
	
	public static final ViewR<Query> VIEW = Views.build(TEMPLATE).r(Query.class).build();
	
	@Impl(QueryImpl.class)
	public static interface Query extends TraceElementDaoBase.Query<Invocation, Query> {
		
		Query inInvocation(Invocation inv);
		
		Query rootOfTest(int i);
	}
	
	static class QueryImpl {
		
		public static void inInvocation(MappedInternalQueryBuilder query, Invocation inv) {
			query.configure(CfgSetField.newInstance("parent", inv));
			query.put("testId =", inv.getTestId());
			query.put("depth =", inv.depth+1);
			query.put("step_BETWEEN", inv.getStep(), inv.exitStep);
			query.put("orderBy-step");
		}
		
		public static void rootOfTest(MappedInternalQueryBuilder query, int testId) {
			query.put("testId =", testId);
			query.put("depth =", 0);
		}
	}

}

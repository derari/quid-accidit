package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.Impl;
import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Put;
import org.cthul.miro.at.Require;
import org.cthul.miro.at.Where;
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
			if (field.equals("exitLine")) {
				injectField(record, field, rs.getInt(i));
				return;
			}
			super.injectField(record, field, rs, i);
		};
	};
	
	private static final MappedTemplateProvider<Invocation> TEMPLATE = new MappedDataQueryTemplateProvider<Invocation>(MAPPING){{
		attributes("e.`testId`, e.`depth`, e.`step`, e.`exitStep`",
				"x.`line` AS `exitLine`", 
				"m.`name` AS `method`, t.`name` AS `type`");
		optionalAttributes("m.`signature` AS `signature`, m.`id` AS `methodId`");
		internalSelect("e.`line` AS `callLine`");
		using("x")
			.select("COALESCE(x.`returned`, 0) AS `returned`, COALESCE(x.`line`, -1) AS `exitLine`");
		internalSelect("e.`parentStep`");
		table("`CallTrace` e");
		join("LEFT OUTER JOIN `ExitTrace` x ON e.`testId` = x.`testId` AND e.`exitStep` = x.`step`");
		join("`Method` m ON e.`methodId` = m.`id`");
		using("m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
		
		where("step_BETWEEN", "e.`step` > ? AND e.`step` < ?");
		where("exit_GT", "e.`exitStep` > ?");
	}};
	
	public static final ViewR<Query> VIEW = Views.build(TEMPLATE).r(Query.class).build();
	
	@Impl(QueryImpl.class)
	public static interface Query extends TraceElementDaoBase.Query<Invocation, Query> {
		
		Query inInvocation(Invocation inv);
		
		Query parentOf(Invocation inv);
		
		Query rootOfTest(int i);
		
		@Put("testId =")
		Query inTest(int i);
		
		@Put("parentStep =")
		Query inCall(long parentStep);
		
		@Put("step =")
		Query atStep(long step);
		
		@Put("methodId =")
		Query ofMethod(int id);
		
		@Require({"m", "t"})
		@Where("t.`name` = ? AND m.`name` = ? AND m.`signature` = ?")
		Query ofMethod(String clazz, String name, String signature);
		
		@Put("exitStep =")
		Query atExitStep(long step);
		
		@Where("e.`exitStep` < ?")
		@Put(value="orderBy-exitStep DESC", mapArgs={})
		Query beforeExit(long exitStep);
		
		@Put("callLine =")
		Query callInLine(int line);
	}
	
	static class QueryImpl {
		
		public static void inInvocation(MappedInternalQueryBuilder query, Invocation inv) {
			query.configure(CfgSetField.newInstance("parent", inv));
			query.put("testId =", inv.getTestId());
			query.put("depth =", inv.depth+1);
			query.put("parentStep =", inv.getStep());
			//query.put("step_BETWEEN", inv.getStep(), inv.exitStep);
			query.put("orderBy-step");
		}
		
		public static void parentOf(MappedInternalQueryBuilder query, Invocation inv) {
			query.put("testId =", inv.getTestId());
			query.put("depth =", inv.depth-1);
			query.put("step <", inv.getStep());
			query.put("exit_GT", inv.getStep());
			//query.put("step_BETWEEN", inv.getStep(), inv.exitStep);
//			query.put("orderBy-step");
		}
		
		public static void rootOfTest(MappedInternalQueryBuilder query, int testId) {
			query.put("testId =", testId);
			query.put("depth =", 0);
		}
	}

}

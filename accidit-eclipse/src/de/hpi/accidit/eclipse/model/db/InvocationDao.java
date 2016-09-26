package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.Invocation;

public class InvocationDao extends TraceElementDaoBase<Invocation, InvocationDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(Invocation.class);
		TraceElementDaoBase.init(sql);
		sql
			.attributes("e.`testId`, e.`depth`, e.`step`, e.`exitStep`, e.`thisId`")
//			.attributes("x.`line` AS `exitLine`")
			.attributes("m.`name` AS `method`, m.`id` AS `methodId`, m.`signature`, t.`name` AS `type`")
			.using("e").attribute("COALESCE(e.`parentStep`,-1) AS `callStep`")
			.using("x").attributes("COALESCE(x.`returned`, 0) AS `returned`, COALESCE(x.`line`, -1) AS `exitLine`")
			.from("`CallTrace` e")
			.join("LEFT `ExitTrace` x ON e.`testId` = x.`testId` AND e.`exitStep` = x.`step`")
			.join("`Method` m ON e.`methodId` = m.`id`")
			.using("m").join("`Type` t ON m.`declaringTypeId` = t.`id`");
	}
	
	protected InvocationDao(InvocationDao source) {
		super(source);
	}

	public InvocationDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Invocation.class));
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<Invocation, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
//		snippetLayer.setUp("callStep_EQ", (qry, a) -> 
//				qry.getStatement().where().sql("e.`parentStep` = ?", a));
//		snippetLayer.setUp("step_BETWEEN", (qry, a) -> 
//				qry.getStatement().where().sql("e.`step` > ? AND e.`step` < ?", a));
//		snippetLayer.setUp("exit_GT", (qry, a) -> 
//				qry.getStatement().where().sql("e.`exitStep` > ?", a));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, 
				"callStep", "depth", "thisId", 
				"exitStep","exitLine", "returned", 
				"method", "methodId", "signature", "type");
//		sql(sql -> sql
//				.select().sql("e.`testId`, e.`depth`, e.`step`, e.`exitStep`, e.`thisId`, " +
//						"x.`line` AS `exitLine`, " + 
//						"m.`name` AS `method`, t.`name` AS `type`, " +
//						"COALESCE(e.`parentStep`,-1) AS `callStep`, " +
//						"COALESCE(x.`returned`, 0) AS `returned`, COALESCE(x.`line`, -1) AS `exitLine`")
//				.leftJoin().id(schema, "ExitTrace").sql(" x ON e.`testId` = x.`testId` AND e.`exitStep` = x.`step`")
//				.join().id(schema, "Method").sql(" m ON e.`methodId` = m.`id`")
//				.join().id(schema, "Type").sql(" t ON m.`declaringTypeId` = t.`id`"));
	}
	
	public InvocationDao rootOfTest(int testId) {
		return setUp(MappingKey.PROPERTY_FILTER, "testId", testId, "depth", 0);
	}
	
	public InvocationDao inTest(int testId) {
		return setUp(MappingKey.PROPERTY_FILTER, "testId", testId);
	}
	
	public InvocationDao atStep(long step) {
		return setUp(MappingKey.PROPERTY_FILTER, "step", step);
	}
	
	public InvocationDao atExitStep(long exitStep) {
		return setUp(MappingKey.PROPERTY_FILTER, "exitStep", exitStep);
	}
	
	public InvocationDao beforeExit(long exitStep) {
		return sql(sql -> sql
				.where().sql("e.`exitStep` < ?", exitStep)
				.orderBy().sql("e.`exitStep` DESC"));
	}
	
	public InvocationDao inCall(long parentStep) {
		return setUp(MappingKey.PROPERTY_FILTER, "callStep", parentStep);
	}
	
	public InvocationDao ofMethod(int id) {
		return sql(sql -> sql
				.where().sql("e.`methodId` = ?", id));
	}
	
	public InvocationDao ofMethod(String name, String signature) {
		return compose(c -> c.require("m"))
				.sql(sql -> sql
						.where().sql("m.`name` = ? AND m.`signature` = ?", name, signature));
	}
	public InvocationDao parentOf(Invocation inv) {
		return setUp(MappingKey.PROPERTY_FILTER, "testId", inv.getTestId(), "step", inv.getCallStep());
	}
}

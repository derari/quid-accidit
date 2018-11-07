package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.Invocation;

public class InvocationDao extends TraceElementDaoBase<Invocation, InvocationDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(Invocation.class);
		TraceElementDaoBase.init(sql);
		sql.sql(
			"SELECT e.`testId`, e.`depth`, e.`step`, e.`exitStep`, e.`thisId`, " +
				"COALESCE(e.`parentStep`,-1) AS `callStep`, " +
				"COALESCE(x.`returned`, 0) AS `returned`, COALESCE(x.`line`, -1) AS `exitLine`, " +
				"COALESCE(x.`primType`, 0) AS `exitPrimType`, COALESCE(x.`valueId`, -1) AS `exitValueId`, " +
				"m.`name` AS `method`, m.`id` AS `methodId`, m.`signature`, t.`name` AS `type` " +
			"FROM `CallTrace` e " +
			"LEFT JOIN `ExitTrace` x ON e.`testId` = x.`testId` AND e.`exitStep` = x.`step` " +
			"JOIN `Method` m ON e.`methodId` = m.`id` " +
			"JOIN `Type` t ON m.`declaringTypeId` = t.`id`");
	}
	
	protected InvocationDao(InvocationDao source) {
		super(source);
	}

	public InvocationDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Invocation.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, 
				"callStep", "depth", "thisId", "returned", 
				"exitStep","exitLine", "exitPrimType", "exitValueId",
				"method", "methodId", "signature", "type");
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
				.where("e.`exitStep` < ?", exitStep)
				.orderBy("e.`exitStep` DESC"));
	}
	
	public InvocationDao inCall(long parentStep) {
		return setUp(MappingKey.PROPERTY_FILTER, "callStep", parentStep);
	}
	
	public InvocationDao ofMethod(int id) {
		return setUp(MappingKey.PROPERTY_FILTER, "methodId", id);
	}
	
	public InvocationDao ofMethod(String name, String signature) {
		return setUp(MappingKey.PROPERTY_FILTER, "method", name, "signature", signature);
	}
	
	public InvocationDao parentOf(Invocation inv) {
		return setUp(MappingKey.PROPERTY_FILTER, "testId", inv.getTestId(), "step", inv.getCallStep());
	}
}

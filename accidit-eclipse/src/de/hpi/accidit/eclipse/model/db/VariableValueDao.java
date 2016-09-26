package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;

public class VariableValueDao extends NamedValueDao<VariableValue, VariableValueDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?, ?> sql = schema.getMappingBuilder(VariableValue.class);
		NamedValueDao.init(sql);
		sql.attributes("m.`name`, m.`id`")
			.from("`Variable` m");
	}

	protected VariableValueDao(ModelDaoBase<VariableValue, VariableValueDao> source) {
		super(source);
	}

	public VariableValueDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(VariableValue.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "id", "name");
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<VariableValue, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		
		snippetLayer.setUp("last_and_next", (qry, a) -> qry.getStatement()
			.select().sql("COALESCE(lastSet.`step`, -1) AS `valueStep`, "
					+ "COALESCE(nextSet.`step`, -1) AS `nextChangeStep`, "
					+ "COALESCE(nextSet.`callStep`, lastSet.`callStep`) AS `callStep`")
			.leftJoin().sql("(SELECT `methodId`, `variableId`, MAX(`step`) AS `step`, `callStep` " +
				    "FROM `VariableTrace` " +
				    "WHERE `testId` = ? AND `callStep` = ? AND `step` < ? " +
				    "GROUP BY `methodId`, `variableId`) " +
				 "lastSet ON m.`id` = lastSet.`variableId` AND m.`methodId` = lastSet.`methodId`", a)
			.leftJoin().sql("(SELECT `methodId`, `variableId`, MIN(`step`) AS `step`, `callStep` " +
				    "FROM `VariableTrace` " +
				    "WHERE `testId` = ? AND `callStep` = ? AND `step` >= ? " +
				    "GROUP BY `methodId`, `variableId`) " +
				"nextSet ON m.`id` = nextSet.`variableId` AND m.`methodId` = nextSet.`methodId`", a)
			.where().sql("(lastSet.`step` IS NOT NULL " +
					   "OR nextSet.`step` IS NOT NULL)")
			.orderBy().sql("m.`id`"));
	}
	
	public VariableValueDao atStep(int testId, long callStep, long step) {
		return doSafe(me -> me
				.snippet("last_and_next", testId, callStep, step)
				.setUp(MappingKey.LOAD, "valueStep", "nextChangeStep", "callStep")
				.configureStep(testId, step));
//				.setUp(MappingKey.LOAD_FIELD, lf -> lf.addAll(
//				"valueStep", "nextChangeStep", "callStep"));
	}
	
	public VariableValueDao history() {
		return sql(sql -> sql
				.select().sql("t.`testId`, t.`step` AS `step`, t.`line`, " + 
						   "t.`step` AS `valueStep`, t.`callStep` AS `callStep`")
				.join().id("VariableTrace").sql(" t ON t.`variableId` = m.`id` AND t.`methodId` = m.`methodId`")
				.orderBy().sql("t.`step`"))
				.setUp(MappingKey.LOAD, "testId", "step", "line", "valueStep", "callStep");
	}
	
	public VariableValueDao inCall(int testId, long callStep) {
		return sql(sql -> sql
				.where().sql("t.`testId` = ? AND t.`callStep` = ?", testId, callStep));
	}
	
	public VariableValueDao byId(int id) {
		return sql(sql -> sql
				.where().sql("m.`id` = ?", id));
	}
	
	public VariableValueDao inTest(int testId) {
		return sql(sql -> sql
				.where().sql("t.`testId` = ?", testId));
	}
	
	public VariableValueDao atStep(long step) {
		return sql(sql -> sql
				.where().sql("t.`step` = ?", step));
	}
}

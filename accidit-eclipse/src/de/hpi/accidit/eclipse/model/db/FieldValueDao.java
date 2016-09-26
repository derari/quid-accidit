package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;

public class FieldValueDao extends NamedValueDao<FieldValue, FieldValueDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(FieldValue.class);
		NamedValueDao.init(sql);
		sql.attributes("m.`name`, m.`id`")
			.from("`Field` m");
	}
	
	protected FieldValueDao(ModelDaoBase<FieldValue, FieldValueDao> source) {
		super(source);
	}

	public FieldValueDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(FieldValue.class));
	}

	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "id", "name");
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<FieldValue, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		
		snippetLayer.setUp("last_and_next", (qry, a) -> qry.getStatement()
			.select().sql("COALESCE(lastPut.`step`, lastGet.`step`, -1) AS `valueStep`, "
					+ "__ISNOTNULL{lastPut.`step`} AS `valueIsPut`, "
					+ "COALESCE(nextPut.`step`, -1) AS `nextChangeStep`, "
					+ "COALESCE(nextGet.`step`, -1) AS `nextGetStep`, "
					+ "COALESCE(lastGet.`step`, -1) AS `lastGetStep`")
			.leftJoin()
				.sql("(SELECT MAX(`step`) AS `step`, `fieldId` " +
					 "FROM `PutTrace` " +
					 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
					 "GROUP BY `fieldId`) " +
				 "lastPut ON lastPut.`fieldId` = m.`id`", a)
			.leftJoin()
				.sql("(SELECT MAX(`step`) AS `step`, `fieldId` " +
					 "FROM `GetTrace` " +
					 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
					 "GROUP BY `fieldId`) " +
				"lastGet ON lastGet.`fieldId` = m.`id`", a)
			.leftJoin()
				.sql("(SELECT MIN(`step`) AS `step`, `fieldId` " +
					 "FROM `PutTrace` " +
					 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
					 "GROUP BY `fieldId`) " +
				 "nextPut ON nextPut.`fieldId` = m.`id`", a)
			.leftJoin()
				.sql("(SELECT MIN(`step`) AS `step`, `fieldId` " +
					 "FROM `GetTrace` " +
					 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
					 "GROUP BY `fieldId`) " +
				 "nextGet ON nextGet.`fieldId` = m.`id`", a)
			.where()
				.sql("(lastPut.`step` IS NOT NULL " +
				   "OR lastGet.`step` IS NOT NULL " +
				   "OR nextPut.`step` IS NOT NULL " +
				   "OR nextGet.`step` IS NOT NULL)")
			.orderBy().sql("m.`name`"));
	}
	
	public FieldValueDao ofField(String field) {
		return sql(sql -> sql.where().sql("m.`name` = ?", field));
	}
	
	public FieldValueDao atStep(int testId, long thisId, long step) {
		return doSafe(me -> me
				.snippet("last_and_next", testId, thisId, step)
				.setUp(MappingKey.LOAD, "valueStep", "valueIsPut", "nextChangeStep", "nextGetStep", "lastGetStep")
				.configureStep(testId, step));
	}
	
	public FieldValueDao setBeforeStep(int testId, long thisId, long step) {
		return sql(sql -> sql
				.select().sql("t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`line`")
				.join().id("PutTrace").sql(" t ON t.`fieldId` = m.`id`")
				.where().sql("t.`testId` = ? AND t.`thisId` = ? AND t.`step` < ?", testId, thisId, step)
				.orderBy().sql("t.`step` DESC"))
			.setUp(MappingKey.SET, sf -> sf.set("testId", testId))
			.setUp(MappingKey.LOAD, "thisId", "step", "callStep", "line");
//			.setUp(MappingKey.LOAD_FIELD, lf -> lf.add("testId"))
	}
	
	public FieldValueDao historyOfObject(int testId, long thisId) {
		return sql(sql -> sql
			.select()
				.sql("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`, val.`valueIsPut` AS `valueIsPut`, val.`line` AS `line`")
				.sql("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`")
				.sql("COALESCE(MIN(nextGet.`step`), -1) AS `nextGetStep`")
//			.from().id("Field").ql(" m")
			.join().sql("(SELECT `testId`, `step`, `fieldId`, 1 AS `valueIsPut`, `line` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? " +
				    "UNION " +
				  	"SELECT `testId`, `step`, `fieldId`, 0 AS `valueIsPut`, `line` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ?) " + 
					"val ON val.`fieldId` = m.`id`", testId, thisId, testId, thisId)
			.leftJoin().sql("`PutTrace` nextPut " +
					"ON nextPut.`fieldId` = m.`id` AND nextPut.`step` > val.`step` " +
					"AND nextPut.`testId` = ? AND nextPut.`thisId` = ?", testId, thisId)
			.leftJoin().sql("`GetTrace` nextGet " +
					"ON nextGet.`fieldId` = m.`id` AND nextGet.`step` >= val.`step` " +
					"AND nextGet.`testId` = ? AND nextGet.`thisId` = ?", testId, thisId)
			.groupBy().sql("m.`id`, val.`step`")
			.orderBy().sql("val.`step`"))
		.setUp(MappingKey.SET, sf -> sf.set("testId", testId))
		.setUp(MappingKey.LOAD, "step", "valueStep", "valueIsPut", "nextChangeStep", "nextGetStep", "line");
	}
	
	public FieldValueDao byId(int id) {
		return sql(sql -> sql.where().sql("m.`id` = ?", id));
	}
	
	public FieldValueDao writesAtStep(int testId, long step) {
		return sql(sql -> sql
				.select().sql("t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`line`")
				.join().id("PutTrace").sql(" t ON t.`fieldId` = m.`id`")
				.where().sql("t.`testId` = ? AND t.`step` = ?", testId, step))
			.setUp(MappingKey.SET, sf -> sf.set("testId", testId))
			.setUp(MappingKey.LOAD, "step", "thisId", "callStep", "line");
	}

	public FieldValueDao readsInInvocation(int testId, long callStep) {
		return sql(sql -> sql
				.select().sql("t.`thisId`, t.`step`, t.`line`")
				.join().id("GetTrace").sql(" t ON t.`fieldId` = m.`id`")
				.where().sql("t.`testId` = ? AND t.`callStep` = ?", testId, callStep))
			.setUp(MappingKey.SET, sf -> sf.set("testId", testId))
			.setUp(MappingKey.SET, sf -> sf.set("callStep", callStep))
			.setUp(MappingKey.LOAD, "step", "thisId", "line");
	}

	
//	attributes("m.`name`, m.`id`");
//	using("val")
//		.select("val.`testId`, val.`callStep`, val.`step` AS `valueStep`, val.`step` AS `step`, COALESCE(val.`thisId`, 0) AS `thisId`");
//	
//	table("`Field` m");
//	
//	join("`GetTrace` val ON val.`fieldId` = m.`id`");
//	
//	where("call_EQ", "val.`testId` = ? AND val.`callStep` = ?");
//	
//	always()
////		.groupBy("m.`id`, val.`step`")
//		.orderBy("val.`step`");
//	always().configure("cfgCnn", SET_CONNECTION)
//			.configure("cfgIsPut", CfgSetField.newInstance("valueIsPut", false));
//}};

}

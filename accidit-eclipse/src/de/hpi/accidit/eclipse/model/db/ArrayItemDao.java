package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;

public class ArrayItemDao extends NamedValueDao<ItemValue, ArrayItemDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(ItemValue.class);
		NamedValueDao.init(sql);
	}
	
	protected ArrayItemDao(ModelDaoBase<ItemValue, ArrayItemDao> source) {
		super(source);
	}
	
	public ArrayItemDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(ItemValue.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
	}

	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<ItemValue, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		
		snippetLayer.setUp("last_and_next", (sql, a) -> sql.getStatement()
			.select().sql("`index` AS `id`, "
					+ "COALESCE(MAX(lastPut_step), MAX(lastGet_step), -1) AS `valueStep`, "
					+ "__ISNOTNULL{MIN(lastPut_step)} AS `valueIsPut`, "
					+ "COALESCE(MIN(nextPut_step), -1) AS `nextChangeStep`, "
					+ "COALESCE(MIN(nextGet_step), -1) AS `nextGetStep`")

			.from()
				.sql("(SELECT * FROM " +
					"(SELECT `index`, " +
					        "MAX(`step`) AS lastPut_step, NULL AS lastGet_step, NULL AS nextPut_step, NULL AS nextGet_step " +
			         "FROM `ArrayPutTrace` " +
			         "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
			         "GROUP BY `index`) tmp ", a)
				.sql("UNION " +
					"(SELECT `index`, " +
					        "NULL AS lastPut_step, MAX(`step`) AS lastGet_step, NULL AS nextPut_step, NULL AS nextGet_step " +
			         "FROM `ArrayGetTrace` " +
			         "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
			         "GROUP BY `index`) ", a)
				.sql("UNION " +
					"(SELECT `index`, " +
					        "NULL AS lastPut_step, NULL AS lastGet_step, MIN(`step`) AS nextPut_step, NULL AS nextGet_step " +
			         "FROM `ArrayPutTrace` " +
			         "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
			         "GROUP BY `index`) ", a)
				.sql("UNION " +
					"(SELECT `index`, " +
					        "NULL AS lastPut_step, NULL AS lastGet_step, NULL AS nextPut_step, MIN(`step`) AS nextGet_step " +
			         "FROM `ArrayGetTrace` " +
			         "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
			         "GROUP BY `index`) " +				
					") t", a)
				.groupBy().id("index")
				.orderBy().id("index"));
		snippetLayer.setUp("index_EQ", (sql, a) -> sql.getStatement()
				.where().sql("t.`index` = ?", a));
	}
	
	public ArrayItemDao atIndex(int index) {
		return build("index_EQ", index);
	}
	
	public ArrayItemDao atStep(int testId, long thisId, long step) {
		return build("last_and_next", testId, thisId, step)
				.setUp(MappingKey.LOAD, "id", "valueStep", "valueIsPut", "nextChangeStep", "nextGetStep")
				.setUp(MappingKey.SET, "testId", testId, "step", step, "thisId", thisId);
	}
	
	public ArrayItemDao setBeforeStep(int testId, long thisId, long step) {
		return sql(sql -> sql
				.select().sql("t.`step`, t.`callStep`, t.`index` AS `id`, t.`line`")
				.from().id("ArrayPutTrace").ql(" t")
				.where().sql("t.`testId` = ? AND t.`thisId` = ? AND t.`step` < ?", testId, thisId, step)
				.orderBy().sql("t.`step` DESC"))
				.setUp(MappingKey.LOAD, "id", "line", "step", "callStep")
				.setUp(MappingKey.SET, "testId", testId, "thisId", thisId);
	}
		
	public ArrayItemDao historyOfObject(int testId, long thisId) {
		return sql(sql -> sql
				.select()
					.sql("val.`index` AS `id`")
					.sql("val.`step` AS `valueStep`, val.`step` AS `step`, val.`valueIsPut` AS `valueIsPut`")
					.sql("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`")
					.sql("COALESCE(MIN(nextGet.`step`), -1) AS `nextGetStep`")
				.from()
					.sql("(SELECT `testId`, `step`, `index`, 1 AS `valueIsPut` FROM `ArrayPutTrace` WHERE `testId` = ? AND `thisId` = ? " +
				    "UNION " +
				    "SELECT `testId`, `step`, `index`, 0 AS `valueIsPut` FROM `ArrayGetTrace` WHERE `testId` = ? AND `thisId` = ?)", 
				    testId, thisId, testId, thisId)
				.leftJoin()
					.sql("`ArrayPutTrace` nextPut " +
					"ON nextPut.`index` = val.`index` AND nextPut.`step` > val.`step` " +
					"AND nextPut.`testId` = ? AND nextPut.`thisId` = ?", testId, thisId)
				.leftJoin()
					.sql("`ArrayGetTrace` nextGet " +
					"ON nextGet.`index` = val.`index` AND nextGet.`step` >= val.`step` " +
					"AND nextGet.`testId` = ? AND nextGet.`thisId` = ?", testId, thisId)
				.groupBy().sql("val.`index`, val.`step`")
				.orderBy().sql("val.`step`"))
			.setUp(MappingKey.LOAD, "id", "step", "valueStep", "valueIsPut", "nextChangeStep", "nextGetStep")
			.setUp(MappingKey.SET, "testId", testId, "thisId", thisId);
	}
	
	public ArrayItemDao byId(int id) {
		return sql(sql -> sql.where().sql("val.`index` = ?", id));
	}
	
	public ArrayItemDao readInCall(int testId, long callStep) {
		return sql(sql -> sql
			.select()
				.sql("val.`index` AS `id`, val.`thisId` AS `thisId`")
				.sql("val.`step` AS `valueStep`, val.`step` AS `step`")
			.from()
				.id("ArrayGetTrace").ql(" val")
			.where()
				.sql("val.`testId` = ? AND val.`callStep` = ?", testId, callStep)
			.orderBy().sql("val.`step`"))
		.setUp(MappingKey.LOAD, "id", "valueStep", "step", "thisId")
		.setUp(MappingKey.SET, "testId", testId, "valueIsPut", false);
	}
}

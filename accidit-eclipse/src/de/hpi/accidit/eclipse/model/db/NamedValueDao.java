package de.hpi.accidit.eclipse.model.db;

import java.sql.SQLException;

import org.cthul.miro.dml.AbstractMappedSelect;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.query.sql.DataQuery;
import org.cthul.miro.result.Results;
import org.cthul.miro.util.CfgSetField;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;

public class NamedValueDao extends ModelDaoBase {
	
	public static final ViewR<VarQuery> VARIABLE_VIEW = Views.build().r(VarQuery.class);
	public static final ViewR<FieldQuery> FIELD_VIEW = Views.build().r(FieldQuery.class);
	public static final ViewR<ItemQuery> ARRAY_ITEM_VIEW = Views.build().r(ItemQuery.class);
	public static final ViewR<VarHistoryQuery> VARIABLE_HISTORY_VIEW = Views.build().r(VarHistoryQuery.class);
	public static final ViewR<ObjHistoryQuery> OBJECT_HISTORY_VIEW = Views.build().r(ObjHistoryQuery.class);
	public static final ViewR<SetFieldQuery> OBJECT_SET_FIELD_VIEW = Views.build().r(SetFieldQuery.class);
	public static final ViewR<ObjGetHistoryQuery> OBJECT_GET_HISTORY_VIEW = Views.build().r(ObjGetHistoryQuery.class);
	public static final ViewR<ArrayHistoryQuery> ARRAY_HISTORY_VIEW = Views.build().r(ArrayHistoryQuery.class);
	public static final ViewR<SetItemQuery> ARRAY_SET_ITEM_VIEW = Views.build().r(SetItemQuery.class);
	public static final ViewR<ArrayGetHistoryQuery> ARRAY_GET_HISTORY_VIEW = Views.build().r(ArrayGetHistoryQuery.class);
	
	private static class NameValueQueryTemplate<E> extends MappedDataQueryTemplateProvider<E> {
		public NameValueQueryTemplate(Mapping<E> mapping) {
			super(mapping);
		}
	}
	
	private static class NamedValueQuery<E, This extends NamedValueQuery<E, This>> extends AbstractMappedSelect<E, Results<E>, This> {
		public NamedValueQuery(MappedTemplateProvider<E> template, String[] select) {
			super(DataQuery.SELECT, template, Results.<E>getBuilder(), select);
		}
		
		protected void configureStep(int testId, long step) {
			configure(CfgSetField.newInstance("testId", testId));
			configure(CfgSetField.newInstance("step", step));
		}
	}
	
//	private static final Mapping<NamedValue> BASE_MAPPING = new ReflectiveMapping<NamedValue>(NamedValue.class);
	
	private static final Mapping<VariableValue> VAR_MAPPING = new ReflectiveMapping<VariableValue>(VariableValue.class);
	
	private static final MappedTemplateProvider<VariableValue> VAR_TEMPLATE = new NameValueQueryTemplate<VariableValue>(VAR_MAPPING) {{
		attributes("m.`name`, m.`id`");
		using("last_and_next")
			.select("COALESCE(lastSet.`step`, -1) AS `valueStep`")
			.select("COALESCE(nextSet.`step`, -1) AS `nextChangeStep`");
//			.select("COALESCE(nextSet.`callStep`, lastSet.`callStep`) AS `callStep`");
		table("`Variable` m");
		join("LEFT OUTER JOIN " +
				 "(SELECT `methodId`, `variableId`, MAX(`step`) AS `step` " +
			    "FROM `VariableTrace` " +
			    "WHERE `testId` = ? AND `callStep` = ? AND `step` < ? " +
			    "GROUP BY `methodId`, `variableId`) " +
			 "lastSet ON m.`id` = lastSet.`variableId` AND m.`methodId` = lastSet.`methodId`");
		join("LEFT OUTER JOIN " +
			 "(SELECT `methodId`, `variableId`, MIN(`step`) AS `step` " +
			    "FROM `VariableTrace` " +
			    "WHERE `testId` = ? AND `callStep` = ? AND `step` >= ? " +
			    "GROUP BY `methodId`, `variableId`) " +
			 "nextSet ON m.`id` = nextSet.`variableId` AND m.`methodId` = nextSet.`methodId`");
		
		using("lastSet", "nextSet")
			.where("last_and_next", 
				     "(lastSet.`step` IS NOT NULL " +
				   "OR nextSet.`step` IS NOT NULL)");
		
		always().orderBy("m.`id`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};

	public static class VarQuery extends NamedValueQuery<VariableValue, VarQuery> {

		public VarQuery(String... select) {
			super(VAR_TEMPLATE, select);
		}

		public VarQuery where() {
			return this;
		}
		
		public VarQuery atStep(int testId, long callStep, long step) {
			put("lastSet", testId, callStep, step);
			put("nextSet", testId, callStep, step);
			configureStep(testId, step);
			return this;
		}
	};
	
	public static final Mapping<FieldValue> FIELD_MAPPING = new ReflectiveMapping<FieldValue>(FieldValue.class) {
		protected void setField(FieldValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "valueIsPut":
				setField(record, "valueIsPut", rs.getInt(i) == 1);
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
		
	private static final MappedTemplateProvider<FieldValue> FIELD_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
		attributes("m.`name`, m.`id`");
		using("last_and_next")
			.select("COALESCE(lastPut.`step`, lastGet.`step`, -1) AS `valueStep`")
			.select("__ISNOTNULL{lastPut.`step`} AS `valueIsPut`")
			.select("COALESCE(nextPut.`step`, -1) AS `nextChangeStep`")
			.select("COALESCE(nextGet.`step`, -1) AS `nextGetStep`")
			.select("COALESCE(lastGet.`step`, -1) AS `lastGetStep`");
		
		table("`Field` m");
		
		join("LEFT OUTER JOIN " +
				"(SELECT MAX(`step`) AS `step`, `fieldId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
				 "GROUP BY `fieldId`) " +
			 "lastPut ON lastPut.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MAX(`step`) AS `step`, `fieldId` " +
				 "FROM `GetTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
				 "GROUP BY `fieldId`) " +
			 "lastGet ON lastGet.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextPut ON nextPut.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId` " +
				 "FROM `GetTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextGet ON nextGet.`fieldId` = m.`id`");
		
		using("lastPut", "lastGet", "nextPut", "nextGet")
			.where("last_and_next", 
				     "(lastPut.`step` IS NOT NULL " +
				   "OR lastGet.`step` IS NOT NULL " +
				   "OR nextPut.`step` IS NOT NULL " +
				   "OR nextGet.`step` IS NOT NULL)");
		always().orderBy("m.`name`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class FieldQuery extends NamedValueQuery<FieldValue, FieldQuery> {

		public FieldQuery(String... select) {
			super(FIELD_TEMPLATE, select);
		}
		
		public FieldQuery where() {
			return this;
		}
		
		public FieldQuery atStep(int testId, long thisId, long step) {
			put("lastPut", testId, thisId, step);
			put("lastGet", testId, thisId, step);
			put("nextPut", testId, thisId, step);
			put("nextGet", testId, thisId, step);
			configureStep(testId, step);
			return this;
		}
	};
	
	private static final MappedTemplateProvider<FieldValue> SET_FIELD_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
		attributes("m.`name`, t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`line`");
		
		table("`Field` m");
		join("`PutTrace` t ON t.`fieldId` = m.`id`");
		
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class SetFieldQuery extends NamedValueQuery<FieldValue, SetFieldQuery> {

		public SetFieldQuery(String[] select) {
			super(SET_FIELD_TEMPLATE, select);
		}
		
		public SetFieldQuery where() {
			return this;
		}
		
		public SetFieldQuery beforeStep(int testId, long thisId, long step) {
			put("testId =", testId);
			put("thisId =", thisId);
			put("step <", step);
			put("orderBy-step DESC");
			return this;
		}
		
		public SetFieldQuery ofField(String field) {
			put("name =", field);
			return this;
		}
	};
	
	private static final Mapping<ItemValue> ARRAY_ITEM_MAPPING = new ReflectiveMapping<ItemValue>(ItemValue.class) {
		protected void setField(ItemValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "valueIsPut":
				setField(record, "valueIsPut", rs.getInt(i) == 1);
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
	
	private static final MappedTemplateProvider<ItemValue> ARRAY_ITEM_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
		select("`index` AS `id`",
			   "COALESCE(MAX(lastPut_step), MAX(lastGet_step), -1) AS `valueStep`",
			   "__ISNOTNULL{MIN(lastPut_step)} AS `valueIsPut`",
			   "COALESCE(MIN(nextPut_step), -1) AS `nextChangeStep`",
			   "COALESCE(MIN(nextGet_step), -1) AS `nextGetStep`");
		
		table("(SELECT * FROM " +
				"(SELECT `index`, " +
				        "MAX(`step`) AS lastPut_step, NULL AS lastGet_step, NULL AS nextPut_step, NULL AS nextGet_step " +
		         "FROM `ArrayPutTrace` " +
		         "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
		         "GROUP BY `index`) tmp " +
	         "UNION " +
				"(SELECT `index`, " +
				        "NULL AS lastPut_step, MAX(`step`) AS lastGet_step, NULL AS nextPut_step, NULL AS nextGet_step " +
		         "FROM `ArrayGetTrace` " +
		         "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
		         "GROUP BY `index`) " +
	         "UNION " +
				"(SELECT `index`, " +
				        "NULL AS lastPut_step, NULL AS lastGet_step, MIN(`step`) AS nextPut_step, NULL AS nextGet_step " +
		         "FROM `ArrayPutTrace` " +
		         "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
		         "GROUP BY `index`) " +
	         "UNION " +
				"(SELECT `index`, " +
				        "NULL AS lastPut_step, NULL AS lastGet_step, NULL AS nextPut_step, MIN(`step`) AS nextGet_step " +
		         "FROM `ArrayGetTrace` " +
		         "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? " +
		         "GROUP BY `index`) " +				
			 ") t");
		
		where("index_EQ", "`index` = ?");
		
		always()
			.groupBy("`index`")
			.orderBy("`index`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class ItemQuery extends NamedValueQuery<ItemValue, ItemQuery> {

		public ItemQuery(String[] select) {
			super(ARRAY_ITEM_TEMPLATE, select);
		}
		
		public ItemQuery where() {
			return this;
		}
		
		public ItemQuery atStep(int testId, long thisId, long step) {
			put("t",
					testId, thisId, step,
					testId, thisId, step,
					testId, thisId, step,
					testId, thisId, step);
			configureStep(testId, step);
			return this;
		}
		
		public ItemQuery atIndex(int i) {
			put("index_EQ", i);
			return this;
		}
		
	};
	
	private static final MappedTemplateProvider<ItemValue> ARRAY_SET_ITEM_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
		attributes("t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`index` AS `id`, t.`line`");
		table("`ArrayPutTrace` t");
		
		where("index_EQ", "`index` = ?");
		
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class SetItemQuery extends NamedValueQuery<ItemValue, SetItemQuery> {

		public SetItemQuery(String[] select) {
			super(ARRAY_SET_ITEM_TEMPLATE, select);
		}
		
		public SetItemQuery where() {
			return this;
		}
		
		public SetItemQuery beforeStep(int testId, long thisId, long step) {
			put("testId =", testId);
			put("thisId =", thisId);
			put("step <", step);
			put("orderBy-step DESC");
			return this;
		}
		
		public SetItemQuery atIndex(int i) {
			put("id =", i);
			return this;
		}
		
	};
	
	private static final MappedTemplateProvider<VariableValue> VAR_HISTORY_TEMPLATE = new NameValueQueryTemplate<VariableValue>(VAR_MAPPING) {{
		attributes("t.`variableId` AS `id`, t.`testId`, t.`step` AS `step`, t.`line`", 
				   "t.`step` AS `valueStep`, v.`name` AS `name`");
		
		table("`VariableTrace` t");
		join("`Variable` v ON t.`variableId` = v.`id` AND t.`methodId` = v.`methodId`");
		where("call_EQ", "t.`testId` = ? AND t.`callStep` = ?");
		always().orderBy("t.`step`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class VarHistoryQuery extends NamedValueQuery<VariableValue, VarHistoryQuery> {

		public VarHistoryQuery(String[] select) {
			super(VAR_HISTORY_TEMPLATE, select);
		}
		
		public VarHistoryQuery where() {
			return this;
		}
		
		public VarHistoryQuery inCall(int testId, long callStep) {
			where("call_EQ", testId, callStep);
			return this;
		}
		
		public VarHistoryQuery byId(int id) {
			where("id =", id);
			return this;
		}
		
		public VarHistoryQuery atStep(long step) {
			where("step =", step);
			return this;
		}
	};
	
	private static final MappedTemplateProvider<FieldValue> OBJ_HISTORY_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
		attributes("m.`name`, m.`id`,");
		using("val")
			.select("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`, val.`valueIsPut` AS `valueIsPut`, val.`line` AS `line`");
		using("nextPut")
			.select("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`");
		using("nextGet")
			.select("COALESCE(MIN(nextGet.`step`), -1) AS `nextGetStep`");
		
		table("`Field` m");
		
		join("val", 
			 "JOIN (SELECT `testId`, `step`, `fieldId`, 1 AS `valueIsPut`, `line` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? " +
			  "UNION " +
			  "SELECT `testId`, `step`, `fieldId`, 0 AS `valueIsPut`, `line` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ?) " + 
			 "val ON val.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN `PutTrace` nextPut " +
			 "ON nextPut.`fieldId` = m.`id` AND nextPut.`step` > val.`step` " +
			 "AND nextPut.`testId` = ? AND nextPut.`thisId` = ?");
		join("LEFT OUTER JOIN `GetTrace` nextGet " +
			 "ON nextGet.`fieldId` = m.`id` AND nextGet.`step` >= val.`step` " +
			 "AND nextGet.`testId` = ? AND nextGet.`thisId` = ?");
		
		where("id_EQ", "m.`id` = ?");
		
		always()
			.groupBy("m.`id`, val.`step`")
			.orderBy("val.`step`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class ObjHistoryQuery extends NamedValueQuery<FieldValue, ObjHistoryQuery> {

		public ObjHistoryQuery(String[] fields) {
			super(OBJ_HISTORY_TEMPLATE, fields);
		}
		
		public ObjHistoryQuery where() {
			return this;
		}
		
		public ObjHistoryQuery ofObject(int testId, long thisId) {
			put("val", testId, thisId, testId, thisId);
			put("nextPut", testId, thisId);
			put("nextGet", testId, thisId);
			return this;
		}
		
		public ObjHistoryQuery byId(int id) {
			where("id_EQ", id);
			return this;
		}
	};
	
	private static final MappedTemplateProvider<FieldValue> OBJ_GET_HISTORY_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
		attributes("m.`name`, m.`id`");
		using("val")
			.select("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`, COALESCE(val.`thisId`, 0) AS `thisId`");
		
		table("`Field` m");
		
		join("`GetTrace` val ON val.`fieldId` = m.`id`");
		
		where("call_EQ", "val.`testId` = ? AND val.`callStep` = ?");
		
		always()
//			.groupBy("m.`id`, val.`step`")
			.orderBy("val.`step`");
		always().configure("cfgCnn", SET_CONNECTION)
				.configure("cfgIsPut", CfgSetField.newInstance("valueIsPut", false));
	}};
	
	public static class ObjGetHistoryQuery extends NamedValueQuery<FieldValue, ObjHistoryQuery> {

		public ObjGetHistoryQuery(String[] fields) {
			super(OBJ_GET_HISTORY_TEMPLATE, fields);
		}
		
		public ObjGetHistoryQuery where() {
			return this;
		}
		
		public ObjGetHistoryQuery inCall(int testId, long step) {
			put("call_EQ", testId, step);
			return this;
		}
	};
	
	private static final MappedTemplateProvider<ItemValue> ARRAY_HISTORY_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
		attributes("val.`index` AS `id`");
		attributes("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`, val.`valueIsPut` AS `valueIsPut`");
		using("nextPut")
			.select("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`");
		using("nextGet")
			.select("COALESCE(MIN(nextGet.`step`), -1) AS `nextGetStep`");
		
		table("(SELECT `testId`, `step`, `index`, 1 AS `valueIsPut` FROM `ArrayPutTrace` WHERE `testId` = ? AND `thisId` = ? " +
			  "UNION " +
			  "SELECT `testId`, `step`, `index`, 0 AS `valueIsPut` FROM `ArrayGetTrace` WHERE `testId` = ? AND `thisId` = ?) " + 
			 "val");
		join("LEFT OUTER JOIN `ArrayPutTrace` nextPut " +
			 "ON nextPut.`index` = val.`index` AND nextPut.`step` > val.`step` " +
			 "AND nextPut.`testId` = ? AND nextPut.`thisId` = ?");
		join("LEFT OUTER JOIN `ArrayGetTrace` nextGet " +
			 "ON nextGet.`index` = val.`index` AND nextGet.`step` >= val.`step` " +
			 "AND nextGet.`testId` = ? AND nextGet.`thisId` = ?");
		
		where("call_EQ", "val.`index` = ?");
		where("id_EQ", "val.`index` = ?");
		
		always()
			.groupBy("val.`index`")
			.groupBy("val.`step`")
			.orderBy("val.`step`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	public static class ArrayHistoryQuery extends NamedValueQuery<ItemValue, ArrayHistoryQuery> {

		public ArrayHistoryQuery(String[] fields) {
			super(ARRAY_HISTORY_TEMPLATE, fields);
		}
		
		public ArrayHistoryQuery where() {
			return this;
		}
		
		public ArrayHistoryQuery ofObject(int testId, long thisId) {
			put("val", testId, thisId, testId, thisId);
			put("nextPut", testId, thisId);
			put("nextGet", testId, thisId);
			return this;
		}
		
		public ArrayHistoryQuery byId(int id) {
			where("id_EQ", id);
			return this;
		}
	};
	
	private static final MappedTemplateProvider<ItemValue> ARRAY_GET_HISTORY_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
		attributes("val.`index` AS `id`, val.`thisId` AS `thisId`");
		attributes("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`");
//		using("nextPut")
//			.select("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`");
		
		table("`ArrayGetTrace` val");
		
		where("call_EQ", "val.`testId` = ? AND val.`callStep` = ?");
		
		always()
//			.groupBy("val.`index`")
//			.groupBy("val.`step`")
			.orderBy("val.`step`");
		always().configure("cfgCnn", SET_CONNECTION)
			.configure("cfgIsPut", CfgSetField.newInstance("valueIsPut", false));
	}};
	
	public static class ArrayGetHistoryQuery extends NamedValueQuery<ItemValue, ArrayHistoryQuery> {

		public ArrayGetHistoryQuery(String[] fields) {
			super(ARRAY_GET_HISTORY_TEMPLATE, fields);
		}
		
		public ArrayGetHistoryQuery where() {
			return this;
		}
		
		public ArrayGetHistoryQuery inCall(int testId, long callStep) {
			put("call_EQ", testId, callStep);
			return this;
		}
	};

//	private static class SetStepAdapter implements EntityInitializer<NamedValue> {
//		private final int testId;
//		private final long step;
//		public SetStepAdapter(int testId, long step) {
//			super();
//			this.testId = testId;
//			this.step = step;
//		}
//		
//		@Override
//		public void apply(NamedValue entity) throws SQLException {
//			BASE_MAPPING.setField(entity, "testId", testId);
//			BASE_MAPPING.setField(entity, "step", step);
//		}
//		
//		@Override
//		public void complete() throws SQLException { }
//		
//		@Override
//		public void close() throws SQLException { }
//	}

}

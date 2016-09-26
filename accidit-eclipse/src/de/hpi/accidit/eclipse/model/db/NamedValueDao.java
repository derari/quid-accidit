package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.graph.TypeBuilder;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.template.TemplateLayer;
import org.cthul.miro.sql.SelectQuery;

import de.hpi.accidit.eclipse.model.NamedValue;

public class NamedValueDao<Entity extends NamedValue, This extends NamedValueDao<Entity, This>> extends ModelDaoBase<Entity, This> {

	protected static void init(TypeBuilder<?,?,?> t) {
		ModelDaoBase.init(t);
	}
	
	protected NamedValueDao(ModelDaoBase<Entity, This> source) {
		super(source);
	}
	
	public NamedValueDao(MiConnection cnn, TemplateLayer<MappedQuery<Entity, SelectQuery>> queryLayer) {
		super(cnn, queryLayer);
	}

	protected This configureStep(int testId, long step) {
		return setUp(MappingKey.SET, sf -> {
			sf.set("testId", testId);
			sf.set("step", step);
		});
	}
	
//
//	public static class VarQuery extends NamedValueQuery<VariableValue, VarQuery> {
//
//		public VarQuery(String... select) {
//			super(VAR_TEMPLATE, select);
//		}
//
//		public VarQuery where() {
//			return this;
//		}
//		
//		public VarQuery atStep(int testId, long callStep, long step) {
//			put("lastSet", testId, callStep, step);
//			put("nextSet", testId, callStep, step);
//			configureStep(testId, step);
//			return this;
//		}
//	};
//	
//	public static final Mapping<FieldValue> FIELD_MAPPING = new ReflectiveMapping<FieldValue>(FieldValue.class) {
//		protected void setField(FieldValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
//			switch (field) {
//			case "valueIsPut":
//				setField(record, "valueIsPut", rs.getInt(i) == 1);
//				break;
//			default:
//				super.setField(record, field, rs, i);
//			}
//		};
//	};
//		

//	
//	private static final MappedTemplateProvider<FieldValue> SET_FIELD_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
//		attributes("m.`name`, t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`line`");
//		
//		table("`Field` m");
//		join("`PutTrace` t ON t.`fieldId` = m.`id`");
//		
//		always().configure("cfgCnn", SET_CONNECTION);
//	}};
//	
//	public static class SetFieldQuery extends NamedValueQuery<FieldValue, SetFieldQuery> {
//
//		public SetFieldQuery(String[] select) {
//			super(SET_FIELD_TEMPLATE, select);
//		}
//		
//		public SetFieldQuery where() {
//			return this;
//		}
//		
//		public SetFieldQuery beforeStep(int testId, long thisId, long step) {
//			put("testId =", testId);
//			put("thisId =", thisId);
//			put("step <", step);
//			put("orderBy-step DESC");
//			return this;
//		}
//		
//		public SetFieldQuery ofField(String field) {
//			put("name =", field);
//			return this;
//		}
//		
//		public SetFieldQuery atStep(int testId, long step) {
//			put("testId =", testId);
//			put("step =", step);
//			return this;
//		}
//	};
//	
//	private static final Mapping<ItemValue> ARRAY_ITEM_MAPPING = new ReflectiveMapping<ItemValue>(ItemValue.class) {
//		protected void setField(ItemValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
//			switch (field) {
//			case "valueIsPut":
//				setField(record, "valueIsPut", rs.getInt(i) == 1);
//				break;
//			default:
//				super.setField(record, field, rs, i);
//			}
//		};
//	};
//	
//	
//	public static class ItemQuery extends NamedValueQuery<ItemValue, ItemQuery> {
//
//		public ItemQuery(String[] select) {
//			super(ARRAY_ITEM_TEMPLATE, select);
//		}
//		
//		public ItemQuery where() {
//			return this;
//		}
//		
//		public ItemQuery atStep(int testId, long thisId, long step) {
//			put("t",
//					testId, thisId, step,
//					testId, thisId, step,
//					testId, thisId, step,
//					testId, thisId, step);
//			configureStep(testId, step);
//			return this;
//		}
//		
//		public ItemQuery atIndex(int i) {
//			put("index_EQ", i);
//			return this;
//		}
//		
//	};
//	
//	private static final MappedTemplateProvider<ItemValue> ARRAY_SET_ITEM_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
//		attributes("t.`testId`, t.`thisId`, t.`step`, t.`callStep`, t.`index` AS `id`, t.`line`");
//		table("`ArrayPutTrace` t");
//		
//		where("index_EQ", "`index` = ?");
//		
//		always().configure("cfgCnn", SET_CONNECTION);
//	}};
//	
//	public static class SetItemQuery extends NamedValueQuery<ItemValue, SetItemQuery> {
//
//		public SetItemQuery(String[] select) {
//			super(ARRAY_SET_ITEM_TEMPLATE, select);
//		}
//		
//		public SetItemQuery where() {
//			return this;
//		}
//		
//		public SetItemQuery beforeStep(int testId, long thisId, long step) {
//			put("testId =", testId);
//			put("thisId =", thisId);
//			put("step <", step);
//			put("orderBy-step DESC");
//			return this;
//		}
//		
//		public SetItemQuery atIndex(int i) {
//			put("id =", i);
//			return this;
//		}
//		
//	};
//	
//	private static final MappedTemplateProvider<VariableValue> VAR_HISTORY_TEMPLATE = new NameValueQueryTemplate<VariableValue>(VAR_MAPPING) {{
//		attributes("t.`variableId` AS `id`, t.`testId`, t.`step` AS `step`, t.`line`", 
//				   "t.`step` AS `valueStep`, t.`callStep` AS `callStep`, v.`name` AS `name`");
//		
//		table("`VariableTrace` t");
//		join("`Variable` v ON t.`variableId` = v.`id` AND t.`methodId` = v.`methodId`");
//		where("call_EQ", "t.`testId` = ? AND t.`callStep` = ?");
//		always().orderBy("t.`step`");
//		always().configure("cfgCnn", SET_CONNECTION);
//	}};
//	
//	public static class VarHistoryQuery extends NamedValueQuery<VariableValue, VarHistoryQuery> {
//
//		public VarHistoryQuery(String[] select) {
//			super(VAR_HISTORY_TEMPLATE, select);
//		}
//		
//		public VarHistoryQuery where() {
//			return this;
//		}
//		
//		public VarHistoryQuery inCall(int testId, long callStep) {
//			where("call_EQ", testId, callStep);
//			return this;
//		}
//		
//		public VarHistoryQuery inTest(int testId) {
//			where("testId =", testId);
//			return this;
//		}
//		
//		public VarHistoryQuery byId(int id) {
//			where("id =", id);
//			return this;
//		}
//		
//		public VarHistoryQuery atStep(long step) {
//			where("step =", step);
//			return this;
//		}
//	};
//	
//	private static final MappedTemplateProvider<FieldValue> OBJ_HISTORY_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
//	}};
//	
//	public static class ObjHistoryQuery extends NamedValueQuery<FieldValue, ObjHistoryQuery> {
//
//		public ObjHistoryQuery(String[] fields) {
//			super(OBJ_HISTORY_TEMPLATE, fields);
//		}
//		
//		public ObjHistoryQuery where() {
//			return this;
//		}
//		
//		public ObjHistoryQuery ofObject(int testId, long thisId) {
//			put("val", testId, thisId, testId, thisId);
//			put("nextPut", testId, thisId);
//			put("nextGet", testId, thisId);
//			return this;
//		}
//		
//		public ObjHistoryQuery byId(int id) {
//			where("id_EQ", id);
//			return this;
//		}
//	};
//	
//	private static final MappedTemplateProvider<FieldValue> OBJ_GET_HISTORY_TEMPLATE = new NameValueQueryTemplate<FieldValue>(FIELD_MAPPING) {{
//	
//	public static class ObjGetHistoryQuery extends NamedValueQuery<FieldValue, ObjHistoryQuery> {
//
//		public ObjGetHistoryQuery(String[] fields) {
//			super(OBJ_GET_HISTORY_TEMPLATE, fields);
//		}
//		
//		public ObjGetHistoryQuery where() {
//			return this;
//		}
//		
//		public ObjGetHistoryQuery inCall(int testId, long step) {
//			put("call_EQ", testId, step);
//			return this;
//		}
//	};
//	
//	private static final MappedTemplateProvider<ItemValue> ARRAY_HISTORY_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
//	}};
//	
//	public static class ArrayHistoryQuery extends NamedValueQuery<ItemValue, ArrayHistoryQuery> {
//
//		public ArrayHistoryQuery(String[] fields) {
//			super(ARRAY_HISTORY_TEMPLATE, fields);
//		}
//		
//		public ArrayHistoryQuery where() {
//			return this;
//		}
//		
//		public ArrayHistoryQuery ofObject(int testId, long thisId) {
//			put("val", testId, thisId, testId, thisId);
//			put("nextPut", testId, thisId);
//			put("nextGet", testId, thisId);
//			return this;
//		}
//		
//		public ArrayHistoryQuery byId(int id) {
//			where("id_EQ", id);
//			return this;
//		}
//	};
//	
//	private static final MappedTemplateProvider<ItemValue> ARRAY_GET_HISTORY_TEMPLATE = new NameValueQueryTemplate<ItemValue>(ARRAY_ITEM_MAPPING) {{
//	}};
//	
//	public static class ArrayGetHistoryQuery extends NamedValueQuery<ItemValue, ArrayHistoryQuery> {
//
//		public ArrayGetHistoryQuery(String[] fields) {
//			super(ARRAY_GET_HISTORY_TEMPLATE, fields);
//		}
//		
//		public ArrayGetHistoryQuery where() {
//			return this;
//		}
//		
//		public ArrayGetHistoryQuery inCall(int testId, long callStep) {
//			put("call_EQ", testId, callStep);
//			return this;
//		}
//	};

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

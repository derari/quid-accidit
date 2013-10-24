package de.hpi.accidit.eclipse.model;

import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.QueryFactoryView;
import org.cthul.miro.dsl.View;
import org.cthul.miro.graph.GraphQuery;
import org.cthul.miro.graph.GraphQueryTemplate;
import org.cthul.miro.graph.SelectByKey;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.EntityInitializer;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class NamedValue extends ModelBase implements NamedEntity {
	
	protected int testId;
	protected long step;
//	protected long callStep;
	protected long valueStep;
	protected long nextChangeStep;
	protected long nextGetStep = -1;
	protected long lastGetStep = -1;
	protected int id = -1;
	protected String name;
	protected Value value;
	protected String method;
	private Value owner;
	
	public NamedValue() { }

	public NamedValue(String name) {
		super();
		this.name = name;
		value = new Value.Primitive('V', 0);
		valueStep = -1;
		nextChangeStep = -1;
	}
	
	public NamedValue(String name, Value value) {
		this.name = name;
		this.value = value;
		valueStep = -1;
		nextChangeStep = -1;
	}

	public int getTestId() {
		return testId;
	}
	
	public long getStep() {
		return step;
	}
	
	public long getValueStep() {
		return valueStep;
	}
	
	public Value getOwner() {
		return owner;
	}
	
	public void setOwner(Value owner) {
		this.owner = owner;
	}
	
//	public long getCallStep() {
//		return callStep;
//	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		//Thread.sleep(500);
		if (value == null) {
			value = fetchValue();
		}
		if (value == null) {
			value = new Value.Primitive(valueStep + "/" + step + "/" + nextChangeStep);
		}
		value.beInitialized();
	}
	
	protected Value fetchValue() throws Exception {
		return new Value.Primitive(valueStep + "/" + step + "/" + nextChangeStep);
	}
	
	public synchronized Value getValue() {
		if (!beInitialized() || !isInitSuccess()) {
			Throwable t = getInitException();
			if (t != null) {
				t.printStackTrace(System.err);
				value = new Value.Primitive(getInitException());
			} else {
				value = new Value.Primitive("-no data-");
			}
		}
		return value;	
	}
	
	public boolean isActiveValue() {
		if (id == -1) {
			// some dummy value, assume active
			return true;
		}
		if (isGetTraced()) {
			if (nextGetStep == -1) {
				// value is never read again
				return false;
			}
			if (nextChangeStep != -1 && nextChangeStep < nextGetStep) {
				// value is changed before next read
				return false;
			}
		}
		if (valueStep == -1 || valueStep > step) {
			// value is in future
			return isActiveBeforeValueStep();
		}
		return true;
	}
	
	protected boolean isActiveBeforeValueStep() {
		return false;
	}
	
	protected boolean isGetTraced() {
		return true;
	}
	
//	public NamedValue[] previewChildren() {
//		if (value == null) return null;
//		return value.previewChildren();
//	}

	private boolean stepOver(long value, long oldStep, long newStep) {
		if (value == oldStep) {
			return value != newStep;
		} else if (value < oldStep) {
			return value >= newStep;
		} else { // value > oldStep
			return value <= newStep;
		}
	}
//	
//	public boolean valueUpdateNeeded(long newStep) {
//		return valueUpdateNeeded(step, newStep);
//	}
	
	public boolean updateNeeded(long newStep) {
		return valueUpdateNeeded(step, newStep) || minorUpdateNeeded(step, newStep);
	}
	
	private boolean valueUpdateNeeded(long oldStep, long newStep) {
		return (nextChangeStep != -1 && stepOver(nextChangeStep, oldStep, newStep))
				|| (valueStep != -1 && stepOver(valueStep, oldStep, newStep));
	}
	
	private boolean minorUpdateNeeded(long oldStep, long newStep) {
		return (nextGetStep != -1 && stepOver(nextGetStep, oldStep, newStep))
				|| (lastGetStep != -1 && stepOver(lastGetStep, oldStep, newStep));
	}
	
	public boolean setStep(long newStep) {
		assert !updateNeeded(newStep);
		step = newStep;
		if (!isInitialized()) {
			return true;
		}
		if (!setValueStep(newStep)) {
			return false;
		}
//		if (valueUpdateNeeded(oldStep, newStep) || minorUpdateNeeded(oldStep, newStep)) {
//			value = null;
//			reInitialize();
//			return false;
//		}
		return true;
	}
	
	private boolean setValueStep(long newStep) {
		if (value.updateNeeded(newStep)) {
			value = null;
			reInitialize();
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name + 
				"@" + Integer.toHexString(hashCode()) +
				(value == null ? "" : " " + Integer.toHexString(value.hashCode()));
	}
	
	/**
	 * Dummy value for the root of the tree,
	 * not actually "named".
	 */
	public static class MethodFrameValue extends NamedValue {

		private long call;
		
		public MethodFrameValue(int testId, long call, long step) {
			this.testId = testId;
			this.call = call;
			this.step = step;
			this.valueStep = -1;
			this.nextChangeStep = -1;
		}

		@Override
		protected Value fetchValue() throws Exception {
			return new Value.MethodSnapshot(testId, call, step);
		}	
	}
	
	public static class VariableValue extends NamedValue { 
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				return new Value.Primitive("--> " + nextChangeStep);
			}
			return DatabaseConnector.cnn()
					.select()
					.from(Value.ofVariable(id, testId, valueStep, step))
					.getSingle().execute();
		}
		
		@Override
		protected boolean isGetTraced() {
			return false;
		}
	}
	
	public static class FieldValue extends NamedValue {
		
		private boolean valueIsPut;
		
		public boolean isPut() {
			return valueIsPut;
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				if (nextGetStep != -1 && 
						(nextChangeStep == -1 || nextGetStep < nextChangeStep)) {
					// assume the next get will tell us the value
					valueStep = nextGetStep;
					valueIsPut = false;
				} else {
					// no value yet
					return new Value.Primitive("--> " + nextChangeStep);
				}
			}
			
			Value v = DatabaseConnector.cnn()
				.select()
				.from(Value.ofField(valueIsPut, id, testId, valueStep, step))
				.getSingle().execute();
			if (v != null) return v;
			
			System.err.println("TODO: fix this");
			v = DatabaseConnector.cnn()
					.select()
					.from(Value.ofField(!valueIsPut, id, testId, valueStep, step))
					.getSingle().execute();
			if (v != null) return v;
			
			return null;
		}
		
		@Override
		protected boolean isActiveBeforeValueStep() {
			return !valueIsPut;
		}
	}
	
	public static class ItemValue extends NamedValue {
		
		private boolean valueIsPut;
		
		@Override
		public String getName() {
			return String.valueOf(id);
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				if (nextGetStep != -1 && 
						(nextChangeStep == -1 || nextGetStep < nextChangeStep)) {
					// assume the next get will tell us the value
					valueStep = nextGetStep;
					valueIsPut = false;
				} else {
					// no value yet
					return new Value.Primitive("--> " + nextChangeStep);
				}
			}
			return DatabaseConnector.cnn()
				.select().from(Value.ofArray(valueIsPut, id, testId, valueStep, step))
				.getSingle().execute();
		}
		
		@Override
		protected boolean isActiveBeforeValueStep() {
			return !valueIsPut;
		}
	}
	
	public static class VariableHistory extends NamedValue {
		public VariableHistory(int testId, long callStep, int varId) {
			super("-", new Value.VariableHistory(testId, callStep, varId));
		}
	}
	
	public static class ObjectHistory extends NamedValue {
		public ObjectHistory(int testId, long callStep, int varId) {
			super("-", new Value.ObjectHistory(testId, callStep, varId));
		}
	}
	
	public static final View<VarQuery> VARIABLE_VIEW = new QueryFactoryView<>(VarQuery.class);
	public static final View<FieldQuery> FIELD_VIEW = new QueryFactoryView<>(FieldQuery.class);
	public static final View<ItemQuery> ARRAY_ITEM_VIEW = new QueryFactoryView<>(ItemQuery.class);
	public static final View<VarHistoryQuery> VARIABLE_HISTORY_VIEW = new QueryFactoryView<>(VarHistoryQuery.class);
	public static final View<ObjHistoryQuery> OBJECT_HISTORY_VIEW = new QueryFactoryView<>(ObjHistoryQuery.class);
	
	private static class NameValueQueryTemplate<E> extends GraphQueryTemplate<E> {{}}
	
	private static final Mapping<VariableValue> VAR_MAPPING = new ReflectiveMapping<VariableValue>(VariableValue.class);
	
	private static final GraphQueryTemplate<VariableValue> VAR_TEMPLATE = new NameValueQueryTemplate<VariableValue>() {{
		select("m.`name`, m.`id`");
		using("last_and_next")
			.select("COALESCE(lastSet.`step`, -1) AS `valueStep`")
			.select("COALESCE(nextSet.`step`, -1) AS `nextChangeStep`");
//			.select("COALESCE(nextSet.`callStep`, lastSet.`callStep`) AS `callStep`");
		from("`Variable` m");
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
	}};

	public static class VarQuery extends GraphQuery<VariableValue> {

		public VarQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, VAR_MAPPING, VAR_TEMPLATE, view);
			select(fields);
		}
		
		public VarQuery where() {
			return this;
		}
		
		public VarQuery atStep(int testId, long callStep, long step) {
			put("lastSet", testId, callStep, step);
			put("nextSet", testId, callStep, step);
			configure(new SetStepAdapter(testId, step));
			return this;
		}
	};
	
	private static final Mapping<FieldValue> FIELD_MAPPING = new ReflectiveMapping<FieldValue>(FieldValue.class) {
		protected void setField(FieldValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "valueIsPut":
				record.valueIsPut = rs.getInt(i) == 1;
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
		
	private static final GraphQueryTemplate<FieldValue> FIELD_TEMPLATE = new NameValueQueryTemplate<FieldValue>() {{
		select("m.`name`, m.`id`");
		using("last_and_next")
			.select("COALESCE(lastPut.`step`, lastGet.`step`, -1) AS `valueStep`")
			.select("__ISNOTNULL{lastPut.`step`} AS `valueIsPut`")
			.select("COALESCE(nextPut.`step`, -1) AS `nextChangeStep`")
			.select("COALESCE(nextGet.`step`, -1) AS `nextGetStep`")
			.select("COALESCE(lastGet.`step`, -1) AS `lastGetStep`");
		
		from("`Field` m");
		
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
		always().orderBy("m.`id`");
	}};
	
	public static class FieldQuery extends GraphQuery<FieldValue> {

		public FieldQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, FIELD_MAPPING, FIELD_TEMPLATE, view);
			select(fields);
		}
		
		public FieldQuery where() {
			return this;
		}
		
		public FieldQuery atStep(int testId, long thisId, long step) {
			put("lastPut", testId, thisId, step);
			put("lastGet", testId, thisId, step);
			put("nextPut", testId, thisId, step);
			put("nextGet", testId, thisId, step);
			configure(new SetStepAdapter(testId, step));
			return this;
		}
	};
	
	private static final Mapping<ItemValue> ARRAY_ITEM_MAPPING = new ReflectiveMapping<ItemValue>(ItemValue.class) {
		protected void setField(ItemValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "valueIsPut":
				record.valueIsPut = rs.getInt(i) == 1;
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
	
	private static final GraphQueryTemplate<ItemValue> ARRAY_ITEM_TEMPLATE = new NameValueQueryTemplate<ItemValue>() {{
		select("`index` AS `id`",
			   "COALESCE(MAX(lastPut_step), MAX(lastGet_step), -1) AS `valueStep`",
			   "__ISNOTNULL{MIN(lastPut_step)} AS `valueIsPut`",
			   "COALESCE(MIN(nextPut_step), -1) AS `nextChangeStep`",
			   "COALESCE(MIN(nextGet_step), -1) AS `nextGetStep`");
		
		from("(SELECT * FROM " +
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
		
		always()
			.groupBy("`index`")
			.orderBy("`index`");
	}};
	
	public static class ItemQuery extends GraphQuery<ItemValue> {

		public ItemQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, ARRAY_ITEM_MAPPING, ARRAY_ITEM_TEMPLATE, view);
			select(fields);
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
			configure(new SetStepAdapter(testId, step));
			return this;
		}
		
	};
	
	private static final GraphQueryTemplate<VariableValue> VAR_HISTORY_TEMPLATE = new NameValueQueryTemplate<VariableValue>() {{
		select("t.`variableId` AS `id`, t.`testId`, t.`step` AS `step`, t.`step` AS `valueStep`, v.`name` AS `name`");
		
		from("`VariableTrace` t");
		join("`Variable` v ON t.`variableId` = v.`id` AND t.`methodId` = v.`methodId`");
		where("call_EQ", "t.`testId` = ? AND t.`callStep` = ?");
		where("id_EQ", "t.`variableId` = ?");
		always().orderBy("t.`step`");
	}};
	
	public static class VarHistoryQuery extends GraphQuery<VariableValue> {

		public VarHistoryQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, VAR_MAPPING, VAR_HISTORY_TEMPLATE, view);
			select(fields);
		}
		
		public VarHistoryQuery where() {
			return this;
		}
		
		public VarHistoryQuery inCall(int testId, long callStep) {
			where("call_EQ", testId, callStep);
			return this;
		}
		
		public VarHistoryQuery byId(int id) {
			where("id_EQ", id);
			return this;
		}
	};
	
	private static final GraphQueryTemplate<FieldValue> OBJ_HISTORY_TEMPLATE = new NameValueQueryTemplate<FieldValue>() {{
		select("m.`name`, m.`id`");
		using("val")
			.select("val.`testId`, val.`step` AS `valueStep`, val.`step` AS `step`, val.`valueIsPut` AS `valueIsPut`");
		using("nextPut")
			.select("COALESCE(MIN(nextPut.`step`), -1) AS `nextChangeStep`");
		using("nextGet")
			.select("COALESCE(MIN(nextGet.`step`), -1) AS `nextGetStep`");
		
		/*JOIN 
		(SELECT `step`, `fieldId`, 1 AS `valueIsPut` FROM `PutTrace` WHERE `testId` = 0 AND `thisId` = 34 
		 UNION
		 SELECT `step`, `fieldId`, 0 AS `valueIsPut` FROM `GetTrace` WHERE `testId` = 0 AND `thisId` = 34 ) val
			ON val.`fieldId` = m.`id`
		LEFT OUTER JOIN `PutTrace` nextPut 
			ON nextPut.`fieldId` = m.`id` AND nextPut.`step` > val.`step` AND nextPut.`testId` = 0 AND nextPut.`thisId` = 34 

		LEFT OUTER JOIN `GetTrace` nextGet ON nextGet.`fieldId` = m.`id` AND nextGet.`step` >= val.`step` AND nextGet.`testId` = 0 AND nextGet.`thisId` = 34 
		 
		ORDER BY val.`step`*/
		
		from("`Field` m");
		
		join("val", 
			 "JOIN (SELECT `testId`, `step`, `fieldId`, 1 AS `valueIsPut` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? " +
			  "UNION " +
			  "SELECT `testId`, `step`, `fieldId`, 0 AS `valueIsPut` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ?) " + 
			 "val ON val.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN `PutTrace` nextPut " +
			 "ON nextPut.`fieldId` = m.`id` AND nextPut.`step` > val.`step` " +
			 "AND nextPut.`testId` = ? AND nextPut.`thisId` = ?");
		join("LEFT OUTER JOIN `GetTrace` nextGet " +
			 "ON nextGet.`fieldId` = m.`id` AND nextGet.`step` >= val.`step` " +
			 "AND nextGet.`testId` = ? AND nextGet.`thisId` = ?");
		
		where("id_EQ", "m.`id` = ?");
		
		always()
			.groupBy("m.`id`")
			.groupBy("val.`step`")
			.orderBy("val.`step`");
	}};
	
	public static class ObjHistoryQuery extends GraphQuery<FieldValue> {

		public ObjHistoryQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, FIELD_MAPPING, OBJ_HISTORY_TEMPLATE, view);
			select(fields);
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
		
		@Override
		protected String queryString() {
			System.out.println(super.queryString());
			return super.queryString();
		}
	};

	private static class SetStepAdapter implements EntityInitializer<NamedValue> {
		private final int testId;
		private final long step;
		public SetStepAdapter(int testId, long step) {
			super();
			this.testId = testId;
			this.step = step;
		}
		
		@Override
		public void apply(NamedValue entity) throws SQLException {
			entity.testId = testId;
			entity.step = step;
		}
		
		@Override
		public void complete() throws SQLException { }
		
		@Override
		public void close() throws SQLException { }
	}

}

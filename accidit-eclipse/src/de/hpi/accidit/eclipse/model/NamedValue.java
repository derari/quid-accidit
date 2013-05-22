package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.View;
import org.cthul.miro.graph.GraphQuery;
import org.cthul.miro.graph.GraphQueryTemplate;
import org.cthul.miro.graph.SelectByKey;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ValueAdapterBase;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;

public class NamedValue extends ModelBase {
	
	private int testId;
	private long step;
	private int id;
	private String name;
	private char primType;
	private long valueId;
	private Value value;
	
	public NamedValue() {
	}

	
	
	public NamedValue(String name) {
		super();
		this.name = name;
		primType = 'V';
	}

	public int getTestId() {
		return testId;
	}
	
	public long getStep() {
		return step;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		value = Value.newValue(primType, valueId, testId, step);
		value.beInitialized();
	}
	
	public Value getValue() {
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

	public static class VariableValue extends NamedValue { 
		
		private long callStep;
		
		public long getCallStep() {
			return callStep;
		}
		
	}
	
	public static final View<VarQuery> VARIABLE_VIEW = new QueryFactoryView<>(VarQuery.class);
	
	private static final Mapping<VariableValue> VAR_MAPPING = new ReflectiveMapping<VariableValue>(VariableValue.class) {
		protected void setField(VariableValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "primType":
				((NamedValue) record).primType = rs.getString(i).charAt(0);
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
	
	private static class NameValueQueryTemplate<E> extends GraphQueryTemplate<E> {{
	}}
	
	private static final GraphQueryTemplate<VariableValue> VAR_TEMPLATE = new NameValueQueryTemplate<VariableValue>() {{
		select("t.primType, t.valueId, m.name, m.id");
		from("VariableTrace t");
		join("(SELECT variableId, MAX(step) AS lastStep " +
			    "FROM VariableTrace " +
			    "WHERE testId = ? AND callStep = ? AND step < ? " +
			    "GROUP BY variableId) " +
			 "step ON t.variableId = step.variableId AND t.step = step.lastStep");
		join("Variable m ON t.methodId = m.methodId AND t.variableId = m.id");
		where("call_EQ", "t.testId = ? AND t.callStep = ?");
	}};

	public static class VarQuery extends GraphQuery<VariableValue> {

		public VarQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, VAR_MAPPING, VAR_TEMPLATE, view);
			select_keys(fields);
			orderBy("t.variableId");
		}
		
		public VarQuery where() {
			return this;
		}
		
		public VarQuery atStep(int testId, long callStep, long step) {
			put("step", testId, callStep, step);
			where_key("call_EQ", testId, callStep);
			adapter(new SetStepAdapter(testId, step));
			return this;
		}
	};
	
	public static final View<ItemQuery> ITEM_VIEW = new QueryFactoryView<>(ItemQuery.class);
	
	public static final View<FieldQuery> FIELD_VIEW = new QueryFactoryView<>(FieldQuery.class);
	
	private static final Mapping<FieldValue> FIELD_MAPPING = new ReflectiveMapping<FieldValue>(FieldValue.class) {
		protected void setField(FieldValue record, String field, java.sql.ResultSet rs, int i) throws SQLException {
			switch (field) {
			case "primType":
				((NamedValue) record).primType = rs.getString(i).charAt(0);
				break;
			case "isPut":
			case "hasGet":
			case "getIsCurrent":
				setField(record, field, rs.getInt(i) == 1);
				break;
			case "index":
				setField(record, "name", rs.getString(i));
				break;
			default:
				super.setField(record, field, rs, i);
			}
		};
	};
	
	private static final GraphQueryTemplate<FieldValue> FIELD_TEMPLATE = new NameValueQueryTemplate<FieldValue>() {{
		select("m.name, m.id");
		using("ensure_put_or_get")
			.select(// coalesce values from put or get table
//					"COALESCE(put.testId, get.testId) AS testId",
					"COALESCE(put.thisId, get.thisId) AS thisId",
					"COALESCE(put.primType, get.primType) AS primType",
					"COALESCE(put.valueId, get.valueId) AS valueId",
					// document source of values
					"IF(put.testId IS NOT NULL, 1, 0) AS isPut",
					"IF(get.testId IS NOT NULL, 1, 0) AS hasGet",
					"IF(nextPut.s IS NULL OR get.step < nextPut.s, 1, 0) AS getIsCurrent");
		from("Field m");
		// next get value, and step of next put
		join("get", "LEFT OUTER JOIN " +
						"(GetTrace `get`  " +
							"JOIN " +
								"(SELECT MIN(step) AS s, fieldId, testId " +
								   "FROM GetTrace " +
								   "WHERE testId = ? AND thisId = ? AND step >= ? " +
								   "GROUP BY fieldId) nextGet " +
							  "ON get.fieldId = nextGet.fieldId AND get.testId = nextGet.testId AND get.step = nextGet.s " +
							"LEFT OUTER JOIN  " +
								"(SELECT MIN(step) AS s, fieldId, testId " +
								 "FROM PutTrace " +
								 "WHERE testId = ? AND thisId = ? AND step >= ? " +
								 "GROUP BY fieldId) nextPut " +
							  "ON get.fieldId = nextPut.fieldId AND get.testId = nextPut.testId) " +
						"ON get.fieldId = m.id");
		// last put value
		join("put", "LEFT OUTER JOIN  " +
						"(PutTrace put " +
							"JOIN " +
								"(SELECT MAX(step) AS s, fieldId, testId " +
								   "FROM PutTrace " +
								   "WHERE testId = ? AND thisId = ? AND step < ? " +
								   "GROUP BY fieldId) lastPut " +
							  "ON put.fieldId = lastPut.fieldId AND put.testId = lastPut.testId AND put.step = lastPut.s) " +
	    				"ON put.fieldId = m.id");
		using("put", "get").
			where("ensure_put_or_get", "(get.testId IS NOT NULL OR put.testId IS NOT NULL)");
	}};
	
	public static class FieldQuery extends GraphQuery<FieldValue> {

		public FieldQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, FIELD_MAPPING, FIELD_TEMPLATE, view);
			select_keys(fields);
			orderBy("m.id");
		}
		
		public FieldQuery where() {
			return this;
		}
		
		public FieldQuery objectAtStep(int testId, long thisId, long step) {
			put("get", testId, thisId, step, testId, thisId, step);
			put("put", testId, thisId, step);
			adapter(new SetStepAdapter(testId, step));
			return this;
		}
	};
	
	private static final GraphQueryTemplate<FieldValue> ITEM_TEMPLATE = new NameValueQueryTemplate<FieldValue>() {{
		select("m.id AS thisId");
		using("ensure_put_or_get")
			.select(// coalesce values from put or get table
//					"COALESCE(put.testId, get.testId) AS testId",
					"COALESCE(put.index, get.index) AS `index`",
					"COALESCE(put.primType, get.primType) AS primType",
					"COALESCE(put.valueId, get.valueId) AS valueId",
					// document source of values
					"IF(put.testId IS NOT NULL, 1, 0) AS isPut",
					"IF(get.testId IS NOT NULL, 1, 0) AS hasGet",
					"IF(nextPut.s IS NULL OR get.step < nextPut.s, 1, 0) AS getIsCurrent");
		from("ObjectTrace m");
		// next get value, and step of next put
		join("get", "LEFT OUTER JOIN " +
						"(ArrayGetTrace `get`  " +
							"JOIN " +
								"(SELECT MIN(step) AS s, `index`, testId " +
								   "FROM ArrayGetTrace " +
								   "WHERE testId = ? AND thisId = ? AND step >= ? " +
								   "GROUP BY `index`) nextGet " +
							  "ON get.index = nextGet.index AND get.testId = nextGet.testId AND get.step = nextGet.s " +
							"LEFT OUTER JOIN  " +
								"(SELECT MIN(step) AS s, `index`, testId " +
								 "FROM ArrayPutTrace " +
								 "WHERE testId = ? AND thisId = ? AND step >= ? " +
								 "GROUP BY `index`) nextPut " +
							  "ON get.index = nextPut.index AND get.testId = nextPut.testId) " +
						"ON get.thisId = m.id");
		// last put value
		join("put", "LEFT OUTER JOIN  " +
						"(ArrayPutTrace put " +
							"JOIN " +
								"(SELECT MAX(step) AS s, `index`, testId " +
								   "FROM ArrayPutTrace " +
								   "WHERE testId = ? AND thisId = ? AND step < ? " +
								   "GROUP BY `index`) lastPut " +
							  "ON put.index = lastPut.index AND put.testId = lastPut.testId AND put.step = lastPut.s) " +
	    				"ON put.thisId = m.id");
		using("put", "get")
			.where("ensure_put_or_get", "(get.testId IS NOT NULL OR put.testId IS NOT NULL)")
			.where("array_EQ", "m.testId = ? AND m.id = ?");
	}};

	public static class ItemQuery extends GraphQuery<FieldValue> {

		public ItemQuery(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, FIELD_MAPPING, ITEM_TEMPLATE, view);
			select_keys(fields);
			orderBy("`index`");
		}
		
		public ItemQuery where() {
			return this;
		}
		
		public ItemQuery objectAtStep(int testId, long thisId, long step) {
			put("get", testId, thisId, step, testId, thisId, step);
			put("put", testId, thisId, step);
			put("array_EQ", testId, thisId);
			adapter(new SetStepAdapter(testId, step));
			return this;
		}
		
		@Override
		protected String queryString() {
			System.out.println(super.queryString());
			return super.queryString();
		}
	};
	
	/*
SELECT m.name, m.id,
		COALESCE(put.testId, get.testId) AS testId,
        COALESCE(put.primType, get.primType) AS primType,
        COALESCE(put.valueId, get.valueId) AS valueId,
		IF(put.testId IS NOT NULL, 1, 0) AS isPut,
		IF(get.testId IS NOT NULL, 1, 0) AS hasGet,
		IF(nextPut.s IS NULL OR get.step < nextPut.s, 1, 0) AS getIsCurrent,
FROM Field m 
LEFT OUTER JOIN 
	(GetTrace get 
		JOIN
			(SELECT MIN(step) AS s, fieldId, testId
			 FROM GetTrace
			 WHERE testId = 1 AND thisId = 212 AND step >= 5119
			 GROUP BY fieldId) nextGet
		  ON get.fieldId = nextGet.fieldId AND get.testId = nextGet.testId AND get.step = nextGet.s
		LEFT OUTER JOIN 
			(SELECT MIN(step) AS s, fieldId, testId
			 FROM PutTrace
			 WHERE testId = 1 AND thisId = 212 AND step >= 5119
			 GROUP BY fieldId) nextPut
		  ON get.fieldId = nextPut.fieldId AND get.testId = nextPut.testId)
    ON get.fieldId = m.id
LEFT OUTER JOIN 
	(PutTrace put 
		JOIN
		(SELECT MAX(step) AS s, fieldId, testId
			FROM PutTrace
			WHERE testId = 1 AND thisId = 212 AND step < 5119
			GROUP BY fieldId) lastPut
		  ON put.fieldId = lastPut.fieldId AND put.testId = lastPut.testId AND put.step = lastPut.s)
    ON put.fieldId = m.id
WHERE (get.testId IS NOT NULL OR put.testId IS NOT NULL)
ORDER BY m.id
	 */
	
	private static class SetStepAdapter extends ValueAdapterBase<NamedValue> {
		private final int testId;
		private final long step;
		public SetStepAdapter(int testId, long step) {
			super();
			this.testId = testId;
			this.step = step;
		}
		@Override
		public void initialize(ResultSet rs) throws SQLException {
		}
		@Override
		public void apply(NamedValue entity) throws SQLException {
			entity.testId = testId;
			entity.step = step;
		}
		@Override
		public void complete() throws SQLException {
		}
		@Override
		public void close() throws SQLException {
		}		
	}
	
}

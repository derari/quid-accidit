package de.hpi.accidit.eclipse.model;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ResultBuilder.ValueAdapter;
import org.cthul.miro.util.MappedQuery;
import org.cthul.miro.util.QueryView;
import org.cthul.miro.util.ReflectiveMapping;

import de.hpi.accidit.eclipse.DatabaseConnector;


public abstract class Value extends ModelBase {
	
	public abstract String getShortString();
	
	public abstract String getLongString();
	
	public abstract boolean hasChildren();
	
	public abstract NamedValue[] getChildren();
	
	/**
	 * Shows temporary children.
	 */
	public NamedValue[] previewChildren() {
		return null;
	}
	
	public void updateChildren(long step, Callback<? super NamedValue> updateCallback) {
		if (!isInitialized()) return;
		if (!hasChildren()) return;
		for (NamedValue nv: getChildren()) {
			nv.updateValue(step, updateCallback);
		}
	}
	
	public boolean needsUpdate(long step) {
		if (!isInitialized()) return false;
		if (!hasChildren()) return false;
		for (NamedValue nv: getChildren()) {
			if (nv.needsUpdate(step)) {
				reInitialize();
				return true;
			}
		}
		return false;
	}
	
	public static class Primitive extends Value {
		
		private final char primType;
		private final String value;
		private final long valueId;
		
		public Primitive(Object value) {
			this.value = String.valueOf(value);
			primType = '?';
			valueId = 0;
		}

		public Primitive(char primType, long value) {
			this.primType = primType;
			this.value = stringValue(primType, value);
			this.valueId = value;
		}
		
		private String stringValue(char primType, long valueId) {
			switch(primType) {
			case 'Z': return String.valueOf(valueId == 1); // boolean
			case 'B': return String.valueOf((byte) valueId); // byte
			case 'C': return String.valueOf((char) valueId); // char
			case 'D': return String.valueOf(Double.longBitsToDouble(valueId)); // double
			case 'F': return String.valueOf(Float.intBitsToFloat((int) valueId)); // float
			case 'I': return String.valueOf(valueId); // int
			case 'J': return String.valueOf(valueId); // long
			case 'S': return String.valueOf((short) valueId); // short
			default: return "null";
			}
		}
		
		public long getValueId() {
			return valueId;
		}

		@Override
		public String getLongString() {
			return value;
		}
		
		@Override
		public String getShortString() {
			return value;
		}
		
		@Override
		public boolean hasChildren() {
			return false;
		}
		
		@Override
		public NamedValue[] getChildren() {
			return new NamedValue[0];
		}
	}

	public static abstract class ValueWithChildren extends Value {
		
		protected NamedValue[] children;
		private boolean[] updateNeeded = null;
		private volatile boolean isInitializing = false;

		@Override
		public boolean hasChildren() {
			return children != null && children.length > 0;
		}

		@Override
		public NamedValue[] getChildren() {
			if (!beInitialized() || !isInitSuccess()) {
				Throwable t = getInitException();
				if (t != null) {
					t.printStackTrace(System.err);
					NamedValue nv = new NamedValue(t.getMessage());
					children = new NamedValue[]{nv};
				} else {
					children = new NamedValue[]{new NamedValue("-no data-")};
				}
			}
			return children;
		}
		
		@Override
		public NamedValue[] previewChildren() {
			return children;
		}
		
		@Override
		public boolean needsUpdate(long step) {
			if (isInitializing) {
				// we would have to block to wait for a result,
				// assume update is needed instead
				reInitialize(); // stop current execution
				return true;
			}
			boolean valueUpdate = false;
			if (!isInitialized()) return false;
			if (!hasChildren()) return false;
			if (updateNeeded == null) return true;
			for (int i = 0; i < children.length; i++) {
				boolean u = children[i].needsUpdate(step);
				updateNeeded[i] = u;
				valueUpdate |= u;
			}
			if (valueUpdate) {
				reInitialize();
			}
			return valueUpdate;
		}
		
		@Override
		protected void lazyInitialize() throws Exception {
			isInitializing = true;
			try {
				NamedValue[] newChildren = fetchChildren();
				if (updateNeeded == null) {
					children = newChildren;
					updateNeeded = new boolean[children.length];
				} else {
					for (int i = 0; i < updateNeeded.length; i++) {
						if (updateNeeded[i]) {
							children[i] = newChildren[i];
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
				NamedValue nv = new NamedValue(e.getMessage());
				children = new NamedValue[]{nv};
				updateNeeded = null;
			} finally {
				isInitializing = false;
			}
		}
		
		protected abstract NamedValue[] fetchChildren() throws Exception;
	}
	
	public static class ObjectSnapshot extends ValueWithChildren {
		
		private int testId = -1;
		private long step = -1;
		private int typeId = -1;
		private Integer arrayLength;
		private String typeName;
		public final long thisId;
		private String longName;
		
		public ObjectSnapshot() {
			this(-1, -1);
		}
		
		public ObjectSnapshot(int testId, long id) {
			this.testId = testId;
			this.thisId = id;
		}
		
		public Integer getArrayLength() {
			return arrayLength;
		}
		
		public String getTypeName() {
			return typeName;
		}
		
		public long getThisId() {
			return thisId;
		}
		
		@Override
		public String getShortString() {
			//beInitialized();
			return "#" + thisId;
		}
		
		@Override
		public String getLongString() {
			beInitialized();
			return longName;
		}
		
		@Override
		public boolean hasChildren() {
			return children == null || children.length > 0;
		}
		
		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			if (arrayLength == null) {
				return DatabaseConnector.cnn()
						.select().from(NamedValue.FIELD_VIEW)
						.where().atStep(testId, thisId, step)
						.asArray().execute();
			} else {
				return DatabaseConnector.cnn()
						.select().from(NamedValue.ARRAY_ITEM_VIEW)
						.where().atStep(testId, thisId, step)
						.asArray().execute();
			}
		}
		
		@Override
		protected void lazyInitialize() throws Exception {
			super.lazyInitialize();
			longName = ValueToString.getLongName(this, children);
		}
		@Override
		public boolean needsUpdate(long step) {
			this.step = step;
			return super.needsUpdate(step);
		}
		@Override
		public void updateChildren(long step, Callback<? super NamedValue> updateCallback) {
			this.step = step;
			super.updateChildren(step, updateCallback);
		}
		
	}
	
	public static class MethodSnapshot extends ValueWithChildren {

		private int testId;
		private long callStep;
		private long step;
		
		public MethodSnapshot(int testId, long callStep, long step) {
			super();
			this.testId = testId;
			this.callStep = callStep;
			this.step = step;
		}

		@Override
		public String getShortString() {
			return "-";
		}

		@Override
		public String getLongString() {
			return "-";
		}

		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			Value thisValue = cnn().select()
					.from(this_inInvocation(testId, callStep, step))
					.getSingle().execute();
			NamedValue[] c = cnn().select()
					.from(NamedValue.VARIABLE_VIEW)
					.where().atStep(testId, callStep, step)
					.asArray().execute();
			if (!(thisValue instanceof ObjectSnapshot)) {
				// static method: `this` is null
				return c;
			}
			NamedValue[] c2 = new NamedValue[c.length+1];
			c2[0] = new NamedValue("this", thisValue);
			System.arraycopy(c, 0, c2, 1, c.length);
			return c2;
		}
		@Override
		public boolean needsUpdate(long step) {
			this.step = step;
			return super.needsUpdate(step);
		}		
		@Override
		public void updateChildren(long step, Callback<? super NamedValue> updateCallback) {
			this.step = step;
			super.updateChildren(step, updateCallback);
		}
	}
	
	public static abstract class ValueHistory extends Value {
		
	}
	
	public static class VariableHistory extends ValueWithChildren {
		
		private int testId;
		private long callStep;
		private int varId;
		
		public VariableHistory(int testId, long callStep, int varId) {
			super();
			this.testId = testId;
			this.callStep = callStep;
			this.varId = varId;
		}

		@Override
		public String getLongString() {
			return "-";
		}
		
		@Override
		public String getShortString() {
			return "-";
		}
		
		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			return DatabaseConnector.cnn()
					.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
					.where().inCall(testId, callStep).byId(varId)
					.asArray().execute();
		}
		
	}
	
	
	private static Value newValue(int testId, char primType, long valueId) {
		if (primType == 'L' && valueId != 0) {
			return new ObjectSnapshot(testId, valueId);
		} else {
			return new Primitive(primType, valueId);
		}
	}
	
	public static View<MappedQuery<Value>> this_inInvocation(int testId, long callStep, long step) {
		return new QueryView<>(MAPPING, 
					"SELECT t.`testId`, 'L' AS `primType`, COALESCE(t.`thisId`, 0) AS `valueId`, o.`arrayLength`, y.`name` AS `typeName` " +
					"FROM `CallTrace` t " +
					"LEFT OUTER JOIN `ObjectTrace` o " +
					  "ON t.`testId` = o.`testId` AND t.`thisId` = o.`id`" +
					"LEFT OUTER JOIN `Type` y " +
					  "ON y.`id` = o.`typeId` " +
					"WHERE t.`testId` = ? AND t.`step` = ?", 
					testId, callStep)
			.adapters(new SetStepAdapter(step));
	}
	
	public static View<MappedQuery<Value>> ofVariable(int varId, int testId, long valueStep, long step) {
		return new ValueQuery("VariableTrace", "variableId", varId, testId, valueStep)
					.adapters(new SetStepAdapter(step));
	}

	public static View<MappedQuery<Value>> ofField(boolean put, int fieldId, int testId, long valueStep, long step) {
		return new ValueQuery(put ? "PutTrace" : "GetTrace", "fieldId", fieldId, testId, valueStep)
					.adapters(new SetStepAdapter(step));
	}
	
	public static View<MappedQuery<Value>> ofArray(boolean put, int index, int testId, long valueStep, long step) {
		return new ValueQuery(put ? "ArrayPutTrace" : "ArrayGetTrace", "index", index, testId, valueStep)
					.adapters(new SetStepAdapter(step));
	}

	private static final String[] C_PARAMS = {"testId", "primType", "valueId"};
	
	private static final Mapping<Value> MAPPING = new ReflectiveMapping<Value>((Class) ObjectSnapshot.class) {
		
		protected String[] getConstructorParameters() {
			return C_PARAMS;
		};
		protected Value newRecord(Object[] args) {
			int testId = (Integer) args[0];
			char primType = ((String) args[1]).charAt(0);
			long valueId = (Long) args[2];
			return newValue(testId, primType, valueId);
		}
		protected void injectField(Value record, String field, Object value) throws SQLException {
			if (record instanceof Primitive) return;
			for (String s: C_PARAMS) {
				if (field.equals(s)) return;
			}
			super.injectField(record, field, value);
		};
	};
	
	protected static class ValueQuery extends QueryView<Value> {

		public ValueQuery(String table, String idField, int id, int testId, long step) {
			super(MAPPING, 
					"SELECT t.`testId`, t.`primType`, t.`valueId`, o.`arrayLength`, y.`name` AS `typeName` " +
					"FROM `" + table + "` t " +
					"LEFT OUTER JOIN `ObjectTrace` o " +
					"ON t.`primType` = 'L' AND t.`testId` = o.`testId` AND t.`valueId` = o.`id` " +
					"LEFT OUTER JOIN `Type` y " +
					"ON y.`id` = o.`typeId` " +
					"WHERE t.`" + idField + "` = ? " +
					  "AND t.`testId` = ? AND t.`step` = ?", 
				id, testId, step);
		}
	}
		
	private static class SetStepAdapter implements ValueAdapter<Value> {
		
		private final long step;
		
		public SetStepAdapter(long step) {
			this.step = step;
		}

		@Override
		public void initialize(ResultSet rs) throws SQLException {
		}

		@Override
		public void apply(Value entity) throws SQLException {
			if (entity instanceof ObjectSnapshot) {
				((ObjectSnapshot) entity).step = step;
			}
		}

		@Override
		public void complete() throws SQLException {
		}

		@Override
		public void close() throws SQLException {
		}
		
	}

}

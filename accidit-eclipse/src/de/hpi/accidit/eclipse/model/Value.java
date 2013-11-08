package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.MappedQueryString;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.ObjHistoryQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.VarHistoryQuery;
import de.hpi.accidit.eclipse.model.db.ValueDao;


public abstract class Value extends ModelBase {
	
	public static View<MappedQueryString<Value>> this_inInvocation(int testId, long callStep, long step) {
		return ValueDao.this_inInvocation(testId, callStep, step);
	}
	
	public static View<MappedQueryString<Value>> ofVariable(int varId, int testId, long valueStep, long step) {
		return ValueDao.ofVariable(varId, testId, valueStep, step);
	}

	public static View<MappedQueryString<Value>> ofField(boolean put, int fieldId, int testId, long valueStep, long step) {
		return ValueDao.ofField(put, fieldId, testId, valueStep, step);
	}
	
	public static View<MappedQueryString<Value>> ofArray(boolean put, int index, int testId, long valueStep, long step) {
		return ValueDao.ofArray(put, index, testId, valueStep, step);
	}
	
	public Value(MiConnection cnn) {
		super(cnn);
	}
	
	public Value() {
	}

	public abstract String getShortString();
	
	public abstract String getLongString();
	
	public abstract boolean hasChildren();
	
	public abstract NamedValue[] getChildren();
	
	public boolean updateNeeded(long newStep) {
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
		
		public ValueWithChildren() {
		}
		
		public ValueWithChildren(MiConnection cnn) {
			super(cnn);
		}
		
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
		
//		@Override
//		public NamedValue[] previewChildren() {
//			return children;
//		}
		
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
			if (longName == null) {
				longName = ValueToString.getLongName(this, children);
			}
			return longName;
		}
		
		@Override
		public boolean hasChildren() {
			return children == null || children.length > 0;
		}
		
		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			NamedValue[] c;
			if (arrayLength == null) {
				c = DatabaseConnector.cnn()
						.select().from(NamedValue.FIELD_VIEW)
						.where().atStep(testId, thisId, step)
						.asArray().execute();
			} else {
				c = DatabaseConnector.cnn()
						.select().from(NamedValue.ARRAY_ITEM_VIEW)
						.where().atStep(testId, thisId, step)
						.asArray().execute();
			}
			for (NamedValue n: c) {
				n.setOwner(this);
			}
			return c;
		}
		
		@Override
		protected void lazyInitialize() throws Exception {
			super.lazyInitialize();
		}
		
		@Override
		public boolean updateNeeded(long newStep) {
			step = newStep;
			if (!isInitialized()) {
				return false;
			}
			for (NamedValue nv: getChildren()) {
				if (nv.updateNeeded(newStep)) {
					return true;
				}
			}
			return false;
		}
	}
	
	public static class MethodSnapshot extends ValueWithChildren {

		private int testId;
		private long callStep;
		private long step;
		
		public MethodSnapshot(MiConnection cnn, int testId, long callStep, long step) {
			super(cnn);
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
		public boolean updateNeeded(long newStep) {
			step = newStep;
			if (!isInitialized()) {
				return false;
			}
			for (NamedValue nv: getChildren()) {
				if (nv.updateNeeded(newStep)) {
					return true;
				}
			}
			return false;
		}
	}
	
//	public static abstract class ValueHistory extends Value {
//		
//	}
	
	public static class VariableHistory extends ValueWithChildren {
		
		private int testId;
		private long callStep;
		private int varId;
		
		public VariableHistory(MiConnection cnn, int testId, long callStep, int varId) {
			super(cnn);
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
			VarHistoryQuery qry = cnn()
					.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
					.where().inCall(testId, callStep);
			if (varId > -1) {
				qry.byId(varId);
			}
			return qry.asArray().execute();
		}
	}
	
	public static class ObjectHistory extends ValueWithChildren {
		
		private int testId;
		private long thisId;
		private int fieldId;
		
		public ObjectHistory(MiConnection cnn, int testId, long thisId, int fieldId) {
			super(cnn);
			this.testId = testId;
			this.thisId = thisId;
			this.fieldId = fieldId;
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
			ObjHistoryQuery qry = cnn()
					.select().from(NamedValue.OBJECT_HISTORY_VIEW)
					.where().ofObject(testId, thisId);
			if (fieldId > -1) {
				qry.byId(fieldId);
			}
			return qry.asArray().execute();
		}
	}

	public static Value newValue(int testId, char primType, long valueId) {
		if (primType == 'L' && valueId != 0) {
			return new ObjectSnapshot(testId, valueId);
		} else {
			return new Primitive(primType, valueId);
		}
	}	
}

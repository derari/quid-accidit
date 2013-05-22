package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.View;
import org.cthul.miro.graph.GraphQuery;
import org.cthul.miro.graph.GraphQueryTemplate;
import org.cthul.miro.graph.SelectByKey;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;

import de.hpi.accidit.eclipse.DatabaseConnector;


public abstract class Value extends ModelBase {
	
	public abstract String getShortString();
	
	public abstract String getLongString();
	
	public abstract boolean hasChildren();
	
	public abstract NamedValue[] getChildren();
	
	public static class Primitive extends Value {
		
		private final char primType;
		private final String value;
		
		public Primitive(Object value) {
			this.value = String.valueOf(value);
			primType = '?';
		}

		public Primitive(char primType, long value) {
			this.primType = primType;
			this.value = stringValue(primType, value);
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

	public static class ObjectSnapshot extends Value {
		
		private int testId = -1;
		private long step = -1;
		private int typeId = -1;
		private Integer arrayLength;
		private String typeName;
		public final long thisId;
		private NamedValue[] children;
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
		protected void lazyInitialize() throws Exception {
			if (arrayLength == null) {
				children = DatabaseConnector.cnn()
						.select().from(NamedValue.FIELD_VIEW)
						.where().objectAtStep(testId, thisId, step)
						.asArray()._execute();
			} else {
				children = DatabaseConnector.cnn()
						.select().from(NamedValue.ITEM_VIEW)
						.where().objectAtStep(testId, thisId, step)
						.asArray()._execute();
			}
			longName = ValueToString.getLongName(this, children);
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
	}
	
	public static Value newValue(char primType, long valueId, int testId) {
		if (primType == 'L') {
			if (valueId == 0) return new Primitive('V', 0);
			Value v = DatabaseConnector.cnn()
					.select().from(VIEW)
					.byId(testId, valueId)
					.getSingle()._execute();
			if (v == null) {
				new Exception("no data: " + testId + " #" + valueId).printStackTrace(System.err);
				v = new Primitive("-no data-");
			}
			return v;
		} else {
			return new Primitive(primType, valueId);
		}
	};

	public static Value newValue(char primType, long valueId, int testId, long step) {
		Value v = newValue(primType, valueId, testId);
		if (v instanceof ObjectSnapshot) {
			ObjectSnapshot o = (ObjectSnapshot) v;
			o.testId = testId;
			o.step = step;
		}
		return v;
	};

	
	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final String[] C_PARAMS = {"thisId", "testId"};
	
	private static final Mapping<Value> MAPPING = new ReflectiveMapping<Value>((Class) ObjectSnapshot.class) {
		
		protected String[] getConstructorParameters() {
			return C_PARAMS;
		};
		protected Value newRecord(Object[] args) {
			long valueId = (Long) args[0];
			int testId = (Integer) args[1];
			return new ObjectSnapshot(testId, valueId);
		}
	};
	
	private static GraphQueryTemplate<Value> TEMPLATE = new GraphQueryTemplate<Value>() {{
		keys("testId", "t.id");
		select("t.testId, t.id AS thisId, t.typeId, t.arrayLength, m.name AS typeName");
		from("ObjectTrace t");
		join("Type m ON t.typeId = m.id");
		where("this_EQ", "testId = ? AND t.id = ?");
	}};
	
	public static class Query extends GraphQuery<Value> {

		public Query(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, MAPPING, TEMPLATE, view);
			select_keys(fields);
		}
		
		public Query where() {
			return this;
		}
		
		public Query byId(int testId, long thisId) {
			where_key("this_EQ", testId, thisId);
			return this;
		}
		
	}
	
}

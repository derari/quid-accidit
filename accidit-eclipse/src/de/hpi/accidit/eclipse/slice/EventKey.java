package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.Method;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.ValueToString;

public class EventKey implements Comparable<EventKey> {
	
	protected final InvocationData invD;
	protected final long step;
	protected Value value;
	protected Boolean isInternal = null;
	
	public EventKey(InvocationData invD, long step) {
		this.invD = invD;
		this.step = step;
	}

//	public DataDependency getDataDependency() {
//		Token t = asToken();
//		if (t == null) return DataDependency.constant();
//		return getDependencyGraph().get(t);
//	}
	
//	public Map<Token, DataDependency> getDependencyGraph() {
//		return invD.getDependencyGraph();
//	}
	
	public String getMethodId() {
		return invD.getMethodId();
	}
	
	public Invocation getInvocation() {
		return invD.getInvocation();
	}
	
	public long getStep() {
		return step;
	}
	
	protected InstructionKey getInstruction() {
		return null;
	}
	
	public Value getValue() {
		return value;
	}
	
	public void setValue(Value value) {
		this.value = value;
	}
	
	public boolean isInternalCode() {
		if (isInternal == null) {
			Method m = getInvocation().quickGetMethod();
			isInternal = m.type.startsWith("java.") 
					|| m.type.startsWith("javax.") 
					|| m.type.startsWith("sun.");
		}
		return isInternal;
	}
	
	public boolean isInternal() {
		return isInternalCode();
	}

	@Override
	public int compareTo(EventKey o) {
		if (o == this) return 0;
		long cStep = (step - o.step);
		int c = cStep < 0 ? -1 : (cStep > 0 ? 1 : 0);
		if (c != 0) return c;
		c = getClass().getName().compareTo(o.getClass().getName());
		if (c != 0) {
			if (this instanceof InvocationKey) return -1;
			if (o instanceof InvocationKey) return 1;
//			if (this instanceof VariableValueKey) return 1;
//			if (o instanceof VariableValueKey) return -1;
//			if (this instanceof InvocationArgKey) return 1;
//			if (o instanceof InvocationArgKey) return -1;
			return c;
		}
		return specificCompareTo(o);
	}
	
	@Override
	public int hashCode() {
		return (int) step;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof EventKey)) return false;
		return ((EventKey) obj).compareTo(this) == 0;
	}
	
	protected int specificCompareTo(EventKey o) {
		return 0; //toString().compareTo(o.toString());
	}
	
	public VariableValueKey newVariableKey(String variable, CodeIndex ci, long step) {
		return new VariableValueKey(invD, step, variable, ci);
	}
	
	public ArrayItemValueKey newArrayKey(long thisId, int index, long getStep) {
		return new ArrayItemValueKey(invD, getStep, thisId, index);
	}
	
//	public FieldValueKey newFieldKey(long thisId, String field, long getStep) {
//		return new FieldValueKey(invD, getStep, thisId, field);
//	}
	
	public FieldValueKey newFieldKey(FieldValue fieldValue) {
		return new FieldValueKey(invD, fieldValue);
	}
	
	public MethodResultKey newResultKey(String clazz, String method, String sig, long step) {
		return MethodResultKey.invocationBefore(invD, clazz, method, sig, step);
	}
	
	public InvocationKey getInvocationKey() {
		return invD.getInvocationKey();
	}
	
	public InvocationArgKey getInvocationArgumentKey(int index) {
		return getInvocationKey().getArg(index);
	}
	
	public InvocationThisKey getInvocationThisKey() {
		return getInvocationKey().getThis();
	}
	
	public InvocationData getInvD() {
		return invD;
	}
	
	public String getUserString() {
		return toString();
	}
	
	@Override
	public String toString() {
		String s;
		InstructionKey t = getInstruction();
		if (t != null) s = t.toString();
		else s = super.toString();
		s = getStep() + "/" + s;
		s = detailString(s);
		String vs = getValueString();
		if (!vs.isEmpty()) s += " = " + vs;
		return s;
	}
	
	protected String detailString(String s) {
		return s;
	}
	
	protected String getValueString() {
		if (value != null) {
			value.beInitialized();
			return value.getLongString();
		}
		return "";
	}
	
	public static class InvocationKey extends EventKey {
		
		private List<InvocationArgKey> args = new ArrayList<>(8);
		private InvocationThisKey thisKey = null;
		private InvocationAndArgsKey allKey = null;
		private Invocation inv;
		private Boolean isUtilMethod = null;

		public InvocationKey(int testId, long step) {
			this(new InvocationData(invocationAtStep(testId, step)));
		}
		
		public InvocationKey(InvocationData invD) {
			this(invD.getParent(), invD);
		}
		
		private InvocationKey(InvocationData parent, InvocationData invD) {
			super(parent != null ? parent : invD, invD.getInvocation().getStep());
			inv = invD.getInvocation();
			invD.invKey = this;
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return InstructionKey.invoke(inv.type, inv.method, inv.signature, CodeIndex.anyAtLine(inv.line));
		}
		
		public InvocationAndArgsKey allArgs() {
			if (allKey == null) {
				allKey = new InvocationAndArgsKey(this);
			}
			return allKey;
		}
		
		public InvocationArgKey getArg(int index) {
			while (args.size() <= index) {
				args.add(new InvocationArgKey(invD, step, args.size(), inv));
			}
			return args.get(index);
		}
		
		public Invocation getThisInvocation() {
			return inv;
		}
		
		public InvocationThisKey getThis() {
			if (thisKey == null) {
				thisKey = new InvocationThisKey(invD, step, inv);
				thisKey.invK = this;
			}
			return thisKey;
		}
		
		public String getMethodKey() {
			return InstructionKey.methodKey(inv.type, inv.method, inv.signature);
		}
		
		@Override
		public boolean isInternal() {
			if (isUtilMethod == null) {
				Method m = inv.quickGetMethod();
				isUtilMethod = m.type.startsWith("java.")
						&& (m.name.equals("valueOf") || m.name.endsWith("Value"));
			}
			if (isUtilMethod) return true;
			return super.isInternal();
		}
		
		@Override
		public String getUserString() {
			String t = inv.type;
			t = t.substring(t.lastIndexOf('.')+1);
			if (inv.thisId != null) {
				t = "<" + t + " #" + inv.thisId + ">";
			}
			String m = inv.method;
			String sig = inv.signature;
			sig = sig.substring(0, sig.indexOf(')')+1);
			return t + "." + m + sig;
		}
	}
	
	public static class InvocationThisKey extends EventKey {
		
		private Invocation inv;
		private InvocationKey invK = null;

		public InvocationThisKey(InvocationData invD, long step) {
			this(invD, step, invD.getInvocation());
		}
		
		public InvocationThisKey(InvocationData invD, long step, Invocation inv) {
			super(invD, step);
			this.inv = inv;
		}
		
		public InvocationKey getThisInvocation() {
			if (invK == null) {
				invK = invD.getInvocationAtCall(inv.getCallStep()).invKey;
			}
			return invK;
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return InstructionKey.invokeThis(inv.type, inv.method, inv.signature, CodeIndex.anyAtLine(inv.line));
		}
		
		@Override
		public boolean isInternal() {
			return getThisInvocation().isInternal();
//			if (!getInvocationKey().isInternal()) return false;
//			return super.isInternal();
		}
		
		@Override
		public String getUserString() {
			return "this = " + getValueString();
		}
	}

	public static class InvocationArgKey extends EventKey {
		
		private int index;
		private Invocation inv;

		public InvocationArgKey(InvocationData invD, long step, int index, Invocation inv) {
			super(invD, step);
			this.index = index;
			this.inv = inv;
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return InstructionKey.invokeArg(inv.type, inv.method, inv.signature, index, CodeIndex.anyAtLine(inv.line));
		}
		
		public InvocationKey getThisInvocation() {
			return invD.getInvocationAtCall(inv.getStep()).getInvocationKey();
		}
		
		@Override
		protected int specificCompareTo(EventKey o) {
			InvocationArgKey a = (InvocationArgKey) o;
			return Integer.compare(index, a.index);
		}
		
		@Override
		public boolean isInternal() {
			return getThisInvocation().isInternal();
		}
		
		@Override
		public String getUserString() {
			return "arg" + index + " = " + getValueString();
		}
	}
	
	public static class InvocationAndArgsKey extends EventKey {
		
		private final InvocationKey invKey;

		public InvocationAndArgsKey(InvocationKey invKey) {
			super(invKey.getInvD(), invKey.getStep());
			this.invKey = invKey;
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return invKey.getInstruction();
		}
		
		@Override
		protected int specificCompareTo(EventKey o) {
			return invKey.compareTo(((InvocationAndArgsKey) o).invKey);
		}
		
		@Override
		public String toString() {
			return invKey.toString() + "[inv-and-args]";
		}
		
		public InvocationKey getThisInvocation() {
			return invKey;
		}

		public String getMethodKey() {
			return invKey.getMethodKey();
		}
	}
	
	public static class MethodResultKey extends EventKey {
		
		public MethodResultKey(int testId, long step) {
			super(new InvocationData(invocationAtExitStep(testId, step)), step);
		}
		
//		private MethodResultKey(InvocationData parent, long exitStep, int callLine) {
//			this(parent.getInvocationBefore(exitStep, callLine));
//		}
		
		public static MethodResultKey invocationBefore(InvocationData parent, String clazz, String method, String sig, long exitStep) {
			InvocationData invD = parent.getInvocationBefore(clazz, method, sig, exitStep);
			if (invD == null) return null;
			return new MethodResultKey(invD);
		}
		
		private MethodResultKey(InvocationData invD) {
			super(invD, invD.getInvocation().exitStep);
			Invocation inv = getInvocation();
			Value v = DSL.select()
					.from(Value.result_ofInvocation(inv.getTestId(), inv.getStep()))
					._execute(cnn());
			setValue(v);
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return InstructionKey.result(CodeIndex.anyAtLine(getInvocation().exitLine));
		}
		
		@Override
		public boolean isInternal() {
//			if (!getInvocationKey().isInternal()) return false;
//			return super.isInternal();
			return getInvocationKey().isInternal();
		}
		
		@Override
		public String getUserString() {
			Invocation inv = getInvocation();
			String t = inv.type;
			t = t.substring(t.lastIndexOf('.')+1);
			if (inv.thisId != null) {
				t = "<" + invD.getObject(inv.thisId).getShortString() + ">";
			}
			String method = getInvocation().method;
			method = method.substring(method.indexOf('#') + 1);
			return t + "." + method + "() \u21B5 " + getValueString();
		}
	}
	
	public static class VariableValueKey extends EventKey {
		
		private String variable;
		private CodeIndex ci;
		
		public VariableValueKey(InvocationData invD, long step, String variable, CodeIndex ci) {
			super(invD, step);
			this.variable = variable;
//			if (line < 0) {
//				//invD.getInvocation().quickGetMethod().
//			}
			this.ci = ci;
		}
		
		@Override
		protected InstructionKey getInstruction() {
			return InstructionKey.variable(variable, ci);
		}
		
		@Override
		protected int specificCompareTo(EventKey o) {
			int c = ci.compareTo(((VariableValueKey) o).ci);
			if (c != 0) return c;
			return variable.compareTo(((VariableValueKey) o).variable);
		}		
	}
	
	public static class FieldValueKey extends EventKey {
		
		private long thisId = -1;
		private String field;
		private int line;
		
//		public FieldValueKey(InvocationData invD, long getStep, long thisId, String field) {
//			this(invD, fieldSetBefore(invD.getInvocation().getTestId(), thisId, field, getStep));
//		}
		
		public FieldValueKey(InvocationData invD, FieldValue fv) {
			this(invD, fv.isPut() ? fv : fieldSetBefore(invD.getInvocation().getTestId(), 
										 fv.getThisId(), fv.getName(), fv.getStep()), 
				 true);
		}
		
		private FieldValueKey(InvocationData invD, FieldValue fv, boolean x) {
			super(invD.getInvocationAtCall(fv != null ? fv.getCallStep() : 0), 
					fv != null ? fv.getStep() : 0);
			if (fv != null) {
				line = fv.getLine();
				field = fv.getName();
				thisId = fv.getThisId();
			} else {
				line = -1;
			}
		}

		@Override
		protected InstructionKey getInstruction() {
			if (line < 0) {
//				System.out.println(".");
//				return null;
			}
			return InstructionKey.field(field, CodeIndex.anyAtLine(line));
		}
		
		@Override
		protected String detailString(String s) {
			if (thisId <= 0) return s;
			int i = s.lastIndexOf('>')+1;
			return s.substring(0, i) + "#" + thisId + "." + s.substring(i);
		}
		
		@Override
		protected int specificCompareTo(EventKey o) {
			if (((FieldValueKey) o).field == null) {
				return field == null ? 0 : 1;
			}
			if (field == null) return -1;
			return field.compareTo(((FieldValueKey) o).field);
		}
		
		@Override
		public boolean isInternal() {
			if (step == 0) return true;
			return super.isInternal();
		}
		
		@Override
		public String getUserString() {
			
//			String t = inv.type;
//			t = t.substring(t.lastIndexOf('.')+1);
			String s = "";
			if (thisId > 0) {
				String shortStr = invD.getObject(thisId).getShortString();
				s = "<" + shortStr + ">";
//				int i = shortStr.lastIndexOf('.');
//				s = "<" + (i < 0 ? shortStr : shortStr.substring(i+1)) + ">";
			}			
			if (field != null) {
				s += "." + field + " = ";
			}
			s += getValueString();
			return s;
//			return toString();
		}
	}
	
	public static class ArrayItemValueKey extends EventKey {
		
		private long thisId;
		private int index;
		private int line;
		
		public ArrayItemValueKey(InvocationData invD, long getStep, long thisId, int index) {
			this(invD, arraySetBefore(invD.getInvocation().getTestId(), thisId, index, getStep));
		}
		
		private ArrayItemValueKey(InvocationData invD, ItemValue iv) {
			super(invD.getInvocationAtCall(iv.getCallStep()), iv.getStep());
			line = iv.getLine();
			thisId = iv.getThisId();
			index = iv.getId();
		}

		@Override
		protected InstructionKey getInstruction() {
			if (line < 0) return null;
			return InstructionKey.array(CodeIndex.anyAtLine(line));
		}
		@Override
		protected String detailString(String s) {
			return s + "#" + thisId + "[" + index + "]";
		}
		
		@Override
		protected int specificCompareTo(EventKey o) {
			ArrayItemValueKey a = (ArrayItemValueKey) o;
			int c = Long.compare(thisId, a.thisId);
			if (c != 0) return c;
			return Integer.compare(index, a.index);
		}
		
		@Override
		public String getUserString() {
			String s = "<" + invD.getObject(thisId).getShortString() + ">";
			s += "[" + index + "] = " + getValueString(); // \u21A4
			return s;
		}
	}
	
	public static class InvocationData {
		
		private final Invocation inv;
		private final Map<Long, InvocationData> others;
		private final Map<Long, Value> objects;
		private InvocationKey invKey = null;
//		private SoftReference<Map<Token, DataDependency>> graphRef = null;
		
		public InvocationData(Invocation inv) {
			this(inv, 
					Collections.synchronizedMap(new HashMap<>()), 
					Collections.synchronizedMap(new HashMap<>()));
		}
		
		public InvocationData(Invocation inv, Map<Long, InvocationData> others, Map<Long, Value> objects) {
			this.inv = inv;
			this.others = others;
			this.objects = objects;
			others.put(inv.getStep(), this);
		}
		
		public String getMethodId() {
			Invocation i = getInvocation();
			return i.type + "#" + i.method + i.signature;
		}
		
		public Value getObject(long thisId) {
			Value os = objects.get(thisId);
			if (os == null) {
				os = Value
						.object(inv.getTestId(), thisId, Long.MAX_VALUE).select()
						._execute(cnn());
				objects.put(thisId, os);
				os.beInitialized();
			}
			return os;
		}
		
		public InvocationData getInvocationAtCall(long callStep) {
			if (callStep < 0) return null;
			InvocationData id = others.get(callStep);
			if (id == null) {
				Invocation i = invocationAtStep(inv.getTestId(), callStep);
				id = new InvocationData(i, others, objects);
			}
			return id;
		}
		
		public InvocationData getInvocationBefore(String clazz, String method, String sig, long exitStep) {
			Invocation i = invocationBefore(inv.getTestId(), inv.getStep(), clazz, method, sig, exitStep);
			if (i == null) return null;
			return getInvocationAtCall(i.getStep());
		}
		
		public InvocationData getParent() {
			return getInvocationAtCall(callStepOf(inv));
		}
		
		public Invocation getInvocation() {
			return inv;
		}
		
		public InvocationKey getInvocationKey() {
			if (invKey == null) {
				invKey = new InvocationKey(this);
			}
			return invKey;
		}
		
//		public Map<Token, DataDependency> getDependencyGraph() {
//			Map<Token, DataDependency> graph = graphRef != null ? graphRef.get() : null;
//			if (graph == null) {
////				DynamicSlice.processing_time += System.currentTimeMillis();
//				graph = MethodDataDependencyCache.getDependencyGraph(inv.type, inv.method, inv.signature);
//				graphRef = new SoftReference<Map<Token,DataDependency>>(graph);
////				DynamicSlice.processing_time -= System.currentTimeMillis();
//			}
//			return graph;
//		}
	}

//	private static Invocation invocationBefore(int testId, long exitStep, int callLine) {
//		Invocation inv = DSL
//					.select().from(Invocation.VIEW)
//					//.ofMethod(clazz, method, sig)
//					.inTest(testId)
//					.beforeExit(exitStep)
//					.callInLine(callLine)
//				._execute(cnn())
//				._getFirst();
//		if (inv == null) {
//			throw new IllegalArgumentException(
//					//clazz + "#" + method + sig + " @ " + 
//					testId + ":" + exitStep + " " + callLine);
//		}
//		return inv;
//	}
	
	private static Invocation invocationBefore(int testId, long parentCall, String clazz, String method, String sig, long exitStep) {
		Invocation inv = DSL
					.select().from(Invocation.VIEW)
					.beforeExit(exitStep)
					//.ofMethod(clazz, method, sig)
					.inTest(testId).inCall(parentCall)
					
					.ofMethod(method, sig) //clazz, 
				._execute(cnn())
				._getFirst();
//		if (inv == null) {
//			throw new IllegalArgumentException(
//					testId + ":" + exitStep + " " + Token.methodKey(clazz, method, sig));
//		}
		return inv;
	}
	
	private static Invocation invocationAtExitStep(int testId, long step) {
		Invocation inv = DSL
					.select("*","signature","methodId").from(Invocation.VIEW)
					//.ofMethod(clazz, method, sig)
					.inTest(testId)
					.atExitStep(step)
				._execute(cnn())
				._getSingle();
		if (inv == null) {
			throw new IllegalArgumentException(
					//clazz + "#" + method + sig + " @ " + 
					testId + ":" + step);
		}
		return inv;
	}
	
	private static Invocation invocationAtStep(int testId, long step) {
		Invocation inv = DSL
				.select("*","signature","methodId").from(Invocation.VIEW)
				.inTest(testId)
				.atStep(step)
			._execute(cnn())
			._getSingle();
		return inv;
	}
	
	private static ItemValue arraySetBefore(int testId, long thisId, int index, long getStep) {
		ItemValue iv = DSL
				.select().from(NamedValue.ARRAY_SET_ITEM_VIEW)
				.beforeStep(testId, thisId, getStep)
				.atIndex(index)
			._execute(cnn())._getFirst();
		return iv;
	}
	
	private static FieldValue fieldSetBefore(int testId, long thisId, String field, long getStep) {
		FieldValue iv = DSL
				.select().from(NamedValue.OBJECT_SET_FIELD_VIEW)
				.beforeStep(testId, thisId, getStep)
				.ofField(field)
			._execute(cnn())._getFirst();
		return iv;
	}
	
	private static long callStepOf(Invocation inv) {
		if (inv.depth == 1) return 0;
		parentOf(inv);
		return inv.getCallStep();
	}
	
	private static Invocation parentOf(Invocation inv) {
		if (inv.depth < 1) return null;
		if (inv.parent != null) return inv.parent;
		List<Invocation> parents = DSL
					.select("*","signature","methodId").from(Invocation.VIEW)
					.parentOf(inv)
				._execute(cnn())
				._asList();
//		if (parents.size()  == 0) {
//			return null;
//		}
		Invocation parent = parents.get(0);
		inv.parent = parent;
		return parent;
	}

}
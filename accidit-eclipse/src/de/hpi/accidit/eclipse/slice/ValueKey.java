package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.Value;

public class ValueKey implements Comparable<ValueKey> {
	
	protected final InvocationData invD;
	protected final long step;
	protected Value value;
	
	public ValueKey(InvocationData invD, long step) {
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
	
	protected Token asToken() {
		return null;
	}
	
	public Value getValue() {
		return value;
	}
	
	public void setValue(Value value) {
		this.value = value;
	}

	@Override
	public int compareTo(ValueKey o) {
		long cStep = (step - o.step);
		int c = cStep < 0 ? -1 : (cStep > 0 ? 1 : 0);
		if (c != 0) return c;
		c = getClass().getName().compareTo(o.getClass().getName());
		if (c != 0) return c;
		return specificCompareTo(o);
	}
	
	@Override
	public int hashCode() {
		return (int) step;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof ValueKey)) return false;
		return ((ValueKey) obj).compareTo(this) == 0;
	}
	
	protected int specificCompareTo(ValueKey o) {
		return 0;
	}
	
	public VariableValueKey newVariableKey(String variable, int line, long step) {
		return new VariableValueKey(invD, step, variable, line);
	}
	
	public ArrayItemValueKey newArrayKey(long thisId, int index, long getStep) {
		return new ArrayItemValueKey(invD, getStep, thisId, index);
	}
	
	public FieldValueKey newFieldKey(long thisId, String field, long getStep) {
		return new FieldValueKey(invD, getStep, thisId, field);
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
	
	@Override
	public String toString() {
		String s;
		Token t = asToken();
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
	
	public static class InvocationKey extends ValueKey {
		
		private List<InvocationArgKey> args = new ArrayList<>(8);
		private InvocationThisKey thisKey = null;
		private Invocation inv;

		public InvocationKey(InvocationData invD) {
			super(invD.getParent(), invD.getInvocation().getStep());
			inv = invD.getInvocation();
		}
		
		public InvocationKey(int testId, long step) {
			this(new InvocationData(invocationAtStep(testId, step)));
			invD.invKey = this;
		}
		
		@Override
		protected Token asToken() {
			return Token.invoke(inv.type, inv.method, inv.signature, inv.line);
		}
		
		public InvocationArgKey getArg(int index) {
			while (args.size() <= index) {
				args.add(new InvocationArgKey(invD, step, args.size(), inv));
			}
			return args.get(index);
		}
		
		public InvocationThisKey getThis() {
			if (thisKey == null) {
				thisKey = new InvocationThisKey(invD, step, inv);
			}
			return thisKey;
		}
		
		public String getMethodKey() {
			return Token.methodKey(inv.type, inv.method, inv.signature);
		}
	}
	
	public static class InvocationThisKey extends ValueKey {
		
		private Invocation inv;

		public InvocationThisKey(InvocationData invD, long step, Invocation inv) {
			super(invD, step);
			this.inv = inv;
		}
		
		@Override
		protected Token asToken() {
			return Token.invokeThis(inv.type, inv.method, inv.signature, inv.line);
		}
	}

	public static class InvocationArgKey extends ValueKey {
		
		private int index;
		private Invocation inv;

		public InvocationArgKey(InvocationData invD, long step, int index, Invocation inv) {
			super(invD, step);
			this.index = index;
			this.inv = inv;
		}
		
		@Override
		protected Token asToken() {
			return Token.invokeArg(inv.type, inv.method, inv.signature, index, inv.line);
		}
	}
	
	public static class MethodResultKey extends ValueKey {
		
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
		protected Token asToken() {
			return Token.result(getInvocation().exitLine);
		}
	}
	
	public static class VariableValueKey extends ValueKey {
		
		private String variable;
		private int line;
		
		public VariableValueKey(InvocationData invD, long step, String variable, int line) {
			super(invD, step);
			this.variable = variable;
			this.line = line;
		}
		
		@Override
		protected Token asToken() {
			return Token.variable(variable, line);
		}
	}
	
	public static class FieldValueKey extends ValueKey {
		
		private long thisId = -1;
		private String field;
		private int line;
		
		public FieldValueKey(InvocationData invD, long getStep, long thisId, String field) {
			this(invD, fieldSetBefore(invD.getInvocation().getTestId(), thisId, field, getStep));
		}
		
		public FieldValueKey(InvocationData invD, FieldValue fv) {
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
		protected Token asToken() {
			if (line < 0) return null;
			return Token.field(field, line);
		}
		
		@Override
		protected String detailString(String s) {
			if (thisId <= 0) return s;
			int i = s.lastIndexOf('>')+1;
			return s.substring(0, i) + "#" + thisId + "." + s.substring(i);
		}
	}
	
	public static class ArrayItemValueKey extends ValueKey {
		
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
		protected Token asToken() {
			if (line < 0) return null;
			return Token.array(line);
		}
		@Override
		protected String detailString(String s) {
			return s + "#" + thisId + "[" + index + "]";
		}
	}
	
	public static class InvocationData {
		
		private final Invocation inv;
		private final Map<Long, InvocationData> others;
		private InvocationKey invKey = null;
//		private SoftReference<Map<Token, DataDependency>> graphRef = null;
		
		public InvocationData(Invocation inv) {
			this(inv, new HashMap<Long, InvocationData>());
		}
		
		public InvocationData(Invocation inv, Map<Long, InvocationData> others) {
			this.inv = inv;
			this.others = others;
			others.put(inv.getStep(), this);
		}
		
		public String getMethodId() {
			Invocation i = getInvocation();
			return i.type + "#" + i.method + i.signature;
		}
		
		public InvocationData getInvocationAtCall(long callStep) {
			InvocationData id = others.get(callStep);
			if (id == null) {
				Invocation i = invocationAtStep(inv.getTestId(), callStep);
				id = new InvocationData(i, others);
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
					
					.ofMethod(clazz, method, sig)
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
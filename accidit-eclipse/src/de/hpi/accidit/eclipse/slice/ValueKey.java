package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.model.Invocation;

public class ValueKey implements Comparable<ValueKey> {
	
	protected final Invocation inv;
	protected final int testId;
	protected final long step;
	
	public ValueKey(Invocation inv, int testId, long step) {
		super();
		this.inv = inv;
		this.testId = testId;
		this.step = step;
	}

	public DataDependency getDataDependency() {
		Token t = asToken();
		return getDependencyGraph().get(t);
	}
	
	public Map<Token, DataDependency> getDependencyGraph() {
		return getDependencyGraph(inv);
	}
	
	public Invocation getInvocation() {
		return inv;
	}
	
	public long getStep() {
		return step;
	}
	
	protected Token asToken() {
		return null;
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
	
	protected int specificCompareTo(ValueKey o) {
		return 0;
	}
	
	public VariableValueKey newVariableKey(String variable, int line, long step) {
		return new VariableValueKey(inv, testId, step, variable, line);
	}
	
	@Override
	public String toString() {
		Token t = asToken();
		if (t != null) return t.toString();
		return super.toString();
	}
	
	public static class MethodResultKey extends ValueKey {
		
		public MethodResultKey(int testId, long step, String clazz, String method, String sig) {
			super(invocationAtExitStep(testId, step, clazz, method, sig), testId, step);
		}
		
		@Override
		protected Token asToken() {
			return Token.result(inv.exitLine);
		}
	}
	
	public static class VariableValueKey extends ValueKey {
		
		private String variable;
		private int line;
		
		public VariableValueKey(Invocation inv, int testId, long step, String variable, int line) {
			super(inv, testId, step);
			this.variable = variable;
			this.line = line;
		}
		
		@Override
		protected Token asToken() {
			return Token.variable(variable, line);
		}
	}

	private static Invocation invocationAtExitStep(int testId, long step, String clazz, String method, String sig){
		Invocation inv = DSL
					.select("*","signature","methodId").from(Invocation.VIEW)
					.ofMethod(clazz, method, sig)
					.atExitStep(step)
				._execute(cnn())
				._getSingle();
		if (inv == null) {
			throw new IllegalArgumentException(
					clazz + "#" + method + sig + " @ " + testId + ":" + step);
		}
		return inv;
	}
	
	private static final Map<Invocation, SoftReference<Map<Token, DataDependency>>> GRAPH_CACHE =
			Collections.synchronizedMap(new WeakHashMap<Invocation, SoftReference<Map<Token, DataDependency>>>());
	
	private static Map<Token, DataDependency> getDependencyGraph(Invocation inv) {
		SoftReference<Map<Token, DataDependency>> ref = GRAPH_CACHE.get(inv);
		Map<Token, DataDependency> graph = ref != null ? ref.get() : null;
		if (graph == null) {
			graph = MethodDataDependencyCache.getDependencyGraph(inv.type, inv.method, inv.signature);
			ref = new SoftReference<Map<Token,DataDependency>>(graph);
			GRAPH_CACHE.put(inv, ref);
		}
		return graph;
	}
	
}
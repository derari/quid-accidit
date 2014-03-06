package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.slice.ValueKey.MethodResultKey;

public class DynamicSlice {
	
	public static void main(String[] args) {
		int testId = 65;
		long exitStep = 3626;
		
//		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=root");
		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=");
		DatabaseConnector.overrideSchema("accidit");
		
		MethodResultKey mrKey = new MethodResultKey(testId, exitStep);
		DynamicSlice slice = new DynamicSlice(mrKey);
		slice.processAll();
		
	}
	
	private final SortedSet<ValueKey> queue = new TreeSet<>(new Comparator<ValueKey>() {
		@Override
		public int compare(ValueKey o1, ValueKey o2) {
			return o2.compareTo(o1);
		}
	});
	
	private final SortedMap<ValueKey, Node> slice = new TreeMap<>();
	private final Map<Invocation, Map<String, List<VariableValue>>> variableHistories = new HashMap<>();

	public DynamicSlice(ValueKey key) {
		queue.add(key);
	}
	
	public void processAll() {
		while (!queue.isEmpty()) {
			processNextValue();
		}
	}
	
	protected void processNextValue() {
		ValueKey key = queue.first();
		queue.remove(key);
		DataDependency dd = key.getDataDependency();
		System.out.print(key);// + ": " + dd);
		Node n = new Node();
		collectDependencies(n.dependencies, key, dd);
		System.out.println(": " + n.dependencies);
		slice.put(key, n);
		queueKeys(n.dependencies.values);
		queueKeys(n.dependencies.control);
	}
	
	protected void queueKeys(Iterable<ValueKey> keys) {
		for (ValueKey key: keys) {
			if (!slice.containsKey(key)) {
				queue.add(key);
			}
		}
	}
	
	protected boolean collectDependencies(DependencySet bag, ValueKey key, DataDependency dd) {
		if (dd instanceof DataDependency.Complex) {
			DataDependency.Complex cplx = (DataDependency.Complex) dd;
			return collectDependencies(bag, key, cplx.getValue())
				 & collectDependencies(bag.controlOnly(), key, cplx.getControl());
		}
		if (dd instanceof DataDependency.All) {
			DataDependency.All all = (DataDependency.All) dd;
			boolean success = true;
			for (DataDependency d: all.getAll()) {
				success &= collectDependencies(bag, key, d);
			}
			return success;
		}
		if (dd instanceof DataDependency.Variable) {
			DataDependency.Variable var = (DataDependency.Variable) dd;
			return collectVariable(bag, key, var.getVar(), var.getLine());
		}
		if (dd instanceof DataDependency.Argument) {
			DataDependency.Argument arg = (DataDependency.Argument) dd;
			key = key.getInvocationKey();
			DataDependency.Invoke ivDd = (DataDependency.Invoke) key.getDataDependency();
			dd = ivDd.getArg(arg.getIndex());
			return collectDependencies(bag, key, dd);
		}
		System.out.print(" " + dd + "\n    ");
		return false;
	}
	
	private boolean collectVariable(DependencySet bag, ValueKey key, String var, int line) {
		List<VariableValue> history = getVariableHistory(key.getInvocation(), var);
		if (history == null) {
			Token t = Token.variable(var, line);
			if (!bag.guardToken(t)) return true;
			DataDependency dd = key.getDependencyGraph().get(t);
			if (dd == null) return false;
			return collectDependencies(bag, key, dd);
		}
		VariableValue match = null;
		for (VariableValue vv: history) {
			if (match == null) {
				match = vv;
			} else if (vv.getStep() <= key.getStep() && vv.getStep() > match.getStep()) {
				if (Math.abs(vv.getLine() - line) <= Math.abs(match.getLine() - line)) {
					match = vv;
				}
			}
		}
		if (match == null) {
			return false;
		}
		ValueKey varKey = key.newVariableKey(var, line, match.getStep());
		bag.addValue(varKey);
		return true;
	}
	
	private List<VariableValue> getVariableHistory(Invocation inv, String var) {
		Map<String, List<VariableValue>> variables = variableHistories.get(inv);
		if (variables == null) {
			variables = new HashMap<>();
			List<VariableValue> list = DSL
						.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
						.inCall(inv.getTestId(), inv.getStep())
					._execute(cnn())._asList();
			for (VariableValue vv: list) {
				List<VariableValue> history = variables.get(vv.getName());
				if (history == null) {
					history = new ArrayList<>();
					variables.put(vv.getName(), history);
				}
				history.add(vv);
			}
		}
		return variables.get(var);
	}
	
	protected static class DependencySet {
		private final Set<ValueKey> values;
		private final Set<ValueKey> control;
		private final Set<Token> tokens;
		
		public DependencySet() {
			values = new TreeSet<>();
			control = new TreeSet<>();
			tokens = new TreeSet<>();
		}
		
		private DependencySet(Set<ValueKey> values, Set<ValueKey> control, Set<Token> tokens) {
			this.values = values;
			this.control = control;
			this.tokens = tokens;
		}
		
		public DependencySet controlOnly() {
			if (values == control) return this;
			return new DependencySet(control, control, tokens);
		}

		public void addValue(ValueKey key) {
			if (values.add(key)) {
				if (values != control) control.remove(key);
				tokens.add(key.asToken());
			}
		}
		
		public void addControl(ValueKey key) {
			if (!values.contains(key)) {
				control.add(key);
				tokens.add(key.asToken());
			}
		}
		
		public boolean guardToken(Token t) {
			return tokens.add(t);
		}
		
		@Override
		public String toString() {
			if (control != values) {
				return values + " " + control;
			}
			return values.toString();
		}
	}

	public static class Node {
		private final DependencySet dependencies = new DependencySet();
	}

}

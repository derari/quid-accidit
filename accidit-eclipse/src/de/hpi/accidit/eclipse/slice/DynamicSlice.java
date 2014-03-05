package de.hpi.accidit.eclipse.slice;

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
import java.util.WeakHashMap;

import org.cthul.miro.DSL;
import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.slice.ValueKey.MethodResultKey;

public class DynamicSlice {
	
	public static void main(String[] args) {
		String clazz = "org.drools.base.evaluators.TimeIntervalParser";
		String method = "parse";
		String signature = "(Ljava/lang/String;)[Ljava/lang/Long;";
		int testId = 65;
		long step = 3626;
		
		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=root");
		DatabaseConnector.overrideSchema("accidit");
		
		MethodResultKey mrKey = new MethodResultKey(testId, step, clazz, method, signature);
		DynamicSlice slice = new DynamicSlice(mrKey);
		slice.processNextValue();
		
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
	
	protected void processNextValue() {
		ValueKey key = queue.first();
		queue.remove(key);
		DataDependency dd = key.getDataDependency();
		System.out.println(dd);
		Node node = new Node();
		collectDependencies(node.dependencies, key, dd);
		
		System.out.println(Arrays.toString(node.dependencies.toArray()));
	}
	
	protected boolean collectDependencies(Set<ValueKey> bag, ValueKey key, DataDependency dd) {
		if (dd instanceof DataDependency.Complex) {
			DataDependency.Complex cplx = (DataDependency.Complex) dd;
			return collectDependencies(bag, key, cplx.getValue())
				 & collectDependencies(bag, key, cplx.getControl());
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
		return false;
	}
	
	private boolean collectVariable(Set<ValueKey> bag, ValueKey key, String var, int line) {
		List<VariableValue> history = getVariableHistory(key.getInvocation(), var);
		if (history == null) {
			Token t = Token.variable(var, line);
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
		ValueKey varKey = key.newVariableKey(var, line, Math.min(match.getStep(), key.getStep()-1));
		bag.add(varKey);
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

	public static class Node {
		private final Set<ValueKey> dependencies = new TreeSet<>();
	}

}

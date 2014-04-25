package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;
import de.hpi.accidit.eclipse.slice.ValueKey.MethodResultKey;

public class DynamicSlice {
	
	public static void main(String[] args) {		
		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=root");
//		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=");
		DatabaseConnector.overrideSchema("accidit");
		
		long time = -System.currentTimeMillis();
		
		ValueKey key;
		
//		int testId = 65;
//		long callStep = 3688;
//		key = new InvocationKey(testId, callStep);
		
		//		long exitStep = 537662;
//		key = new MethodResultKey(testId, exitStep);
		
		// 461 + 210649
		
		int testId = 240;
//		long callStep = 320510;
//		key = new InvocationKey(testId, callStep);
		long exitStep = 380553;
		key = new MethodResultKey(testId, exitStep);
		
		DynamicSlice slice = new DynamicSlice(key);
		slice.processAll();
		System.out.println("\n-----------------------------\n\n\n\n\n\n\n\n\n\n\n\n");
		for (ValueKey vk: slice.slice.keySet()) {
			System.out.println(vk);
		}
		System.out.println("\n\n\n");
		
		time += System.currentTimeMillis();
		
		System.out.println(" total time: " + time);
		System.out.println("depend time:" + MethodDataDependencyAnalysis.total_time);
		System.out.println("slicin time:" + (time-MethodDataDependencyAnalysis.total_time));
		System.out.println(MethodDataDependencyAnalysis.total_time / (1.0 * time));
	}
	
	private final SortedSet<ValueKey> queue = new TreeSet<>(new Comparator<ValueKey>() {
		@Override
		public int compare(ValueKey o1, ValueKey o2) {
			return o2.compareTo(o1);
		}
	});
	
	private final SortedMap<ValueKey, Node> slice = new TreeMap<>();
	private final SortedMap<Token, DependencySet> internalSlice = new TreeMap<>();
	private final Map<Invocation, Map<String, List<VariableValue>>> variableHistories = new HashMap<>();
	private final Map<Invocation, List<ItemValue>> aryElementGetHistories = new HashMap<>();
	private final Map<Invocation, List<FieldValue>> fieldGetHistories = new HashMap<>();

	public DynamicSlice(ValueKey key) {
		queue.add(key);
	}
	
	public void processAll() {
		while (!queue.isEmpty()) {
			processNextValue();
		}
	}
	
	public SortedMap<ValueKey, Node> getSlice() {
		return slice;
	}
	
	protected void processNextValue() {
		ValueKey key = queue.first();
		queue.remove(key);
		DataDependency dd = key.getDataDependency();
		System.out.print(key);// + ": " + dd);
		Node n = new Node();
		
		if (dd != null) {
			collectDependencies(n.dependencies, key, dd);
			System.out.println(": " + n.dependencies);
		} else {
			if (key instanceof InvocationKey) {
				System.out.println("!! NO DATA FOR INVOCATION");
			} else {
				n.dependencies.addValue(key.getInvocationKey());
				System.out.println(":?? " + n.dependencies);
			}
		}
		
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
		if (dd instanceof DataDependency.Choice) {
			DataDependency.Choice choice = (DataDependency.Choice) dd;
			List<DataDependency> options = new ArrayList<>(choice.getChoice());
			Collections.reverse(options);
//			System.out.println("\n OPTIONS:  " + options);
			for (DataDependency d: options) {
				DependencySet bag2 = new DependencySet();
				bag2.tokens.addAll(bag.tokens);
				if (collectDependencies(bag2, key, d)) {
					bag.addValue(bag2);
					return true;
				}
			}
			return false;
		}
		if (dd instanceof DataDependency.Variable) {
			DataDependency.Variable var = (DataDependency.Variable) dd;
			return collectVariable(bag, key, var.getVar(), var.getLine());
		}
		if (dd instanceof DataDependency.ThisValue) {
			ValueKey thisKey = key.getInvocationThisKey();
//			thisKey.setValue(key.getValue());
			bag.addValue(thisKey);
			return true;
			
		}
		if (dd instanceof DataDependency.Argument) {
			DataDependency.Argument arg = (DataDependency.Argument) dd;
			ValueKey argKey = key.getInvocationArgumentKey(arg.getIndex());
			argKey.setValue(key.getValue());
			bag.addValue(argKey);
			return true;
			
		}
		if (dd instanceof DataDependency.Invoke) {
			DataDependency.Invoke iv = (DataDependency.Invoke) dd;
			if (key instanceof InvocationKey && iv.getMethodKey().equals(((InvocationKey) key).getMethodKey())) {
				return collectInvocation(bag, key, iv);						
			}
			ValueKey resultKey = key.newResultKey(iv.getType(), iv.getMethod(), iv.getSignature(), key.getStep());
			if (resultKey == null) {
				System.out.println("!! no result found: " + key + " " + iv);
				return collectInvocation(bag, key, iv);
			}
			bag.addValue(resultKey);
			return true;
			
		}
		if (dd instanceof DataDependency.Field) {
			DataDependency.Field f = (DataDependency.Field) dd;
			boolean success = true;
			success &= collectDependencies(bag, key, f.getInstance());
			success &= collectFields(bag, key, f.getLine());
			return success;
			
		}
		if (dd instanceof DataDependency.Element) {
			DataDependency.Element el = (DataDependency.Element) dd;
			boolean success = true;
			success &= collectDependencies(bag, key, el.getInstance());
			success &= collectDependencies(bag, key, el.getIndex());
			success &= collectArrayItem(bag, key, el.getLine());
			return success;
			
		}
		if (dd instanceof DataDependency.Constant) {
			return true;
		}
		System.out.print(" /" + dd + "\n    ");
		return false;
	}
	
	protected boolean collectInternalDependencies(DependencySet bag, ValueKey key, Token t) {
		DependencySet depSet = internalSlice.get(t);
		if (depSet == null) {
			depSet = new DependencySet();
			internalSlice.put(t, depSet);
			DataDependency dd = key.getDependencyGraph().get(t);
			if (dd == null) return false;
			collectDependencies(depSet, key, dd);
		}
		if (depSet.isEmpty()) return false;
		bag.addValue(depSet);
		return true;
	}
	
	private boolean collectVariable(DependencySet bag, ValueKey key, String var, int line) {
		List<VariableValue> history = getVariableHistory(key.getInvocation(), var);
		if (history == null) {
			Token t = Token.variable(var, line);
			return collectInternalDependencies(bag, key, t);
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
		varKey.setValue(match.getValue());
		return line == match.getLine();
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
	
	private boolean collectArrayItem(DependencySet bag, ValueKey key, int line) {
		List<ItemValue> history = getAryElementHistory(key.getInvocation());
		if (history == null) {
			return false;
		}
		ItemValue match = null;
		for (ItemValue vv: history) {
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
		ValueKey varKey = key.newArrayKey(match.getThisId(), match.getId(), match.getStep());
		bag.addValue(varKey);
		varKey.setValue(match.getValue());
		return true;
	}
	
	private List<ItemValue> getAryElementHistory(Invocation inv) {
		List<ItemValue> items = aryElementGetHistories.get(inv);
		if (items == null) {
			items = DSL
						.select().from(NamedValue.ARRAY_GET_HISTORY_VIEW)
						.inCall(inv.getTestId(), inv.getStep())
					._execute(cnn())._asList();
			aryElementGetHistories.put(inv, items);
		}
		return items;
	}
	
	private boolean collectFields(DependencySet bag, ValueKey key, int line) {
		List<FieldValue> history = getFieldHistory(key.getInvocation());
		if (history == null) {
			return false;
		}
		FieldValue match = null;
		for (FieldValue vv: history) {
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
		ValueKey varKey = key.newFieldKey(match.getThisId(), match.getName(), match.getStep());
		bag.addValue(varKey);
		varKey.setValue(match.getValue());
		return true;
	}
	
	private List<FieldValue> getFieldHistory(Invocation inv) {
		List<FieldValue> items = fieldGetHistories.get(inv);
		if (items == null) {
			items = DSL
						.select().from(NamedValue.OBJECT_GET_HISTORY_VIEW)
						.inCall(inv.getTestId(), inv.getStep())
					._execute(cnn())._asList();
			fieldGetHistories.put(inv, items);
		}
		return items;
	}
	
	private boolean collectInvocation(DependencySet bag, ValueKey key, DataDependency.Invoke iv) {
		DataDependency self = iv.getSelf();
		boolean success = true;
		if (self != null) success &= collectDependencies(bag, key, self);
		for (DataDependency arg: iv.getArgs()) {
			success &= collectDependencies(bag, key, arg);
		}
		return success;
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
		
		public boolean isEmpty() {
			return values.isEmpty() && control.isEmpty();
		}

		public void addValue(ValueKey key) {
			if (values.add(key)) {
				if (values != control) control.remove(key);
				Token t = key.asToken();
				if (t != null) tokens.add(t);
			}
		}
		
		public void addControl(ValueKey key) {
			if (!values.contains(key)) {
				control.add(key);
				Token t = key.asToken();
				if (t != null) tokens.add(t);
			}
		}
		
		public void addValue(DependencySet other) {
			for (ValueKey k: other.values) {
				addValue(k);
			}
			for (ValueKey k: other.control) {
				addControl(k);
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

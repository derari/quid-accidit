package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationAndArgsKey;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationArgKey;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;
import de.hpi.accidit.eclipse.slice.ValueKey.VariableValueKey;

public class DynamicSlice {
	
//	public static void main(String[] args) {		
//		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=root");
////		DatabaseConnector.overrideDBString("jdbc:mysql://localhost:3306/accidit?user=root&password=");
//		DatabaseConnector.overrideSchema("accidit");
//		
//		Timer time = new Timer();
//		time.enter();
//		
//		ValueKey key;
//		
//		int testId = 65;
//		long callStep = 3688;
//		key = new InvocationKey(testId, callStep);
//		
//		//		long exitStep = 537662;
////		key = new MethodResultKey(testId, exitStep);
//		
//		// 461 + 210649
//		
////		int testId = 240;
//////		long callStep = 320510;
//////		key = new InvocationKey(testId, callStep);
////		long exitStep = 380553;
////		key = new MethodResultKey(testId, exitStep);
//		
//		DynamicSlice slice = new DynamicSlice(MethodDataDependencyAnalysis.testSootConfig());
//		slice.addCriterion(key);
//		slice.processAll();
//		System.out.println("\n-----------------------------\n\n\n\n\n\n\n\n\n\n\n\n");
//		for (ValueKey vk: slice.slice.keySet()) {
//			System.out.println(vk);
//		}
//		System.out.println("\n\n\n");
//		
//		time.exit();
//		
//		printTimers(time);
//		
//		EXECUTOR.shutdownNow();
//	}
	
	public static void printTimers(Timer time) {
		if (time != null) System.out.println(" total time: " + time);
		System.out.println("depend time: " + MethodDataDependencyAnalysis.total_time);
		System.out.println("    db time: " + db_time);
		System.out.println("  lock time: " + lock_time);
		System.out.println("slicin time: " + slice_time);
		if (time != null) System.out.println(MethodDataDependencyAnalysis.total_time.value() / (1.0 * time.value()));
	}
	
	private static Timer db_time = new Timer();
	private static Timer lock_time = new Timer();
	private static Timer slice_time = new Timer();
	
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				EXECUTOR.shutdownNow();
			}
		});
	}
	
	public static int VALUE = 1;
	public static int REACH = 2;
	public static int CONTROL = 4;
	public static int ALL_DEPS = VALUE + REACH + CONTROL;
	
	private final Cache<String, MethodSlicer> methodSlicers = new Cache<String, DynamicSlice.MethodSlicer>() {
		@Override
		protected MethodSlicer value(String key) {
			return new MethodSlicer(key);
		}
	};
	
	private final SootConfig cfg;
//	private final Set<ValueKey> guardSet = new ConcurrentSkipListSet<>();
	
	private final Set<Node> criteria = new ConcurrentSkipListSet<>();
	private volatile ConcurrentNavigableMap<ValueKey, Node> slice = null;
	private final ConcurrentNavigableMap<ValueKey, Node> nodes = new ConcurrentSkipListMap<>();
	private final SortedMap<Token, DependencySet> internalSlice = new TreeMap<>();
	private final Map<Invocation, Map<String, List<VariableValue>>> variableHistories = new ConcurrentHashMap<>();
	private final Map<Invocation, List<ItemValue>> aryElementGetHistories = new ConcurrentHashMap<>();
	private final Map<Invocation, List<FieldValue>> fieldGetHistories = new ConcurrentHashMap<>();
	
	private final AtomicInteger pendingTasksCounter = new AtomicInteger(0);
	private final OnSliceUpdate onUpdate;

	public DynamicSlice(SootConfig cfg, OnSliceUpdate onUpdate) {
		this.cfg = cfg;
		this.onUpdate = onUpdate;
	}
	
	public synchronized void clear() {
		for (Node n: criteria) {
			n.setFlags(-1);
		}
		criteria.clear();
		newSlice();
	}
	
	public synchronized void setCriterion(ValueKey key, int flags) {
		Node n = getNode(key);
		if (flags == -1) {
			criteria.remove(n);
		} else {
			criteria.add(n);
		}
		n.setFlags(flags);
		newSlice();
	}
	
	public Node getNode(ValueKey vk) {
		Node n = nodes.get(vk);
		if (n != null) return n;
		n = new Node(vk);
		nodes.putIfAbsent(vk, n);
		return nodes.get(vk);
	}
	
	public boolean isEmpty() {
		return criteria.isEmpty();
	}
	
	private void newSlice() {
		execute(new Runnable() {
			@Override
			public void run() {
				createNewSlice();
			}
		});
	}
	
	private synchronized void createNewSlice() {
		slice = new ConcurrentSkipListMap<>();
		for (Node n: nodes.values()) {
			n.clearInheritedFlags();
		}
		for (Node n: criteria) {
			n.fillSlice(slice);
		}
	}
	
	public SortedMap<ValueKey, Node> getSlice() {
		if (slice == null) return new TreeMap<>();
		return slice;
	}
	
	protected void execute(final Runnable r) {
		pendingTasksCounter.incrementAndGet();
		EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					r.run();
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				} finally {
					int c = pendingTasksCounter.decrementAndGet();
					onUpdate.run(c == 0);
				}
			}
		});
	}
	
//	protected synchronized void processAll() {
//		while (pendingTasksCounter.get() > 0) {
//			try {
//				wait(1000);
////				pendingKeysCounter.decrementAndGet();
//			} catch (InterruptedException e) {
//				Thread.interrupted();
//				return;
//			}
//		}
//	}
	
	protected boolean collectDependencies(DependencySet bag, ValueKey key, DataDependency dd) {
		if (dd instanceof DataDependency.Complex) {
			DataDependency.Complex cplx = (DataDependency.Complex) dd;
			return collectDependencies(bag, key, cplx.getValue())
				 & collectDependencies(bag.controlOnly(), key, cplx.getControl());
		}
		if (dd instanceof DataDependency.Reach) {
			DataDependency.Reach reach = (DataDependency.Reach) dd;
			return collectDependencies(bag, key, reach.getValue())
				 & collectDependencies(bag.reachOnly(), key, reach.getReach());
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
		if (dd instanceof DataDependency.Invocation) {
			bag.addValue(key.getInvocationKey());
			return true;
		}
		if (dd instanceof DataDependency.InvocationResult) {
			DataDependency.InvocationResult iv = (DataDependency.InvocationResult) dd;
			if (key instanceof InvocationKey && iv.getMethodKey().equals(((InvocationKey) key).getMethodKey())) {
				// special case: the invocation itself
				return true;
			}
			if (key instanceof InvocationAndArgsKey && iv.getMethodKey().equals(((InvocationAndArgsKey) key).getMethodKey())) {
				// special case: we want the invocation, not the return value
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
			DataDependency dd =  methodSlicers.get(key.getMethodId()).dependencyGraph().get(t);
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
			lock_time.enter();
			try {
				synchronized (this) {
					variables = variableHistories.get(inv);
					if (variables != null) return variables.get(var);
					db_time.enter();
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
					variableHistories.put(inv, variables);
					db_time.exit();
				}
			} finally {
				lock_time.exit();
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
			lock_time.enter();
			try {
				synchronized (this) {
					items = aryElementGetHistories.get(inv);
					if (items != null) return items;
					db_time.enter();
					items = DSL
								.select().from(NamedValue.ARRAY_GET_HISTORY_VIEW)
								.inCall(inv.getTestId(), inv.getStep())
							._execute(cnn())._asList();
					aryElementGetHistories.put(inv, items);
					db_time.exit();
				}
			} finally {
				lock_time.exit();
			}
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
			lock_time.enter();
			try {
				synchronized (this) {
					items = fieldGetHistories.get(inv);
					if (items != null) return items;
					db_time.enter();
					items = DSL
								.select().from(NamedValue.OBJECT_GET_HISTORY_VIEW)
								.inCall(inv.getTestId(), inv.getStep())
							._execute(cnn())._asList();
					fieldGetHistories.put(inv, items);
					db_time.exit();
				}
			} finally {
				lock_time.exit();
			}
		}
		return items;
	}
	
	private boolean collectInvocation(DependencySet bag, ValueKey key, DataDependency.InvocationResult iv) {
		DataDependency self = iv.getSelf();
		boolean success = true;
		if (self != null) success &= collectDependencies(bag, key, self);
		for (DataDependency arg: iv.getArgs()) {
			success &= collectDependencies(bag, key, arg);
		}
		if (key instanceof InvocationAndArgsKey) {
			bag.addReach(((InvocationAndArgsKey) key).getThisInvocation());
		}
		return success;
	}
	
//	private boolean collectInvocationAndArgs(DependencySet bag, ValueKey key, DataDependency.Invoke iv) {
//		DataDependency self = iv.getSelf();
//		boolean success = true;
//		if (self != null) success &= collectDependencies(bag, key, self);
//		for (DataDependency arg: iv.getArgs()) {
//			success &= collectDependencies(bag, key, arg);
//		}
//		return success;
//	}
	
	public class MethodSlicer {
		
		private final String methodId;
		private volatile Map<Token, DataDependency> dependencyGraph = null;
		
		public MethodSlicer(String methodId) {
			this.methodId = methodId;
		}
		
		private synchronized Map<Token, DataDependency> dependencyGraph() {
			if (dependencyGraph == null) {
				dependencyGraph = cfg.analyse(methodId);
			}
			return dependencyGraph;
		}
		
		private void fillDependencies(Node n) {
			ValueKey key = n.key;
			Token t = key.asToken();
			DataDependency dd = t == null ? DataDependency.constant() : dependencyGraph().get(t);
			System.out.print(key);// + ": " + dd);
			if (dd != null) {
				collectDependencies(n.dependencies, key, dd);
				System.out.println(": " + n.dependencies);
			} else {
				if (t.toString().startsWith("-1:")) {
					String search = t.toString().substring(2);
					int max = 9999999;
					Token match = null;
					for (Token t2: dependencyGraph().keySet()) {
						String t2String = t2.toString();
						if (t2String.endsWith(search)) {
							int i = Integer.parseInt(t2String.substring(0, t2String.indexOf(':')));
							if (i < max) {
								max = i;
								match = t2;
							}
						}
					}
					if (match != null) {
						collectDependencies(n.dependencies, key, dependencyGraph().get(match));
						System.out.println(": " + n.dependencies);
						return;
					}
				}
				if (key instanceof InvocationKey) {
					System.out.println("!! NO DATA FOR INVOCATION");
				} else {
					n.dependencies.addValue(key.getInvocationKey().allArgs());
					
//					String invocationStep = key.getInvocationKey().getStep() + ":";
//					for (Token t2: dependencyGraph().keySet()) {
//						if (t2.toString().startsWith(invocationStep)) {
//							collectDependencies(n.dependencies, key, dependencyGraph().get(t2));
//						}
//					}
					
					System.out.println(":?? " + n.dependencies);
				}
			}
		}
	}
	
	private static final Set<ValueKey> DEV_NULL = new AbstractSet<ValueKey>() {
		public boolean add(ValueKey arg0) {
			return false;
		};
		@Override
		public Iterator<ValueKey> iterator() {
			return Collections.emptyIterator();
		}
		@Override
		public int size() {
			return 0;
		}
	};
	
	protected static class DependencySet {
		
		private final Set<ValueKey> values;
		private final Set<ValueKey> reach;
		private final Set<ValueKey> control;
		private final Set<Token> tokens;
		
		public DependencySet() {
			values = new TreeSet<>();
			control = new TreeSet<>();
			reach = new TreeSet<>();
			tokens = new TreeSet<>();
		}
		
		private DependencySet(Set<ValueKey> values, Set<ValueKey> reach, Set<ValueKey> control, Set<Token> tokens) {
			this.values = values;
			this.reach = reach;
			this.control = control;
			this.tokens = tokens;
		}
		
		public DependencySet controlOnly() {
			if (values == control) return this;
			return new DependencySet(control, DEV_NULL, control, tokens);
		}
		
		public DependencySet reachOnly() {
			if (values == control) return this;
			return new DependencySet(reach, reach, reach, tokens);
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
		
		public void addReach(ValueKey key) {
			reach.add(key);
		}
		
		public void addValue(DependencySet other) {
			for (ValueKey k: other.values) {
				addValue(k);
			}
			for (ValueKey k: other.control) {
				addControl(k);
			}
			for (ValueKey k: other.reach) {
				addReach(k);
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
	
	public class Node implements Comparable<Node> {
		private final ValueKey key;
		private DependencySet dependencies = null;
		private List<Node> dependenciesInSlice = null;
		private int configuredFlags = -1;
		private int inheritedFlags = 0;
		private int currentFlags = 0;
		private int dependencyFlags = 0;
		private Node representative = null;
		
		public Node(ValueKey key) {
			this.key = key;
		}
		
		public ValueKey getKey() {
			return key;
		}
		
		public void setFlags(final int flags) {
			configuredFlags = flags;
		}
		
		public synchronized void clearInheritedFlags() {
			dependenciesInSlice = null;
			inheritedFlags = 0;
			currentFlags = 0;
			dependencyFlags = 0;
		}
		
		private void inheritFlag(int dependencyFlags, ConcurrentNavigableMap<ValueKey, Node> slice, int flags) {
			this.dependencyFlags |= dependencyFlags; 
			inheritedFlags |= flags;
			fillSlice(slice);
		}
		
		private synchronized void initialize() {
			if (dependencies != null) return;
			dependencies = new DependencySet();
			MethodSlicer slicer = methodSlicers.get(key.getMethodId());
			slicer.fillDependencies(this);
		}
		
		public int getFlags() {
			if (configuredFlags == -1) return inheritedFlags;
			return configuredFlags;// | inheritedFlags;
		}
		
		public int getDependencyFlags() {
			if (configuredFlags > -1) return 8;
			return dependencyFlags;
		}
		
		public void fillSlice(final ConcurrentNavigableMap<ValueKey, Node> slice) {
			execute(new Runnable() {
				@Override
				public void run() {
					updateSlice(slice);
				}
			});
		}

		private synchronized void updateSlice(ConcurrentNavigableMap<ValueKey, Node> slice) {
			if (slice != DynamicSlice.this.slice) return;
			int actualFlags = getFlags();
			if (actualFlags == 0) return;
			if (currentFlags == actualFlags) return;
			int missingFlags = currentFlags ^ actualFlags;
			if (dependenciesInSlice == null) dependenciesInSlice = new ArrayList<>();
			initialize();
			
			slice.put(key, this);
			if ((missingFlags & VALUE) != 0) {
				for (ValueKey k: dependencies.values) {
					Node n = getNode(k);
					if (k instanceof InvocationArgKey) {
						n.representative = this;
					}
					dependenciesInSlice.add(n);
					n.inheritFlag(VALUE, slice, actualFlags);
				}
			}
			if ((missingFlags & CONTROL) != 0) {
				for (ValueKey k: dependencies.control) {
					Node n = getNode(k);
					dependenciesInSlice.add(n);
					n.inheritFlag(CONTROL, slice, actualFlags);
				}
			}
			if ((missingFlags & REACH) != 0) {
				for (ValueKey k: dependencies.reach) {
					Node n = getNode(k);
					dependenciesInSlice.add(n);
					n.inheritFlag(REACH, slice, actualFlags);
				}
			}
			currentFlags = actualFlags;
			
		}
		
		public List<Node> getDependenciesInSlice() {
			if (dependenciesInSlice == null) return Collections.emptyList();
			return dependenciesInSlice;
		}

		@Override
		public int compareTo(Node arg0) {
			return key.compareTo(arg0.key);
		}
		
		@Override
		public String toString() {
			return key.toString() + "(" + getFlags() + ")";
		}
		
//		public boolean isInternalNode() {
//			return (key instanceof InvocationArgKey) || (key instanceof InvocationAndArgsKey);
//		}
		public Node getRepresentative() {
			if (representative != null) {
				return representative;
			}
			if (key instanceof InvocationAndArgsKey) {
				Node n = getNode(((InvocationAndArgsKey) key).getThisInvocation());
				n.dependencyFlags |= dependencyFlags;
				return n;
			}
			return representative = this;
		}
		
		public Node contextNode() {
			if (key instanceof VariableValueKey) {
				Node n = getNode(key.getInvocationKey());
//				n.dependencyFlags |= dependencyFlags;
				return n;
			}
			return this;
		}
		
		public boolean isInvocation() {
			return key instanceof InvocationKey;
		}
		
		public long getStep() {
			return getKey().getStep();
		}
		
		public long getEndStep() {
			if (key instanceof InvocationKey) {
				return ((InvocationKey) key).getThisInvocation().exitStep;
			}
			return getStep();
		}
		
	}
	
	public interface OnSliceUpdate {
		void run(boolean done);
	}
}

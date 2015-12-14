package de.hpi.accidit.eclipse.slice;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.cthul.miro.DSL;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.slice.CodeDependency.CVDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.TriDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.ValueDependency;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationAndArgsKey;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationArgKey;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationKey;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationThisKey;
import de.hpi.accidit.eclipse.slice.EventKey.VariableValueKey;
import de.hpi.accidit.eclipse.views.util.WorkPool;

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
		System.out.println("depend time: " + MethodDependencyAnalysis.total_time);
		System.out.println("    db time: " + db_time);
		System.out.println("  lock time: " + lock_time);
		System.out.println("slicin time: " + slice_time);
		if (time != null) System.out.println(MethodDependencyAnalysis.total_time.value() / (1.0 * time.value()));
	}
	
	private static Timer db_time = new Timer();
	private static Timer lock_time = new Timer();
	private static Timer slice_time = new Timer();
		
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
	private volatile ConcurrentNavigableMap<EventKey, Node> slice = null;
	private final ConcurrentNavigableMap<EventKey, Node> nodes = new ConcurrentSkipListMap<>();
	private final SortedMap<EventKey, DependencySet> internalSlice = new TreeMap<>();
	private final Map<Invocation, Map<String, List<VariableValue>>> variableHistories = new ConcurrentHashMap<>();
	private final Map<Invocation, List<ItemValue>> aryElementGetHistories = new ConcurrentHashMap<>();
	private final Map<Invocation, List<FieldValue>> fieldGetHistories = new ConcurrentHashMap<>();
	
	private final AtomicInteger pendingTasksCounter = new AtomicInteger(0);
	private final OnSliceUpdate onUpdate;
	private long updateId = 0;

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
	
	public synchronized void setCriterion(EventKey key, int flags) {
		Node n = getNode(key);
		if (flags == -1) {
			criteria.remove(n);
		} else {
			criteria.add(n);
		}
		n.setFlags(flags);
		newSlice();
	}
	
	public Node getNode(EventKey vk) {
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
		if (criteria.isEmpty()) {
			nodes.clear();
		}
		slice = new ConcurrentSkipListMap<>();
		for (Node n: nodes.values()) {
			n.clearInheritedFlags();
		}
		for (Node n: criteria) {
			n.fillSlice(slice);
		}
		internalSlice.clear(); // most of the data is redundant by now
	}
	
	public SortedMap<EventKey, Node> getSlice() {
		if (slice == null) return new TreeMap<>();
		return slice;
	}
	
	protected void execute(final Runnable r) {
		pendingTasksCounter.incrementAndGet();
		WorkPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					r.run();
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				} finally {
					int c = pendingTasksCounter.decrementAndGet();
					updateId++;
					onUpdate.run(c == 0);
				}
			}
		});
	}
	
	protected boolean collectDependencies(DependencySet bag, EventKey key, TriDependency dd) {
		return collectDependencies(bag, key, dd.getCValue())
			 & collectDependencies(bag.reachOnly(), key, dd.getReach());
	}
	
	protected boolean collectDependencies(DependencySet bag, EventKey key, CVDependency dd) {
		return collectDependencies(bag, key, dd.getValue())
			 & collectDependencies(bag.controlOnly(), key, dd.getControl());
	}
	
	protected boolean collectDependencies(DependencySet bag, EventKey key, ValueDependency dd) {
		if (dd instanceof CodeDependency.Constant) {
			return true;
		}
		if (dd instanceof CodeDependency.AllValues) {
			CodeDependency.AllValues all = (CodeDependency.AllValues) dd;
			boolean success = true;
			for (CVDependency d: all.getAll()) {
				success &= collectDependencies(bag, key, d);
			}
			return success;
		}
		if (dd instanceof CodeDependency.AnyValue) {
			CodeDependency.AnyValue choice = (CodeDependency.AnyValue) dd;
			DependencySet bestMatch = null;
			for (CVDependency d: choice.getChoice()) {
				DependencySet bag2 = bag.fork();
//				bag2.instructionGuard.addAll(bag.instructionGuard);
				if (collectDependencies(bag2, key, d)) {
					if (bestMatch == null || bestMatch.lastValueStep < bag2.lastValueStep) {
						bestMatch = bag2;
					}
				}
			}
			if (bestMatch == null) return false;
			bag.join(bestMatch);
			return true;
		}
		if (dd instanceof CodeDependency.Variable) {
			CodeDependency.Variable var = (CodeDependency.Variable) dd;
			return collectVariable(bag, key, var.getVar(), var.getCodeIndex());
		}
		if (dd instanceof CodeDependency.ThisValue) {
			EventKey thisKey = key.getInvocationThisKey();
//			thisKey.setValue(key.getValue());
			bag.addValue(thisKey);
			return true;
		}
		if (dd instanceof CodeDependency.Argument) {
			CodeDependency.Argument arg = (CodeDependency.Argument) dd;
			EventKey argKey = key.getInvocationArgumentKey(arg.getIndex());
			argKey.setValue(key.getValue());
			bag.addValue(argKey);
			return true;
			
		}
		if (dd instanceof CodeDependency.Invocation) {
			bag.addValue(key.getInvocationKey());
			bag.addControl(key.getInvocationThisKey());
			return true;
		}
		if (dd instanceof CodeDependency.InvocationResult) {
			CodeDependency.InvocationResult iv = (CodeDependency.InvocationResult) dd;
			if (key instanceof InvocationKey && iv.getMethodKey().equals(((InvocationKey) key).getMethodKey())) {
				// special case: the invocation itself
				return true;
			}
			if (key instanceof InvocationAndArgsKey && iv.getMethodKey().equals(((InvocationAndArgsKey) key).getMethodKey())) {
				// special case: we want the invocation, not the return value
				return collectInvocation(bag, key, iv);						
			}
			EventKey resultKey = key.newResultKey(iv.getType(), iv.getMethod(), iv.getSignature(), key.getStep());
			if (resultKey == null) {
				System.out.println("!! no result found: " + key.getInvocationKey() + " " + iv);
				return collectInvocation(bag, key, iv);
			}
			while(resultKey != null && !bag.addValue(iv, resultKey)) {
				// if the same invocation was added for an other code dependency, check if there are more
				resultKey = key.newResultKey(iv.getType(), iv.getMethod(), iv.getSignature(), resultKey.getStep());
			}
			return true;
			
		}
		if (dd instanceof CodeDependency.Field) {
			CodeDependency.Field f = (CodeDependency.Field) dd;
			boolean success = true;
			success &= collectDependencies(bag.controlOnly(), key, f.getInstance());
			success &= collectFields(bag, key, f.getField(), f.getCodeIndex().getLine());
			return success;
			
		}
		if (dd instanceof CodeDependency.Element) {
			CodeDependency.Element el = (CodeDependency.Element) dd;
			boolean success = true;
			success &= collectDependencies(bag, key, el.getInstance());
			success &= collectDependencies(bag, key, el.getIndex());
			success &= collectArrayItem(bag, key, el.getCodeIndex().getLine());
			return success;
			
		}
		System.out.print(" /" + dd + "\n    ");
		return false;
	}
	
	/** Collect dependencies for internal variable */
	protected boolean collectInternalDependencies(DependencySet bag, EventKey key, InstructionKey t) {
		EventKey intVarKey = key.newVariableKey(t.getVar(), t.getCodeIndex(), key.getStep());
		DependencySet depSet = internalSlice.get(intVarKey);
		if (depSet == null) {
			depSet = new DependencySet();
			depSet.codeDependencies.addAll(bag.codeDependencies);
			internalSlice.put(intVarKey, depSet);
			Map<InstructionKey, TriDependency> graph = methodSlicers.get(key.getMethodId()).dependencyGraph();
			for (TriDependency td: t.getValuesFrom(graph)) {
				collectDependencies(depSet, key, td);
			}
		}
		if (depSet.isEmpty()) return false;
		bag.join(depSet);
		return true;
	}
	
	private boolean collectVariable(DependencySet bag, EventKey key, String var, CodeIndex ci) {
		List<VariableValue> history = getVariableHistory(key.getInvocation(), var);
		if (history == null) {
			if (var.equals("this")) {
				CVDependency thisDd = new CodeDependency.ThisValue();
				return collectDependencies(bag, key, thisDd);
			} else {
				InstructionKey t = InstructionKey.variable(var, ci);
				return collectInternalDependencies(bag, key, t);
			}
		}
		final int line = ci.getLine();
		VariableValue match = null;
		for (VariableValue vv: history) {
			if (match == null) {
				match = vv;
			} else if (vv.getStep() < key.getStep() && vv.getStep() > match.getStep()) {
				if (Math.abs(vv.getLine() - line) <= Math.abs(match.getLine() - line)) {
					match = vv;
				}
			}
		}
		if (match == null) {
			return false;
		}
		EventKey varKey = key.newVariableKey(var, ci, match.getStep());
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
	
	private boolean collectArrayItem(DependencySet bag, EventKey key, int line) {
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
		EventKey varKey = key.newArrayKey(match.getThisId(), match.getId(), match.getStep());
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
	
	private boolean collectFields(DependencySet bag, EventKey key, String name, int line) {
		List<FieldValue> history = getFieldHistory(key.getInvocation());
		if (history == null) {
			return false;
		}
		FieldValue match = null;
		for (FieldValue vv: history) {
			if (match == null) {
				match = vv;
			} else if (vv.getStep() < key.getStep() && vv.getStep() > match.getStep()) {
				if (!name.equals(match.getName()) || name.equals(vv.getName())) {
					if (Math.abs(vv.getLine() - line) <= Math.abs(match.getLine() - line)) {
						match = vv;
					}
				}
			}
		}
		if (match == null) {
			return false;
		}
		EventKey varKey = key.newFieldKey(match);
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
	
	private boolean collectInvocation(DependencySet bag, EventKey key, CodeDependency.InvocationResult iv) {
		CVDependency self = iv.getSelf();
		boolean success = true;
		if (self != null) success &= collectDependencies(bag, key, self);
		for (CVDependency arg: iv.getArgs()) {
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
		private volatile Map<InstructionKey, TriDependency> dependencyGraph = null;
		
		public MethodSlicer(String methodId) {
			this.methodId = methodId;
		}
		
		private synchronized Map<InstructionKey, TriDependency> dependencyGraph() {
			if (dependencyGraph == null) {
				dependencyGraph = cfg.analyse(methodId);
			}
			return dependencyGraph;
		}
		
		private void fillDependencies(Node n) {
			EventKey key = n.key;
			InstructionKey t = key.getInstruction();
			boolean found = false;
			try {
				if (t != null) {
					for (TriDependency td: t.getValuesFrom(dependencyGraph())) {
						found = true;
						collectDependencies(n.dependencies, key, td);
					}
					if (found) return;
					if (t.toString().startsWith("-1:")) {
						InstructionKey match = guessInstruction(t);
						if (match != null) {
							collectDependencies(n.dependencies, key, dependencyGraph().get(match));
							return;
						}
					}
				}
			} catch (RuntimeException ex) {
				System.err.println(ex.getClass() + " " + ex.getMessage());
				System.err.println(ex.getStackTrace()[0]);
			}
			if (key instanceof InvocationKey) {
				if (n.dependencyFlags != REACH) {
					System.out.println("!! NO DATA FOR INVOCATION: " + key);
//					System.out.println("  " + dependencyGraph());
				}
			} else {
				if (n.dependencyFlags != REACH) {
					System.out.println("!! REDIRECT TO CALL: " + key + " -> " + key.getInvocationKey());
//					System.out.println("  " + dependencyGraph());
				}
				n.dependencies.addValue(key.getInvocationKey().allArgs());
//				System.out.println(key + " :?? " + n.dependencies);
			}
		}
	
		private InstructionKey guessInstruction(InstructionKey t) {
			String search = t.toString().substring(2);
			int max = 9999999;
			InstructionKey match = null;
			for (InstructionKey t2: dependencyGraph().keySet()) {
				String t2String = t2.toString();
				if (t2String.endsWith(search)) {
					int i = Integer.parseInt(t2String.substring(0, t2String.indexOf(':')));
					if (i < max) {
						max = i;
						match = t2;
					}
				}
			}
			return match;
		}
			
	}
	
	private static final Set<EventKey> DEV_NULL = new AbstractSet<EventKey>() {
		public boolean add(EventKey arg0) {
			return false;
		};
		@Override
		public Iterator<EventKey> iterator() {
			return Collections.emptyIterator();
		}
		@Override
		public int size() {
			return 0;
		}
	};
	
	protected static class DependencySet {
		
		private final Set<EventKey> values;
		private final Set<EventKey> reach;
		private final Set<EventKey> control;
		private final Set<EventKey> all = new TreeSet<>();;
		private final Set<ValueDependency> codeDependencies;
		private long lastValueStep = 0;
		private boolean allDirty = true;
//		private final Set<InstructionKey> instructionGuard;
		
		public DependencySet() {
			values = new TreeSet<>();
			control = new TreeSet<>();
			reach = new TreeSet<>();
			codeDependencies = new TreeSet<>();
//			instructionGuard = new TreeSet<>();
		}
		
		private DependencySet(Set<EventKey> values, Set<EventKey> reach, Set<EventKey> control, Set<ValueDependency> codeDependencies) {
			this.values = values;
			this.reach = reach;
			this.control = control;
			this.codeDependencies = codeDependencies;
//			this.instructionGuard = tokens;
		}
		
		public DependencySet controlOnly() {
			if (values == control) return this;
			return new DependencySet(control, DEV_NULL, control, codeDependencies);
		}
		
		public DependencySet reachOnly() {
			if (values == control) return this;
			return new DependencySet(reach, reach, reach, codeDependencies);
		}
		
		public DependencySet fork() {
			Set<EventKey> v, r, c;
			Set<ValueDependency> cd = new TreeSet<>(codeDependencies);
			v = new TreeSet<>(values);
			c = values == control ? v : new TreeSet<>(control);
			r = values == reach ? v : new TreeSet<>(reach);
			return new DependencySet(v, r, c, cd);
		}
		
		public boolean isEmpty() {
			return values.isEmpty() && control.isEmpty();
		}
		
		public boolean addValue(ValueDependency vd, EventKey key) {
			if (addValue(key)) {
				codeDependencies.add(vd);
				return true;
			}
			return codeDependencies.contains(vd);
		}

		public boolean addValue(EventKey key) {
			if (values.add(key)) {
				allDirty = true;
				if (values != control) control.remove(key);
				lastValueStep = Math.max(lastValueStep, key.getStep());
//				InstructionKey t = key.getInstruction();
//				if (t != null) instructionGuard.add(t);
				return true;
			}
			return false;
		}
		
		public void addControl(EventKey key) {
			if (!values.contains(key)) {
				allDirty = true;
				control.add(key);
//				InstructionKey t = key.getInstruction();
//				if (t != null) instructionGuard.add(t);
			}
		}
		
		public void addReach(EventKey key) {
			allDirty = true;
			reach.add(key);
		}
		
		public void join(DependencySet other) {
			for (EventKey k: other.values) {
				addValue(k);
			}
			for (EventKey k: other.control) {
				addControl(k);
			}
			for (EventKey k: other.reach) {
				addReach(k);
			}
			codeDependencies.addAll(other.codeDependencies);
		}
		
		public boolean isValueOrControl(EventKey key) {
			return values.contains(key) || control.contains(key);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("v: ").append(values);
			if (control != values) {
				sb.append(" c: ").append(control);
			}
			if (reach != values) {
				sb.append(" r: ").append(reach);
			}
			return sb.toString();
		}
	}
	
	public class Node implements Comparable<Node> {
		private final EventKey key;
		private DependencySet dependencies = null;
		private Set<Node> dependenciesInSlice = null;
		private Set<Node> filteredDependenciesInSlice = null;
		private long lastUpdate = -1;
		private int configuredFlags = -1;
		private int inheritedFlags = 0;
		private int currentFlags = 0;
		private int dependencyFlags = 0;
		private Node representative = null;
		
		public Node(EventKey key) {
			this.key = key;
		}
		
		public EventKey getKey() {
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
		
		private void inheritFlag(int dependencyFlags, ConcurrentNavigableMap<EventKey, Node> slice, int flags) {
			this.dependencyFlags |= dependencyFlags; 
			inheritedFlags |= flags;
			fillSlice(slice);
		}
		
		private synchronized void initialize() {
			if (dependencies != null) return;
			dependencies = new DependencySet();
			MethodSlicer slicer = methodSlicers.get(key.getMethodId());
			slicer.fillDependencies(this);
			logDependencies();
		}
		
		public int getFlags() {
			if (configuredFlags == -1) return inheritedFlags;
			return configuredFlags;// | inheritedFlags;
		}
		
		public int getDependencyFlags() {
			if (configuredFlags > -1) return 8;
			return dependencyFlags;
		}
		
		public void fillSlice(final ConcurrentNavigableMap<EventKey, Node> slice) {
			execute(new Runnable() {
				@Override
				public void run() {
					updateSlice(slice);
				}
			});
		}

		private synchronized void updateSlice(ConcurrentNavigableMap<EventKey, Node> slice) {
			if (slice != DynamicSlice.this.slice) return;
			int actualFlags = getFlags();
			if (actualFlags == 0) return;
			if (currentFlags == actualFlags) return;
			int missingFlags = currentFlags ^ actualFlags;
			if (dependenciesInSlice == null) dependenciesInSlice = new HashSet<>();
			initialize();
			
			slice.put(key, this);
			if ((missingFlags & VALUE) != 0) {
				for (EventKey k: dependencies.values) {
					Node n = getNode(k);
					if (k instanceof InvocationArgKey
							&& (key instanceof VariableValueKey)) {
						// `n` is a method arg, this is the variable where the arg is stored
						n.representative = this;
					}
					dependenciesInSlice.add(n);
					n.inheritFlag(VALUE, slice, actualFlags);
				}
			}
			if ((missingFlags & CONTROL) != 0) {
				for (EventKey k: dependencies.control) {
					Node n = getNode(k);
					dependenciesInSlice.add(n);
					n.inheritFlag(CONTROL, slice, actualFlags);
				}
			}
			if ((missingFlags & REACH) != 0) {
				for (EventKey k: dependencies.reach) {
					Node n = getNode(k);
					dependenciesInSlice.add(n);
					n.inheritFlag(REACH, slice, actualFlags);
				}
			}
			currentFlags = actualFlags;
		}
		
		public Set<Node> getDependenciesInSlice() {
			if (dependenciesInSlice == null) return Collections.emptySet();
			return dependenciesInSlice;
		}
		
		public Set<Node> getFilteredDependenciesInSlice() {
			if (filteredDependenciesInSlice == null || lastUpdate != updateId) {
				lastUpdate = updateId;
				boolean noInternals = true;
				for (Node n: getDependenciesInSlice()) {
					if (n.isInternal()) {
						noInternals = false;
						break;
					}
				}
				if (noInternals) {
					filteredDependenciesInSlice = getDependenciesInSlice();
				} else {
					filteredDependenciesInSlice = new HashSet<>();
					for (Node n: getDependenciesInSlice()) {
						if (n.isInternal()) {
							n.addFilteredValueControlDependencies(filteredDependenciesInSlice::add);
						} else {
							filteredDependenciesInSlice.add(n);
						}
					}
				}
			}
			return filteredDependenciesInSlice;
		}
		
		public void addFilteredValueControlDependencies(Consumer<Node> target) {
			for (Node n: getFilteredDependenciesInSlice()) {
				if (dependencies.isValueOrControl(n.getKey()) ||
						!dependenciesInSlice.contains(n)) {
					target.accept(n);
				}
			}
//				.stream().filter(d -> dependencies.isValueOrControl(d.getKey()))
//				.forEach(target);
		}

		@Override
		public int compareTo(Node arg0) {
			if (arg0 instanceof RepresentativeNode) return -arg0.compareTo(this);
			return key.compareTo(arg0.key);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RepresentativeNode) {
				return obj.equals(this);
			}
			return super.equals(obj);
		}
		
		public void logDependencies() {
			System.out.println(key + ": " + dependencies);
		}
		
		@Override
		public String toString() {
			return key.toString() + "(" + getFlags() + ")";
		}
		
		public boolean isInternal() {
			return key.isInternal();
		}

		public Node getRepresentative() {
			if (representative != null) {
				return representative;
			}
			Node n = this;
			if (key instanceof InvocationAndArgsKey) {
				n = getNode(((InvocationAndArgsKey) key).getThisInvocation());
				return representative = new RepresentativeNode(this, n);
			} else if (key instanceof InvocationThisKey) {
				n = getNode(((InvocationThisKey) key).getThisInvocation());
			} else if (key instanceof InvocationArgKey) {
				n = getNode(((InvocationArgKey) key).getThisInvocation());
			}
			if (n != this) n = new RepresentativeNode(this, n);
			return representative = n;
		}
		
		public Node contextNode() {
			if (key instanceof InvocationArgKey) {
				Node n = getNode(((InvocationArgKey) key).getInvocationThisKey());
//				n.dependencyFlags |= dependencyFlags;
				return n;
			}
			if (key instanceof VariableValueKey) {
				Node n = getNode(key.getInvocationKey());
//				n.dependencyFlags |= dependencyFlags;
				return n;
			}
			if (key instanceof InvocationThisKey) {
				throw new UnsupportedOperationException("should not happen");
//				Node n = getNode(key.getInvocationKey());
//				n.dependencyFlags |= dependencyFlags;
//				return n;
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
	
	public class RepresentativeNode extends Node {
		private final Node actual;
		private final Node rep;
		public RepresentativeNode(Node actual, Node rep) {
			super(rep.getKey());
			this.actual = actual;
			this.rep = rep;
			((Node) this).dependencyFlags = actual.dependencyFlags | rep.dependencyFlags;
		}
		
		@Override
		public int compareTo(Node arg0) {
			return rep.compareTo(arg0);
		}
		
		@Override
		public boolean equals(Object obj) {
			return rep.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return rep.hashCode();
		}
		
		@Override
		public int getFlags() {
			return actual.getFlags();
		}
		
		@Override
		public Node contextNode() {
			Node c = rep.contextNode();
			if (c == rep) return this;
			return c;
		}
		
		@Override
		public Node getRepresentative() {
			return this;
		}
	}
	
	public interface OnSliceUpdate {
		void run(boolean done);
	}
}

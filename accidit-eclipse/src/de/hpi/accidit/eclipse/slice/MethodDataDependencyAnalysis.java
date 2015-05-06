package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class MethodDataDependencyAnalysis extends ForwardFlowAnalysis<Unit, DataDependencyFlow> {
	
	public static Timer total_time = new Timer();
	public static Timer get_method_time = new Timer();
	
	static boolean LOG_ALL = false;
	
	public static SootConfig testSootConfig() {
		String drools = "C:/Users/derari/hpi/phd/testprojects/drools B";
		String mvn = "C:/Users/derari/.m2";
		String extra = "";
//		String drools = "/Users/at/projects/drools";
//		String mvn = "/Users/at/.m2";
//		String sep = ":";
//		String extra = "/Library/Java/JavaVirtualMachines/jdk1.7.0_15.jdk/Contents/Home/jre/lib/rt.jar" + sep;
		
		final String[] libs = { 
					drools + "/drools-core/target/classes",
					drools + "/drools-core/target/test-classes",
					mvn + "/repository/junit/junit/4.11/junit-4.11.jar",
					mvn + "/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
					mvn + "/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar",
					mvn + "/repository/org/drools/knowledge-api/5.5.0.Final/knowledge-api-5.5.0.Final.jar",
					mvn + "/repository/org/drools/knowledge-internal-api/5.5.0.Final/knowledge-internal-api-5.5.0.Final.jar",
					mvn + "/repository/org/slf4j/slf4j-api/1.7.2/slf4j-api-1.7.2.jar",
					mvn + "/repository/org/mvel/mvel2/2.1.5.Final/mvel2-2.1.5.Final.jar",
		};
		return new SootConfig() {
			@Override
			protected List<String> getClassPath() {
				return Arrays.asList(libs);
			}
		};
	}
	
	public static void main(String[] args) {
		SootConfig.enable(testSootConfig());
		LOG_ALL = true;
//		{
//			String clazz = "org.drools.base.evaluators.TimeIntervalParser";
//			String method = "parse";
//			String signature = "(Ljava/lang/String;)[Ljava/lang/Long;";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}
//		{
//			String clazz = "java.lang.Long";
//			String method = "valueOf";
//			String signature = "(J)Ljava/lang/Long;";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}
		{
			String clazz = "java.lang.String";
			String method = "trim";
			String signature = "()Ljava/lang/String;";
			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
			System.out.println(printMap(map));
		}
//		{
//			String clazz = "org.drools.base.evaluators.TimeIntervalParserTest";
//			String method = "sootTest";
//			String signature = "(I)I";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}
//		{
//			String clazz = "org.drools.base.evaluators.TimeIntervalParserTest";
//			String method = "testParse3";
//			String signature = "()V";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}
//		{
//			String clazz = "org.drools.time.TimeUtils";
//			String method = "parseTimeString";
//			String signature = "(Ljava/lang/String;)J";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}	
	}
	
	public static Map<Token, DataDependency> analyseMethod(SootMethod sMethod) {
		Body b;
		synchronized (MethodDataDependencyAnalysis.class) {
			b = sMethod.retrieveActiveBody();
		}
		System.out.println("Creating static dependency graph of");
		System.out.println("    " + sMethod);
//		Body b = sMethod.getSource().getBody(sMethod, "");
		UnitGraph graph = new ExceptionalUnitGraph(b);
		MethodDataDependencyAnalysis analysis = new MethodDataDependencyAnalysis(graph);
//		System.out.println(slice.toString());
		return analysis.dependencies;
	}
	
	private static SootClass getClass(String clazz) {
		SootClass sClass = null;
		try {
			sClass = Scene.v().getSootClass(clazz);
			if (sClass.resolvingLevel() < SootClass.BODIES) sClass = null;
		} catch (RuntimeException e) {
			// expected if class is not loaded
		}
		if (sClass == null) {
			sClass = reloadClass(clazz);
		}
		return sClass;
	}
	
	private static SootClass reloadClass(String clazz) {
		System.out.println("!!!!!!!!!!!!!!! " + clazz);
		for (java.lang.reflect.Field f: Scene.class.getDeclaredFields()) {
			if (f.getName().equals("doneResolving")) {
				try {
					f.setAccessible(true);
					f.set(Scene.v(), false);
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
				break;
			}
		}
		SootClass sClass = Scene.v().loadClass(clazz, SootClass.BODIES); //loadClassAndSupport(clazz);
		sClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		return sClass;
	}
	
	private static synchronized SootMethod getMethod(String clazz, String method, String signature) {
		
		SootClass sClass = getClass(clazz);
		SootMethod sMethod = null;
		for (SootMethod m0: sClass.getMethods()) {
			if (m0.getName().equals(method) && matchSignature(m0, signature)) {
				sMethod = m0;
//				System.out.println("    <<<" + m0 + ">>>");
				break;
			}
		}
		if (sMethod == null) {
			throw new RuntimeException("Method not found: " + clazz + "#" + method + signature);
		}
		return sMethod;
	}
	
	public static Map<Token, DataDependency> analyseMethod(String clazz, String method, String signature) {
		total_time.enter();
		try {
			SootMethod sMethod = getMethod(clazz, method, signature);
			return analyseMethod(sMethod);
		} finally {
			total_time.exit();
		}
	}
	
	private static boolean matchSignature(SootMethod m, String signature) {
		return m.getBytecodeSignature().endsWith(signature + ">");
	}
	
	Map<Token, DataDependency> dependencies = new TreeMap<>();
	
	private MethodDataDependencyAnalysis(UnitGraph g) {
		super(g);
		doAnalysis();
	}
	
	private int getLineNumber(Unit d) {
		Tag t = d.getTag("LineNumberTag");
		LineNumberTag lt = (LineNumberTag) t;
		if (lt != null) return lt.getLineNumber();
		return -1;
	}
	
	private void logThrough(Unit d, Object o, DataDependencyFlow out) {
		System.out.println("~~~~ " + getLineNumber(d) + ":" + d.getClass().getSimpleName() + " " + d);
		if (o != null) {
			System.out.println("    " + o.getClass() + " " + o);
		}
		System.out.println("    " + out);
	}
	
	private DataDependency getDataDependency(Value rv, DataDependencyFlow out, int line) {
		
		if (rv instanceof ThisRef) {
			return DataDependency.thisValue();
			
		} else if (rv instanceof ParameterRef) {
			ParameterRef p = (ParameterRef) rv;
			return DataDependency.argument(p.getIndex());
			
		} else if (rv instanceof JimpleLocal) {
			JimpleLocal l = (JimpleLocal) rv;
			return out.getVariableDependency(l.getName());
			
		} else if (rv instanceof InstanceFieldRef) {
			InstanceFieldRef f = (InstanceFieldRef) rv;
			String name = f.getField().getName();
			DataDependency inst = getDataDependency(f.getBase(), out, line);
			return DataDependency.field(inst, name, line);
			
		} else if (rv instanceof ArrayRef) {
			ArrayRef a = (ArrayRef) rv;
			DataDependency inst = getDataDependency(a.getBase(), out, line);
			DataDependency index = getDataDependency(a.getIndex(), out, line);
			return DataDependency.element(inst, index, line);
			
		} else if (rv instanceof StaticFieldRef) {
			StaticFieldRef f = (StaticFieldRef) rv;
			String name = f.getField().getName();
			DataDependency inst = DataDependency.constant();
			return DataDependency.field(inst, name, line);
			
		} else if (rv instanceof CaughtExceptionRef) {
			//CaughtExceptionRef e = (CaughtExceptionRef) rv;
			return DataDependency.caughtException();
			
		} else if (rv instanceof Constant) {
			return DataDependency.constant();
			
		} else if (rv instanceof NewExpr) {
			return getDataDependenciesOfBoxes(rv.getUseBoxes(), out, line);
			
		} else if (rv instanceof InvokeExpr) {
			InvokeExpr inv = (InvokeExpr) rv;
			DataDependency self = null;
			if (rv instanceof InstanceInvokeExpr) {
				Value vSelf = ((InstanceInvokeExpr) rv).getBase();
				self = getDataDependency(vSelf, out, line);
			}
			List<DataDependency> args = new ArrayList<>();
			for (int i = 0; i < inv.getArgCount(); i++) {
				args.add(getDataDependency(inv.getArg(i), out, line));
			}
			DataDependency.InvocationResult d = DataDependency.invocationResult(inv.getMethod(), self, args);
			out.setInvoke(d.getMethodKey(), line, d);
			return d;
			
		} 
		else if (!rv.getUseBoxes().isEmpty()) {
			return getDataDependenciesOfBoxes(rv.getUseBoxes(), out, line);
		}
		
		System.out.println(rv.getClass() + " " + rv);
		return null;
	}
	
	private DataDependency getDataDependenciesOfBoxes(List<?> boxes, DataDependencyFlow out, int line) {
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<ValueBox> useBoxes = (List) boxes;
		List<DataDependency> dependencies = new ArrayList<>();
		for (ValueBox vb: useBoxes) {
			DataDependency dd = getDataDependency(vb.getValue(), out, line);
			if (dd != null) dependencies.add(dd);
		}
		if (dependencies.size() == 1) return dependencies.get(0);
		return DataDependency.all(dependencies);
	}
	
	@Override
	protected void flowThrough(DataDependencyFlow in, Unit d, DataDependencyFlow out) {
		int line = getLineNumber(d);
		boolean logUnit = false;
		Object logObject = null;
		boolean isReturn = false;
		boolean isThrow = false;
		
		Value leftValue = null;
		DataDependency value = null;
		in.copyTo(out);
		if (d instanceof DefinitionStmt) {
			DefinitionStmt jId = (DefinitionStmt) d;
			leftValue = jId.getLeftOp();
			
			Value rv = jId.getRightOp();
			value = getDataDependency(rv, out, line);
			if (rv instanceof CaughtExceptionRef) {
				
			}
			
		} else if (d instanceof InvokeStmt) {
			InvokeStmt ivStmt = (InvokeStmt) d;
			value = getDataDependency(ivStmt.getInvokeExpr(), out, line);
			
		} else if (d instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) d;
			DataDependency condition = getDataDependenciesOfBoxes(ifStmt.getCondition().getUseBoxes(), out, line);
			out.pushCondition(condition);
			
		} else if (d instanceof LookupSwitchStmt) {
			DataDependency condition = getDataDependenciesOfBoxes(d.getUseBoxes(), out, line);
			out.pushCondition(condition);
			
		} else if (d instanceof GotoStmt) {
			// no-op
			
		} else if (d instanceof ThrowStmt) {
			value = getDataDependenciesOfBoxes(d.getUseBoxes(), out, line);
			isThrow = true;
			
		} else if (d instanceof ReturnStmt) {
			value = getDataDependenciesOfBoxes(d.getUseBoxes(), out, line);
			isReturn = true;
			
		} else if (d instanceof ReturnVoidStmt) {
			value = DataDependency.constant();
			isReturn = true;
			
		} else if (d instanceof MonitorStmt) {
			// no-op
			
		} else {
			logUnit = true;
		}
		
		if (!setValue(leftValue, value, out, line, isReturn, isThrow)) {
			if (logObject == null) logObject = leftValue;
		} 
			
		if (logUnit || logObject != null || LOG_ALL) { 
			if (LOG_ALL && (logUnit || logObject != null)) System.out.println("!!!!!");
			logThrough(d, logObject, out);
		}
	}
	
	private boolean setValue(Value leftValue, DataDependency value, DataDependencyFlow out, int line, boolean isReturn, boolean isThrow) {
		if (isReturn) {
			if (value != null) {
				out.setReturn(line, value);
				return true;
			}
		} else if (isThrow) {
			if (value != null) {
				out.setThrow(line, value);
				return true;
			}
		} else if (leftValue != null) {
			if (leftValue instanceof JimpleLocal) {
				JimpleLocal local = (JimpleLocal) leftValue;
				if (value != null) {
					out.setVariable(line, local.getName(), value);
					return true;
				}
			} else if (leftValue instanceof FieldRef) {
				FieldRef field = (FieldRef) leftValue;
				if (value != null) {
					out.setField(line, field.getField().getName(), value);
					return true;
				}
			} else if (leftValue instanceof ArrayRef) {
//				ArrayRef aRef = (ArrayRef) leftValue;
				if (value != null) {
					out.setArray(line, value);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected DataDependencyFlow newInitialFlow() {
		DataDependencyFlow ddf = new DataDependencyFlow(dependencies);
		ddf.pushCondition(DataDependency.invocation());
		return ddf;
	}

	@Override
	protected DataDependencyFlow entryInitialFlow() {
		return newInitialFlow();
	}

	@Override
	protected void merge(DataDependencyFlow in1, DataDependencyFlow in2, DataDependencyFlow out) {
		if (LOG_ALL) System.out.println("merge\n    " + in1 + "\n  + " + in2);
		in1.merge(in2, out);
		if (LOG_ALL) System.out.println("  = " + out);
	}

	@Override
	protected void copy(DataDependencyFlow source, DataDependencyFlow dest) {
		source.copyTo(dest);
	}
	
	@Override
	public String toString() {
		return printMap(dependencies);
	}
	
	public static String printMap(Map<?,?> map) {
		String s = "";
		for (Map.Entry<?, ?> e: map.entrySet()) {
			s += e.getKey() + " = " + e.getValue() + "\n";
		}
		return s;
	}
}

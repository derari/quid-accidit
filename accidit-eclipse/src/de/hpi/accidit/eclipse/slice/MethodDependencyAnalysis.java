package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.hpi.accidit.eclipse.slice.CodeDependency.CVDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.TriDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.ValueDependency;
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
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class MethodDependencyAnalysis extends ForwardFlowAnalysis<Unit, CodeDependencyFlow> {
	
	public static Timer total_time = new Timer();
	public static Timer get_method_time = new Timer();
	
	static boolean LOG_ALL = false;
	
//	public static SootConfig testSootConfig() {
//		String drools = "C:/Users/derari/hpi/phd/testprojects/drools B";
//		String mvn = "C:/Users/derari/.m2";
//		String extra = "";
////		String drools = "/Users/at/projects/drools";
////		String mvn = "/Users/at/.m2";
////		String sep = ":";
////		String extra = "/Library/Java/JavaVirtualMachines/jdk1.7.0_15.jdk/Contents/Home/jre/lib/rt.jar" + sep;
//		
//		final String[] libs = { 
//					drools + "/drools-core/target/classes",
//					drools + "/drools-core/target/test-classes",
//					mvn + "/repository/junit/junit/4.11/junit-4.11.jar",
//					mvn + "/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
//					mvn + "/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar",
//					mvn + "/repository/org/drools/knowledge-api/5.5.0.Final/knowledge-api-5.5.0.Final.jar",
//					mvn + "/repository/org/drools/knowledge-internal-api/5.5.0.Final/knowledge-internal-api-5.5.0.Final.jar",
//					mvn + "/repository/org/slf4j/slf4j-api/1.7.2/slf4j-api-1.7.2.jar",
//					mvn + "/repository/org/mvel/mvel2/2.1.5.Final/mvel2-2.1.5.Final.jar",
//		};
//		return new SootConfig() {
//			@Override
//			protected List<String> getClassPath() {
//				return Arrays.asList(libs);
//			}
//		};
//	}
//	
//	public static void main(String[] args) {
//		SootConfig.enable(testSootConfig());
//		LOG_ALL = true;
////		{
////			String clazz = "org.drools.base.evaluators.TimeIntervalParser";
////			String method = "parse";
////			String signature = "(Ljava/lang/String;)[Ljava/lang/Long;";
////			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
////			System.out.println(printMap(map));
////		}
////		{
////			String clazz = "java.lang.Long";
////			String method = "valueOf";
////			String signature = "(J)Ljava/lang/Long;";
////			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
////			System.out.println(printMap(map));
////		}
//		{
//			String clazz = "java.lang.String";
//			String method = "trim";
//			String signature = "()Ljava/lang/String;";
//			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
//			System.out.println(printMap(map));
//		}
////		{
////			String clazz = "org.drools.base.evaluators.TimeIntervalParserTest";
////			String method = "sootTest";
////			String signature = "(I)I";
////			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
////			System.out.println(printMap(map));
////		}
////		{
////			String clazz = "org.drools.base.evaluators.TimeIntervalParserTest";
////			String method = "testParse3";
////			String signature = "()V";
////			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
////			System.out.println(printMap(map));
////		}
////		{
////			String clazz = "org.drools.time.TimeUtils";
////			String method = "parseTimeString";
////			String signature = "(Ljava/lang/String;)J";
////			Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
////			System.out.println(printMap(map));
////		}	
//	}
//	
	public static Map<InstructionKey, TriDependency> analyseMethod(SootMethod sMethod) {
		Body b;
		synchronized (MethodDependencyAnalysis.class) {
			b = sMethod.retrieveActiveBody();
		}
		System.out.println("Creating static dependency graph of");
		System.out.println("    " + sMethod);
		UnitGraph graph = new ExceptionalUnitGraph(b);
		MethodDependencyAnalysis analysis = new MethodDependencyAnalysis(graph);
		if (LOG_ALL) System.out.println("--- " + sMethod + " DONE.");
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
		System.out.println("Reloading for class: " + clazz);
//		for (java.lang.reflect.Field f: Scene.class.getDeclaredFields()) {
//			if (f.getName().equals("doneResolving")) {
//				try {
//					f.setAccessible(true);
//					f.set(Scene.v(), false);
//				} catch (Exception e1) {
//					throw new RuntimeException(e1);
//				}
//				break;
//			}
//		}
//		SootClass sClass = Scene.v().loadClass(clazz, SootClass.BODIES); //loadClassAndSupport(clazz);
		SootClass sClass = Scene.v().forceResolve(clazz, SootClass.BODIES);
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
				break;
			}
		}
		if (sMethod == null) {
			throw new RuntimeException("Method not found: " + clazz + "#" + method + signature);
		}
		return sMethod;
	}
	
	public static Map<InstructionKey, TriDependency> analyseMethod(String clazz, String method, String signature) {
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
	
	
	final Map<InstructionKey, TriDependency> dependencies = new TreeMap<>();
	final CodeIndex.Manager codeIndices = new CodeIndex.Manager();
	
	private MethodDependencyAnalysis(UnitGraph g) {
		super(g);
		doAnalysis();
	}
	
	private void logThrough(Unit d, Object o, CodeDependencyFlow out) {
		System.out.println("unit: " + codeIndices.get(d) + ":" + d.getClass().getSimpleName() + " " + d);
		if (o != null) {
			System.out.println("   o: " + o.getClass() + " " + o);
		}
		System.out.println("   f: " + out);
	}
	
	/**
	 * Turns a `value` expression into a data dependency.
	 * @param value
	 * @param out
	 * @param line
	 * @return
	 */
	private CVDependency getValueDependency(Value value, CodeDependencyFlow out, CodeIndex ci) {
		if (value instanceof ThisRef) {
			return CodeDependency.thisValue();
			
		} else if (value instanceof ParameterRef) {
			ParameterRef p = (ParameterRef) value;
			return CodeDependency.argument(p.getIndex());
			
		} else if (value instanceof JimpleLocal) {
			JimpleLocal l = (JimpleLocal) value;
			return out.getVariableDependency(l.getName());
			
		} else if (value instanceof InstanceFieldRef) {
			InstanceFieldRef f = (InstanceFieldRef) value;
			String name = f.getField().getName();
			CVDependency inst = getValueDependency(f.getBase(), out, ci);
			return CodeDependency.field(inst, name, ci);
			
		} else if (value instanceof ArrayRef) {
			ArrayRef a = (ArrayRef) value;
			CVDependency inst = getValueDependency(a.getBase(), out, ci);
			CVDependency index = getValueDependency(a.getIndex(), out, ci);
			return CodeDependency.element(inst, index, ci);
			
		} else if (value instanceof StaticFieldRef) {
			StaticFieldRef f = (StaticFieldRef) value;
			String name = f.getField().getName();
			ValueDependency inst = CodeDependency.constant();
			return CodeDependency.field(inst, name, ci);
			
		} else if (value instanceof CaughtExceptionRef) {
			//CaughtExceptionRef e = (CaughtExceptionRef) rv;
			return CodeDependency.caughtException();
			
		} else if (value instanceof Constant) {
			return CodeDependency.constant();
			
		} else if (value instanceof NewExpr) {
			return getDataDependenciesOfBoxes(value.getUseBoxes(), out, ci);
			
		} else if (value instanceof InvokeExpr) {
			InvokeExpr inv = (InvokeExpr) value;
			CVDependency self = null;
			if (value instanceof InstanceInvokeExpr) {
				Value vSelf = ((InstanceInvokeExpr) value).getBase();
				self = getValueDependency(vSelf, out, ci);
			} else {
				self = CodeDependency.constant();
			}
			List<CVDependency> args = new ArrayList<>();
			for (int i = 0; i < inv.getArgCount(); i++) {
				args.add(getValueDependency(inv.getArg(i), out, ci));
			}
			CodeDependency.InvocationResult d = CodeDependency.invocationResult(inv.getMethod(), self, args, ci);
			out.addInvocation(d.getMethodKey(), ci, d);
			return d;
			
		} 
		else if (!value.getUseBoxes().isEmpty()) {
			return getDataDependenciesOfBoxes(value.getUseBoxes(), out, ci);
		}
		
		System.out.println(value.getClass() + " " + value);
		return null;
	}
	
	private CVDependency getDataDependenciesOfBoxes(List<?> boxes, CodeDependencyFlow out, CodeIndex ci) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<ValueBox> useBoxes = (List) boxes;
		List<CVDependency> dependencies = new ArrayList<>();
		for (ValueBox vb: useBoxes) {
			CVDependency dd = getValueDependency(vb.getValue(), out, ci);
			if (dd != null) dependencies.add(dd);
		}
		if (dependencies.size() == 1) return dependencies.get(0);
		return CodeDependency.allCV(dependencies);
	}
	
	@Override
	protected void flowThrough(CodeDependencyFlow in, Unit unit, CodeDependencyFlow out) {
		CodeIndex ci = codeIndices.get(unit);
		boolean logUnit = false;
		Object logObject = null;
		boolean isReturn = false;
		boolean isThrow = false;
		
		Value leftValue = null;
		CVDependency value = null;
		in.copyTo(out);
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt jId = (DefinitionStmt) unit;
			leftValue = jId.getLeftOp();
			
			Value rv = jId.getRightOp();
			value = getValueDependency(rv, out, ci);
			if (rv instanceof CaughtExceptionRef) {
				
			}
			
		} else if (unit instanceof InvokeStmt) {
			InvokeStmt ivStmt = (InvokeStmt) unit;
			value = getValueDependency(ivStmt.getInvokeExpr(), out, ci);
			
		} else if (unit instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) unit;
			CVDependency condition = getDataDependenciesOfBoxes(ifStmt.getCondition().getUseBoxes(), out, ci);
			out.pushCondition(condition);
			
		} else if (unit instanceof LookupSwitchStmt) {
			CVDependency condition = getDataDependenciesOfBoxes(unit.getUseBoxes(), out, ci);
			out.pushCondition(condition);
			
		} else if (unit instanceof GotoStmt) {
			// no-op
			
		} else if (unit instanceof ThrowStmt) {
			value = getDataDependenciesOfBoxes(unit.getUseBoxes(), out, ci);
			isThrow = true;
			
		} else if (unit instanceof ReturnStmt) {
			value = getDataDependenciesOfBoxes(unit.getUseBoxes(), out, ci);
			isReturn = true;
			
		} else if (unit instanceof ReturnVoidStmt) {
			value = CodeDependency.constant();
			isReturn = true;
			
		} else if (unit instanceof MonitorStmt) {
			// no-op
			
		} else {
			logUnit = true;
		}
		
		if (!addDependencyLink(leftValue, value, out, ci, isReturn, isThrow)) {
			if (logObject == null) logObject = leftValue;
		} 
			
		if (logUnit || logObject != null || LOG_ALL) { 
			if (LOG_ALL && (logUnit || logObject != null)) System.out.println("!!!!!");
			logThrough(unit, logObject, out);
		}
	}
	
	private boolean addDependencyLink(Value leftValue, CVDependency value, CodeDependencyFlow out, CodeIndex ci, boolean isReturn, boolean isThrow) {
		if (isReturn) {
			if (value != null) {
				out.addReturn(ci, value);
				return true;
			}
		} else if (isThrow) {
			if (value != null) {
				out.addThrow(ci, value);
				return true;
			}
		} else if (leftValue != null) {
			if (leftValue instanceof JimpleLocal) {
				JimpleLocal local = (JimpleLocal) leftValue;
				if (value != null) {
					out.addVariableAssign(ci, local.getName(), value);
					return true;
				}
			} else if (leftValue instanceof FieldRef) {
				FieldRef field = (FieldRef) leftValue;
				if (value != null) {
					out.addFieldWrite(ci, field.getField().getName(), value);
					return true;
				}
			} else if (leftValue instanceof ArrayRef) {
//				ArrayRef aRef = (ArrayRef) leftValue;
				if (value != null) {
					out.addArrayWrite(ci, value);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected CodeDependencyFlow newInitialFlow() {
		CodeDependencyFlow ddf = new CodeDependencyFlow(dependencies);
		ddf.pushCondition(CodeDependency.invocation());
		return ddf;
	}

	@Override
	protected CodeDependencyFlow entryInitialFlow() {
		return newInitialFlow();
	}

	@Override
	protected void merge(CodeDependencyFlow in1, CodeDependencyFlow in2, CodeDependencyFlow out) {
		if (LOG_ALL) System.out.println("merge\n    " + in1 + "\n  + " + in2);
		in1.merge(in2, out);
		if (LOG_ALL) System.out.println("  = " + out);
	}

	@Override
	protected void copy(CodeDependencyFlow source, CodeDependencyFlow dest) {
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

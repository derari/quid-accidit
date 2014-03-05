package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
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
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
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
	
	static {
		Options.v().parse(new String[]{"-keep-line-number", "-p", "jb", "use-original-names:true"});
		String cp = Scene.v().defaultClassPath();
		
		String drools = "C:/Users/derari/hpi/phd/testprojects/drools B";
		String mvn = "C:/Users/derari/.m2";
		String sep = ";";
//		String drools = "/Users/at/projects/drools";
//		String mvn = "/Users/at/.m2";
//		String sep = ":";
		
		Scene.v().setSootClassPath(cp + sep + 
					drools + "/drools-core/target/classes" + sep +
					drools + "/drools-core/target/test-classes" + sep +
					mvn + "/repository/junit/junit/4.11/junit-4.11.jar" + sep +
					mvn + "/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" + sep +
					mvn + "/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar" + sep +
//					"/Library/Java/JavaVirtualMachines/jdk1.7.0_15.jdk/Contents/Home/jre/lib/rt.jar" + sep +
					"");
	}
	
	public static synchronized Map<Token, DataDependency> analyseMethod(SootMethod sMethod) {
		Body b = sMethod.retrieveActiveBody();
//		Body b = sMethod.getSource().getBody(sMethod, "");
		UnitGraph graph = new ExceptionalUnitGraph(b);
		MethodDataDependencyAnalysis analysis = new MethodDataDependencyAnalysis(graph);
//		System.out.println(slice.toString());
		return analysis.dependencies;
	}
	
	public static synchronized Map<Token, DataDependency> analyseMethod(String clazz, String method, String signature) {
		SootClass sClass = Scene.v().loadClassAndSupport(clazz);
		sClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		
		SootMethod sMethod = null;
		for (SootMethod m0: sClass.getMethods()) {
			if (m0.getName().equals(method) && matchSignature(m0, signature)) {
				sMethod = m0;
				System.out.println(m0);
				break;
			}
		}
		if (sMethod == null) {
			throw new RuntimeException("Method not found: " + clazz + "#" + method + signature);
		}
		return analyseMethod(sMethod);
	}
	
	private static boolean matchSignature(SootMethod m, String signature) {
		return m.getBytecodeSignature().endsWith(signature + ">");
	}
	
	public static void main(String[] args) {
		
		String clazz = "org.drools.base.evaluators.TimeIntervalParser";
		String method = "parse";
		String signature = "(Ljava/lang/String;)[Ljava/lang/Long;";
//		String clazz = "org.drools.base.evaluators.TimeIntervalParserTest";
//		String method = "sootTest";
//		String signature = "(I)I";

		
		Map<Token, DataDependency> map = analyseMethod(clazz, method, signature);
		System.out.println(printMap(map));
		
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
		System.out.println("~~~~ " + d.getClass().getSimpleName() + " " + d);
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
			
		} else if (!rv.getUseBoxes().isEmpty()) {
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
			value = getDataDependenciesOfBoxes(d.getUseBoxes(), out, line);
			
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

		} else {
			logUnit = true;
		}
		JimpleLocal local = null;
		if (leftValue instanceof JimpleLocal) local = (JimpleLocal) leftValue;
		if (local != null) {
			if (value != null) {
				out.setVariable(line, local.getName(), value);
			} else {
				logUnit = true;
			}
		} else if (isReturn) {
			if (value != null) {
				out.setReturn(line, value);
			} else {
				logUnit = true;
			}
		} else if (isThrow) {
			if (value != null) {
				out.setThrow(line, value);
			} else {
				logUnit = true;
			}
		}  else if (value != null) {
			Token t = Token.variable("?assign?", line);
			out.setOther(t, value);
		} 
			
		if (logUnit || logObject != null) 
//		{
//			System.out.println("!!!!!");
//		}
		{
			logThrough(d, logObject, out);
		}
	}

	@Override
	protected DataDependencyFlow newInitialFlow() {
		return new DataDependencyFlow(dependencies);
	}

	@Override
	protected DataDependencyFlow entryInitialFlow() {
		return newInitialFlow();
	}

	@Override
	protected void merge(DataDependencyFlow in1, DataDependencyFlow in2, DataDependencyFlow out) {
//		System.out.println("merge\n    " + in1 + "\n  + " + in2);
		in1.merge(in2, out);
//		System.out.println("  = " + out);
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

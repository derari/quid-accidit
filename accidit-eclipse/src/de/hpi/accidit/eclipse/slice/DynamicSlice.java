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
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ThisRef;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class DynamicSlice extends ForwardFlowAnalysis<Unit, DataDependencyGraph> {
	
	public static void main(String[] args) {
		Options.v().parse(new String[]{"-keep-line-number", "-p", "jb", "use-original-names:true"});
		
		String db = "jdbc:mysql://localhost:3306/accidit?user=root&password=root";
		
		String cp = Scene.v().defaultClassPath();
		
//		String drools = "C:/Users/derari/hpi/phd/testprojects/drools B";
//		String mvn = "C:/Users/derari/.m2";
		String drools = "/Users/at/projects/drools";
		String mvn = "/Users/at/.m2";
		
		Scene.v().setSootClassPath(cp + ":" + 
					drools + "/drools-core/target/classes:" +
					drools + "/drools B/drools-core/target/test-classes:" +
					mvn + "/repository/junit/junit/4.11/junit-4.11.jar:" +
					mvn + "/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:" +
					mvn + "/repository/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar:" +
					"/Library/Java/JavaVirtualMachines/jdk1.7.0_15.jdk/Contents/Home/jre/lib/rt.jar:" +
					"");
		
		String className = "org.drools.base.evaluators.TimeIntervalParser";
//		String className = "org.drools.base.evaluators.TimeIntervalParserTest";

		SootClass sClass = Scene.v().loadClassAndSupport(className);
		sClass.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		
		SootMethod sMethod = null;
		for (SootMethod m0: sClass.getMethods()) {
			if (m0.getName().equals("parse")) {
				sMethod = m0;
				System.out.println(m0);
				break;
			}
		}
		
		
		Body b = sMethod.retrieveActiveBody();
//		Body b = sMethod.getSource().getBody(sMethod, "");
		UnitGraph graph = new ExceptionalUnitGraph(b);
		DynamicSlice slice = new DynamicSlice(graph);
		System.out.println(slice.toString());
		
		// org.drools.base.evaluators.TimeIntervalParserTest #testParse3
	}
	
	Map<Token, DataDependency> dependencies = new TreeMap<>();
	
	public DynamicSlice(UnitGraph g) {
		super(g);
		doAnalysis();
		
	}
	
	private int getLineNumber(Unit d) {
		Tag t = d.getTag("LineNumberTag");
		LineNumberTag lt = (LineNumberTag) t;
		if (lt != null) return lt.getLineNumber();
		return -1;
	}
	
	private void logThrough(Unit d, Object o, DataDependencyGraph out) {
		System.out.println("~~~~ " + d.getClass().getSimpleName() + " " + d);
		if (o != null) {
			System.out.println("    " + o.getClass() + " " + o);
		}
		System.out.println("    " + out);
	}
	
	private DataDependency getDataDependency(Value rv, DataDependencyGraph out, int line) {
		
		if (rv instanceof ThisRef) {
			return DataDependency.thisValue();
			
		} else if (rv instanceof ParameterRef) {
			ParameterRef p = (ParameterRef) rv;
			return DataDependency.argument(p.getIndex());
			
		} else if (rv instanceof JimpleLocal) {
			JimpleLocal l = (JimpleLocal) rv;
			return out.getVariableDependency(l.getName());
			
		} else if (rv instanceof JInstanceFieldRef) {
			JInstanceFieldRef f = (JInstanceFieldRef) rv;
			String name = f.getField().getName();
			DataDependency inst = getDataDependency(f.getBase(), out, line);
			return DataDependency.field(inst, name, line);
			
		} else if (rv instanceof Constant) {
			return DataDependency.constant();
			
		} else if (!rv.getUseBoxes().isEmpty()) {
			return getDataDependenciesOfBoxes(rv.getUseBoxes(), out, line);
		}
		
		return null;
	}
	
	private DataDependency getDataDependenciesOfBoxes(List boxes, DataDependencyGraph out, int line) {
		
		List<ValueBox> useBoxes = boxes;
		List<DataDependency> dependencies = new ArrayList<>();
		for (ValueBox vb: useBoxes) {
			DataDependency dd = getDataDependency(vb.getValue(), out, line);
			if (dd != null) dependencies.add(dd);
		}
		if (dependencies.size() == 1) return dependencies.get(0);
		return DataDependency.all(dependencies);
	}
	
	@Override
	protected void flowThrough(DataDependencyGraph in, Unit d, DataDependencyGraph out) {
		int line = getLineNumber(d);
		boolean logUnit = false;
		boolean isReturn = false;
		Value leftValue = null;
		DataDependency value = null;
		in.copyTo(out);
		if (d instanceof JIdentityStmt) {
			JIdentityStmt jId = (JIdentityStmt) d;
			leftValue = jId.leftBox.getValue();
			
			Value rv = jId.rightBox.getValue();
			value = getDataDependency(rv, out, line);
			
		} else if (d instanceof JAssignStmt) {
			JAssignStmt jA = (JAssignStmt) d;
			leftValue = jA.leftBox.getValue();
			
			Value rv = jA.rightBox.getValue();
			value = getDataDependency(rv, out, line);
			
		} else if (d instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) d;
			DataDependency condition = getDataDependenciesOfBoxes(ifStmt.getCondition().getUseBoxes(), out, line);
			out.pushCondition(condition);
			
		} else if (d instanceof GotoStmt) {
			GotoStmt gotoStmt = (GotoStmt) d;
			JGotoStmt s;
			
		} else if (d instanceof ReturnStmt) {
			ReturnStmt retStmt = (ReturnStmt) d;
			value = getDataDependenciesOfBoxes(retStmt.getUseBoxes(), out, line);
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
		} else if (value != null) {
			Token t = Token.variable("?assign?", line);
			out.setOther(t, value);
		} 
			
//		if (logUnit) 
		{
			logThrough(d, null, out);
		}
	}

	@Override
	protected DataDependencyGraph newInitialFlow() {
		return new DataDependencyGraph(dependencies);
	}

	@Override
	protected DataDependencyGraph entryInitialFlow() {
		return newInitialFlow();
	}

	@Override
	protected void merge(DataDependencyGraph in1, DataDependencyGraph in2, DataDependencyGraph out) {
		System.out.println("merge\n    " + in1 + "\n  + " + in2);
		in1.merge(in2, out);
		System.out.println("  = " + out);
	}

	@Override
	protected void copy(DataDependencyGraph source, DataDependencyGraph dest) {
		source.copyTo(dest);
	}
	
	@Override
	public String toString() {
		String s = "";
		for (Map.Entry<Token, DataDependency> e: dependencies.entrySet()) {
			s += e.getKey() + " = " + e.getValue() + "\n";
		}
		return s;
	}
}

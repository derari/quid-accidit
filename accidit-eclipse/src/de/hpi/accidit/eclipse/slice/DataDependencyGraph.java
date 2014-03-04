package de.hpi.accidit.eclipse.slice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class DataDependencyGraph {
	
	Deque<DataDependency> conditionStack = new ArrayDeque<>();
	Map<Token, DataDependency> dependencies;
	Map<String, DataDependency> variableValues = new TreeMap<>();
	Map<String, DataDependency> currentDependencies = new TreeMap<>();
	
	public DataDependencyGraph(Map<Token, DataDependency> dependencies) {
		super();
		this.dependencies = dependencies;
	}

	public void setVariable(int line, String name, DataDependency value) {
		Token t = Token.variable(name, line);
		variableValues.put(name, value);
		dependencies.put(t, value);
		currentDependencies.put(name, DataDependency.variable(name, line));
	}
	
	public DataDependency getVariableDependency(String name) {
		return currentDependencies.get(name);
	}
	
	public void pushCondition(DataDependency cond) {
		conditionStack.push(cond);
	}
	
	public void setReturn(int line, DataDependency value) {
//		if (!conditionStack.isEmpty()) {
//			List<DataDependency> deps = new ArrayList<>(conditionStack);
//			deps.add(value);
//			value = DataDependency.all(deps);
//		}
		setVariable(line, "<return>", value);
	}
	
	public void setOther(Token t, DataDependency value) {
		dependencies.put(t, value);
	}
	
	public void copyTo(DataDependencyGraph dest) {
		dest.variableValues.clear();
		dest.variableValues.putAll(variableValues);
//		dest.dependencies.clear();
//		dest.dependencies.putAll(dependencies);
		dest.currentDependencies.clear();
		dest.currentDependencies.putAll(currentDependencies);
		dest.conditionStack.clear();
		dest.conditionStack.addAll(conditionStack);
	}
	
	public void merge(DataDependencyGraph in, DataDependencyGraph dest) {
		copyTo(dest);
		dest.dependencies.putAll(in.dependencies);
		DataDependency condition = dest.conditionStack.poll();
		if (condition == null) {
			System.out.println(" ! no condition !");
			condition = DataDependency.constant();
		}
		Map<String, DataDependency> inMap = new HashMap<>(in.currentDependencies);
		for (Map.Entry<String, DataDependency> e: currentDependencies.entrySet()) {
			String var = e.getKey();
			DataDependency v1 = e.getValue();
			DataDependency v2 = inMap.remove(var);
			if (v2 != null && !v1.equals(v2)) {
				DataDependency v = DataDependency.conditional(condition, v1, v2);
				dest.currentDependencies.put(var, v);
				dest.variableValues.put(var, v);
			}
		}
		dest.currentDependencies.putAll(inMap);
		dest.variableValues.putAll(inMap);
	}
	
	@Override
	public String toString() {
		String s = "";
		for (Map.Entry<String, DataDependency> e: variableValues.entrySet()) {
			s += e.getKey() + " = " + e.getValue() + "; ";
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(currentDependencies);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataDependencyGraph other = (DataDependencyGraph) obj;
		if (currentDependencies == null) {
			if (other.currentDependencies != null)
				return false;
		} else if (!currentDependencies.equals(other.currentDependencies))
			return false;
//		if (dependencies == null) {
//			if (other.dependencies != null)
//				return false;
//		} else if (!dependencies.equals(other.dependencies))
//			return false;
		return true;
	}
	
	
}
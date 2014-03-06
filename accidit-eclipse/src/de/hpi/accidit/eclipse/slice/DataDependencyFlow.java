package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DataDependencyFlow {
	
	List<DataDependency> controlDependencies = new ArrayList<>();
	Map<Token, DataDependency> dependencies;
	Map<String, DataDependency> variableValues = new TreeMap<>();
	Map<String, DataDependency> currentDependencies = new TreeMap<>();
	
	public DataDependencyFlow(Map<Token, DataDependency> dependencies) {
		super();
		this.dependencies = dependencies;
	}

	public DataDependency getVariableDependency(String name) {
		return currentDependencies.get(name);
	}
	
	public void pushCondition(DataDependency cond) {
		controlDependencies.add(cond);
	}
	
	public void setVariable(int line, String name, DataDependency value) {
		setValue(Token.variable(name, line), line, value);
	}
	
	public void setArray(int line, DataDependency value) {
		setValue(Token.array(line), line, value);
	}
	
	public void setReturn(int line, DataDependency value) {
		setValue(Token.result(line), line, value);
	}
	
	public void setThrow(int line, DataDependency value) {
		setValue(Token.thrown(line), line, value);
	}
	
	public void setInvoke(String methodKey, int line, DataDependency.Invoke value) {
		setValue(Token.invoke(methodKey, line), line, value);
		int i = 0;
		for (DataDependency ddA: value.args) {
			setValue(Token.invokeArg(methodKey, i++, line), line, ddA);
		}
	}
	
	public void setValue(Token t, int line, DataDependency value) {
		if (!controlDependencies.isEmpty()) {
//			value = DataDependency.complex(DataDependency.all(controlDependencies), value);
		}
		dependencies.put(t, value);
		String name = t.getVar();
		if (name != null) {
			variableValues.put(name, value);
			currentDependencies.put(name, DataDependency.variable(name, line));
		}
	}
	
	public void copyTo(DataDependencyFlow dest) {
		dest.variableValues.clear();
		dest.variableValues.putAll(variableValues);
//		dest.dependencies.clear();
//		dest.dependencies.putAll(dependencies);
		dest.currentDependencies.clear();
		dest.currentDependencies.putAll(currentDependencies);
		dest.controlDependencies.clear();
		dest.controlDependencies.addAll(controlDependencies);
	}
	
	public void merge(DataDependencyFlow in, DataDependencyFlow dest) {
		copyTo(dest);
		dest.dependencies.putAll(in.dependencies);
		
		int maxStackSize = Math.min(dest.controlDependencies.size(), in.controlDependencies.size());
		int stackSize = Math.max(0, maxStackSize-1); // pop at least one element
		for (int i = 0; i < maxStackSize; i++) {
			if (!dest.controlDependencies.get(i).equals(in.controlDependencies.get(i))) {
				stackSize = i;
			}
		}
		
		List<DataDependency> dp1 = dest.controlDependencies.subList(stackSize, dest.controlDependencies.size());
		List<DataDependency> dp2 = in.controlDependencies.subList(stackSize, in.controlDependencies.size());
		Set<DataDependency> mergeDependencies = new TreeSet<>();
		mergeDependencies.addAll(dp1);
		mergeDependencies.addAll(dp2);
		DataDependency condition = DataDependency.all(mergeDependencies);
		dp1.clear();
		
		Map<String, DataDependency> inMap = new HashMap<>(in.currentDependencies);
		for (Map.Entry<String, DataDependency> e: currentDependencies.entrySet()) {
			String var = e.getKey();
			DataDependency v1 = e.getValue();
			DataDependency v2 = inMap.remove(var);
			if (v2 != null && !v1.equals(v2)) {
				DataDependency v = DataDependency.complex(condition, DataDependency.choice(v1, v2));
				dest.currentDependencies.put(var, v);
				dest.variableValues.put(var, v);
			}
		}
		dest.currentDependencies.putAll(inMap);
		dest.variableValues.putAll(inMap);
	}
	
	@SuppressWarnings("unused")
	@Override
	public String toString() {
		String s = "";
		for (DataDependency dd: controlDependencies) {
			s += //dd + 
					"/";
		}
		for (Map.Entry<String, DataDependency> e: variableValues.entrySet()) {
			s += " " + e.getKey() + " = " + e.getValue() + ";";
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
		DataDependencyFlow other = (DataDependencyFlow) obj;
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
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
	
	List<DataDependency> branchDependencyStack = new ArrayList<>();
	Map<String, DataDependency> variableValues = new TreeMap<>();
	Map<String, DataDependency> currentDependencies = new TreeMap<>();
	
	Map<Token, DataDependency> dependencies;
	
	public DataDependencyFlow(Map<Token, DataDependency> dependencies) {
		super();
		this.dependencies = dependencies;
	}

	public DataDependency getVariableDependency(String name) {
		return currentDependencies.get(name);
	}
	
	public void pushCondition(DataDependency cond) {
		branchDependencyStack.add(cond);
	}
	
	public void setVariable(int line, String name, DataDependency value) {
		setValue(Token.variable(name, line), line, value);
	}
	
	public void setField(int line, String name, DataDependency value) {
		setValue(Token.field(name, line), line, value);
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
	
	public void setInvoke(String methodKey, int line, DataDependency.InvocationResult value) {
		setValue(Token.invoke(methodKey, line), line, value);
		DataDependency self = value.getSelf();
		if (self != null) {
			setValue(Token.invokeThis(methodKey, line), line, self);
		}
		int i = 0;
		for (DataDependency ddA: value.args) {
			setValue(Token.invokeArg(methodKey, i++, line), line, ddA);
		}
	}
	
	public void setValue(Token t, int line, DataDependency value) {
		if (branchDependencyStack.isEmpty()) {
			dependencies.put(t, value);
		} else {
			DataDependency reachable = DataDependency.reach(DataDependency.all(branchDependencyStack), value);
			dependencies.put(t, reachable);
		}
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
		dest.branchDependencyStack.clear();
		dest.branchDependencyStack.addAll(branchDependencyStack);
	}
	
	public void merge(DataDependencyFlow in, DataDependencyFlow dest) {
		copyTo(dest);
		dest.dependencies.putAll(in.dependencies);
		
		int maxStackSize, stackSize;
		if (in.branchDependencyStack.isEmpty()) {
			maxStackSize = stackSize = dest.branchDependencyStack.size();
		} else {
			maxStackSize = Math.min(dest.branchDependencyStack.size(), in.branchDependencyStack.size());
			stackSize = dest.branchDependencyStack.size() != in.branchDependencyStack.size()
					? maxStackSize // pop only delta
					: Math.max(0, maxStackSize-1); // pop at least one element
			for (int i = 0; i < maxStackSize; i++) {
				if (!dest.branchDependencyStack.get(i).equals(in.branchDependencyStack.get(i))) {
					stackSize = i;
				}
			}
		}
		List<DataDependency> dp1 = dest.branchDependencyStack.subList(stackSize, dest.branchDependencyStack.size());
		stackSize = Math.min(stackSize, in.branchDependencyStack.size()); 
		List<DataDependency> dp2 = in.branchDependencyStack.subList(stackSize, in.branchDependencyStack.size());
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
	
	@Override
	public String toString() {
		String s = "";
		for (DataDependency dd: branchDependencyStack) {
			s += (MethodDataDependencyAnalysis.LOG_ALL ? dd : "") + 
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
package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.hpi.accidit.eclipse.slice.CodeDependency.CVDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.TriDependency;
import de.hpi.accidit.eclipse.slice.CodeDependency.ValueDependency;

/**
 * The dependency configuration at a specific instruction.
 */
public class CodeDependencyFlow {
	
	/** Currently open branches */
	List<ValueDependency> branchDependencyStack = new ArrayList<>();
	
	/** Only for debugging purposes, values of variables */
	Map<String, CodeDependency> variableValues = new TreeMap<>();
	
	/** Dependencies at this instruction */
	Map<String, CVDependency> currentDependencies = new TreeMap<>();
	
	/** All known dependencies */
	Map<InstructionKey, TriDependency> dependencies;
	
	public CodeDependencyFlow(Map<InstructionKey, TriDependency> dependencies) {
		super();
		this.dependencies = dependencies;
	}

	public CVDependency getVariableDependency(String name) {
		return currentDependencies.get(name);
	}
	
	public void pushCondition(CVDependency cond) {
		branchDependencyStack.add(cond.getValue());
	}
	
	public void addVariableAssign(CodeIndex ci, String name, CVDependency value) {
		addDependency(InstructionKey.variable(name, ci), value);
	}
	
	public void addFieldWrite(CodeIndex ci, String name, CVDependency value) {
		addDependency(InstructionKey.field(name, ci), value);
	}
	
	public void addArrayWrite(CodeIndex ci, CVDependency value) {
		addDependency(InstructionKey.array(ci), value);
	}
	
	public void addReturn(CodeIndex ci, CVDependency value) {
		addDependency(InstructionKey.result(ci), value);
	}
	
	public void addThrow(CodeIndex ci, CVDependency value) {
		addDependency(InstructionKey.thrown(ci), value);
	}
	
	public void addInvocation(String methodKey, CodeIndex ci, CodeDependency.InvocationResult value) {
		addDependency(InstructionKey.invoke(methodKey, ci), value);
		CVDependency self = value.getSelf();
		if (self != null) {
			addDependency(InstructionKey.invokeThis(methodKey, ci), self);
		}
		int i = 0;
		for (CVDependency ddA: value.args) {
			addDependency(InstructionKey.invokeArg(methodKey, i++, ci), ddA);
		}
	}
	
	/**
	 * @param id    current instruction
	 * @param value dependency value
	 */
	public void addDependency(InstructionKey id, CVDependency value) {
		if (branchDependencyStack.isEmpty()) {
			dependencies.put(id, value.reachVia(CodeDependency.constant()));
		} else {
			CVDependency reach = CodeDependency.allFlat(branchDependencyStack);
			dependencies.put(id, value.reachVia(reach));
		}
		String name = id.getVar();
		if (name != null) {
			variableValues.put(name, value);
			currentDependencies.put(name, CodeDependency.variable(name, id.getCodeIndex()));
		}
	}
	
	public void copyTo(CodeDependencyFlow dest) {
		dest.variableValues.clear();
		dest.variableValues.putAll(variableValues);
//		dest.dependencies.clear();
//		dest.dependencies.putAll(dependencies);
		dest.currentDependencies.clear();
		dest.currentDependencies.putAll(currentDependencies);
		dest.branchDependencyStack.clear();
		dest.branchDependencyStack.addAll(branchDependencyStack);
	}
	
	public void merge(CodeDependencyFlow in, CodeDependencyFlow dest) {
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
					break;
				}
			}
		}
		List<ValueDependency> dp1 = dest.branchDependencyStack.subList(stackSize, dest.branchDependencyStack.size());
		stackSize = Math.min(stackSize, in.branchDependencyStack.size()); 
		List<ValueDependency> dp2 = in.branchDependencyStack.subList(stackSize, in.branchDependencyStack.size());
		Set<CVDependency> mergeDependencies = new TreeSet<>();
		mergeDependencies.addAll(dp1);
		mergeDependencies.addAll(dp2);
		ValueDependency condition = CodeDependency.allFlat(mergeDependencies);
		dp1.clear();
		
		Map<String, CVDependency> inMap = new HashMap<>(in.currentDependencies);
		for (Map.Entry<String, CVDependency> e: currentDependencies.entrySet()) {
			String var = e.getKey();
			CVDependency v1 = e.getValue();
			CVDependency v2 = inMap.remove(var);
			if (v2 != null && !v1.equals(v2)) {
				CVDependency v = CodeDependency.anyOf(v1, v2).withControl(condition);
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
		for (CodeDependency dd: branchDependencyStack) {
			s += (MethodDependencyAnalysis.LOG_ALL ? dd : "") + 
					"/";
		}
		for (Map.Entry<String, CodeDependency> e: variableValues.entrySet()) {
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
		CodeDependencyFlow other = (CodeDependencyFlow) obj;
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
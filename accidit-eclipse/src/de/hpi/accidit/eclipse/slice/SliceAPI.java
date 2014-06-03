package de.hpi.accidit.eclipse.slice;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

import de.hpi.accidit.eclipse.slice.SootConfig.JavaProjectConfig;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationData;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;

public class SliceAPI {
	
	private IJavaProject project = null;
	private SootConfig sootConfig = null;
	private Set<ValueKey> slicingCritera = new LinkedHashSet<>();
	
	private int testId = -1;
	private ValueKey rootInvocation;
	
	public void reset(IJavaProject project, int testId) {
		this.project = project;
		this.sootConfig = null;
		this.testId = testId;
		clear();
	}
	
	public void clear() {
		if (project != null) {
			sootConfig = new JavaProjectConfig(project);
		}
		if (testId >= 0) {
			rootInvocation = new InvocationKey(testId, 0L);
		}
		slicingCritera.clear();
	}
	
	public void addCriterion(ValueKey key) {
		slicingCritera.add(key);
	}

	public void removeCriterion(ValueKey key) {
		slicingCritera.remove(key);
	}
	
	public InvocationData getInvocationData() {
		return rootInvocation.getInvD();
	}
}

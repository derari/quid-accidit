package de.hpi.accidit.eclipse.slice;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.slice.DynamicSlice.Node;
import de.hpi.accidit.eclipse.slice.SootConfig.JavaProjectConfig;
import de.hpi.accidit.eclipse.slice.SootConfig.WorkspaceConfig;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationData;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;

public class SliceAPI {
	
	private final Runnable onUpdate;
	
	private IJavaProject project = null;
	private SootConfig sootConfig = null;
	private Set<ValueKey> slicingCritera = new LinkedHashSet<>();
	private SortedSet<Long> sliceSteps = null;
	
	private int testId = -1;
	private ValueKey rootInvocation;
	
	public SliceAPI(Runnable onUpdate) {
		this.onUpdate = onUpdate;
	}
	
	public void reset(IJavaProject project, int testId) {
		init(project, testId);
		clear();
	}
	
	private void init(IJavaProject project, int testId) {
		this.project = project;
		this.testId = testId;
		if (project != null) {
			sootConfig = new JavaProjectConfig(project);
		} else {
			sootConfig = new WorkspaceConfig();
		}
		if (testId >= 0) {
			rootInvocation = new InvocationKey(testId, 0L);
		}
	}
	
	private void init() {
		if (sootConfig == null) {
			IProject p = DatabaseConnector.getSelectedProject();
			IJavaProject jp = JavaCore.create(p);
			init(jp, TraceNavigatorUI.getGlobal().getTestId());
		}
	}
	
	public void clear() {
		slicingCritera.clear();
		updateSlice();
	}
	
	public void addCriterion(ValueKey key) {
		slicingCritera.add(key);
		updateSlice();
	}

	public void removeCriterion(ValueKey key) {
		slicingCritera.remove(key);
		updateSlice();
	}
	
	public InvocationData getInvocationData() {
		init();
		return rootInvocation.getInvD();
	}
	
	public int getTestId() {
		init();
		return testId;
	}
	
	private static final Timer time = new Timer();
	
	private void updateSlice() {
		sliceSteps = null;
		if (!slicingCritera.isEmpty()) {
			time.enter();
			init();
			DynamicSlice slicer = new DynamicSlice(sootConfig);
			for (ValueKey k: slicingCritera) {
				slicer.addCriterion(k);
			}
			SortedMap<ValueKey, Node> result = slicer.getSlice();
			sliceSteps = new TreeSet<>();
			for (ValueKey k: result.keySet()) {
				sliceSteps.add(k.getStep());
			}
			time.exit();
			DynamicSlice.printTimers(time);
		}
		onUpdate.run();
	}
	
	public SortedSet<Long> getSliceSteps() {
		return sliceSteps;
	}
}

package de.hpi.accidit.eclipse.slice;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.slice.DynamicSlice.Node;
import de.hpi.accidit.eclipse.slice.DynamicSlice.OnSliceUpdate;
import de.hpi.accidit.eclipse.slice.SootConfig.JavaProjectConfig;
import de.hpi.accidit.eclipse.slice.SootConfig.WorkspaceConfig;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationData;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;

public class SliceAPI {
	
	private final Runnable onUpdate;
	
	private IJavaProject project = null;
	private SootConfig sootConfig = null;
	private DynamicSlice dynamicSlice = null;
	private Set<ValueKey> slicingCritera = new LinkedHashSet<>();
	private SortedSet<Long> sliceSteps = null;
	private long lastUpdate = 0;
	private final Object updateGuard = new Object();
	
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
		dynamicSlice = null;
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
	
	private synchronized DynamicSlice dynamicSlice() {
		init();
		if (dynamicSlice == null) {
			OnSliceUpdate onUpdate = new OnSliceUpdate() {
				@Override
				public void run(boolean done) { onSliceUpdate(done); }
			};
			dynamicSlice = new DynamicSlice(sootConfig, onUpdate);
		}
		return dynamicSlice;
	}
	
	private void onSliceUpdate(boolean done) {
		synchronized (updateGuard) {
			if (!done && lastUpdate + 500 > System.currentTimeMillis()) {
				return;
			}
			lastUpdate = System.currentTimeMillis();
		}
		synchronized (this) {
			SortedMap<ValueKey, Node> result = dynamicSlice().getSlice();
			sliceSteps = new TreeSet<>();
			for (ValueKey k: result.keySet()) {
				sliceSteps.add(k.getStep());
			}
			onUpdate.run();			
		}
	}
	
	public void clear() {
		slicingCritera.clear();
		onUpdate.run();
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
	
	public SortedSet<Long> getSliceSteps() {
		return sliceSteps;
	}

	public void setCriterion(ValueKey key, int flags) {
		dynamicSlice().setCriterion(key, flags);
	}
	
	public void removeCriterion(ValueKey key) {
		setCriterion(key, -1);
	}
}

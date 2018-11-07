package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
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
import de.hpi.accidit.eclipse.slice.DynamicSlice.OnSliceUpdate;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationData;
import de.hpi.accidit.eclipse.slice.EventKey.InvocationKey;
import de.hpi.accidit.eclipse.slice.SootConfig.JavaProjectConfig;
import de.hpi.accidit.eclipse.slice.SootConfig.WorkspaceConfig;

/**
 * The Eclipse plugin interacts with the slicing component through this API.
 */
public class SliceAPI {
	
	private final Runnable onUpdate;
	
	private IJavaProject project = null;
	private SootConfig sootConfig = null;
	private DynamicSlice dynamicSlice = null;
	private Set<EventKey> slicingCritera = new LinkedHashSet<>();
	private SortedSet<Long> sliceSteps = null;
	private long lastUpdate = 0;
	private final Object updateGuard = new Object();
	
	private int testId = -1;
	private EventKey rootInvocation;
	
	public SliceAPI(Runnable onUpdate) {
		this.onUpdate = onUpdate;
	}
	
	public void reset(IJavaProject project, int testId) {
		try {
			init(project, testId);
		} catch (NoSuchElementException e) {
		} catch (RuntimeException e) {
			e.printStackTrace(System.err);
		}
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
			SortedMap<EventKey, Node> result = dynamicSlice().getSlice();
			sliceSteps = new TreeSet<>();
			for (EventKey k: result.keySet()) {
				sliceSteps.add(k.getStep());
			}
			onUpdate.run();			
		}
	}
	
	public void clear() {
		dynamicSlice().clear();
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
	
//	private static final Timer time = new Timer();
	
	public SortedSet<Long> getSliceSteps() {
		return sliceSteps;
	}

	public void setCriterion(EventKey key, int flags) {
		dynamicSlice().setCriterion(key, flags);
	}
	
	public void removeCriterion(EventKey key) {
		setCriterion(key, -1);
	}
	
	public Set<Node> getCriteria() {
		return dynamicSlice().getCriteria();
	}

	public int getFlags(EventKey key) {
		return dynamicSlice().getNode(key).getFlags();
	}

	public SortedMap<EventKey, Node> getSlice() {
		return dynamicSlice().getSlice();
	}
	
	public Collection<Node> getFilteredSlice() {
		Collection<Node> result = new ArrayList<>();
		Collection<Node> slice = getSlice().values();
		for (Node n: slice) {
			if (!n.isInternal()) {
				result.add(n);
			}
		}
		return result;
	}
}

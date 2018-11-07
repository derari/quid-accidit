package de.hpi.accidit.eclipse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.hpi.accidit.eclipse.breakpoints.BreakpointsManager;
import de.hpi.accidit.eclipse.breakpoints.BreakpointsView;
import de.hpi.accidit.eclipse.history.HistoryView;
import de.hpi.accidit.eclipse.model.ExitEvent;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.Trace;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.model.db.TraceDB;
import de.hpi.accidit.eclipse.slice.SliceAPI;
import de.hpi.accidit.eclipse.slice.SlicingCriteriaView;
import de.hpi.accidit.eclipse.views.AcciditView;
import de.hpi.accidit.eclipse.views.TraceExplorerView;
import de.hpi.accidit.eclipse.views.VariablesView;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;
import de.hpi.accidit.eclipse.views.util.JavaSrcFilesLocator;
import de.hpi.accidit.eclipse.views.util.WorkPool;

// TODO rename

public class TraceNavigatorUI {

	private static TraceNavigatorUI GLOBAL = null;
	
	public static TraceNavigatorUI getGlobal() {
		if (GLOBAL == null) GLOBAL = new TraceNavigatorUI();
		return GLOBAL;
	}
	
	private IWorkbenchPage mainPage = null;
	
	private final Set<AcciditView> views = Collections.synchronizedSet(new HashSet<AcciditView>());
	
	private final JavaSrcFilesLocator srcFilesLocator = new JavaSrcFilesLocator();
	private final BreakpointsManager breakpointsManager = new BreakpointsManager();
	
	// Trace
	private int testId;
	private Trace trace;
	private TraceElement current;
	private boolean currentBefore;
	
	private final SliceAPI sliceApi = new SliceAPI(new Runnable() {
		@Override
		public void run() {
			sliceSteps = null;
			DoInUiThread.run(new Runnable() {
				@Override
				public void run() {
					for (AcciditView v: views) {
						v.sliceChanged();
					}
				}
			});
		}
	});
	private SortedSet<Long> sliceSteps = null;
	
	public TraceNavigatorUI() {	}

	public void setTraceExplorer(TraceExplorerView traceExplorer) {
		this.mainPage = traceExplorer.getViewSite().getPage();
		addView(traceExplorer);
	}
	
	public void addView(AcciditView view) {
		if (current != null) {
			view.setStep(current, currentBefore);
		}
		views.add(view);
	}
	
	public void removeView(AcciditView view) {
		views.remove(view);
	}
	
	public <T> T findView(Class<T> type) {
		for (Object o: views) {
			if (type.isInstance(o)) {
				return type.cast(o);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T findOrOpenView(Class<T> type, String id) {
		T t = findView(type);
		if (t != null) return t;
		try {
			t = (T) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id);
		} catch (PartInitException e) {
			// TODO:: decide: print stack trace or raise exception
			e.printStackTrace();
		}
		return t;
	}
	
	public Trace getTrace() {
		if (trace == null || trace.id != testId) {
			trace = new Trace(testId, this);
		}
		return trace;
	}
	
	public TraceExplorerView getTraceExplorer() {
		return findView(TraceExplorerView.class);
	}
	
	public BreakpointsView getBreakpointsView() {
		return findView(BreakpointsView.class);
	}
	
	public VariablesView getVariablesView() {
		return findView(VariablesView.class);
	}
	
//	public HistoryView getHistoryView() {
//		return findView(HistoryView.class);
//	}
	
	public HistoryView getOrOpenHistoryView() {
		return findOrOpenView(HistoryView.class, HistoryView.ID);
	}
	
	public SlicingCriteriaView getSlicingCriteriaView() {
		return findView(SlicingCriteriaView.class);
	}
	
	public SlicingCriteriaView getOrOpenSlicingCriteriaView() {
		return findOrOpenView(SlicingCriteriaView.class, SlicingCriteriaView.ID);
//		SlicingCriteriaView slicingCriteriaView = getSlicingCriteriaView();
//		if (slicingCriteriaView == null) {
//			try {
//				slicingCriteriaView = (SlicingCriteriaView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(SlicingCriteriaView.ID);
//			} catch (PartInitException e) {
//				// TODO:: decide: print stack trace or raise exception
//				e.printStackTrace();
//			}
//		}
//		return slicingCriteriaView;
	}
	
	public TraceDB db() {
		return DatabaseConnector.getTraceDB();
	}
	
//	public void refresh() {
//		Display.getDefault().asyncExec(new Runnable() {
//		    public void run() {
//		    	if (traceExplorer != null) traceExplorer.refresh();
//		    }
//		});
//	}
	
	public int getTestId() {
		return testId;
	}
	
	public long getStep() {
		return current.getStep();
	}
	
	public long getCallStep() {
		return current.getCallStep();
	}
	
	public void setTestId(final int testId) {
		this.testId = testId;
		
		// current project may have changed, too
		IProject project = DatabaseConnector.getSelectedProject();
		IJavaProject jp = JavaCore.create(project);
		getSliceApi().reset(jp, testId);
		if (getSlicingCriteriaView() != null) {
			getSlicingCriteriaView().clear();
		}

		// TODO: open trace explorer
		if (getTraceExplorer() != null) {
			getTraceExplorer().refreshTrace();
		}
		
		setStep(0, true);
	}
	
	public void setStep(long newStep, boolean before) {
		WorkPool.executePriority(() -> {
			try {
				TraceElement te = getTrace().getStep(newStep);
				DoInUiThread.run(() -> setStep(te, before));
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		});
	}
	
	public void setStep(TraceElement le, boolean before) {
		current = le;
		currentBefore = before;
		for (AcciditView v: views) {
			v.setStep(le, before);
		}
		if (!before) {
			if (le instanceof ExitEvent) {
				le = le.parent;
			} else if (le instanceof Invocation) {
				TraceElement[] te = ((Invocation) le).getChildren();
				if (te != null && te.length > 0) {
					le = te[0];
				}
			}
		}
		if (le == null) return;
		if (le.parent == null) {
			TraceElement te = getTrace().getStep(le.getStep());
			le.parent = te.parent;
		}
		showCode(le);
	}
	
	public void showCode(TraceElement le) {
		srcFilesLocator.open(le, mainPage, getTraceExplorer());
	}
	
	public BreakpointsManager getBreakpointsManager() {
		return breakpointsManager;
	}
	
	public SliceAPI getSliceApi() {
		return sliceApi;
	}
	
	public SortedSet<Long> getSliceSteps() {
		if (sliceSteps == null) {
			sliceSteps = sliceApi.getSliceSteps();
		}
		return sliceSteps;
	}
}

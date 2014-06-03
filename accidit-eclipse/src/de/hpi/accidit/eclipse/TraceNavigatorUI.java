package de.hpi.accidit.eclipse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cthul.miro.MiConnection;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.IWorkbenchPage;

import de.hpi.accidit.eclipse.breakpoints.BreakpointsManager;
import de.hpi.accidit.eclipse.breakpoints.BreakpointsView;
import de.hpi.accidit.eclipse.history.HistoryView;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.slice.SliceAPI;
import de.hpi.accidit.eclipse.slice.SlicingCriteriaView;
import de.hpi.accidit.eclipse.views.AcciditView;
import de.hpi.accidit.eclipse.views.TraceExplorerView;
import de.hpi.accidit.eclipse.views.VariablesView;
import de.hpi.accidit.eclipse.views.util.JavaSrcFilesLocator;

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
	private TraceElement current;
	
	private final SliceAPI sliceApi = new SliceAPI();
	
	public TraceNavigatorUI() {	}

	public void setTraceExplorer(TraceExplorerView traceExplorer) {
		this.mainPage = traceExplorer.getViewSite().getPage();
		addView(traceExplorer);
	}
	
	public void addView(AcciditView view) {
		if (current != null) {
			view.setStep(current);
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
	
	public TraceExplorerView getTraceExplorer() {
		return findView(TraceExplorerView.class);
	}
	
	public BreakpointsView getBreakpointsView() {
		return findView(BreakpointsView.class);
	}
	
	public VariablesView getVariablesView() {
		return findView(VariablesView.class);
	}
	
	public HistoryView getHistoryView() {
		return findView(HistoryView.class);
	}
	
	public SlicingCriteriaView getSlicingCriteriaView() {
		return findView(SlicingCriteriaView.class);
	}
	
	public MiConnection cnn() {
		return DatabaseConnector.getValidOConnection();
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
		if (project instanceof IJavaProject) {
			getSliceApi().reset((IJavaProject) project, testId);
		} else {
			getSliceApi().reset(null, testId);
		}
		
		if (getTraceExplorer() == null) {
			// TODO: open trace explorer
		}
		setStep(0);
	}
	
	public void setStep(final long newStep) {
		setStep(new TraceElement() {{
			this.testId = TraceNavigatorUI.this.testId;
			this.step = newStep;
		}});
	}
	
	public void setStep(TraceElement le) {
		current = le;
		for (AcciditView v: views) {
			v.setStep(le);
		}
		if (le.parent != null) {
			String filePath = le.parent.type;
			srcFilesLocator.open(filePath, le.line, mainPage, getTraceExplorer());
		}
	}
	
	public BreakpointsManager getBreakpointsManager() {
		return breakpointsManager;
	}
	
	public SliceAPI getSliceApi() {
		return sliceApi;
	}
}

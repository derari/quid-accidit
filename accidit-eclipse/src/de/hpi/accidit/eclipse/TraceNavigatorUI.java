package de.hpi.accidit.eclipse;

import org.cthul.miro.MiConnection;
import org.eclipse.ui.IWorkbenchPage;

import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.TraceExplorerView;
import de.hpi.accidit.eclipse.views.util.JavaSrcFilesLocator;

// TODO rename

public class TraceNavigatorUI {

	private static TraceNavigatorUI GLOBAL = null;
	
	public static TraceNavigatorUI getGlobal() {
		if (GLOBAL == null) GLOBAL = new TraceNavigatorUI();
		return GLOBAL;
	}
	
	// UI
	private IWorkbenchPage mainPage = null;
	private TraceExplorerView traceExplorer = null;
	private LocalsExplorerView localsExplorer = null;
	
	private final JavaSrcFilesLocator srcFilesLocator = new JavaSrcFilesLocator();
	
	// Trace
	private int testId;
	private long callStep;
	private long step;
	
	public TraceNavigatorUI() { }

	public void setTraceExplorer(TraceExplorerView traceExplorer) {
		this.mainPage = traceExplorer.getViewSite().getPage();
		this.traceExplorer = traceExplorer;
		traceExplorer.setTestCaseId(testId);
	}
	
	public void unsetTraceExplorer(TraceExplorerView traceExplorer) {
		if (this.traceExplorer == traceExplorer) {
			this.traceExplorer = null;
		}
	}
	
	public TraceExplorerView getTraceExplorer() {
		return traceExplorer;
	}
	
	public void setLocalsExprorer(LocalsExplorerView localsExprorer) {
		this.localsExplorer = localsExprorer;
	}
	
	public void unsetLocalsExprorer(LocalsExplorerView localsExprorer) {
		if (this.localsExplorer == localsExprorer) {
			this.localsExplorer = null;
		}
	}
	
	public LocalsExplorerView getLocalsExplorer() {
		return localsExplorer;
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
		return step;
	}
	
	public long getCallStep() {
		return callStep;
	}
	
	public void setTestId(int testId) {
		this.step = 0;
		this.testId = testId;
		if (traceExplorer == null) {
			// TODO: open trace explorer
		}
		if (traceExplorer != null) traceExplorer.setTestCaseId(testId);
		if (localsExplorer != null) localsExplorer.setStep(testId, 0, 0);
	}
	
	public void setStep(long step) {
		this.step = step;
	}

	public void setStep(TraceElement le) {
		setStep(le.step);
		callStep = 0;
		if (le.parent != null) {
			callStep = le.parent.step;
			String filePath = le.parent.type;
			srcFilesLocator.open(filePath, le.line, mainPage, traceExplorer);

			if (localsExplorer != null) {
				localsExplorer.setStep(le.parent.testId, le.parent.step, le.step);
			}			
		}
	}
	
}

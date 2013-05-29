package de.hpi.accidit.eclipse;

import org.cthul.miro.MiConnection;
import org.eclipse.ui.IWorkbenchPage;

import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;
import de.hpi.accidit.eclipse.views.util.JavaSrcFilesLocator;

public class TraceNavigatorUI {

	private static TraceNavigatorUI GLOBAL = null;
	
	public static TraceNavigatorUI getGlobal() {
		if (GLOBAL == null) GLOBAL = new TraceNavigatorUI();
		return GLOBAL;
	}
	
	// UI
	private IWorkbenchPage mainPage = null;
	private MethodExplorerView traceExplorer = null;
	private LocalsExplorerView localsExplorer = null;
	
	private final JavaSrcFilesLocator srcFilesLocator = new JavaSrcFilesLocator();
	
	// Trace
	private int testId;
	private long step;
	
	public TraceNavigatorUI() {
	}

	public void setTraceExplorer(MethodExplorerView traceExplorer) {
		this.mainPage = traceExplorer.getViewSite().getPage();
		this.traceExplorer = traceExplorer;
		traceExplorer.setTestCaseId(testId);
	}
	
	public void unsetTraceExplorer(MethodExplorerView traceExplorer) {
		if (this.traceExplorer == traceExplorer) {
			this.traceExplorer = null;
		}
	}
	
	public void setLocalsExprorer(LocalsExplorerView localsExprorer) {
		this.localsExplorer = localsExprorer;
	}
	
	public void unsetLocalsExprorer(LocalsExplorerView localsExprorer) {
		if (this.localsExplorer == localsExprorer) {
			this.localsExplorer = null;
		}
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
	
	public void setTestId(int testId) {
		this.step = 0;
		this.testId = testId;
		if (traceExplorer != null) traceExplorer.setTestCaseId(testId);
		if (localsExplorer != null) localsExplorer.setStep(testId, 0, 0);
	}
	
	public void setStep(long step) {
		this.step = step;
	}

	public void setStep(TraceElement le) {
		setStep(le.step);
		if (le.parent != null) {
			String filePath = le.parent.type;
			srcFilesLocator.open(filePath, le.line, mainPage, traceExplorer);

			if (localsExplorer != null) {
				localsExplorer.setStep(le.parent.testId, le.parent.step, le.step);
			}
		}
	}
	
}

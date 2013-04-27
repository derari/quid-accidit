package de.hpi.accidit.eclipse;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.LineElement;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;
import de.hpi.accidit.eclipse.views.util.JavaSrcFilesLocator;
import de.hpi.accidit.orm.OConnection;

public class TraceNavigatorUI {

	private static TraceNavigatorUI GLOBAL = null;
	
	public static TraceNavigatorUI getGlobal() {
		if (GLOBAL == null) GLOBAL = new TraceNavigatorUI();
		return GLOBAL;
	}
	
	// UI
	private IWorkbenchPage mainPage = null;
	private MethodExplorerView traceExplorer = null;
	private LocalsExplorerView localsExprorer = null;
	
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
		this.localsExprorer = localsExprorer;
	}
	
	public void unsetLocalsExprorer(LocalsExplorerView localsExprorer) {
		if (this.localsExprorer == localsExprorer) {
			this.localsExprorer = null;
		}
	}
	
	public OConnection cnn() {
		return DatabaseConnector.getValidOConnection();
	}
	
	public void refresh() {
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {
		    	if (traceExplorer != null) traceExplorer.refresh();
		    }
		});
	}
	
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
	}
	
	public void setStep(long step) {
		this.step = step;
	}

	public void setStep(TraceElement le) {
		setStep(le.step);
		if (le.parent != null) {
			String filePath = le.parent.type;
			srcFilesLocator.open(filePath, le.line, mainPage);
		}
	}
	
}

package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.TestCase;
import de.hpi.accidit.eclipse.handlers.util.TestCaseSelectionDialog;
import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class SelectTestCaseHandler extends AbstractHandler {
	
	private static final String ACCIDIT_PERSPECTIVE_ID = "de.hpi.accidit.eclipse.AcciditPerspective";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ElementTreeSelectionDialog dialog = 
				new TestCaseSelectionDialog(
						HandlerUtil.getActiveShell(event), 
						new TestCaseSelectionDialog.TestCaseSelectionLabelProvider(), 
						new TestCaseSelectionDialog.TestCaseSelectionContentProvider());
		dialog.setTitle("Select a test case");
		dialog.setBlockOnOpen(true);
		dialog.setInput(HandlerUtil.getActiveEditorInput(event));
		dialog.setEmptyListMessage("The specified database contains no test cases.");
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result == null || result.length < 1) return null;
			
			TestCase newTestCase = (TestCase) result[0];
			TraceNavigatorUI.getGlobal().setTestId(newTestCase.id);
			
			try {
				IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.showPerspective(ACCIDIT_PERSPECTIVE_ID, workbench.getActiveWorkbenchWindow());
			} catch (WorkbenchException e) {
				e.printStackTrace();
			}
			
			TraceExplorerView traceExplorer = 
				(TraceExplorerView) HandlerUtil.getActiveWorkbenchWindow(event)
					.getActivePage().findView(TraceExplorerView.ID);
			traceExplorer.refresh();
		}

		return null;
	}
	
}

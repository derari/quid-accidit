package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.handlers.util.TestCase;
import de.hpi.accidit.eclipse.handlers.util.TestCaseSelectionContentProvider;
import de.hpi.accidit.eclipse.handlers.util.TestCaseSelectionDialog;
import de.hpi.accidit.eclipse.handlers.util.TestCaseSelectionLabelProvider;
import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class SelectTestCaseHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MethodExplorerView methodExplorerView = 
				(MethodExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(MethodExplorerView.ID);
				
		ITreeContentProvider contentProvider = new TestCaseSelectionContentProvider();
		ILabelProvider labelProvider = new TestCaseSelectionLabelProvider();
		
		ElementTreeSelectionDialog dialog = 
				new TestCaseSelectionDialog(HandlerUtil.getActiveShell(event), labelProvider, contentProvider);
		dialog.setTitle("Select a test case");
		dialog.setBlockOnOpen(true);
		dialog.setInput(HandlerUtil.getActiveEditorInput(event));
		dialog.setEmptyListMessage("The specified database contains no test cases at all.");
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result == null || result.length < 1) return null;
			TestCase newTestCase = (TestCase) result[0];

			methodExplorerView.setTestCaseId(newTestCase.id);
			methodExplorerView.refresh();
		}

		return null;
	}
	
}

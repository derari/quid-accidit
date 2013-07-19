package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.TestCase;
import de.hpi.accidit.eclipse.handlers.util.TestCaseSelectionDialog;
import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class SelectTestCaseForProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection.isEmpty()) return null;
		if (!(currentSelection instanceof TreeSelection)) return null;
		
		Object firstElement = ((TreeSelection) currentSelection).getFirstElement();
		if (!(firstElement instanceof IProject)) return null;
		
		IProject currentlySelectedProject = (IProject) firstElement;
		IProject previouslySelectedProject = DatabaseConnector.getSelectedProject();
		
		DatabaseConnector.setSelectedProject(currentlySelectedProject);
		boolean successfulTestCaseSelection = openTestCaseSelectionDialog(event);
		if (!successfulTestCaseSelection) {
			DatabaseConnector.setSelectedProject(previouslySelectedProject);
		}
		
		return null;
	}

	private boolean openTestCaseSelectionDialog(ExecutionEvent event) {
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
			if (result == null || result.length < 1) return false;
			
			TestCase newTestCase = (TestCase) result[0];
			TraceNavigatorUI.getGlobal().setTestId(newTestCase.id);
			
			MethodExplorerView methodExplorerView = 
				(MethodExplorerView) HandlerUtil.getActiveWorkbenchWindow(event)
					.getActivePage().findView(MethodExplorerView.ID);
			methodExplorerView.refresh();
			
			return true;
		}
		return false;
	}
}

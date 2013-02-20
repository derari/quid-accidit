package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class SelectTestCaseHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MethodExplorerView methodExplorerView = 
				(MethodExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(MethodExplorerView.ID);
		
		InputDialog dialog = new InputDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell(),
				"Select Test Case",	"Please enter the test case id!", String.valueOf(methodExplorerView.getTestCaseId()), null);
		if (dialog.open() == IStatus.OK) {
			String value = dialog.getValue();

			methodExplorerView.setTestCaseId(Integer.parseInt(value));
			methodExplorerView.refresh();
			
			// TODO remove localsExplorer here and add refresh to methodExplorer ...
			LocalsExplorerView localsExplorerView = 
					(LocalsExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(LocalsExplorerView.ID);
			localsExplorerView.selectedMethodChanged(null);
		}

		return null;
	}
	
}

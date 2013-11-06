package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		NamedValue treeViewerInput = null;
		NamedValue selectedLocal = null;
		if (!selectedLocals.isEmpty()) {
			selectedLocal = (NamedValue) selectedLocals.getFirstElement();
			if (selectedLocal instanceof NamedValue.VariableValue) {
				treeViewerInput = new NamedValue.VariableHistory(
						TraceNavigatorUI.getGlobal().getTestId(), 
						TraceNavigatorUI.getGlobal().getCallStep(), 
						selectedLocal.getId());
			}
		}

		LocalsHistoryDialog dialog = new LocalsHistoryDialog(
				HandlerUtil.getActiveShell(event), 
				selectedLocal, 
				new LocalsHistoryDialog.TreeViewerContentProvider(),
				treeViewerInput, 
				TraceNavigatorUI.getGlobal().getLocalsExplorer().getRootElements());

		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length > 0) {
				NamedValue.VariableValue variableValue = (NamedValue.VariableValue) result[0];
				long step = variableValue.getStep();
				
				TraceExplorerView traceExplorer = TraceNavigatorUI.getGlobal().getTraceExplorer();
				traceExplorer.getSelectionAdapter().selectAtStep(step);
			}
		}
		return null;
	}
}

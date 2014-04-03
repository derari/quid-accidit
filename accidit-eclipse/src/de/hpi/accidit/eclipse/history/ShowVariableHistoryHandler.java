package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		ITreeSelection treeSelection = (ITreeSelection) selection;
		NamedValueNode node = (NamedValueNode) treeSelection.getFirstElement();
		
		HistoryDialog dialog = new HistoryDialog(HandlerUtil.getActiveShell(event), node);
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length > 0) {
				NamedValue variableValue = ((NamedValueNode) result[0]).getValue();
				long step = variableValue.getStep();
				TraceNavigatorUI.getGlobal().setStep(step);
			}
		}
		return null;
	}
}

package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class ShowLastChangeStepHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection sel = (ITreeSelection) TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		if (sel == null || sel.isEmpty()) return null;

		NamedValueNode selection = (NamedValueNode) sel.getFirstElement();
		if (selection == null || selection.getDepth() != 1) return null;

		NamedValue variableValue = selection.getValue();
		if (variableValue instanceof NamedValue.VariableValue) {
			long step = variableValue.getValueStep();
			if (step < 0) { // the correct step is already selected
				TraceNavigatorUI.getGlobal().getTraceExplorer().setFocus();
			} else {
				TraceNavigatorUI.getGlobal().setStep(step, true);
			}
		}

		return null;
	}
}

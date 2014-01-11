package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class ShowLastChangeStepHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection selection = (ITreeSelection) TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		if (selection.isEmpty()) return null;
		
		NamedValue selectedVariable = (NamedValue) selection.getFirstElement();		
		if (selectedVariable instanceof NamedValue.VariableValue) {
			TraceExplorerView traceExplorer = TraceNavigatorUI.getGlobal().getTraceExplorer();
			
			long step = selectedVariable.getValueStep();
			if (step < 0) { // the correct step is already selected
				traceExplorer.setFocus();
			} else {
				TraceNavigatorUI.getGlobal().setStep(step);
//				traceExplorer.getSelectionAdapter().selectAtStep(step);
			}
		}
		
		return null;
	}
}

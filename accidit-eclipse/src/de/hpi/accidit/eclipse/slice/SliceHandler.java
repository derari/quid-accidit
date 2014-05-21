package de.hpi.accidit.eclipse.slice;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class SliceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection sel = (ITreeSelection) TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		if (sel == null || sel.isEmpty()) return null;
		
		NamedValueNode selection = (NamedValueNode) sel.getFirstElement();
		if (selection == null) return null;
		
		NamedValue variableValue = selection.getValue();
		if (variableValue instanceof NamedValue.VariableValue) {
			SlicingCriteriaView slicingCriteriaView = TraceNavigatorUI.getGlobal().getSlicingCriteriaView();
			String message = variableValue.getName();
			slicingCriteriaView.addLine(message);
		}
		
		return null;
	}
}

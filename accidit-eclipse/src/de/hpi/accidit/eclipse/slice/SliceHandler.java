package de.hpi.accidit.eclipse.slice;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class SliceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection sel = (ITreeSelection) TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		if (sel == null || sel.isEmpty()) return null;
		
		NamedValueNode selection = (NamedValueNode) sel.getFirstElement();
		if (selection == null) return null;
		
//		SlicingCriteriaView slicingCriteriaView = TraceNavigatorUI.getGlobal().getSlicingCriteriaView();
		SlicingCriteriaView slicingCriteriaView = TraceNavigatorUI.getGlobal().getOrOpenSlicingCriteriaView();
		
		NamedValue value = selection.getValue();
		if (value instanceof NamedValue.VariableValue) {
			slicingCriteriaView.addVariableValue((VariableValue) value);
		} else if (value instanceof NamedValue.FieldValue) {
			slicingCriteriaView.addFieldValue((FieldValue) value);
		}
				
		return null;
	}
}

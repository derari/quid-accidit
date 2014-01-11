package de.hpi.accidit.eclipse.history;

import static org.cthul.miro.DSL.select;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.history.HistorySource.MethodCallSource;
import de.hpi.accidit.eclipse.history.HistorySource.ObjectSource;
import de.hpi.accidit.eclipse.model.ArrayIndex;
import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		int currentTestId = TraceNavigatorUI.getGlobal().getTestId();
		long currentCallStep = TraceNavigatorUI.getGlobal().getCallStep();
		
		ISelection selection = TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		ITreeSelection treeSelection = (ITreeSelection) selection;
		NamedValueNode nvn = (NamedValueNode) treeSelection.getFirstElement();
		NamedValue nv = (NamedValue) nvn.getValue();
		
		int selectedNamedValueId = nv.getId();
		NamedEntity[] options = null;
		HistorySource src = null;

		if (nv instanceof NamedValue.VariableValue) {
			src = new MethodCallSource(currentTestId, currentCallStep);
			options = select().from(Variable.VIEW)
					.inCall(currentTestId, currentCallStep).orderById()
					._execute(DatabaseConnector.cnn())._asArray();
		} else if (nv instanceof NamedValue.FieldValue) {
			ObjectSnapshot owner = (ObjectSnapshot) nv.getOwner();
			long thisId = owner.getThisId();

			src = new ObjectSource(currentTestId, thisId, false);
			options = select().from(Field.VIEW)
					.ofObject(currentTestId, thisId).orderById()
					._execute(DatabaseConnector.cnn())._asArray();
		} else if (nv instanceof NamedValue.ItemValue) {
			ObjectSnapshot owner = (ObjectSnapshot) nv.getOwner();
			long thisId = owner.getThisId();
			int arrayLength = owner.getArrayLength();
			
			src = new ObjectSource(currentTestId, thisId, true);
			options = ArrayIndex.newIndexArray(arrayLength);
		} else {
			throw new UnsupportedOperationException(String.valueOf(selectedNamedValueId));
		}

		HistoryDialog dialog = new HistoryDialog(
				HandlerUtil.getActiveShell(event), 
				src,
				selectedNamedValueId, 
				options);

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

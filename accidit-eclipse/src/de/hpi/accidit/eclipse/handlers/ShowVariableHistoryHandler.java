package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.localsHistory.HistorySource;
import de.hpi.accidit.eclipse.localsHistory.HistorySource.MethodCallSource;
import de.hpi.accidit.eclipse.localsHistory.HistorySource.ObjectSource;
import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryDialog;
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
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		NamedValueNode nvn = (NamedValueNode) selectedLocals.getFirstElement();
		NamedValue nv = (NamedValue) nvn.getValue();
		int selectedNamedValueId = nv.getId();
		int testId = TraceNavigatorUI.getGlobal().getTestId();

		NamedEntity[] options = null;
		HistorySource src;

		if (nv instanceof NamedValue.VariableValue) {
			long callStep = TraceNavigatorUI.getGlobal().getCallStep();
			
			src = new MethodCallSource(testId, callStep);
			options = DatabaseConnector.cnn().select()
					.from(Variable.VIEW).inCall(testId, callStep).orderById()
					.asArray()._execute();
		} else if (nv instanceof NamedValue.FieldValue) {
			ObjectSnapshot owner = (ObjectSnapshot) nv.getOwner();
			long thisId = owner.getThisId();

			src = new ObjectSource(testId, thisId, false);
			options = DatabaseConnector.cnn().select()
					.from(Field.VIEW).ofObject(testId, thisId).orderById()
					.asArray()._execute();
		} else if (nv instanceof NamedValue.ItemValue) {
			ObjectSnapshot owner = (ObjectSnapshot) nv.getOwner();
			long thisId = owner.getThisId();
			int arrayLength = owner.getArrayLength();
			
			src = new ObjectSource(testId, thisId, true);
			options = ArrayIndex.newIndexArray(arrayLength);
		} else {
			throw new UnsupportedOperationException(String.valueOf(selectedNamedValueId));
		}

		LocalsHistoryDialog dialog = new LocalsHistoryDialog(
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

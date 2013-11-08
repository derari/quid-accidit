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
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog.HistorySource;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog.MethodCallSource;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog.ObjectSource;
import de.hpi.accidit.eclipse.model.ArrayIndex;
import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.views.TraceExplorerView;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		NamedEntity[] options = null;
		int selected = -1;
		boolean variable = false;
		long thisId = -1;
		int arrayLength = -1;
		Value owner = null;
		if (!selectedLocals.isEmpty()) {
			NamedValueNode nvn = (NamedValueNode) selectedLocals.getFirstElement();
			NamedValue nv = (NamedValue) nvn.getValue();
			if (nv instanceof NamedValue.VariableValue) {
				variable = true;
				selected = nv.getId();
			} else if (nv instanceof NamedValue.FieldValue) {
				selected = nv.getId();
				owner = nv.getOwner();
				if (owner instanceof ObjectSnapshot) {
					thisId = ((ObjectSnapshot) owner).getThisId();
				}
			} else if (nv instanceof NamedValue.ItemValue) {
				selected = nv.getId();
				owner = nv.getOwner();
				if (owner instanceof ObjectSnapshot) {
					thisId = ((ObjectSnapshot) owner).getThisId();
					arrayLength = ((ObjectSnapshot) owner).getArrayLength();
				}
			}
		}
		
		HistorySource src;
		if (variable) {
			long testId = TraceNavigatorUI.getGlobal().getTestId();
			long callStep = TraceNavigatorUI.getGlobal().getCallStep(); 
			src = new MethodCallSource(testId, callStep);
			options = DatabaseConnector.cnn().select()
					.from(Variable.VIEW).inCall(testId, callStep).orderById()
					.asArray()._execute();
		} else if (thisId != -1) {
			long testId = TraceNavigatorUI.getGlobal().getTestId();
			if (arrayLength > -1) {
				src = new ObjectSource(testId, thisId, true);
				options = ArrayIndex.newIndexArray(arrayLength);
			} else {
				src = new ObjectSource(testId, thisId, false);
				options = DatabaseConnector.cnn().select()
						.from(Field.VIEW).ofObject(testId, thisId).orderById()
						.asArray()._execute();
			}
		} else {
			throw new UnsupportedOperationException(String.valueOf(selected));
		}

		LocalsHistoryDialog dialog = new LocalsHistoryDialog(
				HandlerUtil.getActiveShell(event), 
				src,
				selected, 
				options);

		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length > 0) {
				NamedValue variableValue = ((NamedValueNode) result[0]).getValue();
				long step = variableValue.getStep();
				TraceNavigatorUI.getGlobal().setStep(step);
//				TraceExplorerView traceExplorer = TraceNavigatorUI.getGlobal().getTraceExplorer();
//				traceExplorer.getSelectionAdapter().selectAtStep(step);
			}
		}
		return null;
	}
}

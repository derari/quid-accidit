package de.hpi.accidit.eclipse.history;

import static org.cthul.miro.DSL.select;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;

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

public class ShowFieldsHistoriesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		int currentTestId = TraceNavigatorUI.getGlobal().getTestId();
		long currentCallStep = TraceNavigatorUI.getGlobal().getCallStep();
		
		NamedValueNode node = (NamedValueNode) selectedLocals.getFirstElement();
		NamedValueNode childNode = (NamedValueNode) node.getChild(0);
		if (childNode == null) return null;
		NamedValue namedValue = (NamedValue) childNode.getValue();
				
		HistorySource src = null;
		NamedEntity[] options = null;
		
		if (namedValue instanceof NamedValue.VariableValue) {
			src = new MethodCallSource(currentTestId, currentCallStep);
			options = select().from(Variable.VIEW)
					.inCall(currentTestId, currentCallStep).orderById()
					._execute(DatabaseConnector.cnn())._asArray();
		} else if (namedValue instanceof NamedValue.FieldValue) {
			ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
			long thisId = owner.getThisId();
			
			src = new ObjectSource(currentTestId, thisId, false);
			options = select().from(Field.VIEW)
					.ofObject(currentTestId, thisId).orderById()
					._execute(DatabaseConnector.cnn())._asArray();
		} else if (namedValue instanceof NamedValue.ItemValue) {
			ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
			long thisId = owner.getThisId();
			int arrayLength = owner.getArrayLength();
			
			src = new ObjectSource(currentTestId, thisId, true);
			options = ArrayIndex.newIndexArray(arrayLength);
		} else {
			return null;
		}
		
		LocalsHistoryView localsHistoryView = TraceNavigatorUI.getGlobal().getLocalsHistoryView();
		LocalsHistoryContainer localsHistory = localsHistoryView.getContainer();
		localsHistory.setHistorySource(src);
		localsHistory.setComboViewerOptions(options);
		localsHistory.setComboViewerSelection(-1);
		localsHistory.refresh();

		return null;
	}

}

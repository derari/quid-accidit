package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class ShowFieldsHistoriesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = TraceNavigatorUI.getGlobal().getVariablesView().getSelection();
		ITreeSelection treeSelection = (ITreeSelection) selection;
		NamedValueNode node = (NamedValueNode) treeSelection.getFirstElement();
		NamedValueNode childNode = (NamedValueNode) node.getChild(0);
		
		HistoryView variablesView = TraceNavigatorUI.getGlobal().getHistoryView();
		HistoryContainer historyContainer = variablesView.getContainer();
		historyContainer.updateFromContentNode(childNode);
		historyContainer.showAll();

		return null;
	}

}

package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.history.HistoryContainer.ContentNodesPathway;

public class GoBackHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ContentNodesPathway contentNodesPathway = TraceNavigatorUI.getGlobal().getOrOpenHistoryView().getContainer().getContentNodesPathway();
		contentNodesPathway.goBack();
		return null;
	}

}

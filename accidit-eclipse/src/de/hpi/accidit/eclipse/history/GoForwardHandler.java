package de.hpi.accidit.eclipse.history;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.history.HistoryContainer.ContentNodesPathway;

public class GoForwardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ContentNodesPathway contentNodesPathway = TraceNavigatorUI.getGlobal().getHistoryView().getContainer().getContentNodesPathway();
		contentNodesPathway.goForward();
		return null;
	}

}

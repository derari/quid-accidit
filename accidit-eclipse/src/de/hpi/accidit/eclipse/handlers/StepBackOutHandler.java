package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class StepBackOutHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TraceExplorerView traceExplorer = 
				(TraceExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(TraceExplorerView.ID);
		traceExplorer.getSelectionAdapter().selectParentElement();
		return null;
	}
}

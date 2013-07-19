package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class OpenTraceExplorerViewHandler extends AbstractHandler {
	
	public OpenTraceExplorerViewHandler() {}

	/**
	 * The command has been executed, so extract the needed information from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		try {
			page.showView(TraceExplorerView.ID);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}

		return null;
	}
}

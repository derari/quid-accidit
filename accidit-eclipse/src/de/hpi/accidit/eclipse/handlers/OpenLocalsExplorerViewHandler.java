package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.LocalsExplorerView;

public class OpenLocalsExplorerViewHandler extends AbstractHandler {

	public OpenLocalsExplorerViewHandler() {}

	/**
	 * The command has been executed, so extract the needed information from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage page = window.getActivePage();

		try {
			page.showView(LocalsExplorerView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		return null;
	}
}

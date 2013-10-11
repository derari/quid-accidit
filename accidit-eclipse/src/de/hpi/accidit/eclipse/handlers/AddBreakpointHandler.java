package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.BreakpointsViewx;

public class AddBreakpointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		BreakpointsViewx breakpointsView = 
				(BreakpointsViewx) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(BreakpointsViewx.ID);
		breakpointsView.addBreakpointLine();
		return null;
	}

}

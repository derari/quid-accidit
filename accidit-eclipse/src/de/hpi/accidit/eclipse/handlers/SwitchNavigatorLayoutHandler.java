package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.NavigatorView;

public class SwitchNavigatorLayoutHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		NavigatorView view = 
				(NavigatorView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(NavigatorView.ID);
		view.switchLayout();
		return null;
	}

}

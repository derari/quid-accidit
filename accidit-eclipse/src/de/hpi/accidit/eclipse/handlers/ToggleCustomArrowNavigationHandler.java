package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public class ToggleCustomArrowNavigationHandler extends AbstractToggleHandler {
	
	/** The ID of the command as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.commands.toggleCustomArrowNavigation";
	
	@Override
	protected void executeToggle(ExecutionEvent event, boolean checked) {
		if (!checked) {
			ICommandService commandService = (ICommandService) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow()
					.getService(ICommandService.class);
			Command command = commandService.getCommand(ToggleAutoCollapseHandler.ID);
			AbstractToggleHandler.setCommandState(command, false);
		}
	}

}

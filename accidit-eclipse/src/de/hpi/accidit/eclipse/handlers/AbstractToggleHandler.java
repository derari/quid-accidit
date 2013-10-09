package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

public abstract class AbstractToggleHandler extends AbstractHandler {

	protected abstract void executeToggle(ExecutionEvent event, boolean checked);

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Command command = event.getCommand();
		final boolean oldValue = HandlerUtil.toggleCommandState(command);

		executeToggle(event, !oldValue);
		return null;
	}

	/** Sets the new state of the command. */
	public static void setCommandState(final Command command, final boolean value) {
		State state = getState(command);
		if (!state.getValue().equals(value))
			state.setValue(new Boolean(value));
	}

	/** Get the current command state. */
	public static boolean getCommandState(final Command command) {
		return (Boolean) getState(command).getValue();
	}

	private static State getState(final Command command) {
		return command.getState(RegistryToggleState.STATE_ID);
	}

}

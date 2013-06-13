package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class SelectTestCaseForProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection.isEmpty()) return null;
		if (!(currentSelection instanceof TreeSelection)) return null;
		
		Object firstElement = ((TreeSelection) currentSelection).getFirstElement();
		if (!(firstElement instanceof IProject)) return null;
		
		IProject selectedProject = (IProject) firstElement;
		
		// TODO store selected project in property
		// TODO retrieve requested test cases
		
		return null;
	}
}

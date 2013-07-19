package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class StepIntoHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TraceExplorerView traceExplorer = 
				(TraceExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(TraceExplorerView.ID);
		TreeViewer viewer = traceExplorer.getTreeViewer();
		
		TreeItem selectedItem = viewer.getTree().getSelection()[0];
		if (selectedItem == null) { // no selection, no tree navigation
			// TODO: better feedback for the user
			return null;
		}
		
		if (selectedItem.getItemCount() != 0) { // base case: select the subelement
			viewer.expandToLevel(selectedItem.getData(), 1);
			viewer.setSelection(new StructuredSelection(selectedItem.getItem(0).getData()));
			return null;
		}
		
		// TODO make this case unavailable!
		viewer.setSelection(new StructuredSelection());
		return null;

	}
}

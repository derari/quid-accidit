package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class StepOutHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {		
		MethodExplorerView methodExplorerView = 
				(MethodExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(MethodExplorerView.ID);
		TreeViewer viewer = methodExplorerView.getTreeViewer();
		
		TreeItem selectedItem = viewer.getTree().getSelection()[0];
		if (selectedItem == null) { // no selection, no tree navigation
			// TODO: better feedback for the user
			return null;
		}		
		
		TreeItem parentItem = selectedItem.getParentItem();	
		if (parentItem == null) { // root item selected
			viewer.setSelection(new StructuredSelection());
			return null;
		}
		
		// get next item
		TreeItem parentsParentItem = parentItem.getParentItem();	
		if (parentsParentItem == null) { // stepped out into root level
			viewer.setSelection(new StructuredSelection(parentItem.getData()));
			parentItem.setExpanded(false);
			return null;
		}

		int nextIndex = parentsParentItem.indexOf(parentItem) + 1;
		if (nextIndex == parentsParentItem.getItemCount()) { // stepped out from last subelement
			viewer.setSelection(new StructuredSelection(parentItem.getData()));
			parentItem.setExpanded(false);
			return null;
		}
		
		TreeItem newSelectedItem = parentsParentItem.getItems()[nextIndex];
		viewer.setSelection(new StructuredSelection(newSelectedItem.getData()));
		parentItem.setExpanded(false);
		return null;
	}

}

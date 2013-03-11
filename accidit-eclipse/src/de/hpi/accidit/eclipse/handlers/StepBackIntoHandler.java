package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class StepBackIntoHandler extends AbstractHandler {

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

		int newParentIndex = parentItem.indexOf(selectedItem) - 1;
		if (newParentIndex == -1) { // stepped out from last subelement, indexOutOfBounds
			viewer.setSelection(new StructuredSelection(parentItem.getData()));
			parentItem.setExpanded(false);
			return null;
		}
		
		// expand and load child items
		TreeItem newParentItem = parentItem.getItem(newParentIndex);
		viewer.expandToLevel(newParentItem.getData(), 1);
		
		int newParentItemCount = newParentItem.getItemCount();
		if (newParentItemCount == 0) {
			viewer.setSelection(new StructuredSelection());
			return null;			
		}

		TreeItem newSelectedItem = newParentItem.getItem(newParentItemCount - 1);
		viewer.setSelection(new StructuredSelection(newSelectedItem.getData()));
		return null;
	}
}

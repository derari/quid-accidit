package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class StepOutHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "StepOutHandler in Place!", "Go on! >>> ");
		
		MethodExplorerView methodExplorerView = 
				(MethodExplorerView) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(MethodExplorerView.ID);
		
		Tree tree = methodExplorerView.getTreeViewer().getTree();
		TreeItem selectedItem = tree.getSelection()[0];
		TreeItem parentItem = selectedItem.getParentItem();	
		
		//tree.setSelection(parentItem);
		TreeViewer viewer = methodExplorerView.getTreeViewer();
		viewer.setSelection(new StructuredSelection(parentItem.getData()));
		
		parentItem.setExpanded(false);
		
		int index = parentItem.indexOf(selectedItem);
		System.out.println("dings: " + index);
		
//		int parentIndex = parentItem.getParentItem().indexOf(parentItem);
		
		
//		TreeViewer viewer = methodExplorerView.getTreeViewer();
//		ITreeSelection selection = (ITreeSelection) viewer.getSelection();
//		CalledMethod selectedMethod = (CalledMethod) selection.getFirstElement();
//		viewer.setSelection(selectedMethod);
//		
//		viewer.setS
		
		return null;
	}

}

package de.hpi.accidit.eclipse.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.MethodExplorerView;

public class ShowVariableHistoryHandler extends AbstractHandler {
	
	// TODO put setSelection() in Display.getDefault().asyncExec() ...

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		NamedValue treeViewerInput = null;
		NamedValue selectedLocal = null;
		if (!selectedLocals.isEmpty()) {
			selectedLocal = (NamedValue) selectedLocals.getFirstElement();
			if (selectedLocal instanceof NamedValue.VariableValue) {
				treeViewerInput = new NamedValue.VariableHistory(
						TraceNavigatorUI.getGlobal().getTestId(), 
						TraceNavigatorUI.getGlobal().getCallStep(), 
						selectedLocal.getId());
			}
		}
		
		LocalsHistoryDialog dialog = new LocalsHistoryDialog(
				HandlerUtil.getActiveShell(event), 
				selectedLocal, 
				new LocalsHistoryDialog.TreeViewerContentProvider(null),
				treeViewerInput, 
				TraceNavigatorUI.getGlobal().getLocalsExplorer().getRootElements());
		
		if (dialog.open() != Window.OK) return null;
		Object[] result = dialog.getResult();
		
		if (result.length < 1) return null;
		NamedValue.VariableValue variableValue = (NamedValue.VariableValue) result[0];
		long step = variableValue.getStep();
		
		MethodExplorerView traceExplorer = TraceNavigatorUI.getGlobal().getTraceExplorer();
		TreeViewer treeViewer = traceExplorer.getTreeViewer();
		
		// targetElement
		
		TraceElement[] elements = traceExplorer.getRootElements();
		List<Object> pathSegments = new ArrayList<Object>();
		
		while (true) {
			TraceElement currentElement = null;
			for (int i = 0; i < elements.length; i++) {
				currentElement = elements[i];
				
				if (currentElement.step == step) {
					pathSegments.add(currentElement);
					treeViewer.setSelection(new TreeSelection(new TreePath(pathSegments.toArray())));
					return null;
				}

				// Too far in the tree - go back to previous element. 
				if (currentElement.step > step) {
					if (i >= 1) currentElement = elements[i - 1];
					break;
				}
			}
			
			if (currentElement == null) return null;
			pathSegments.add(currentElement);
			
			if (currentElement instanceof Invocation) {
				treeViewer.expandToLevel(new TreePath(pathSegments.toArray()), 1);
				elements = ((Invocation) currentElement).getChildren();
			} else {
				
				
				
				treeViewer.setSelection(new TreeSelection(new TreePath(pathSegments.toArray()).getParentPath()));
				return null;
			}			
		}
	}
}

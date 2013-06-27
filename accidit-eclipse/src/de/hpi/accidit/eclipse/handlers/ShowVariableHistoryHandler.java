package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryContentProvider;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryDialog;
import de.hpi.accidit.eclipse.model.NamedValue;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		
		ITreeSelection selectedLocals = (ITreeSelection) sel;
		if (selectedLocals.size() < 1) return null;
		NamedValue selectedLocal = (NamedValue) selectedLocals.getFirstElement();

		// TODO eval: no treeViewerInput for NamedValue (this)
		NamedValue treeViewerInput = null;
		if (selectedLocal instanceof NamedValue.VariableValue) {
			treeViewerInput = new NamedValue.VariableHistory(
					TraceNavigatorUI.getGlobal().getTestId(), 
					TraceNavigatorUI.getGlobal().getCallStep(), 
					selectedLocal.getId());
		}
		
		LocalsHistoryContentProvider treeViewerContentProvider = new LocalsHistoryContentProvider(null);
		treeViewerContentProvider.setStep(TraceNavigatorUI.getGlobal().getTestId(), TraceNavigatorUI.getGlobal().getCallStep(), -1);
		
		NamedValue[] comboViewerInput = TraceNavigatorUI.getGlobal().getLocalsExplorer().getRootElements();
		
		LocalsHistoryDialog dialog = new LocalsHistoryDialog(
				HandlerUtil.getActiveShell(event), 
				selectedLocal, 
				treeViewerContentProvider,
				treeViewerInput, 
				comboViewerInput);
		treeViewerContentProvider.setRoot(treeViewerInput);
		dialog.setTreeViewerInput(treeViewerInput);
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length < 1) return null;
			
			// TODO: process result to navigate in the MethodExplorerView
			
			System.out.println("Dialog result: " + result[0]);
		}
		
		return null;
	}
}

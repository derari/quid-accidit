package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.handlers.util.LocalsHistorySelectionDialog;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;
import de.hpi.accidit.eclipse.views.dataClasses.Method;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;
import de.hpi.accidit.eclipse.views.provider.LocalsHistoryContentProvider;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelectionService selectionService = HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService();
		ITreeSelection selectedMethods = (ITreeSelection) selectionService.getSelection(MethodExplorerView.ID);
		ITreeSelection selectedLocals = (ITreeSelection) selectionService.getSelection(LocalsExplorerView.ID);

		Method method = (Method) selectedMethods.getFirstElement();
		if (selectedLocals.size() < 1) return null;
		LocalBase local = (LocalBase) selectedLocals.getFirstElement();
		
		ITreeContentProvider contentProvider = new LocalsHistoryContentProvider(method.testId, method, local);
		ILabelProvider labelProvider = new LocalsLabelProvider();
		
		ElementTreeSelectionDialog dialog = 
				new LocalsHistorySelectionDialog(HandlerUtil.getActiveShell(event), labelProvider, contentProvider);
		dialog.setTitle("Varible History");
		dialog.setBlockOnOpen(true);
		dialog.setInput(HandlerUtil.getActiveEditorInput(event));
		dialog.setEmptyListMessage("The selected variable wasn't set in the traced context at all.");		
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length < 1) return null;
			
			// TODO: process result to navigate in the MethodExplorerView
			
			System.out.println("Dialog result: " + result[0]);
		}
		
		return null;
	}
}

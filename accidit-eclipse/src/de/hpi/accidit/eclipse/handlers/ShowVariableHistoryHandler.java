package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistoryContentProvider;
import de.hpi.accidit.eclipse.handlers.util.LocalsHistorySelectionDialog;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class ShowVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = TraceNavigatorUI.getGlobal().getLocalsExplorer().getSelection();
		
		ITreeSelection selectedLocals = (ITreeSelection) sel;

		if (selectedLocals.size() < 1) return null;
		NamedValue var = (NamedValue) selectedLocals.getFirstElement();
		
		LocalsHistoryContentProvider cp = new LocalsHistoryContentProvider(null);
		
		ElementTreeSelectionDialog dialog = 
				new LocalsHistorySelectionDialog(HandlerUtil.getActiveShell(event), var, cp);
		
		NamedValue root = null;
		cp.setStep(TraceNavigatorUI.getGlobal().getTestId(), TraceNavigatorUI.getGlobal().getCallStep(), -1);
		if (var instanceof NamedValue.VariableValue) {
			root = new NamedValue.VariableHistory(TraceNavigatorUI.getGlobal().getTestId(), TraceNavigatorUI.getGlobal().getCallStep(), var.getId());
		}
		
		cp.setRoot(root);
		dialog.setTitle("History of \"" + var.getName() + "\"");
		dialog.setBlockOnOpen(true);
		dialog.setInput(root);
		dialog.setEmptyListMessage("No Data.");
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length < 1) return null;
			
			// TODO: process result to navigate in the MethodExplorerView
			
			System.out.println("Dialog result: " + result[0]);
		}
		
		return null;
	}
}

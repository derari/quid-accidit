package de.hpi.accidit.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

// TODO remove as unused ...

public class OpenMethodExplorerViewActionDelegate implements IWorkbenchWindowActionDelegate {

    public static final String ID = "de.hpi.accidit.eclipse.views.MethodExplorerView";
    
	private IWorkbenchWindow window;
	
	@Override
	public void init(IWorkbenchWindow window) {
        this.window = window;
     }

	@Override
	public void dispose() {}
	
	@Override
	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();

        try {
           page.showView(ID);
        } catch (PartInitException e) {
        	e.printStackTrace();
        }
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {}

}

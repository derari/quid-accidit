package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.views.dataClasses.Method;
import de.hpi.accidit.eclipse.views.provider.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsExplorerView extends ViewPart implements ISelectionListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.LocalsExplorerView";

	private TreeViewer viewer;
	private LocalsContentProvider contentProvider;

	public LocalsExplorerView() {}

	@Override
	public void createPartControl(Composite parent) {		
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setHeaderVisible(true);
		
		TreeColumn column0 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column0.setText("Local Name");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(100);
		TreeColumn column2 = new TreeColumn(viewer.getTree(), SWT.RIGHT);
		column2.setText("Change Step");
		column2.setWidth(75);
		TreeColumn column3 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column3.setText("Type");
		column3.setWidth(500);
		
		contentProvider = new LocalsContentProvider();		
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new LocalsLabelProvider());
		viewer.setInput(getViewSite());
		
		getSite().setSelectionProvider(viewer);
		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part instanceof MethodExplorerView && selection instanceof ITreeSelection) {
			Object obj = ((ITreeSelection) selection).getFirstElement();
			if(obj instanceof Method) {
				Method method = (Method) obj;
				selectedMethodChanged(method);
			}
		}
	}

	public void selectedMethodChanged(Method selectedMethod) {
		contentProvider.setSelectedMethod(selectedMethod);
		viewer.refresh();
	}

}

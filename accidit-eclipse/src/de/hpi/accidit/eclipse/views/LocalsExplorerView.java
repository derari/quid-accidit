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

import de.hpi.accidit.eclipse.views.elements.CalledMethod;
import de.hpi.accidit.eclipse.views.elements.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.elements.LocalsLabelProvider;

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
		column0.setWidth(150);
		TreeColumn column1 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(150);
		TreeColumn column2 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column2.setText("Type");
		column2.setWidth(50);
		TreeColumn column3 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column3.setText("Last Change");
		column3.setWidth(50);
		
		contentProvider = new LocalsContentProvider();		
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new LocalsLabelProvider());
		viewer.setInput(getViewSite());
		
		getSite().setSelectionProvider(viewer);
		
		getSite().getPage().addSelectionListener(MethodExplorerView.ID, this);
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
			ITreeSelection treeSelection = (ITreeSelection) selection;			
			selectedMethodChanged((CalledMethod) treeSelection.getFirstElement());
		}
	}

	public void selectedMethodChanged(CalledMethod selectedMethod) {
		contentProvider.setSelectedMethod(selectedMethod);
		viewer.refresh();
	}

}

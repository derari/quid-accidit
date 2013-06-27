package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsExplorerView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.LocalsExplorerView";

	private TreeViewer viewer;
	private LocalsContentProvider contentProvider;

	public LocalsExplorerView() {}

	@Override
	public void createPartControl(Composite parent) {		
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		viewer.getTree().setHeaderVisible(true);
		viewer.setUseHashlookup(true);
		
		TreeColumn column0 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column0.setText("Local Name");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(viewer.getTree(), SWT.LEFT | SWT.FILL);
		column1.setText("Value");
		column1.setWidth(100);
		
		contentProvider = new LocalsContentProvider(viewer);		
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new LocalsLabelProvider());
		
		TraceNavigatorUI ui = TraceNavigatorUI.getGlobal();
		ui.setLocalsExprorer(this);
	}
	
	public ISelection getSelection() {
		return viewer.getSelection();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void setStep(int testId, long call, long step) {
		contentProvider.setStep(testId, call, step);
//		viewer.refresh();
	}
	
	public NamedValue[] getRootElements() {
		TreeItem[] treeItems = viewer.getTree().getItems();
		NamedValue[] rootElements = new NamedValue[treeItems.length];
		for (int i = 0; i < treeItems.length; i++) {
			rootElements[i] = (NamedValue) treeItems[i].getData();
			
//			System.out.println(rootElements[i].getClass() + " ||| " + rootElements[i]);
			
		}
		return rootElements;
	}
	
}

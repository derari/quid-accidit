package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.views.elements.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.elements.LocalsLabelProvider;

public class LocalsExplorerView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.LocalsExplorerView";

	private TreeViewer viewer;

	public LocalsExplorerView() {}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setHeaderVisible(true);
		
		TreeColumn column0 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column0.setText("Local Name");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(50);
		TreeColumn column2 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column2.setText("Step");
		column2.setWidth(30);
		TreeColumn column3 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column3.setText("Arg");
		column3.setWidth(30);
		
		viewer.setContentProvider(new LocalsContentProvider());
		viewer.setLabelProvider(new LocalsLabelProvider());
		viewer.setInput(getViewSite());
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

}

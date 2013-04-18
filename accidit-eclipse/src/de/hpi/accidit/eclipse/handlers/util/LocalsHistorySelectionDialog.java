package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

public class LocalsHistorySelectionDialog extends ElementTreeSelectionDialog {

	public LocalsHistorySelectionDialog(Shell parent,
			ILabelProvider labelProvider, 
			ITreeContentProvider contentProvider) {
		
		super(parent, labelProvider, contentProvider);
	}
	
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		TreeViewer viewer = super.doCreateTreeViewer(parent, style);
		extendTree(viewer);
		return viewer;
	}
	
	private void extendTree(TreeViewer viewer) {
		if (viewer == null || viewer.getTree() == null) {
			return;
		}
		
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		
		TreeColumn column0 = new TreeColumn(tree, SWT.LEFT);
		column0.setText("Local Name");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(100);
		TreeColumn column2 = new TreeColumn(tree, SWT.RIGHT);
		column2.setText("Change Step");
		column2.setWidth(75);
		TreeColumn column3 = new TreeColumn(tree, SWT.LEFT);
		column3.setText("Type");
		column3.setWidth(200);
	}

}

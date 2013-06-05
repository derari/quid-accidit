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

public class TestCaseSelectionDialog extends ElementTreeSelectionDialog {

	public TestCaseSelectionDialog(Shell parent,
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
		
		TreeColumn column0 = new TreeColumn(tree, SWT.RIGHT);
		column0.setText("Id");
		column0.setWidth(50);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Id");
		column1.setWidth(350);
	}

}

package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsHistorySelectionDialog extends ElementTreeSelectionDialog {

	private NamedValue var;
	public Tree t;
	
	public LocalsHistorySelectionDialog(Shell parent,
			NamedValue var, LocalsHistoryContentProvider cp) {
		
		super(parent, new LocalsLabelProvider(), cp);
		this.var = var;
	}
	
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		TreeViewer viewer = super.doCreateTreeViewer(parent, style | SWT.VIRTUAL);
		extendTree(viewer);
		return viewer;
	}
	
	private void extendTree(TreeViewer viewer) {
		if (viewer == null || viewer.getTree() == null) {
			return;
		}
		viewer.setUseHashlookup(true);
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		t = tree;
		
		TreeColumn column0 = new TreeColumn(tree, SWT.LEFT);
		column0.setText("Key");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(100);		
		
	}

}

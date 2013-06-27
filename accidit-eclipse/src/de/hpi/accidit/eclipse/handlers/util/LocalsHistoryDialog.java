package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsHistoryDialog extends Dialog {
	
	private Object[] dialogResultCache = null;

	private Label label;
	
	private TreeViewer treeViewer;
	private IBaseLabelProvider treeViewerLabelProvider;
	private IContentProvider treeViewerContentProvider; // LocalsHistoryContentProvider 
	private Object treeViewerInput; // root
	
	private ComboViewer comboViewer;
//	private IBaseLabelProvider comboViewerLabelProvider;
	private IContentProvider comboViewerContentProvider;
	private Object[] comboViewerInput;
	private NamedValue selectedObject;
	
	public LocalsHistoryDialog(
			Shell parent,
			NamedValue selectedObject, 
			IContentProvider treeViewerContentProvider,
			Object treeViewerInput, 
			NamedValue[] localsRootElements) {
		super(parent);
		
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		
		this.treeViewerContentProvider = treeViewerContentProvider;
		this.treeViewerLabelProvider = new LocalsLabelProvider();
		this.treeViewerInput = treeViewerInput; // VariableHistory || null for NamedValue
		
		this.comboViewerContentProvider = ArrayContentProvider.getInstance();
		this.comboViewerInput = localsRootElements; // NamedValue this && VariableValue ex
		
		this.selectedObject = selectedObject; // VariableValue
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.numColumns = 2;
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		label.setText("local variable name");
		
		comboViewer = new ComboViewer(container, SWT.NONE);
		Combo combo = comboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboViewer.setContentProvider(comboViewerContentProvider);
		// TODO add real label provider
//		comboViewer.setLabelProvider(comboViewerLabelProvider);
		comboViewer.setInput(comboViewerInput);
		comboViewer.setSelection(new StructuredSelection(selectedObject));
		
		treeViewer = new TreeViewer(container, SWT.VIRTUAL);
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tree.setHeaderVisible(true);
		addColumns(tree);
		treeViewer.setUseHashlookup(true);
		treeViewer.setContentProvider(treeViewerContentProvider);
		treeViewer.setLabelProvider(treeViewerLabelProvider);
		treeViewer.setInput(treeViewerInput);
		
		// TODO select correct element in treeViewer to remove focus from text box
//		treeViewer.setSelection(null);		
		
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.isEmpty()) return;
				
				NamedValue selectedValue = (NamedValue) selection.getFirstElement();
				NamedValue treeViewerInput = null;
				if (selectedValue instanceof NamedValue.VariableValue) {
					treeViewerInput = new NamedValue.VariableHistory(
							TraceNavigatorUI.getGlobal().getTestId(), 
							TraceNavigatorUI.getGlobal().getCallStep(), 
							selectedValue.getId());
				}
				treeViewer.setInput(treeViewerInput);
			}
		});
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	
	@Override
	public void okPressed() {
		ITreeSelection iSelection = (ITreeSelection) treeViewer.getSelection();
		if (iSelection.isEmpty()) super.okPressed();
		
		TreePath treePath = iSelection.getPaths()[0];
		if (treePath.getSegmentCount() != 1) return;

		dialogResultCache = new Object[] {treePath.getLastSegment()};
		super.okPressed();
	}
	
	@Override
	public void cancelPressed() {
		dialogResultCache = null;
		super.cancelPressed();
	}
	
	@Override
	public int open() {
		dialogResultCache = null;
		return super.open();
	}
	
	public void setTreeViewerContentProvider(IContentProvider provider) {
		treeViewerContentProvider = provider;
	}
	
	public void setTreeViewerInput(Object input) {
		treeViewerInput = input;
	}
	
	public Object[] getResult() {
		if (dialogResultCache == null)
			return new Object[0];
		return dialogResultCache;
	}
	
	private void addColumns(Tree tree) {
		if (tree == null) return;
		
		TreeColumn column0 = new TreeColumn(tree, SWT.LEFT);
		column0.setText("Key");
		column0.setWidth(100);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Value");
		column1.setWidth(100);
	}

}

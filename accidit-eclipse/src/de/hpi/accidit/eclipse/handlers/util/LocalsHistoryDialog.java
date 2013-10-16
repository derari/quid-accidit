package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import de.hpi.accidit.eclipse.views.provider.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsHistoryDialog extends Dialog {

	private Object[] dialogResultCache = null;

	
	private TreeViewer treeViewer;
	private IBaseLabelProvider treeViewerLabelProvider;
	private IContentProvider treeViewerContentProvider; // LocalsHistoryContentProvider 
	private Object treeViewerInput; // root
	
	private ComboViewer comboViewer;
	private IContentProvider comboViewerContentProvider;
	private Object[] comboViewerInput;

	private Label titleLabel;
	
	 /* The selection in the comboViewer and the element whose history is displayed in the treeViewer. */
	private Object selectedObject;
	
	public LocalsHistoryDialog(
			Shell parent,
			NamedValue selectedObject, 
			IContentProvider treeViewerContentProvider,
			Object treeViewerInput, 
			Object[] localsRootElements) {
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
		
		titleLabel = new Label(container, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		refreshTitleLabel();
		
		comboViewer = new ComboViewer(container, SWT.READ_ONLY | SWT.V_SCROLL);
		Combo combo = comboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboViewer.setContentProvider(comboViewerContentProvider);
		comboViewer.setInput(comboViewerInput);
		comboViewer.setLabelProvider(new ComboViewerLabelProvider());
		StructuredSelection comboViewerSelection =
				(selectedObject != null) ? new StructuredSelection(selectedObject) : null;
		comboViewer.setSelection(comboViewerSelection);

		Composite treeContainer = new Composite(container, SWT.NONE);
		treeContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		treeViewer = new TreeViewer(treeContainer, SWT.VIRTUAL | SWT.SINGLE);
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		
		TreeColumn column0 = new TreeColumn(tree, SWT.LEFT);
		column0.setText("Key");
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Value");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(column0, new ColumnWeightData(30, 50));
		layout.setColumnData(column1, new ColumnWeightData(70, 50));
		
		treeViewer.setUseHashlookup(true);
		treeViewer.setContentProvider(treeViewerContentProvider);
		treeViewer.setLabelProvider(treeViewerLabelProvider);
		treeViewer.setInput(treeViewerInput);
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});	
		
		comboViewer.addSelectionChangedListener(new ComboViewerSelectionListener());
		
		return parent;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Variable History");
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
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			TreePath treePath = selection.getPaths()[0];
			if (treePath.getSegmentCount() != 1) return;

			dialogResultCache = new Object[] {selection.getFirstElement()};
		}
		super.okPressed();
	}
	
	@Override
	public void cancelPressed() {
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
	
	public Object[] getResult() {
		if (dialogResultCache == null)
			return new Object[0];
		return dialogResultCache;
	}
	
	private void refreshTitleLabel() {
		String title = (selectedObject != null) ?
				"History of \"" + ((NamedValue) selectedObject).getName() + "\"" :
				"Select a variable:";
		titleLabel.setText(title);
	}
	
	private final class ComboViewerSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (selection.isEmpty()) return;
			
			NamedValue selectedObject = (NamedValue) selection.getFirstElement();
			NamedValue treeViewerInput = null;
			if (selectedObject instanceof NamedValue.VariableValue) {
				treeViewerInput = new NamedValue.VariableHistory(
						TraceNavigatorUI.getGlobal().getTestId(), 
						TraceNavigatorUI.getGlobal().getCallStep(), 
						selectedObject.getId());
			}
			treeViewer.setInput(treeViewerInput);
			LocalsHistoryDialog.this.selectedObject = selectedObject;
			refreshTitleLabel();
		}
	}
	
	private static class ComboViewerLabelProvider extends BaseLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof NamedValue.VariableValue)) return null;
			NamedValue.VariableValue value = (NamedValue.VariableValue) element;
			return value.getName();
		}
	}
	
	public static class TreeViewerContentProvider extends LocalsContentProvider implements ITreeContentProvider {

		public TreeViewerContentProvider(TreeViewer viewer) {
			super(viewer);
		}

		@Override
		public void setStep(int testId, long call, long step) { }

		public void setRoot(NamedValue root) {
			this.root = root;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[1];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasChildren(Object element) {
			throw new UnsupportedOperationException();
		}

	}
}

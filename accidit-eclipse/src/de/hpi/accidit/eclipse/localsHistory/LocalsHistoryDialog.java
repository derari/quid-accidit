package de.hpi.accidit.eclipse.localsHistory;

import org.cthul.miro.MiConnection;
import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.util.FinalFuture;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class LocalsHistoryDialog extends Dialog {
	
	private static final NamedValue ALL = new NamedValue("(all)");
	
	private static Image IMG_PUT = null;
	private static Image IMG_GET = null;
	static {
		Display d = Display.getDefault();
		IMG_PUT = new Image(d, LocalsHistoryDialog.class.getResourceAsStream("/put.png"));
		IMG_GET = new Image(d, LocalsHistoryDialog.class.getResourceAsStream("/get.png"));
	}

	private Object[] dialogResultCache = null;

	private TreeViewer treeViewer;
	private HistoryNode contentNode;
	private HistorySource source;
	
	private ComboViewer comboViewer;
	private NamedEntity[] comboViewerInput;

	private Label titleLabel;
	
	 /* The selection in the comboViewer and the element whose history is displayed in the treeViewer. */
	private NamedEntity selectedObject;
	
	private long currentStep = 0;
	
	public LocalsHistoryDialog(
			Shell parent,
			HistorySource source,
			int selectedObject, 
			NamedEntity[] options) {
		super(parent);

		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		
		if (source == null) throw new NullPointerException("source");
		this.source = source;
		for (int i = 0; i < options.length; i++) {
			if (options[i].getId() == selectedObject) {
				this.selectedObject = options[i];
				break;
			}
		}
		if (this.selectedObject == null) {
			this.selectedObject = ALL;
		}
		
		// NamedValue this && VariableValue ex
		this.comboViewerInput = new NamedEntity[options.length+1];
		comboViewerInput[0] = ALL;
		System.arraycopy(options, 0, comboViewerInput, 1, options.length);
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
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
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
		column0.setText("Name");
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Value");
		TreeColumn column2 = new TreeColumn(tree, SWT.RIGHT);
		column2.setText("Step");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(column0, new ColumnWeightData(30, 50));
		layout.setColumnData(column1, new ColumnWeightData(70, 50));
		layout.setColumnData(column2, new ColumnWeightData(20, 50));
		
		treeViewer.setUseHashlookup(true);
		treeViewer.setContentProvider(ThreadsafeContentProvider.INSTANCE);
		treeViewer.setLabelProvider(new HistoryLabelProvider());
		contentNode = new HistoryNode(treeViewer);
		treeViewer.setInput(contentNode);
		source.show(contentNode, selectedObject.getId());
		
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				NamedValueNode nvn = getSelectedElement();
				if (nvn == null) return;
				NamedValue nv = nvn.getValue();
				long step = nv.getStep();
				if (step == currentStep) return;
				currentStep = step;
				refreshTitleLabel();
			}
		});
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});	
		
		comboViewer.addSelectionChangedListener(new ComboViewerSelectionListener());
		
		treeViewer.getTree().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				if (event.index != 0) return;
				TreeItem item = (TreeItem) event.item;
				NamedValueNode node = (NamedValueNode) item.getData();
				if (node == null || node.getDepth() != 1) return;
				Image image = getImage(node);
				if (image == null) return; 
				event.gc.drawImage(image, event.x+3, event.y);
			}
		});
		
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
	
	private NamedValueNode getSelectedElement() {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			return (NamedValueNode) selection.getFirstElement();
		}
		return null;
	}
	
	@Override
	public void okPressed() {
		NamedValueNode sel = getSelectedElement();
		if (sel == null || sel.getDepth() > 1) return;
		dialogResultCache = new Object[]{sel};
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
	
	public Object[] getResult() {
		if (dialogResultCache == null)
			return new Object[0];
		return dialogResultCache;
	}
	
	public Image getImage(NamedValueNode node) {
		if (node.getDepth() != 1) return null;
		
		NamedValue nv = (NamedValue) node.getValue();
		if (nv instanceof VariableValue) {
			//return imgPut;
		} else if (nv instanceof FieldValue) {
			FieldValue fv = (FieldValue) nv;
			return fv.isPut() ? IMG_PUT : IMG_GET;
		} else if (nv instanceof ItemValue) {
			ItemValue iv = (ItemValue) nv;
			return iv.isPut() ? IMG_PUT : IMG_GET;
		}
		return null;
	}
	
	private void refreshTitleLabel() {
		final long step = currentStep;
		source.getTitle(DatabaseConnector.cnn(), step).onComplete(new DoInUiThread<String>() {
			@Override
			protected void run(String value, Throwable error) {
				if (step != currentStep) return;
				if (error != null) error.printStackTrace(System.err);
				String title = "History of " + (value != null ? value : error);
				titleLabel.setText(title);
				titleLabel.getParent().layout();
			}
		});
	}
	
	private final class ComboViewerSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (selection.isEmpty()) return;
			
			NamedEntity selectedObject = (NamedEntity) selection.getFirstElement();
			source.show(contentNode, selectedObject.getId());
			LocalsHistoryDialog.this.selectedObject = selectedObject;
		}
	}
	
	private static class ComboViewerLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof NamedEntity)) return null;
			NamedEntity value = (NamedEntity) element;
			return value.getName();
		}
	}
	
	public static abstract class HistorySource {
		public abstract void show(HistoryNode content, long id);
		
		public abstract MiFuture<String> getTitle(MiConnection cnn, long step);
	}
	
	public static class MethodCallSource extends HistorySource {
		private final long testId;
		private final long callStep;
		
		public MethodCallSource(long testId, long callStep) {
			this.testId = testId;
			this.callStep = callStep;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showVariables(testId, callStep, id);
		}
		
		@Override
		public MiFuture<String> getTitle(MiConnection cnn, long step) {
			String s = String.valueOf("#" + callStep);
			return new FinalFuture<String>(s);
		}
	}
	
	public static class ObjectSource extends HistorySource {
		private final long testId;
		private final long thisId;
		private final boolean isArray;
		
		public ObjectSource(long testId, long thisId, boolean isArray) {
			this.testId = testId;
			this.thisId = thisId;
			this.isArray = isArray;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showFields(testId, thisId, id, isArray);
		}

		@Override
		public MiFuture<String> getTitle(MiConnection cnn, long step) {
			return cnn.select().from(Value.object((int) testId, thisId, step))
					.getSingle()._submit()
					.onComplete(new MiFutureAction<MiFuture<Value>, String>() {
					@Override
					public String call(MiFuture<Value> arg) throws Exception {
						if (arg.hasFailed()) {
							arg.getException().printStackTrace(System.err);
							return arg.getException().getMessage();
						}
						return arg.getResult().getLongString();
					}
			});
		}
	}
	
	private static class HistoryNode extends NamedValueNode {

		public HistoryNode(TreeViewer viewer) {
			super(viewer);
		}
		
		public void showVariables(long testId, long callStep, long variableId) {
			MiConnection cnn = DatabaseConnector.cnn();
			setValue(new NamedValue.VariableHistory(cnn, (int) testId, callStep, (int) variableId));
		}
		
		public void showFields(long testId, long callStep, long thisId, boolean isArray) {
			MiConnection cnn = DatabaseConnector.cnn();
			if (isArray) {
				setValue(new NamedValue.ArrayHistory(cnn, (int) testId, callStep, (int) thisId));
			} else {
				setValue(new NamedValue.ObjectHistory(cnn, (int) testId, callStep, (int) thisId));
			}
		}
	}
	
	private class HistoryLabelProvider extends LocalsLabelProvider {
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				NamedValueNode node = (NamedValueNode) element;
				if (node.getDepth() == 1 && ! (node.getValue() instanceof VariableValue)) {
					return "    " + super.getColumnText(element, columnIndex);
				}
			}
			if (columnIndex == 2) {
				NamedValueNode node = (NamedValueNode) element;
				if (node.getDepth() == 1) {
					NamedValue nv = (NamedValue) node.getValue();
					return String.valueOf(nv.getStep());
				}
				return null;
			}
			return super.getColumnText(element, columnIndex);
		}
	}
	
}

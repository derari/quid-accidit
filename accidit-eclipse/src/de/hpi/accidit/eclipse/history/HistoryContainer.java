package de.hpi.accidit.eclipse.history;

import org.cthul.miro.MiConnection;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import static org.cthul.miro.DSL.*;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.history.HistorySource.MethodCallSource;
import de.hpi.accidit.eclipse.history.HistorySource.ObjectSource;
import de.hpi.accidit.eclipse.model.ArrayIndex;
import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class HistoryContainer {
	
	private static final NamedValue ALL = new NamedValue("(all)");
	
	private static Image IMG_PUT = null;
	private static Image IMG_GET = null;
	static {
		Display d = Display.getDefault();
		IMG_PUT = new Image(d, HistoryView.class.getResourceAsStream("/put.png"));
		IMG_GET = new Image(d, HistoryView.class.getResourceAsStream("/get.png"));
	}
	
	private TreeViewer treeViewer;
	private HistoryNode contentNode;
	private HistorySource source;
	
	private ComboViewer comboViewer;
	private NamedEntity[] comboViewerInput;
	private NamedEntity comboViewerSelection; //selection in ComboViewer

	private Label titleLabel;

	private long currentStep = 0;

	public HistoryContainer() { }
	
	public void setHistorySource(HistorySource source) {
		this.source = source;
	}
	
	public void createPartControl(final Composite container) {		
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		titleLabel = new Label(container, SWT.NONE);
		GridData labelLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		labelLayoutData.widthHint = 250;
		titleLabel.setLayoutData(labelLayoutData);
		refreshTitleLabel();
		
		comboViewer = new ComboViewer(container, SWT.READ_ONLY | SWT.V_SCROLL);
		Combo combo = comboViewer.getCombo();
		GridData comboLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		comboLayoutData.widthHint = 150;
		comboLayoutData.minimumWidth = 80;
		combo.setLayoutData(comboLayoutData);
		comboViewer.setLabelProvider(new ComboViewerLabelProvider());
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.isEmpty()) return;
				
				NamedEntity selectedObject = (NamedEntity) selection.getFirstElement();
				source.show(contentNode, selectedObject.getId());
				HistoryContainer.this.comboViewerSelection = selectedObject;
				
				refreshTitleLabel();
			}
		});

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
		
		TreeColumnLayout treeLayout = new TreeColumnLayout();
		treeContainer.setLayout(treeLayout);
		treeLayout.setColumnData(column0, new ColumnWeightData(30, 50));
		treeLayout.setColumnData(column1, new ColumnWeightData(70, 50));
		treeLayout.setColumnData(column2, new ColumnWeightData(20, 50));
		
		treeViewer.setUseHashlookup(true);
		treeViewer.setContentProvider(ThreadsafeContentProvider.INSTANCE);
		treeViewer.setLabelProvider(new HistoryLabelProvider());
		contentNode = new HistoryNode(treeViewer);
		treeViewer.setInput(contentNode);
		
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			private long currentStep;

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
				NamedValueNode selectedNode = getSelectedElement();
				if (selectedNode == null || selectedNode.getDepth() < 2) return;
				
				int currentTestId = TraceNavigatorUI.getGlobal().getTestId();
				long currentCallStep = TraceNavigatorUI.getGlobal().getCallStep();
				
				NamedValue namedValue = (NamedValue) selectedNode.getValue();
				int selectedNamedValueId = namedValue.getId();
				HistorySource src = null;
				NamedEntity[] options = null;
				
				if (namedValue instanceof NamedValue.VariableValue) {
					src = new MethodCallSource(currentTestId, currentCallStep);
					options = select().from(Variable.VIEW)
							.inCall(currentTestId, currentCallStep).orderById()
							._execute(DatabaseConnector.cnn())._asArray();
				} else if (namedValue instanceof NamedValue.FieldValue) {
					ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
					long thisId = owner.getThisId();
					
					src = new ObjectSource(currentTestId, thisId, false);
					options = select().from(Field.VIEW)
							.ofObject(currentTestId, thisId).orderById()
							._execute(DatabaseConnector.cnn())._asArray();
				} else if (namedValue instanceof NamedValue.ItemValue) {
					ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
					long thisId = owner.getThisId();
					int arrayLength = owner.getArrayLength();
					
					src = new ObjectSource(currentTestId, thisId, true);
					options = ArrayIndex.newIndexArray(arrayLength);
				}
				
				setHistorySource(src);
				setComboViewerOptions(options);
				setComboViewerSelection(selectedNamedValueId);
				refresh();
			}
		});
		
		treeViewer.getTree().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {
				if (event.index != 0) return;
				TreeItem item = (TreeItem) event.item;
				NamedValueNode node = (NamedValueNode) item.getData();
				if (node == null || node.getDepth() != 1) return;
				Image image = getImage(node);
				if (image == null) return;
				event.gc.drawImage(image, event.x + 3, event.y);
			}
		});
	}
	
	/** Schedules a new combo viewer input. Call refresh to apply it. */
	public void setComboViewerOptions(NamedEntity[] options) {
		comboViewerInput = new NamedEntity[options.length + 1];
		comboViewerInput[0] = ALL;
		System.arraycopy(options, 0, comboViewerInput, 1, options.length);
	}

	/** Schedules a new combo viewer selection. Call refresh to apply it. */
	public void setComboViewerSelection(int namedValueId) {
		for (int i = 0; i < comboViewerInput.length; i++) {
			if (comboViewerInput[i].getId() == namedValueId) {
				comboViewerSelection = comboViewerInput[i];
				return;
			}
		}
		comboViewerSelection = ALL;
	}

	/** Applies changes to combo viewer's input and selection. */
	public void refresh() {
		comboViewer.setInput(comboViewerInput);
		comboViewer.setSelection(new StructuredSelection(comboViewerSelection));
	}
	
	public ComboViewer getComboViewer() {
		return comboViewer;
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public Control getControl() {
		return treeViewer.getControl();
	}
	
	public NamedValueNode getSelectedElement() {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		if (selection == null || selection.isEmpty()) {
			return null;
		}
		return (NamedValueNode) selection.getFirstElement();
	}
	
	private void refreshTitleLabel() {
		if (source == null) return;
		
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
	
	private Image getImage(NamedValueNode node) {
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
	
	public static class HistoryLabelProvider extends LocalsLabelProvider {
		
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
	
	public static class ComboViewerLabelProvider extends LabelProvider {

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
	
	public static class HistoryNode extends NamedValueNode {

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
}

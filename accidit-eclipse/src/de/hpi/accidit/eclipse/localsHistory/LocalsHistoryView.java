package de.hpi.accidit.eclipse.localsHistory;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryDialog.ComboViewerLabelProvider;
import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryDialog.HistoryNode;
import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryDialog.HistorySource;
import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryDialog.MethodCallSource;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.ItemValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.views.AcciditView;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class LocalsHistoryView extends ViewPart implements AcciditView {
	
	private static final NamedValue ALL = new NamedValue("(all)");
	
	private static Image IMG_PUT = null;
	private static Image IMG_GET = null;
	static {
		Display d = Display.getDefault();
		IMG_PUT = new Image(d, LocalsHistoryView.class.getResourceAsStream("/put.png"));
		IMG_GET = new Image(d, LocalsHistoryView.class.getResourceAsStream("/get.png"));
	}

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.localsHistory.LocalsHistoryView";
	
	private TreeViewer treeViewer;
	private HistorySource source;
	private HistoryNode contentNode;
	
	private ComboViewer comboViewer;
	private NamedEntity[] comboViewerInput;
	private NamedEntity selectedObject; //the selected comboViewer element

	private Label titleLabel;

	private long currentStep = 0;

	public LocalsHistoryView() { }

	@Override
	public void createPartControl(Composite parent) {		
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		titleLabel = new Label(parent, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
//		refreshTitleLabel(); TODO
		
		comboViewer = new ComboViewer(parent, SWT.READ_ONLY | SWT.V_SCROLL);
		Combo combo = comboViewer.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setInput(comboViewerInput);
		comboViewer.setLabelProvider(new ComboViewerLabelProvider());
		StructuredSelection comboViewerSelection =
				(selectedObject != null) ? new StructuredSelection(selectedObject) : null;
		comboViewer.setSelection(comboViewerSelection);

		Composite treeContainer = new Composite(parent, SWT.NONE);
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
//		source.show(contentNode, selectedObject.getId()); TODO
		
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
				NamedValueNode sel = getSelectedElement();
				if (sel == null || sel.getDepth() > 1) return;
				
				NamedValue variableValue = sel.getValue();
				TraceNavigatorUI.getGlobal().setStep(variableValue.getStep());
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
		
		TraceNavigatorUI.getGlobal().addView(this);
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
	
	private NamedValueNode getSelectedElement() {
		ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
		if (selection == null || selection.isEmpty()) {
			return null;
		}
		return (NamedValueNode) selection.getFirstElement();
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

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	@Override
	public void setStep(TraceElement te) {
		long testId = te.getTestId();
		long callStep = te.getCallStep(); 
		source = new MethodCallSource(testId, callStep);
		NamedEntity[] options = DatabaseConnector.cnn().select()
				.from(Variable.VIEW).inCall(testId, callStep).orderById()
				.asArray()._execute();

		comboViewerInput = new NamedEntity[options.length + 1];
		comboViewerInput[0] = ALL;
		System.arraycopy(options, 0, comboViewerInput, 1, options.length);

		selectedObject = ALL;
		comboViewer.setInput(comboViewerInput);
		StructuredSelection comboViewerSelection = new StructuredSelection(selectedObject);
		comboViewer.setSelection(comboViewerSelection);
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		super.dispose();
	}
	
	public class HistoryLabelProvider extends LocalsLabelProvider {
		
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
	
	public final class ComboViewerSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			// TODO check if selection did not change: return
			
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (selection.isEmpty()) return;
			
			NamedEntity selectedObject = (NamedEntity) selection.getFirstElement();
			source.show(contentNode, selectedObject.getId());
			LocalsHistoryView.this.selectedObject = selectedObject;
		}
	}
}

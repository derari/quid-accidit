package de.hpi.accidit.eclipse.localsHistory;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.localsHistory.HistorySource.MethodCallSource;
import de.hpi.accidit.eclipse.localsHistory.HistorySource.ObjectSource;
import de.hpi.accidit.eclipse.model.ArrayIndex;
import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.views.AcciditView;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class LocalsHistoryView extends ViewPart implements AcciditView, ISelectionListener {
	
	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.localsHistory.LocalsHistoryView";
	
	private LocalsHistoryContainer localsHistory;
	
	private long currentTestId = -1;
	private long currentCallStep = -1;

	public LocalsHistoryView() {}

	@Override
	public void createPartControl(final Composite parent) {		
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		localsHistory = new LocalsHistoryContainer();
		localsHistory.createPartControl(parent);
		
		localsHistory.getTreeViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				NamedValueNode sel = localsHistory.getSelectedElement();
				if (sel == null || sel.getDepth() != 1) return;
				
				NamedValue variableValue = sel.getValue();
				TraceNavigatorUI.getGlobal().setStep(variableValue.getStep());
			}
		});

		getSite().setSelectionProvider(localsHistory.getComboViewer());
		getSite().getPage().addSelectionListener(this);
		
		TraceNavigatorUI.getGlobal().addView(this);
	}

	@Override
	public void setFocus() {
		localsHistory.getControl().setFocus();
	}

	@Override
	public void setStep(TraceElement te) {		
		long testId = te.getTestId();
		long callStep = te.getCallStep();
		if (testId == currentTestId && callStep == currentCallStep) return;

		currentTestId = testId;
		currentCallStep = callStep;

		localsHistory.setHistorySource(new MethodCallSource(testId, callStep));
		NamedEntity[] options = DatabaseConnector.cnn().select()
				.from(Variable.VIEW).inCall(testId, callStep).orderById()
				.asArray()._execute();

		localsHistory.setComboViewerOptions(options);
		localsHistory.setComboViewerSelection(-1);		
		localsHistory.refresh();
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(part instanceof LocalsExplorerView)) return;
		if (!(selection instanceof ITreeSelection) || selection.isEmpty()) return;
		
		ITreeSelection selectedLocals = (ITreeSelection) selection;
		NamedValueNode node = (NamedValueNode) selectedLocals.getFirstElement();
		NamedValue namedValue = (NamedValue) node.getValue();

		int selectedNamedValueId = namedValue.getId();
		HistorySource src = null;
		NamedEntity[] options = null;
		
		if (namedValue instanceof NamedValue.VariableValue) {
			src = new MethodCallSource(currentTestId, currentCallStep);
			options = DatabaseConnector.cnn().select()
					.from(Variable.VIEW).inCall(currentTestId, currentCallStep).orderById()
					.asArray()._execute();
		} else if (namedValue instanceof NamedValue.FieldValue) {
			ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
			long thisId = owner.getThisId();
			
			src = new ObjectSource(currentTestId, thisId, false);
			options = DatabaseConnector.cnn().select()
					.from(Field.VIEW).ofObject(currentTestId, thisId).orderById()
					.asArray()._execute();
		} else if (namedValue instanceof NamedValue.ItemValue) {
			ObjectSnapshot owner = (ObjectSnapshot) namedValue.getOwner();
			long thisId = owner.getThisId();
			int arrayLength = owner.getArrayLength();
			
			src = new ObjectSource(currentTestId, thisId, true);
			options = ArrayIndex.newIndexArray(arrayLength);
		} else {
			localsHistory.setComboViewerSelection(-1);
			localsHistory.refresh();
			return;
		}
		
		localsHistory.setHistorySource(src);
		localsHistory.setComboViewerOptions(options);
		localsHistory.setComboViewerSelection(selectedNamedValueId);
		localsHistory.refresh();
	}
	
	public LocalsHistoryContainer getContainer() {
		return localsHistory;
	}
}

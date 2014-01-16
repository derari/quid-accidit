package de.hpi.accidit.eclipse.history;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.AcciditView;
import de.hpi.accidit.eclipse.views.VariablesView;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class HistoryView extends ViewPart implements AcciditView, ISelectionListener {
	
	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.history.HistoryView";
	
	private HistoryContainer historyContainer;
	
	private long currentTestId = -1;
	private long currentCallStep = -1;

	public HistoryView() {}

	@Override
	public void createPartControl(final Composite parent) {		
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		historyContainer = new HistoryContainer();
		historyContainer.createPartControl(parent);
		
		historyContainer.getTreeViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				NamedValueNode sel = historyContainer.getSelectedElement();
				if (sel == null || sel.getDepth() != 1) return;
				
				NamedValue variableValue = sel.getValue();
				TraceNavigatorUI.getGlobal().setStep(variableValue.getStep());
			}
		});

		getSite().setSelectionProvider(historyContainer.getComboViewer());
		getSite().getPage().addSelectionListener(this);
		
		TraceNavigatorUI.getGlobal().addView(this);
	}

	@Override
	public void setFocus() {
		historyContainer.getControl().setFocus();
	}

	@Override
	public void setStep(TraceElement te) {		
		int testId = te.getTestId();
		long callStep = te.getCallStep();
		if (testId == currentTestId && callStep == currentCallStep) return;

		currentTestId = testId;
		currentCallStep = callStep;		
		historyContainer.updateFromStep(testId, callStep);
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(part instanceof VariablesView)) return;
		if (!(selection instanceof ITreeSelection) || selection.isEmpty()) return;
		ITreeSelection treeSelection = (ITreeSelection) selection;
		
		NamedValueNode node = (NamedValueNode) treeSelection.getFirstElement();
		historyContainer.updateFromContentNode(node);
	}
	
	public HistoryContainer getContainer() {
		return historyContainer;
	}
}

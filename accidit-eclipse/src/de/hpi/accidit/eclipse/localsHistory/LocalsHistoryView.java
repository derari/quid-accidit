package de.hpi.accidit.eclipse.localsHistory;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.localsHistory.HistorySource.MethodCallSource;
import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.model.Variable;
import de.hpi.accidit.eclipse.views.AcciditView;

public class LocalsHistoryView extends ViewPart implements AcciditView {
	

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.localsHistory.LocalsHistoryView";
	
	private LocalsHistoryContainer localsHistory;

	public LocalsHistoryView() {}

	@Override
	public void createPartControl(final Composite parent) {		
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);
		
		localsHistory = new LocalsHistoryContainer();
		localsHistory.createPartControl(parent);
		
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
		super.dispose();
	}
}

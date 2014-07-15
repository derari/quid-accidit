package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.DSL;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.NamedValue.VariableValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationData;
import de.hpi.accidit.eclipse.views.AcciditView;

public class SlicingCriteriaView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.slice.SlicingCriteriaView";
	
	private Composite parent;
	private List<Control> headlineControls;
	private Image removeImage;

	public SlicingCriteriaView() {
		removeImage = Activator.getImageDescriptor("icons/remove_breakpoint_2.png").createImage();
	}

	@Override
	public void setStep(TraceElement te) { }

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);
		this.parent = parent;

		addHeadline();

		TraceNavigatorUI.getGlobal().addView(this);
	}

	@Override
	public void setFocus() { }
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		removeImage.dispose();
		super.dispose();
	}
	
	private void addHeadline() {
		headlineControls = new ArrayList<Control>(3);
		headlineControls.add(new Label(parent, SWT.NONE));
		
		Label typeLabel = new Label(parent, SWT.NONE);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText("Slicing Criteria");
		headlineControls.add(typeLabel);

		headlineControls.add(new Label(parent, SWT.NONE));
	}
	
	public void clear() {
		TraceNavigatorUI.getGlobal().getSliceApi().clear();
				
		for (Control control : parent.getChildren()) {
			if (!headlineControls.contains(control)) {
				control.dispose();
			}
		}
	}
	
	public void addVariableValue(VariableValue value) {
		InvocationData invD = TraceNavigatorUI.getGlobal().getSliceApi().getInvocationData();
		
		if (value.getLine() < 0 || value.getCallStep() < 0) {
			VariableValue newValue = DSL.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
						.inTest(value.getTestId())
						.atStep(value.getValueStep())
						.byId(value.getId())._execute(DatabaseConnector.cnn())._getSingle();
			if (newValue != null) value = newValue;
		}
		
		invD = invD.getInvocationAtCall(value.getCallStep());
		ValueKey key = new ValueKey.VariableValueKey(invD, value.getValueStep(), value.getName(), value.getLine());
		addEntry(key);
	}
	
	public void addThisValue(NamedValue value) {
		InvocationData invD = TraceNavigatorUI.getGlobal().getSliceApi().getInvocationData();

		if (value.getLine() < 0 || value.getCallStep() < 0) {
			VariableValue newValue = DSL.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
						.inTest(value.getTestId())
						.atStep(value.getValueStep())
						.byId(value.getId())._execute(DatabaseConnector.cnn())._getSingle();
			if (newValue != null) value = newValue;
		}
		
		invD = invD.getInvocationAtCall(value.getCallStep());
		ValueKey key = new ValueKey.InvocationThisKey(invD, value.getValueStep());
		addEntry(key);
	}
	
	public void addFieldValue(FieldValue value) {
		InvocationData invD = TraceNavigatorUI.getGlobal().getSliceApi().getInvocationData();
		
		if (value.getLine() < 0 || value.getCallStep() < 0) {
			FieldValue newValue = DSL.select().from(NamedValue.OBJECT_SET_FIELD_VIEW)
						.atStep(value.getTestId(), value.getValueStep())
						._execute(DatabaseConnector.cnn())._getSingle();
			if (newValue != null) value = newValue;
		}
		
//		System.out.printf("%s %s %s %s%n", value.getCallStep(), value.getStep(), value.getThisId(), value.getName());
//		invD = invD.getInvocationAtCall(value.getCallStep());
		ValueKey key = new ValueKey.FieldValueKey(invD, value);
		addEntry(key);
	}
	
	public void addInvocation(Invocation invocation) {
		InvocationData invD = TraceNavigatorUI.getGlobal().getSliceApi().getInvocationData();
		invD = invD.getInvocationAtCall(invocation.getStep());
		addEntry(new ValueKey.InvocationKey(invD));
	}
	
	public void addInvocationArguments(Invocation invocation) {
		InvocationData invD = TraceNavigatorUI.getGlobal().getSliceApi().getInvocationData();
		invD = invD.getInvocationAtCall(invocation.getStep());
		
		List<VariableValue> variables = DSL.select().from(NamedValue.VARIABLE_HISTORY_VIEW)
				.inTest(invocation.getTestId())
				.atStep(invocation.getStep())
				._execute(DatabaseConnector.cnn())._asList();
		
		for (VariableValue v: variables) {
			ValueKey key = new ValueKey.VariableValueKey(invD, v.getValueStep(), v.getName(), v.getLine());
			addEntry(key);
		}
	}
	
	public void addEntry(final ValueKey key) {
		TraceNavigatorUI.getGlobal().getSliceApi().addCriterion(key);
		
		final Button detailsButton = new Button(parent, SWT.BORDER);
		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		detailsButton.setText("v");
		
		final Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText(key.toString());
		
		final Label removeButton = new Label(parent, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		removeButton.setImage(removeImage);
		removeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				TraceNavigatorUI.getGlobal().getSliceApi().removeCriterion(key);
				detailsButton.dispose();
				label.dispose();
				removeButton.dispose();
				parent.layout();
			}
		});
		
		parent.layout();
	}
}

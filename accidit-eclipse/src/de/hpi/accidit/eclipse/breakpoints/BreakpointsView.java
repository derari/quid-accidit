package de.hpi.accidit.eclipse.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.AcciditView;

public class BreakpointsView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.BreakpointsView";
	
	private Composite parent;
	
	private BreakpointsManager breakpointsManager;
	
	public BreakpointsView() {
		breakpointsManager = TraceNavigatorUI.getGlobal().getBreakpointsManager();
	}

	@Override
	public void createPartControl(Composite parent) {		
		GridLayout layout = new GridLayout(4, false);
		parent.setLayout(layout);
		this.parent = parent;

		addHeadline();

		TraceNavigatorUI.getGlobal().addView(this);
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		super.dispose();
	}

	@Override
	public void setFocus() { }
	
	@Override
	public void setStep(TraceElement te) { }
	
	private void addHeadline() {
		@SuppressWarnings("unused")
		final Label placeHolder1 = new Label(parent, SWT.NONE);
		
		final Label typeLabel = new Label(parent, SWT.NONE);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText("Type");
		
		final Label locationLabel = new Label(parent, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		locationLabel.setText("Location");

		@SuppressWarnings("unused")
		final Label placeHolder2 = new Label(parent, SWT.NONE);
	}
	
	public void addBreakpointLine(final LineBreakpoint breakpoint) {
		final Button detailsButton = new Button(parent, SWT.BORDER);
		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		detailsButton.setText("v");
		breakpoint.addUIElement(detailsButton);
		
		final Combo typeCombo = new Combo(parent, SWT.READ_ONLY | SWT.V_SCROLL);
		typeCombo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		typeCombo.setItems(new String[] {"line", "field", "exception"});
		typeCombo.setText("line");
		breakpoint.addUIElement(typeCombo);
		
		final Text locationText = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		locationText.setText(breakpoint.getLocationInformation());
		breakpoint.addUIElement(locationText);
		
		final Label removeButton = new Label(parent, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		Image removeImage = Activator.getImageDescriptor("icons/remove_breakpoint_2.png").createImage();
		removeButton.setImage(removeImage);
		removeButton.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				try {
					breakpointsManager.removeBreakpoint(breakpoint);
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void mouseDown(MouseEvent e) { }
			
			@Override
			public void mouseDoubleClick(MouseEvent e) { }
		});
		breakpoint.addUIElement(removeButton);
		
		parent.layout();
	}
	
	public void removeBreakpointLine(LineBreakpoint breakpoint) {
		for (Widget widget : breakpoint.getUIElements()) {
			widget.dispose();
		}
		parent.layout();
	}
}

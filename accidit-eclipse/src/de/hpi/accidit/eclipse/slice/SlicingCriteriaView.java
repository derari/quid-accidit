package de.hpi.accidit.eclipse.slice;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.AcciditView;

public class SlicingCriteriaView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.slice.SlicingCriteriaView";
	
	private Composite parent;
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
		@SuppressWarnings("unused")
		final Label placeHolder1 = new Label(parent, SWT.NONE);
		
		final Label typeLabel = new Label(parent, SWT.NONE);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText("Label 1");

		@SuppressWarnings("unused")
		final Label placeHolder2 = new Label(parent, SWT.NONE);
	}
	
	public void addLine(String message) {
		final Button detailsButton = new Button(parent, SWT.BORDER);
		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		detailsButton.setText("v");
		
		final Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText(message);
		
		final Label removeButton = new Label(parent, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		removeButton.setImage(removeImage);
		removeButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				detailsButton.dispose();
				label.dispose();
				removeButton.dispose();
				parent.layout();
			}
		});
		
		parent.layout();
	}
}

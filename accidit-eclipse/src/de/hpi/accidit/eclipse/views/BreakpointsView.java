package de.hpi.accidit.eclipse.views;

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
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;

public class BreakpointsView extends ViewPart {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.BreakpointsView";
	
	private Composite parent;
	
	public BreakpointsView() { }

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(5, false);
		parent.setLayout(layout);
		this.parent = parent;

		addHeadline();
		addBreakpointLine();
	}

	@Override
	public void setFocus() { }
	
	private void addHeadline() {
		@SuppressWarnings("unused")
		final Label placeHolder1 = new Label(parent, SWT.NONE);
		
		final Label typeLabel = new Label(parent, SWT.NONE);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText("Type");
		
		final Label locationLabel = new Label(parent, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		locationLabel.setText("Location");
		
		final Label detailsLabel = new Label(parent, SWT.NONE);
		detailsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		detailsLabel.setText("Location details");

		@SuppressWarnings("unused")
		final Label placeHolder2 = new Label(parent, SWT.NONE);
		
//		final Button removeButton = new Button(parent, SWT.BORDER);
//		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
//		removeButton.setText("X");
//		removeButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				placeHolder1.dispose();
//				typeLabel.dispose();
//				locationLabel.dispose();
//				detailsLabel.dispose();
//				removeButton.dispose();
//				parent.layout();
//			}
//			
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) { }
//		});
	}
	
	public void addBreakpointLine() {
		final Button detailsButton = new Button(parent, SWT.BORDER);
		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		detailsButton.setText("v");
		
		final Combo typeCombo = new Combo(parent, SWT.READ_ONLY | SWT.V_SCROLL);
		typeCombo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		typeCombo.setItems(new String[] {"line", "field", "exception"});
		typeCombo.setText("line");
		
		final Text locationText = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Text detailsText = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		detailsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Label removeButton = new Label(parent, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		Image removeImage = Activator.getImageDescriptor("icons/remove_breakpoint_2.png").createImage();
		removeButton.setImage(removeImage);
		removeButton.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				detailsButton.dispose();
				typeCombo.dispose();
				locationText.dispose();
				detailsText.dispose();
				removeButton.dispose();
				parent.layout();
			}
			
			@Override
			public void mouseDown(MouseEvent e) { }
			
			@Override
			public void mouseDoubleClick(MouseEvent e) { }
		});

//		final Button removeButton = new Button(parent, SWT.NONE);
//		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
//		Image removeImage = Activator.getImageDescriptor("icons/remove_breakpoint.png").createImage();
//		removeButton.setImage(removeImage);
//		removeButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				detailsButton.dispose();
//				typeCombo.dispose();
//				locationText.dispose();
//				detailsText.dispose();
//				removeButton.dispose();
//				parent.layout();
//			}
//			
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) { }
//		});
		
		parent.layout();
	}

}

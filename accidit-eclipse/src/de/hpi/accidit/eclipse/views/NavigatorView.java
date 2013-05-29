package de.hpi.accidit.eclipse.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.ViewPart;

public class NavigatorView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.NavigatorView";
	
	private Composite overComposite;
	private Label intoLabel, overLabel;
	private Label upLabel, leftLabel, rightLabel, downLabel;
	private Label upLabel2, leftLabel2;
	
	public NavigatorView() { }

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);
		
		Label previousLabel = new Label(parent, SWT.NONE);
		previousLabel.setText("Previous");
		GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		layoutData.horizontalSpan = 2;
		previousLabel.setLayoutData(layoutData);
		
		Composite intoComposite = new Group(parent, SWT.NONE);
		RowLayout defaultLayout = new RowLayout();
		intoComposite.setLayout(defaultLayout);
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.verticalSpan = 2;
		intoComposite.setLayoutData(layoutData);
		intoLabel = new Label(intoComposite, SWT.NONE);
		intoLabel.setText("Into");

		Label outLabel = new Label(parent, SWT.NONE);
		outLabel.setText("Out");
		layoutData = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
		outLabel.setLayoutData(layoutData);
		
		Label upArrow = new Label(parent, SWT.NONE);
		upArrow.setText("A");
		layoutData = new GridData(SWT.CENTER, SWT.BOTTOM, false, false);
		upArrow.setLayoutData(layoutData);
		
		Label leftArrow = new Label(parent, SWT.NONE);
		leftArrow.setText("<");
		layoutData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		leftArrow.setLayoutData(layoutData);
		
		Label placeHolder = new Label(parent, SWT.NONE);
		
		Label rightArrow = new Label(parent, SWT.NONE);
		rightArrow.setText(">");
		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		rightArrow.setLayoutData(layoutData);
		
		Label resultLabel = new Label(parent, SWT.NONE);
		resultLabel.setText("Result");
		layoutData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		resultLabel.setLayoutData(layoutData);
		
		Label downArrow = new Label(parent, SWT.NONE);
		downArrow.setText("V");
		layoutData = new GridData(SWT.CENTER, SWT.TOP, false, false);
		downArrow.setLayoutData(layoutData);
		
		overComposite = new Group(parent, SWT.NONE);
		overComposite.setLayout(defaultLayout);
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.verticalSpan = 2;
		overComposite.setLayoutData(layoutData);
		overLabel = new Label(overComposite, SWT.NONE);
		overLabel.setText("Over");
				
		Label nextLabel = new Label(parent, SWT.NONE);
		nextLabel.setText("Next");
		layoutData = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
		layoutData.horizontalSpan = 2;
		nextLabel.setLayoutData(layoutData);
		
		parent.pack();
				
		Display.getDefault().addFilter(SWT.KeyDown, new Listener(){

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				if (event.type != SWT.KeyDown)  return;
				if(!(getSite().getPage().getActivePart() instanceof NavigatorView)) return; // check if navigator view has focus
				
				switch(event.keyCode) {
				case SWT.ARROW_DOWN: System.out.println("arrow down!"); break;
				default: break;
				}
			}	
		});
		
//		hookListener(upComposite);
//		hookListener(upLabel);
//		hookListener(upLabel2);
//		
//		hookListener(leftLabel2);
//		
//		hookListener(downComposite);
	}
	
	private void hookListener(final Control control) {
		control.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				System.out.println("Event! - " + control);
			}
		});
	}

	private void createImage(Composite parent) {
		Display compositeDisplay = parent.getDisplay();
		Image image = new Image(compositeDisplay, 16, 16);
		Color color = compositeDisplay.getSystemColor(SWT.COLOR_RED);
		GC gc = new GC(image);
		gc.setBackground(color);
		gc.fillRectangle(image.getBounds());
		gc.dispose();
		
		CLabel label = new CLabel(parent, SWT.BORDER);
		Rectangle clientArea = parent.getClientArea();
		label.setLocation(clientArea.x, clientArea.y);
		label.setImage(image);
		label.setText("the text");
		label.pack();
	}

	@Override
	public void setFocus() {
//		upComposite.setFocus();
	}
}

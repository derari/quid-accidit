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

import de.hpi.accidit.eclipse.Activator;

public class NavigatorView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.NavigatorView";
	
	private Composite leftComposite;
	private Composite intoComposite;
	private Composite downLeftComposite;
	private Composite overComposite;
	
	private boolean currentlyLeftFilled = false;
	
	public NavigatorView() { }

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);
		
//		Label previousLabel = new Label(parent, SWT.NONE);
//		previousLabel.setText("Previous");
//		GridData layoutData = new GridData(SWT.LEFT, SWT.TOP, false, false);
//		layoutData.horizontalSpan = 2;
//		previousLabel.setLayoutData(layoutData);
		
		leftComposite = new Group(parent, SWT.NONE);
		RowLayout defaultLayout = new RowLayout();
		leftComposite.setLayout(defaultLayout);
		leftComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		Label leftLabel = new Label(leftComposite, SWT.NONE);
		leftLabel.setText("left");
		
		Label upArrow = new Label(parent, SWT.NONE);
		Image upImage = Activator.getImageDescriptor("icons/go-up.png").createImage();
		upArrow.setImage(upImage);
		upArrow.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false));
		
		intoComposite = new Group(parent, SWT.NONE);
		intoComposite.setLayout(defaultLayout);
		intoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label intoLabel = new Label(intoComposite, SWT.NONE);
		intoLabel.setText("Into");
		
		Label leftArrow = new Label(parent, SWT.NONE);
		Image leftImage = Activator.getImageDescriptor("icons/go-previous.png").createImage();
		leftArrow.setImage(leftImage);
		leftArrow.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		Label placeHolder = new Label(parent, SWT.NONE);
		
		Label rightArrow = new Label(parent, SWT.NONE);
		Image rightImage = Activator.getImageDescriptor("icons/go-next.png").createImage();
		rightArrow.setImage(rightImage);
		rightArrow.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		downLeftComposite = new Group(parent, SWT.NONE);
		downLeftComposite.setLayout(defaultLayout);
		downLeftComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		Label downLeftLabel = new Label(downLeftComposite, SWT.NONE);
		downLeftLabel.setText("Over");
		
		Label downArrow = new Label(parent, SWT.NONE);
		Image downImage = Activator.getImageDescriptor("icons/go-down.png").createImage();
		downArrow.setImage(downImage);
		downArrow.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		overComposite = new Group(parent, SWT.NONE);
		overComposite.setLayout(defaultLayout);
		overComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label overLabel = new Label(overComposite, SWT.NONE);
		overLabel.setText("Over");
		
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
	}
	
	public void switchLayout() {
		setFillLeftSide(!currentlyLeftFilled, leftComposite.getParent());
		currentlyLeftFilled = !currentlyLeftFilled;
	}
	
	public void setFillLeftSide(boolean leftSide, Composite parent) {
		setHorizontalFill(leftComposite, leftSide);
		setHorizontalFill(downLeftComposite, leftSide);
		setHorizontalFill(intoComposite, !leftSide);
		setHorizontalFill(overComposite, !leftSide);
		parent.layout();
	}
	
	private void setHorizontalFill(Composite composite, boolean grabExcessHorizontalSpace) {
		GridData gridData = (GridData) composite.getLayoutData();
		gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		gridData.horizontalAlignment = (grabExcessHorizontalSpace) ? SWT.FILL : SWT.LEFT;
		composite.setLayoutData(gridData);
	}
	
	private void hookListener(final Control control) {
		control.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				System.out.println("Event! - " + control);
			}
		});
	}

	@Override
	public void setFocus() {
//		upComposite.setFocus();
	}
}

package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.SideEffects;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.ContentNode;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;
import de.hpi.accidit.eclipse.views.provider.VariablesLabelProvider;

public class NavigatorView extends ViewPart implements AcciditView {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.NavigatorView";
	
	private Composite nwComposite;
	private Composite neComposite;
	private Composite swComposite;
	private Composite seComposite;
	
	private int currentTest = -1;
	private long currentCall = -1;
	
	SideEffects sideEffects;
	private SideEffectsNode sideEffectsBefore;
	
	private boolean currentlyLeftFilled = false;
	
	public NavigatorView() { }

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);
		
		FillLayout defaultLayout = new FillLayout();
		
		nwComposite = new Composite(parent, SWT.NONE);
		nwComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		nwComposite.setLayout(defaultLayout);
		
		Label upArrow = new Label(parent, SWT.NONE);
		Image upImage = Activator.getImageDescriptor("icons/go-up.png").createImage();
		upArrow.setImage(upImage);
		upArrow.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false));
		
		neComposite = new Composite(parent, SWT.NONE);
		neComposite.setLayout(defaultLayout);
		neComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label leftArrow = new Label(parent, SWT.NONE);
		Image leftImage = Activator.getImageDescriptor("icons/go-previous.png").createImage();
		leftArrow.setImage(leftImage);
		leftArrow.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		@SuppressWarnings("unused")
		Label placeHolder = new Label(parent, SWT.NONE);
		
		Label rightArrow = new Label(parent, SWT.NONE);
		Image rightImage = Activator.getImageDescriptor("icons/go-next.png").createImage();
		rightArrow.setImage(rightImage);
		rightArrow.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		swComposite = new Group(parent, SWT.NONE);
		swComposite.setLayout(defaultLayout);
		swComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		
		Label downArrow = new Label(parent, SWT.NONE);
		Image downImage = Activator.getImageDescriptor("icons/go-down.png").createImage();
		downArrow.setImage(downImage);
		downArrow.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		seComposite = new Composite(parent, SWT.NONE);
		seComposite.setLayout(defaultLayout);
		seComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		parent.pack();

		TreeViewer sideEffectsBeforeTree = new TreeViewer(nwComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		sideEffectsBeforeTree.setUseHashlookup(true);
		TreeViewer sideEffectsAfterTree = new TreeViewer(swComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		sideEffectsAfterTree.setUseHashlookup(true);
		TreeViewer sideEffectsIntoTree = new TreeViewer(neComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		sideEffectsIntoTree.setUseHashlookup(true);
		TreeViewer callSummaryTree = new TreeViewer(seComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		callSummaryTree.setUseHashlookup(true);
		
		sideEffectsBefore = new SideEffectsNode(sideEffectsBeforeTree);
		sideEffectsBeforeTree.setLabelProvider(new SideEffectsLabelProvider());

		TraceNavigatorUI.getGlobal().addView(this);
		
//		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
//			@Override
//			public void handleEvent(org.eclipse.swt.widgets.Event event) {
//				if (event.type != SWT.KeyDown)  return;
//				if(!(getSite().getPage().getActivePart() instanceof NavigatorView)) return; // check if navigator view has focus
//				
//				switch(event.keyCode) {
//				case SWT.ARROW_DOWN: System.out.println("arrow down!"); break;
//				default: break;
//				}
//			}	
//		});
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		super.dispose();
	}
	
	@Override
	public void setStep(TraceElement te) {
		if (te.getTestId() != currentTest || te.getCallStep() != currentCall) {
			if (te.parent != null) {
				System.out.println("!!!!!!!!" + te.parent.getStep());
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().cnn(), 
						te.getTestId(), te.parent.getStep(), te.parent.exitStep);
			} else if (te instanceof Invocation) {
				System.out.println("root");
				Invocation root = (Invocation) te;
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().cnn(), 
						te.getTestId(), root.getStep(), root.exitStep,
						root.getStep(), root.exitStep);
			} else {
				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				System.out.println(te);
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().cnn(), 
						te.getTestId(), 0, 0, 0, 0);
			}
			currentTest = te.getTestId();
			currentCall = te.getCallStep();
		}
		sideEffectsBefore.setStep(sideEffects, te.getStep());
	}
	
	public void switchLayout() {
		setFillLeftSide(!currentlyLeftFilled, nwComposite.getParent());
		currentlyLeftFilled = !currentlyLeftFilled;
	}
	
	public void setFillLeftSide(boolean leftSide, Composite parent) {
		setHorizontalFill(nwComposite, leftSide);
		setHorizontalFill(swComposite, leftSide);
		setHorizontalFill(neComposite, !leftSide);
		setHorizontalFill(seComposite, !leftSide);
		parent.layout();
	}
	
	private void setHorizontalFill(Composite composite, boolean grabExcessHorizontalSpace) {
		GridData gridData = (GridData) composite.getLayoutData();
		gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		gridData.horizontalAlignment = (grabExcessHorizontalSpace) ? SWT.FILL : SWT.LEFT;
		composite.setLayoutData(gridData);
	}

	@Override
	public void setFocus() {
//		upComposite.setFocus();
	}
	
	private static class SideEffectsNode extends ContentNode {

		private SideEffects se;
		
		public SideEffectsNode(TreeViewer viewer) {
			super(viewer);
		}
		
		public void setStep(SideEffects se, long step) {
			this.se = se;
			setValue(se);
		}
		
		@Override
		protected void initialize() {
			super.initialize();
			int i = 0;
			for (NamedValue nv: se.getChildren()) {
				getChild(i++).setValue(nv);
			}
		}
		
		@Override
		protected ContentNode newNode() {
			return new NamedValueNode(this);
		}
	}
	
	private static class SideEffectsLabelProvider extends VariablesLabelProvider {
		
	}
}

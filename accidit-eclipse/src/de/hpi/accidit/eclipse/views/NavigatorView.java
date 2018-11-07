package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.SideEffects;
import de.hpi.accidit.eclipse.model.SideEffects.InstanceEffects;
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

		swComposite = new Composite(parent, SWT.NONE);
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
		
		TreeColumn column0 = new TreeColumn(sideEffectsBeforeTree.getTree(), SWT.LEFT);
		column0.setText("Name");
		TreeColumn column1 = new TreeColumn(sideEffectsBeforeTree.getTree(), SWT.LEFT);
		column1.setText("Value");
		TreeColumn column2 = new TreeColumn(sideEffectsBeforeTree.getTree(), SWT.LEFT);
		column2.setText("Step");
		
		sideEffectsBefore = new SideEffectsNode(sideEffectsBeforeTree);
		sideEffectsBeforeTree.setLabelProvider(new SideEffectsLabelProvider());
		sideEffectsBeforeTree.setInput(sideEffectsBefore);
		
		TreeColumnLayout tlayout = new TreeColumnLayout();
		nwComposite.setLayout(tlayout);
		tlayout.setColumnData(column0, new ColumnWeightData(40, 40));
		tlayout.setColumnData(column1, new ColumnWeightData(60, 50));
		tlayout.setColumnData(column2, new ColumnWeightData(10, 20));

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
	public void sliceChanged() { }
	
	@Override
	public void setStep(TraceElement te, boolean before) {
		if (te.getTestId() != currentTest || te.getCallStep() != currentCall) {
			currentTest = te.getTestId();
			currentCall = te.getCallStep();
			if (te.parent != null) {
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().db(), 
						te.getTestId(), te.parent.getStep(), te.parent.exitStep);
			} else if (te instanceof Invocation) {
				Invocation root = (Invocation) te;
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().db(), 
						te.getTestId(), root.getStep(), root.exitStep,
						root.getStep(), root.exitStep);
			} else {
				currentCall = -1;
				sideEffects = new SideEffects(
						TraceNavigatorUI.getGlobal().db(), 
						te.getTestId(), 0, 0, 0, 0);
			}
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
		protected void updateNode(boolean valueIsInitialized) {
			System.out.println("Navigator: " + valueIsInitialized);
			if (!valueIsInitialized || se == null) {
				setSize(0);
				return;
			}
//			new Exception().printStackTrace();
			NamedValue[] instances = se.getChildren();
			for (int i = 0; i < instances.length; i++) {
				((NamedValueNode) getChild(i)).setNamedValue(instances[i]);
			}
			setSize(instances.length);
			System.out.println("Navigator: " + instances.length);
		}
		
		@Override
		protected ContentNode newNode() {
			return new NamedValueNode(this);
		}
	}
	
	private static class SideEffectsLabelProvider extends VariablesLabelProvider {
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ContentNode) {
				ContentNode cn = (ContentNode) element;
				element = cn.getNodeValue();
			}
			if (!(element instanceof NamedValue)) {
				System.out.println(":(" + element);
				return String.valueOf(element);
			}
			NamedValue nv = (NamedValue) element;
			switch (columnIndex) {
			case 0: 
				return nv.getName();
			case 1: 
				if (nv.isInitialized()) {
					if (nv instanceof InstanceEffects) {
						return "---";
					}
					return nv.getValue().getLongString();
				} else {
					return "Pending...";
				}
			}
			return "-";
		}
		
	}
}

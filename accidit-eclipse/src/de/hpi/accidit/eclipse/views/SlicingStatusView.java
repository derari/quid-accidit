package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.slice.DynamicSlice;
import de.hpi.accidit.eclipse.slice.DynamicSlice.Node;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class SlicingStatusView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.SlicingStatusView";
	
	private ScrolledComposite scroll;
	private Composite parent;
	private List<Control> headlineControls;
	private Image removeImage;
	private SortedSet<DynamicSlice.Node> nodes;
	private Set<DynamicSlice.Node> currentNodes;
	private long step;
	private Font boldFont;
	private Color greyFont;
	private boolean stepLabelExists = false;
	
	public SlicingStatusView() {
		removeImage = Activator.getImageDescriptor("icons/remove_breakpoint_2.png").createImage();
	}
	
	@Override
	public void sliceChanged() {
		scheduleUpdate();
	}

	@Override
	public void setStep(TraceElement te) {
		this.step = te.getStep();
		scheduleUpdate();
	}
	
	private void scheduleUpdate() {
		final SortedSet<DynamicSlice.Node> nodes = new TreeSet<>();
		this.nodes = nodes;
		this.currentNodes = new HashSet<>();
		new Thread(){
			public void run() {
				collectNodes(nodes);
				DoInUiThread.run(new Runnable() {
					@Override
					public void run() {
						showNodes(nodes);
					}
				});
			};
		}.start();
		
	}
	
	private void collectNodes(SortedSet<DynamicSlice.Node> nodes) {
		Set<DynamicSlice.Node> currentNodes = this.currentNodes; 
		if (nodes != SlicingStatusView.this.nodes) return;
		for (Node n: TraceNavigatorUI.getGlobal().getSliceApi().getSlice().values()) {
			if (n.getKey().getStep() >= step) {
				boolean cur = n.getKey().getStep() == step;
				boolean matched = false;
				if (cur) {
					currentNodes.add(n);
					currentNodes.addAll(n.getDependenciesInSlice());
				}
				for (Node n2: n.getDependenciesInSlice()) {
					if (n2.getKey().getStep() <= step) {
						matched = true;
						nodes.add(n2);
						if (n2.getKey().getStep() == step) {
							currentNodes.add(n);
						}
					}
				}
				if (matched) nodes.add(n);
			}
		}
	}
	
	private void showNodes(SortedSet<DynamicSlice.Node> nodes) {
		if (nodes != SlicingStatusView.this.nodes) return;
		clearUi();
		int section = -1;
		for (Node n: nodes) {
			if (n.getFlags() <= 0) continue;
			Node rep = n.getRepresentative();
			if (rep != n && nodes.contains(rep)) continue;
			if (section == -1) {
				section++;
				if (n.getKey().getStep() < step) {
					addSection("Previous Steps");
				}
			}
			if (section == 0 && n.getKey().getStep() >= step) {
				section++;
				if (n.getKey().getStep() == step) {
					addSection("Current Step");
				}
			}
			if (section == 1 && n.getKey().getStep() > step) {
				section++;
				addSection("Next Steps");
			}
			boolean jumpOverCurrent = !currentNodes.contains(n);
			addNode(rep, jumpOverCurrent);
		}
		scroll.setMinHeight(
				parent.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y);
		parent.layout();
	}

	@Override
	public void createPartControl(Composite parent) {
		
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setBackgroundMode(SWT.INHERIT_FORCE);
		
		Composite content = new Composite(scroll, SWT.NONE);
		scroll.setContent(content);
		
		GridLayout layout = new GridLayout(3, false);
		content.setLayout(layout);
		content.setBackgroundMode(SWT.INHERIT_FORCE);
		this.parent = content;
		addHeadline();
		parent.layout();

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
	
	private void addSection(String text) {
		Label l = new Label(parent, SWT.NONE);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, stepLabelExists ? 3 : 2, 1));
		l.setText(text);		
		if (boldFont == null) {
			FontDescriptor descriptor = FontDescriptor.createFrom(l.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			boldFont = descriptor.createFont(parent.getDisplay());
			greyFont = new Color(l.getDisplay(), 159, 159, 159);
		}
		l.setFont(boldFont);
		if (!stepLabelExists) {
			l = new Label(parent, SWT.NONE);
			l.setText("Step #");
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			l.setFont(boldFont);
			stepLabelExists = true;
		}
	}
	
	private void addNode(final Node n, boolean jumpOverCurrent) {
		final long step = n.getKey().getStep();
		MouseAdapter onClickGotoStep = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				TraceNavigatorUI.getGlobal().setStep(step);
			}
		};
		
		MouseAdapter onClickSlice = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				SlicingFilterDialog dlg = new SlicingFilterDialog(parent.getShell(), n.getFlags());
				if (dlg.open() == SWT.OK) {
					TraceNavigatorUI.getGlobal().getOrOpenSlicingCriteriaView().addEntry(n.getKey(), dlg.getFlags());
				}
			}
		};
		Label l = new Label(parent, SWT.NONE);
		l.setImage(DEP[n.getDependencyFlags()]);
		l.addMouseListener(onClickSlice);
		l = new Label(parent, SWT.NONE);
		String s = n.getKey().getUserString();
		l.setText(s.substring(s.indexOf(':') + 1));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		l.addMouseListener(onClickGotoStep);
		if (jumpOverCurrent) l.setForeground(greyFont);
		l = new Label(parent, SWT.NONE);
		l.setText(String.valueOf(step));
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		l.addMouseListener(onClickGotoStep);
		if (jumpOverCurrent) l.setForeground(greyFont);
	}
	
	private void addHeadline() {
		headlineControls = new ArrayList<Control>();
//		Label l;
//		
//		// previous
//		
//		Label prevLabel = new Label(parent, SWT.NONE);
//		prevLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
//		prevLabel.setText("Previous Steps");
//		headlineControls.add(prevLabel);
//		l = new Label(parent, SWT.NONE);
//		l.setText("Step #");
//		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
//		headlineControls.add(l);
//		
//		// next
//		
//		Label nextLabel = new Label(parent, SWT.NONE);
//		nextLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
//		nextLabel.setText("Next Steps");
//		headlineControls.add(nextLabel);
//		
//		// fonts
		
		
	}
	
//	public void clear() {
//		TraceNavigatorUI.getGlobal().getSliceApi().clear();
//		clearUi();
//	}
//	
	public void clearUi() {
		stepLabelExists = false;
		for (Control control : parent.getChildren()) {
			if (!headlineControls.contains(control)) {
				control.dispose();
			}
		}
	}

	
//	public void addEntry() {
////		TraceNavigatorUI.getGlobal().getSliceApi().addCriterion(key);
////		
////		final Button detailsButton = new Button(parent, SWT.BORDER);
////		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
////		detailsButton.setText("v");
////		
////		final Label label = new Label(parent, SWT.NONE);
////		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
////		label.setText(key.toString());
////		
////		final Label removeButton = new Label(parent, SWT.NONE);
////		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
////		removeButton.setImage(removeImage);
////		removeButton.addMouseListener(new MouseAdapter() {
////			@Override
////			public void mouseUp(MouseEvent e) {
////				TraceNavigatorUI.getGlobal().getSliceApi().removeCriterion(key);
////				detailsButton.dispose();
////				label.dispose();
////				removeButton.dispose();
////				parent.layout();
////			}
////		});
//		
//		parent.layout();
//	}
	
	public static final Image DEP_X;
	public static final Image DEP_V;
	public static final Image DEP_R;
	public static final Image DEP_C;
	public static final Image DEP_VR;
	public static final Image DEP_VC;
	public static final Image DEP_RC;
	public static final Image DEP_VCR;
	public static final Image DEP_SLICE;
	public static final Image[] DEP;
	
	static {
		DEP_X = Activator.getImageDescriptor("icons/dep_0.png").createImage();
		DEP_V = Activator.getImageDescriptor("icons/dep_v.png").createImage();
		DEP_R = Activator.getImageDescriptor("icons/dep_r.png").createImage();
		DEP_C = Activator.getImageDescriptor("icons/dep_c.png").createImage();
		DEP_VR = Activator.getImageDescriptor("icons/dep_vr.png").createImage();
		DEP_VC = Activator.getImageDescriptor("icons/dep_vc.png").createImage();
		DEP_RC = Activator.getImageDescriptor("icons/dep_cr.png").createImage();
		DEP_VCR = Activator.getImageDescriptor("icons/dep_vcr.png").createImage();
		DEP_SLICE = Activator.getImageDescriptor("icons/slice.png").createImage();
		DEP = new Image[]{DEP_X, DEP_V, DEP_R, DEP_VR, DEP_C, DEP_VC, DEP_RC, DEP_VCR, DEP_SLICE};
	}
}

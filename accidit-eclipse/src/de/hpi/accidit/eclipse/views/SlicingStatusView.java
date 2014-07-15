package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.TraceElement;

public class SlicingStatusView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.SlicingStatusView";
	
	private Composite parent;
	private List<Control> headlineControls;
	private Image removeImage;

	public SlicingStatusView() {
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
		headlineControls = new ArrayList<Control>();
		Label l;
		
		// previous
		
		Label prevLabel = new Label(parent, SWT.NONE);
		prevLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		prevLabel.setText("Previous Steps");
		headlineControls.add(prevLabel);
		headlineControls.add(new Label(parent, SWT.NONE));
		
		// ...
		
		l = new Label(parent, SWT.NONE);
		l.setImage(DEP_V);
		headlineControls.add(l);
		l = new Label(parent, SWT.NONE);
		l.setText("asdasd asdasd");
		headlineControls.add(l);
		headlineControls.add(new Label(parent, SWT.NONE));
		
		// next
		
		Label nextLabel = new Label(parent, SWT.NONE);
		nextLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		nextLabel.setText("Next Steps");
		headlineControls.add(nextLabel);
		headlineControls.add(new Label(parent, SWT.NONE));
		
		
		// fonts
		
		FontDescriptor descriptor = FontDescriptor.createFrom(prevLabel.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		Font boldFont = descriptor.createFont(prevLabel.getDisplay());
		prevLabel.setFont(boldFont);
		nextLabel.setFont(boldFont);
	}
	
	public void clear() {
		TraceNavigatorUI.getGlobal().getSliceApi().clear();
				
		for (Control control : parent.getChildren()) {
			if (!headlineControls.contains(control)) {
				control.dispose();
			}
		}
	}

	
	public void addEntry() {
//		TraceNavigatorUI.getGlobal().getSliceApi().addCriterion(key);
//		
//		final Button detailsButton = new Button(parent, SWT.BORDER);
//		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
//		detailsButton.setText("v");
//		
//		final Label label = new Label(parent, SWT.NONE);
//		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		label.setText(key.toString());
//		
//		final Label removeButton = new Label(parent, SWT.NONE);
//		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
//		removeButton.setImage(removeImage);
//		removeButton.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseUp(MouseEvent e) {
//				TraceNavigatorUI.getGlobal().getSliceApi().removeCriterion(key);
//				detailsButton.dispose();
//				label.dispose();
//				removeButton.dispose();
//				parent.layout();
//			}
//		});
		
		parent.layout();
	}
	
	private static final Image DEP_V;
	private static final Image DEP_C;
	private static final Image DEP_VCR;
	
	static {
		DEP_V = Activator.getImageDescriptor("icons/dep_v.png").createImage();
		DEP_C = Activator.getImageDescriptor("icons/dep_c.png").createImage();
		DEP_VCR = Activator.getImageDescriptor("icons/dep_vcr.png").createImage();
	}
}

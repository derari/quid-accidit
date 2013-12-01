package de.hpi.accidit.eclipse.localsHistory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class LocalsHistoryView extends ViewPart {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.localsHistory.LocalsHistoryView";

	public LocalsHistoryView() { }

	@Override
	public void createPartControl(Composite parent) {
		new Label(parent, SWT.NONE).setText("Locals History View");
		parent.pack();
	}

	@Override
	public void setFocus() {
		// TODO implement setFocus on treeViewer control
//		treeViewer.getControl().setFocus();
	}

}

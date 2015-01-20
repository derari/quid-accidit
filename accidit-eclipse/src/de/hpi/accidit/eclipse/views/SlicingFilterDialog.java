package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SlicingFilterDialog extends Dialog {
	
	private Button cbValue;
	private Button cbReach;
	private Button cbControl;
	
	public SlicingFilterDialog(Shell shell) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)  super.createDialogArea(parent);
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		cbValue = new Button(parent, SWT.CHECK);
		cbValue.setText("Value");
		cbReach = new Button(parent, SWT.CHECK);
		cbReach.setText("Reachability");
		cbControl = new Button(parent, SWT.CHECK);
		cbControl.setText("Control");
		
		return parent;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(200, 100);
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	public int getFlags() {
		return 1 + 2 + 4;
	}
}

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

import de.hpi.accidit.eclipse.slice.DynamicSlice;

public class SlicingFilterDialog extends Dialog {
	
	private Button cbValue;
	private Button cbReach;
	private Button cbControl;
	private int flags;
	
	public SlicingFilterDialog(Shell shell, int flags) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		this.flags = flags;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)  super.createDialogArea(parent);
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		cbValue = new Button(parent, SWT.CHECK);
		cbValue.setText("Value");
		cbValue.setSelection((flags & DynamicSlice.VALUE) != 0);
		cbReach = new Button(parent, SWT.CHECK);
		cbReach.setText("Reachability");
		cbReach.setSelection((flags & DynamicSlice.REACH) != 0);
		cbControl = new Button(parent, SWT.CHECK);
		cbControl.setText("Control");
		cbControl.setSelection((flags & DynamicSlice.CONTROL) != 0);
		
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
		return new Point(250, 150);
	}
	
	@Override
	protected void okPressed() {
		flags = (cbValue.getSelection() ? DynamicSlice.VALUE : 0) +
				(cbReach.getSelection() ? DynamicSlice.REACH : 0) +
				(cbControl.getSelection() ? DynamicSlice.CONTROL : 0);
		super.okPressed();
		setReturnCode(SWT.OK);
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
		setReturnCode(SWT.CANCEL);
	}

	public int getFlags() {
		return flags;
	}
}

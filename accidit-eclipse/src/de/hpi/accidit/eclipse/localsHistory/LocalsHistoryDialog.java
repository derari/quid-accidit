package de.hpi.accidit.eclipse.localsHistory;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.hpi.accidit.eclipse.model.NamedEntity;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class LocalsHistoryDialog extends Dialog {

	private Object[] dialogResultCache = null;
	
	private LocalsHistoryContainer localsHistory;
	
	public LocalsHistoryDialog(
			Shell parent,
			HistorySource source,
			int selectedObject, 
			NamedEntity[] options) {
		super(parent);

		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		
		if (source == null) throw new NullPointerException("source");
		localsHistory = new LocalsHistoryContainer();
		localsHistory.setHistorySource(source);
		localsHistory.setComboViewerOptions(options);
		localsHistory.setComboViewerSelection(selectedObject);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.numColumns = 2;
		
		localsHistory.createPartControl(container);
		localsHistory.refresh();		
		return parent;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Variable History");
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	
	@Override
	public void okPressed() {
		NamedValueNode sel = localsHistory.getSelectedElement();
		if (sel == null || sel.getDepth() > 1) return;
		dialogResultCache = new Object[] {sel};
		super.okPressed();
	}
	
	@Override
	public void cancelPressed() {
		super.cancelPressed();
	}
	
	@Override
	public int open() {
		dialogResultCache = null;
		return super.open();
	}
	
	public Object[] getResult() {
		if (dialogResultCache == null)
			return new Object[0];
		return dialogResultCache;
	}	
}

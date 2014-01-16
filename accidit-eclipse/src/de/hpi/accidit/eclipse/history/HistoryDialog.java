package de.hpi.accidit.eclipse.history;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class HistoryDialog extends Dialog {
	
	private HistoryContainer historyContainer;

	private Object[] dialogResultCache = null;

	private NamedValueNode node;
	
	public HistoryDialog(Shell parent, NamedValueNode node) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		
		this.node = node;
		historyContainer = new HistoryContainer();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.numColumns = 2;
		
		historyContainer.createPartControl(container);
		historyContainer.updateFromContentNode(node);
		
		historyContainer.getTreeViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		
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
		NamedValueNode sel = historyContainer.getSelectedElement();
		if (sel == null || sel.getDepth() != 1) return;
		
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

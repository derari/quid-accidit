package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.LocalsContentProvider;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;

public class LocalsExplorerView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.LocalsExplorerView";

	private TreeViewer viewer;
	private LocalsContentProvider contentProvider;

	public LocalsExplorerView() {}

	@Override
	public void createPartControl(Composite parent) {		
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		viewer.getTree().setHeaderVisible(true);
		viewer.setUseHashlookup(true);
		
		TreeColumn column0 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column0.setText("Name");
		TreeColumn column1 = new TreeColumn(viewer.getTree(), SWT.LEFT);
		column1.setText("Value");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		parent.setLayout(layout);
		layout.setColumnData(column0, new ColumnWeightData(40, 50));
		layout.setColumnData(column1, new ColumnWeightData(60, 50));
		
		contentProvider = new LocalsContentProvider(viewer);		
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new LocalsLabelProvider());
		
		TraceNavigatorUI ui = TraceNavigatorUI.getGlobal();
		ui.setLocalsExprorer(this);
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand("de.hpi.accidit.eclipse.commands.showVariableHistory", null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public ISelection getSelection() {
		return viewer.getSelection();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void setStep(int testId, long call, long step) {
		contentProvider.setStep(testId, call, step);
	}
	
	/**
	 * Returns an array of root elements. Their type is {@link NamedValue.VariableValue}.
	 * 
	 * @return
	 */
	public Object[] getRootElements() {
		TreeItem[] treeItems = viewer.getTree().getItems();
		List<Object> rootElements = new ArrayList<Object>(treeItems.length - 1); // -1 as this gets removed
		for (TreeItem item : treeItems) {
			if (item.getData() instanceof NamedValue.VariableValue) {
				rootElements.add(item.getData());
			}
		}
		return rootElements.toArray();
	}
	
}

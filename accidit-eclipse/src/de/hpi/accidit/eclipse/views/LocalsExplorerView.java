package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.MiConnection;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.provider.LocalsLabelProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.NamedValueNode;

public class LocalsExplorerView extends ViewPart implements AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.LocalsExplorerView";
	
	private static final String DEFAULT_COMMAND_ID = "de.hpi.accidit.eclipse.commands.showVariableHistory";

	private TreeViewer viewer;
	private MethodNode rootNode;

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
		
		viewer.setContentProvider(ThreadsafeContentProvider.INSTANCE);
		viewer.setLabelProvider(new LocalsLabelProvider());
		getSite().setSelectionProvider(viewer);
		
		rootNode = new MethodNode(viewer);
		viewer.setInput(rootNode);
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand(DEFAULT_COMMAND_ID, null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					e.printStackTrace();
				}
			}
		});
		
		/* Context menu registration. */
		MenuManager menuManager = new MenuManager();
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		final Menu contextMenu = menuManager.createContextMenu(viewer.getTree());
		viewer.getControl().setMenu(contextMenu);		
		getSite().registerContextMenu(menuManager, viewer);
		
		TraceNavigatorUI.getGlobal().addView(this);
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		super.dispose();
	}
	
	public ISelection getSelection() {
		return viewer.getSelection();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	@Override
	public void setStep(TraceElement te) {
		rootNode.setStep(te.getTestId(), te.getCallStep(), te.getStep());
	}
	
	/**
	 * Returns an array of root elements of the type {@link NamedValue.VariableValue}.
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
	
	public static class MethodNode extends NamedValueNode {
		
//		private int testId = -1;
//		private long callStep = -1;
//		private long step;
		
		protected NamedValue root;

		public MethodNode(TreeViewer viewer) {
			super(viewer);
		}

		public void setStep(int testId, long call, long step) {
//			if (testId != this.testId || call != this.callStep) {
				MiConnection cnn = DatabaseConnector.cnn();
				root = new NamedValue.MethodFrameValue(cnn, testId, call, step);
				setValue(root);
				root.onInitialized(asyncUpdate());
				
//			} else if (step != this.step) {
//				updateStep(step);
////				root = new NamedValue.MethodFrameValue(testId, call, step);
////				root.onInitialized(asyncUpdate());
////				//root.beInitialized();
////				setValue(root);
//////				root.updateValue(step, cbUpdateNamedValue);
//////				if (!root.isInitialized()) {
//////				}
//			}
				
//			this.testId = testId;
//			this.callStep = call;
//			this.step = step;
		}
	}
	
}

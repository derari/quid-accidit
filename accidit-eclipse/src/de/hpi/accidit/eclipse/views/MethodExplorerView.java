package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.model.Trace;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.views.provider.MethodsLabelProvider;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class MethodExplorerView extends ViewPart implements ISelectionChangedListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.MethodExplorerView";

	private TraceNavigatorUI ui;
	private TreeViewer treeViewer;

	public MethodExplorerView() { }

	@Override
	public void createPartControl(Composite parent) {
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		treeViewer = new TreeViewer(tree);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.setUseHashlookup(true);
		
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(), SWT.LEFT | SWT.FILL);
		column0.setText("Method");
		column0.setWidth(500);
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(), SWT.RIGHT);
		column1.setText("Call Step");
		column1.setWidth(60);
		
		treeViewer.setContentProvider(new TraceContentProvider(treeViewer));
		treeViewer.setLabelProvider(new MethodsLabelProvider());
		
		getSite().setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(this);
		
		ui = TraceNavigatorUI.getGlobal();
		ui.setTraceExplorer(this);
		
		treeViewer.getTree().addKeyListener(new TraceExplorerKeyAdapter());
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public void setTestCaseId(int id) {
		treeViewer.setInput(new Trace(id, ui));
	}
	
	public void refresh() {
		treeViewer.refresh();
	}
	
	public TraceElement[] getRootElements() {
		Trace trace = (Trace) treeViewer.getInput();
		return trace.getRootElements();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof ITreeSelection) {
			Object obj = ((ITreeSelection) selection).getFirstElement();

			if (obj instanceof TraceElement) {
				TraceElement te = (TraceElement) obj;
				ui.setStep(te);
			}
			setFocus();
		}	
	}
	
	class TraceExplorerKeyAdapter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			e.doit = false;
			
			switch(e.keyCode) {
			case SWT.ARROW_UP: handleArrowUp(); break;
			case SWT.ARROW_LEFT: handleArrowLeft(); break;
			case SWT.ARROW_RIGHT: handleArrowRight(); break;
			case SWT.ARROW_DOWN: handleArrowDown(); break;
			default: break;
			}
		}
		
		private void handleArrowUp() {
			ITreeSelection currentSelection = (ITreeSelection) treeViewer.getSelection();
			if (currentSelection.isEmpty()) return;
			
			TreePath path = currentSelection.getPaths()[0];
			TreePath parentPath = path.getParentPath();
			
			if (parentPath.getSegmentCount() > 0) {
				// build path from tree
				TreeItem item = treeViewer.getTree().getSelection()[0];
				TreeItem parentItem = item.getParentItem();

				int itemIndex = parentItem.indexOf(item);
				if (itemIndex > 0) {
					TreeItem previousItem = parentItem.getItems()[itemIndex - 1];
					TreePath previousItemPath = parentPath.createChildPath(previousItem.getData());
					treeViewer.setSelection(new TreeSelection(previousItemPath));
				} else {
					treeViewer.setSelection(new TreeSelection(parentPath));
					treeViewer.collapseToLevel(parentPath, 1);
				}
			}
		}

		private void handleArrowLeft() {
			ITreeSelection currentSelection = (ITreeSelection) treeViewer.getSelection();
			if (currentSelection.isEmpty()) return;
			
			TreePath path = currentSelection.getPaths()[0];
			TreePath parentPath = path.getParentPath();
			
			if (parentPath.getSegmentCount() > 0) {
				TreeSelection newSelection = new TreeSelection(parentPath);
				treeViewer.setSelection(newSelection);
				treeViewer.collapseToLevel(parentPath, 1);
			}
		}
		
		private void handleArrowRight() {
			ITreeSelection currentSelection = (ITreeSelection) treeViewer.getSelection();
			if (currentSelection.isEmpty()) return;
			
			TreePath path = currentSelection.getPaths()[0];
			treeViewer.expandToLevel(path, 1); // load elements asynchronously
						
			TreeItem item = treeViewer.getTree().getSelection()[0];
			if (item.getItemCount() > 0) {
				TreePath childPath = path.createChildPath(item.getItem(0).getData());
				treeViewer.setSelection(new TreeSelection(childPath));
			}
		}
		
		private void handleArrowDown() {
			ITreeSelection currentSelection = (ITreeSelection) treeViewer.getSelection();
			if (currentSelection.isEmpty()) {
//				treeViewer.expandToLevel(TreePath.EMPTY, 1);
				TreeItem[] rootItems = treeViewer.getTree().getItems();
				if (rootItems.length > 0) {
					treeViewer.setSelection(new StructuredSelection(rootItems[0].getData()));
				}
				return;
			}
			
			TreePath path = currentSelection.getPaths()[0];
			TreePath parentPath = path.getParentPath();
			if (parentPath.getSegmentCount() > 0) {
				// build path from tree
				TreeItem item = treeViewer.getTree().getSelection()[0];
				TreeItem parentItem = item.getParentItem();

				int itemIndex = parentItem.indexOf(item);
				if (parentItem.getItemCount() > itemIndex + 1) {
					TreeItem nextItem = parentItem.getItems()[itemIndex + 1];
					TreePath nextItemPath = parentPath.createChildPath(nextItem.getData());
					treeViewer.setSelection(new TreeSelection(nextItemPath));
				} else {
					// find find item behind the parent
					// assumption: there's always a next item exactly one level above (return)
					TreeItem parentItemParent = parentItem.getParentItem();
					if (parentItemParent == null) {
						treeViewer.setSelection(null);
						treeViewer.collapseAll();
						return;
					}
					
					int parentItemIndex = parentItemParent.indexOf(parentItem);
					TreeItem nextParentItem = parentItemParent.getItem(parentItemIndex + 1); 
					TreePath nextParentItemPath = parentPath.getParentPath().createChildPath(nextParentItem.getData());
					treeViewer.setSelection(new TreeSelection(nextParentItemPath));
					treeViewer.collapseToLevel(parentPath, 1);
				}
			}
		}
	}
	
	public static class TraceContentProvider implements ILazyTreeContentProvider {
		
		private TreeViewer viewer;
		private Trace trace = null;
		
		public TraceContentProvider(TreeViewer viewer) {
			this.viewer = viewer;
		}
		
		private DoInUiThread<Invocation> updateInvocation = new DoInUiThread<Invocation>() {
			@Override
			protected void run(Invocation inv, Throwable error) {
				if (error != null) {
					error.printStackTrace(System.err);
				}
				viewer.setChildCount(inv, 0);
				viewer.setChildCount(inv, inv.getChildren().length);
				viewer.update(inv, null);
			}
		};

		@Override
		public void dispose() { }

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			trace = (Trace) newInput;
		}

		@Override
		public Object getParent(Object arg0) {
			return null;
		}

		@Override
		public void updateChildCount(Object element, int currentChildCount) {
			if (element instanceof Trace) {
				viewer.setChildCount(element, trace.root.length);
			}
			if (element instanceof Invocation) {
				final Invocation inv = (Invocation) element;
				if (inv.isInitialized()) {
					viewer.setChildCount(inv, inv.getChildren().length);
				} else {
					viewer.setChildCount(inv, 1);
					viewer.replace(inv, 0, new Pending());
					inv.onInitialized(updateInvocation);
				}
			}
		}

		@Override
		public void updateElement(Object parent, final int index) {
			if (parent instanceof Trace) {
				Trace trace = (Trace) parent;
				viewer.replace(parent, index, trace.root[index]);
				updateChildCount(trace.root[index], -1);
			}
			if (parent instanceof Invocation) {
				final Invocation inv = (Invocation) parent;
				if (!inv.isInitialized()) {
					System.out.println(" --- should not happen --- ");
					inv.beInitialized();
				}
				viewer.replace(inv, index, inv.getChildren()[index]);
				updateChildCount(inv.getChildren()[index], -1);
			}
		}
	}
}

package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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

	public MethodExplorerView() { 
		System.out.println("");
	}

	@Override
	public void createPartControl(Composite parent) {
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		treeViewer = new TreeViewer(tree);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.setUseHashlookup(true);
		
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column0.setText("Method");
		column0.setWidth(500);
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(), SWT.RIGHT);
		column1.setText("Call Step");
		column1.setWidth(60);
		TreeColumn column2 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column2.setText("Call Location");
		column2.setWidth(200);
		TreeColumn column3 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column3.setText("Method Id");
		column3.setWidth(50);
		
		treeViewer.setContentProvider(new TraceContentProvider(treeViewer));
		treeViewer.setLabelProvider(new MethodsLabelProvider());
		
		getSite().setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(this);
		
		ui = TraceNavigatorUI.getGlobal();
		ui.setTraceExplorer(this);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public int getTestCaseId() {
		return 0;
	}
	
	public void setTestCaseId(int id) {
		treeViewer.setInput(new Trace(id, ui));
	}
	
	public void refresh() {
		treeViewer.refresh();
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
	
	public static class TraceContentProvider implements ILazyTreeContentProvider {
		
		private TreeViewer viewer;
		private Trace trace = null;
		
		public TraceContentProvider(TreeViewer viewer) {
			this.viewer = viewer;
		}

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
				if (inv.asyncChildren().isDone()) {
					viewer.setChildCount(inv, inv.getChildren().length);
				} else {
					viewer.setChildCount(inv, 1);
					inv.asyncChildren().onComplete(new DoInUiThread<TraceElement[]>() {
						@Override
						protected void run(TraceElement[] value, Throwable error) {
							viewer.setChildCount(inv, 0);
							if (error != null) {
								error.printStackTrace(System.err);
							} else {
								viewer.setChildCount(inv, value.length);
							}
							viewer.update(inv, null);
						}
					});
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
				if (inv.asyncChildren().isDone()) {
					viewer.replace(inv, index, inv.getChildren()[index]);
					updateChildCount(inv.getChildren()[index], -1);
				} else {
					Pending p = new Pending();
					viewer.replace(inv, index, p);
					viewer.setChildCount(p, 0);
				}
			}
		}
		
	}
}

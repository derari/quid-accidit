package de.hpi.accidit.eclipse.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.views.elements.CalledMethod;
import de.hpi.accidit.eclipse.views.elements.CalledMethodContentProvider;
import de.hpi.accidit.eclipse.views.elements.CalledMethodLabelProvider;
import de.hpi.accidit.eclipse.views.elements.JavaSrcFilesLocator;

public class MethodExplorerView extends ViewPart implements ISelectionChangedListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.MethodExplorerView";

	private TreeViewer treeViewer;
	private CalledMethodContentProvider contentProvider;
	private JavaSrcFilesLocator srcFilesLocator;

	public MethodExplorerView() { }

	@Override
	public void createPartControl(Composite parent) {
		srcFilesLocator = new JavaSrcFilesLocator();
		
		treeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.getTree().setHeaderVisible(true);
		
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
		
		contentProvider = new CalledMethodContentProvider();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new CalledMethodLabelProvider());
		treeViewer.setInput(getViewSite());
		
		getSite().setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(this);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	public int getTestCaseId() {
		return contentProvider.getCurrentTestCaseId();
	}
	
	public void setTestCaseId(int id) {
		contentProvider.setCurrentTestCaseId(id);
	}
	
	public void refresh() {
		treeViewer.refresh();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof ITreeSelection) {
			Object obj = ((ITreeSelection) selection).getFirstElement();
			if (obj instanceof CalledMethod) {
				CalledMethod method = (CalledMethod) obj;
				
				String filePath = (method.parentMethod != null) ? method.parentMethod.type : method.type;
				int line = method.callLine;
				srcFilesLocator.open(filePath, line, getViewSite().getPage());
				setFocus();
			}
		}	
	}
}

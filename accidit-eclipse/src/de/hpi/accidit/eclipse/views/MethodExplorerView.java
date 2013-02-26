package de.hpi.accidit.eclipse.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hpi.accidit.eclipse.views.elements.CalledMethod;
import de.hpi.accidit.eclipse.views.elements.CalledMethodContentProvider;
import de.hpi.accidit.eclipse.views.elements.CalledMethodLabelProvider;

public class MethodExplorerView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "de.hpi.accidit.eclipse.views.MethodExplorerView";

	private TreeViewer treeViewer;
	private CalledMethodContentProvider contentProvider;

	public MethodExplorerView() { }

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
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
//		TreeColumn column3 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
//		column3.setText("Method Id");
//		column3.setWidth(50);
		
		contentProvider = new CalledMethodContentProvider();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new CalledMethodLabelProvider());
		treeViewer.setInput(getViewSite());
		
		getSite().setSelectionProvider(treeViewer);
		
		hookDoubleCLickAction();
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
	
	private void hookDoubleCLickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				
				// TODO refactor all ...
				
				if (!(obj instanceof CalledMethod)) return;
				
				CalledMethod method = (CalledMethod) obj;
				String filePath = (method.parentMethod != null) ? method.parentMethod.type : method.type;
				// TODO put in own class to avoid multiple instances etc and use it outside of the ui thread
				IProject[] workspaceProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for(IProject project : workspaceProjects) {
					try {
						// Check if it is a Java project
						boolean isJavaProject = false;
						String[] natureIds = project.getDescription().getNatureIds();
						for(String natureId : natureIds) {
							if(natureId.equals("org.eclipse.jdt.core.javanature")) {
								isJavaProject = true;
								break;
							}
						}
						if(!isJavaProject) {
							// only search for source code files in java projects
							continue;
						}

						// Search the IFile
						IJavaProject javaProject = JavaCore.create(project);
						IType javaFileType = javaProject.findType(filePath);

						if(javaFileType != null) {
							IResource iResource = javaFileType.getResource();
							if (iResource == null) {
								// TODO create window here and remove the error message.
								System.err.println("iResource is null.");
								continue;
							}

							IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iResource.getFullPath());
							IWorkbenchPage dPage = MethodExplorerView.this.getViewSite().getWorkbenchWindow().getActivePage();
							if (dPage != null) {
								try {										
									IEditorPart textEditor = IDE.openEditor(dPage, iFile, true);
									highlightLine(textEditor, method.callLine);
								}catch (Exception e) {
									// log exception
								}
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			};
		});
	}
	
	private void highlightLine(IEditorPart editorPart, int lineNumber) {
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) return;
		
		ITextEditor textEditor = (ITextEditor) editorPart;
		IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		if (document != null) {
			IRegion lineInfo = null;
			try {
				// line count internally starts with 0, and not with 1 like in GUI
				lineInfo = document.getLineInformation(lineNumber - 1);
			} catch (BadLocationException e) {
				// ignored because line number may not exist in document
				e.printStackTrace();
			}
			if (lineInfo != null) {
				textEditor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		}
	}
	
}

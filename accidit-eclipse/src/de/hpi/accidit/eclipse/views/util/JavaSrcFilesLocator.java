package de.hpi.accidit.eclipse.views.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.TraceElement;

public class JavaSrcFilesLocator {
	
	private static final String ORG_ECLIPSE_JDT_CORE_JAVANATURE = "org.eclipse.jdt.core.javanature";
	private List<IJavaProject> javaWorkspaceProjects;
	private IProject lastSelectedProject = null;
	
	private List<IJavaProject> getProjects() {
		if (!Objects.equals(lastSelectedProject, DatabaseConnector.getSelectedProject())) {
			lastSelectedProject = DatabaseConnector.getSelectedProject();
			javaWorkspaceProjects = null;
		}
		if (javaWorkspaceProjects != null) return javaWorkspaceProjects;
		
		// Lazy loading failed. Initialize the projects member.
		List<IJavaProject> projects = new LinkedList<IJavaProject>();
		List<IProject> workspaceProjects = new ArrayList<>(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
		workspaceProjects.remove(lastSelectedProject);
		workspaceProjects.add(0, lastSelectedProject);
		
		for(IProject project : workspaceProjects) {
			if (!project.isOpen()) continue;
			try {
				boolean isJavaProject = false;
				String[] natureIds = project.getDescription().getNatureIds();
				for(String natureId : natureIds) {
					if(natureId.equals(ORG_ECLIPSE_JDT_CORE_JAVANATURE)) {
						isJavaProject = true;
						break;
					}
				}
				if(!isJavaProject) {
					// drop results that are no java projects
					continue;
				}

				projects.add(JavaCore.create(project));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		javaWorkspaceProjects = projects;
		return projects;
	}
	
	public void open(final TraceElement te, final IWorkbenchPage dPage, final ViewPart activeView) {
		if (dPage == null || te == null || te.parent == null) return;
		
		Job job = new Job("My Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final IFile file = getFile(te.parent.type);
				if (file == null) {
					open(te.parent, dPage, activeView);
					return Status.OK_STATUS;
				}
				
				// Update the UI
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							IEditorPart textEditor = IDE.openEditor(dPage, file, true);							
							if (highlightLine(textEditor, te)) {
								activeView.setFocus();
							} else {
								open(te.parent, dPage, activeView);
							}
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				});

				return Status.OK_STATUS;
			}
		};

		// Start the Job
		job.schedule(); 
	}
	
	private IFile getFile(String filePath) {
		for(IJavaProject javaProject : getProjects()) {
			IType javaFileType;
			try {
				javaFileType = javaProject.findType(filePath);
			} catch (JavaModelException e) {
				e.printStackTrace();
				continue;
			}
			if(javaFileType == null) continue;
			
			IResource iResource = javaFileType.getResource();
			if (iResource == null) continue;

			return ResourcesPlugin.getWorkspace().getRoot().getFile(iResource.getFullPath());
		}
		return null;
	}
	
//	public void open_old(String filePath, int line, IWorkbenchPage dPage) {
//		for(IJavaProject javaProject : getProjects()) {
//			IType javaFileType;
//			try {
//				javaFileType = javaProject.findType(filePath);
//			} catch (JavaModelException e) {
//				e.printStackTrace();
//				continue;
//			}
//
//			if(javaFileType == null) continue;
//			IResource iResource = javaFileType.getResource();
//			
//			if (iResource == null) continue;
//			IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iResource.getFullPath());
//			
//			if (dPage != null) {
//				try {										
//					IEditorPart textEditor = IDE.openEditor(dPage, iFile, true);
//					highlightLine(textEditor, line);
//				}catch (Exception e) {
//					e.printStackTrace();
//					// log exception
//				}
//			}
//		}
//	}
	
	private boolean highlightLine(IEditorPart editorPart, TraceElement te) {
		int lineNumber = te.line;
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) return false;
		
		ITextEditor textEditor = (ITextEditor) editorPart;
		IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		if (document == null) return false;
		IRegion lineInfo = null;
		try {
			// line count internally starts with 0, and not with 1 like in GUI
			lineInfo = document.getLineInformation(lineNumber - 1);
		} catch (BadLocationException e) {
			// ignored because line number may not exist in document
			// e.printStackTrace();
		}
		if (lineInfo == null) return false;
		int offset = lineInfo.getOffset();
		int length = lineInfo.getLength();
		if (te instanceof Invocation) {
			String name = ((Invocation) te).method;
			int i = name.indexOf('(');
			if (i > 0) name = name.substring(0, i);
			if (name.equals("<init>")) name = ((Invocation) te).type;
//			String line = document.get().substring(offset, offset+length);
			Matcher m = Pattern.compile("(" + name + ")\\s*\\(").matcher(document.get());
			if (m.find(offset)) {
				offset = m.start(1);
				length = m.end(1) - offset;
			}
		}
		textEditor.selectAndReveal(offset, length);
		return true;
	}

}

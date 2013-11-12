package de.hpi.accidit.eclipse.breakpoints;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.ui.texteditor.ITextEditor;

public class AcciditBreakpointAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ITextEditor) {
			ITextEditor editorPart = (ITextEditor) adaptableObject;
			IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				String extension = resource.getFileExtension();
				if (extension != null && extension.equals("pda")) {
					
					System.out.println("Adapter factory here!");
					
					return null;
//					return new PDALineBreakpointAdapter();
				}
			} 
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] {IToggleBreakpointsTarget.class};
	}

}

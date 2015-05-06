package de.hpi.accidit.eclipse.breakpoints;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hpi.accidit.eclipse.TraceNavigatorUI;

public class ToggleLineBreakpointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editorPart = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor) editorPart.getAdapter(ITextEditor.class);
		
		IVerticalRulerInfo rulerInfo = (IVerticalRulerInfo) textEditor.getAdapter(IVerticalRulerInfo.class);
		int lineNumber = rulerInfo.getLineOfLastMouseButtonActivity() + 1;
		
		if (textEditor != null) {
			IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
			TraceNavigatorUI.getGlobal().getBreakpointsManager().toggleBreakpoint(resource, lineNumber);		
		}
		
		return null;
	}
}

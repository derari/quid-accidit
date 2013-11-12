package de.hpi.accidit.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hpi.accidit.eclipse.breakpoints.AcciditLineBreakpoint;

public class ToggleAcciditLineBreakpointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		System.out.println("TOGGLE: Accidit Line Breakpoint");
		
		IEditorPart editorPart = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor) editorPart.getAdapter(ITextEditor.class);
		
		IVerticalRulerInfo rulerInfo = (IVerticalRulerInfo) textEditor.getAdapter(IVerticalRulerInfo.class);
		int lineNumber = rulerInfo.getLineOfLastMouseButtonActivity() + 1;
		
		if (textEditor != null) {
			IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
			IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint[] breakpoints = breakpointManager.getBreakpoints(AcciditLineBreakpoint.DEBUG_MODEL);
			
			for (IBreakpoint breakpoint : breakpoints) {
				if (breakpoint instanceof AcciditLineBreakpoint) {
					if (resource.equals(breakpoint.getMarker().getResource())) {
						try {
							if (((ILineBreakpoint) breakpoint).getLineNumber() == (lineNumber)) {
								breakpoint.delete();
								return null;
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
	
			try {
				AcciditLineBreakpoint lineBreakpoint = new AcciditLineBreakpoint(resource, lineNumber);
				DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}

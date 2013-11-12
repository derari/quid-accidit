package de.hpi.accidit.eclipse.breakpoints;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.views.BreakpointsView;

public class AcciditLineBreakpoint extends LineBreakpoint {
	
	public static final String DEBUG_MODEL = "de.hpi.accidit.eclipse.debugModel";
	private static final String MARKER_ID = "de.hpi.accidit.eclipse.acciditBreakpointMarker";

	public AcciditLineBreakpoint(IResource resource, int lineNumber) throws CoreException {
		IMarker marker = resource.createMarker(MARKER_ID);
		setMarker(marker);
		setEnabled(true);
		ensureMarker().setAttribute(IMarker.LINE_NUMBER, lineNumber);
		ensureMarker().setAttribute(IBreakpoint.ID, DEBUG_MODEL);
		
		addToBreakpointsView();
	}

	@Override
	public String getModelIdentifier() {
		return DEBUG_MODEL;
	}
	
	public void delete() throws CoreException {
		super.delete();
	}
	
	private void addToBreakpointsView() {
		BreakpointsView breakpointsView = TraceNavigatorUI.getGlobal().getBreakpointsView();
		
		IResource resource = getMarker().getResource();
		String breakpointPath = resource.getFullPath().toString();
		int breakpointLineNumber = getMarker().getAttribute(IMarker.LINE_NUMBER, 0);
		
		breakpointsView.addBreakpointLine(String.format("%1$s : %2$s", breakpointPath, breakpointLineNumber));
	}
}

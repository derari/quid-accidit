package de.hpi.accidit.eclipse.breakpoints;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.hpi.accidit.eclipse.TraceNavigatorUI;

public class BreakpointsManager {
	
	private List<LineBreakpoint> currentBreakpoints = new ArrayList<LineBreakpoint>();
	
	public void toggleBreakpoint(IResource resource, int lineNumber) {
		try {
			LineBreakpoint breakpoint = getBreakpoint(resource, lineNumber);
			if (breakpoint == null) {
				addBreakpoint(resource, lineNumber);
			} else {
				removeBreakpoint(breakpoint);
			}		
		} catch (CoreException e) {
			e.printStackTrace();
		}	
	}
	
	private LineBreakpoint getBreakpoint(IResource resource, int lineNumber) {
		for (LineBreakpoint breakpoint : currentBreakpoints) {
			if (breakpoint.getResource() == resource && breakpoint.getLineNumber() == lineNumber)
				return breakpoint;
		}
		return null;
	}
	
	public void addBreakpoint(IResource resource, int lineNumber) throws CoreException {
		LineBreakpoint breakpoint = new LineBreakpoint(resource, lineNumber);

		BreakpointsView breakpointsView = TraceNavigatorUI.getGlobal().getBreakpointsView();
		breakpointsView.addBreakpointLine(breakpoint);
		
		currentBreakpoints.add(breakpoint);
	}
	
	public void removeBreakpoint(LineBreakpoint breakpoint) throws CoreException {
		breakpoint.delete();

		BreakpointsView breakpointsView = TraceNavigatorUI.getGlobal().getBreakpointsView();
		breakpointsView.removeBreakpointLine(breakpoint);
		
		currentBreakpoints.remove(breakpoint);
	}
}

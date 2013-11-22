package de.hpi.accidit.eclipse.breakpoints;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Widget;

public class AcciditLineBreakpoint {
	
	private static final String MARKER_ID = "de.hpi.accidit.eclipse.acciditAnnotationMarker";
	
	private IResource resource;
	private int lineNumber;
	
	private IMarker marker;
	
	private List<Widget> widgets;

	public AcciditLineBreakpoint(IResource resource, int lineNumber) throws CoreException {
		this.resource = resource;
		this.lineNumber = lineNumber;

		IMarker marker = resource.createMarker(MARKER_ID);
		marker.setAttribute(IMarker.SEVERITY, 1);
		marker.setAttribute(IMarker.MESSAGE, "Accidit Breakpoint");
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		this.marker = marker;
		
		this.widgets = new ArrayList<Widget>();
	}
	
	public IResource getResource() {
		return resource;
	}
	
	public void setResource(IResource resource) {
		this.resource = resource;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public IMarker getMarker() {
		return marker;
	}
	
	public void addUIElement(Widget widget) {
		widgets.add(widget);
	}
	
	public List<Widget> getUIElements() {
		return widgets;
	}
	
	public String getLocationInformation() {
		return String.format("%1$s : %2$s", resource.getFullPath().toString(), lineNumber);
	}

	public void delete() throws CoreException {
		marker.delete();
	}
}

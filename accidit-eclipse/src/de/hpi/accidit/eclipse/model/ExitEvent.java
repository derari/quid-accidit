package de.hpi.accidit.eclipse.model;

public class ExitEvent extends TraceElement {

	public boolean returned;
	
	public ExitEvent(Invocation parent, boolean returned, int line, long step) {
		this.parent = parent;
		this.returned = returned;
		this.line = line;
		this.step = step;
	}
	
	@Override
	public String getImage() {
		return returned ? "trace_return.png" : "trace_fail.png";
	}
	
	@Override
	public String getShortText() {
		return returned ? "return" : "fail";
	}
	
}

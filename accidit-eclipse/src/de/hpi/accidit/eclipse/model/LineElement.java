package de.hpi.accidit.eclipse.model;

public class LineElement extends TraceElement {

	public LineElement(Invocation inv, long step, int line) {
		this.parent = inv;
		this.step = step;
		this.line = line;
	}
	
	@Override
	public String getImage() {
		return "trace_line.png";
	}
	
	@Override
	public String getShortText() {
		return "line " + line;
	}
	
}

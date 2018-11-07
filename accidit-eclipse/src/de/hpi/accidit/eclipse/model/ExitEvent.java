package de.hpi.accidit.eclipse.model;

public class ExitEvent extends TraceElement {

	public boolean returned;
	private Value value;
	
	public ExitEvent(Invocation parent, boolean returned, int line, long step, Value value) {
		this.parent = parent;
		this.returned = returned;
		this.line = line;
		this.step = step;
		this.value = value;
	}
	
	@Override
	public String getImage() {
		return returned ? "trace_return.png" : "trace_fail.png";
	}
	
	@Override
	public String getShortText() {
		if (value == null) {
			return returned ? "return" : "fail";
		} else {
			return returned ? "return " : "fail  --  " + value.getShortString();
		}
	}
	
}

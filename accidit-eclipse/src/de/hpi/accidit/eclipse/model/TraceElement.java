package de.hpi.accidit.eclipse.model;

public class TraceElement extends ModelBase implements Comparable<TraceElement> {

	public Invocation parent;
	public int line;
	public long step;
	
	@Override
	public int compareTo(TraceElement o) {
		int c = Long.compare(step, o.step);
		if (c != 0) return c;
		return deepCompare(o);
	}

	protected int deepCompare(TraceElement o) {
		return 0;
	}
	
	public String getImage() {
		return "";
	}
	
	public String getShortText() {
		return "";
	}
}

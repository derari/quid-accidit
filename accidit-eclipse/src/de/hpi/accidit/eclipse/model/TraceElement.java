package de.hpi.accidit.eclipse.model;

public class TraceElement extends ModelBase implements Comparable<TraceElement> {

	protected int testId = -1;
	public Invocation parent;
	public int line;
	protected long step;
	
	@Override
	public int compareTo(TraceElement o) {
		int c = Long.compare(getStep(), o.getStep());
		if (c != 0) return c;
		return deepCompare(o);
	}
	
	public int getTestId() {
		if (testId == -1 && parent != null) {
			testId = parent.getTestId();
		}
		return testId;
	}
	
	public long getStep() {
		return step;
	}
	
	public long getCallStep() {
		if (parent == null) return 0;
		return parent.getStep();
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

package de.hpi.accidit.eclipse.model;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.MiConnection;
import static org.cthul.miro.DSL.*;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.db.SideEffectsDao;

public class SideEffects extends Value.ValueWithChildren {

	private final int testId;
	private final long captureStart;
	private final long captureEnd;
	private final long targetStart;
	private final long targetEnd;
	
	public SideEffects(MiConnection cnn, int testId, long start, long end) {
		super(cnn);
		this.testId = testId;
		this.captureStart = start;
		this.captureEnd = end;
		this.targetStart = captureEnd+1;
		this.targetEnd = Long.MAX_VALUE;
	}

	public SideEffects(MiConnection cnn, int testId, long captureStart, long captureEnd, long targetStart, long targetEnd) {
		super(cnn);
		this.testId = testId;
		this.captureStart = captureStart;
		this.captureEnd = captureEnd;
		this.targetStart = targetStart;
		this.targetEnd = targetEnd;
	}
	
	@Override
	public String getLongString() {
		return "_";
	}
	
	@Override
	public String getShortString() {
		return "_";
	}
	
	@Override
	protected NamedValue[] fetchChildren() throws Exception {
		List<NamedValue> children = new ArrayList<>();
		List<FieldValue> fields = select()
			.from(SideEffectsDao.FIELDS)
			.inTest(testId)
			.captureBetween(captureStart, captureEnd)
			.targetBetween(targetStart, targetEnd)
			.execute(cnn()).asList();
		children.addAll(fields);
		return children.toArray(new NamedValue[children.size()]);
	}
	
	public class InstanceEffects extends ObjectSnapshot implements Comparable<InstanceEffects> {
		
		List<NamedValue> events = new ArrayList<>();
		
		public InstanceEffects(long id) {
			super(SideEffects.this.cnn(),
					SideEffects.this.testId,
					id,
					SideEffects.this.targetStart);
		}

		@Override
		public int compareTo(InstanceEffects o) {
			int s = events.size() - o.events.size();
			if (s != 0) return s;
			return (thisId - o.thisId) < 0 ? -1 : 1;
		}
		
	}
}

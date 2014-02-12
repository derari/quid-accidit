package de.hpi.accidit.eclipse.model;

import static org.cthul.miro.DSL.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cthul.miro.MiConnection;

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
		Map<Long, InstanceEffects> instances = new HashMap<>();
		List<FieldEffect> fields = select()
			.from(SideEffectsDao.FIELDS)
			.inTest(testId)
			.captureBetween(captureStart, captureEnd)
			.targetBetween(targetStart, targetEnd)
			._execute(cnn()).asList();
		for (FieldEffect fe: fields) {
			long thisId = fe.getThisId();
			InstanceEffects inst = instances.get(thisId);
			if (inst == null) {
				inst = new InstanceEffects(thisId);
				instances.put(thisId, inst);
			}
			inst.events.add(fe);
		}
		
		Set<NamedValue> children = new TreeSet<>();
		children.addAll(instances.values());
		return children.toArray(new NamedValue[children.size()]);
	}
	
	private class InstanceEffects extends NamedValue implements Comparable<InstanceEffects> {
		
		private long thisId;
		private ObjectSnapshot object;
		private List<NamedValue> events = new ArrayList<>();
		
		public InstanceEffects(long thisId) {
			this.thisId = thisId;
			object = new ObjectSnapshot(SideEffects.this.cnn(),
										SideEffects.this.testId,
										thisId,
										SideEffects.this.targetStart);
		}
		
		@Override
		protected void lazyInitialize() throws Exception {
			object.getLongString();
			super.lazyInitialize();
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			return new InstanceEffectsValue(this);
		}
		
		@Override
		public String getName() {
			return object.getLongString();
		}

		@Override
		public int compareTo(InstanceEffects o) {
			int s = events.size() - o.events.size();
			if (s != 0) return s;
			return (thisId - o.thisId) < 0 ? -1 : 1;
		}
	}
	
	private class InstanceEffectsValue extends ValueWithChildren {
		
		private final InstanceEffects ie;
		
		public InstanceEffectsValue(InstanceEffects ie) {
			this.ie = ie;
		}
		
		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			return ie.events.toArray(new NamedValue[0]);
		}

		@Override
		public String getShortString() {
			return ie.object.getShortString();
		}

		@Override
		public String getLongString() {
			return ie.object.getLongString();
		}
	}
	
	public class FieldEffect extends FieldValue {
		
		private final List<FieldValue> reads = new ArrayList<>();
		private long thisId;
		
		public long getThisId() {
			return thisId;
		}
		
		public void addRead(FieldValue read) {
			reads.add(read);
		}
		
		public List<FieldValue> getReads() {
			return reads;
		}
	}
}

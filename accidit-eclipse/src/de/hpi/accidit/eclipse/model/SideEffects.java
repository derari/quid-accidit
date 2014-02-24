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
import de.hpi.accidit.eclipse.model.db.ObjectOccurranceDao;
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
			select().from(ObjectOccurranceDao.OBJECTS)
			.inTest(testId)
			.beforeStep(captureEnd);
		
		System.out.println(instances.size() + " instances with SEs");
		Set<NamedValue> children = new TreeSet<>();
		children.addAll(instances.values());
		return children.toArray(new NamedValue[children.size()]);
	}
	
	public class InstanceEffects extends NamedValue implements Comparable<InstanceEffects> {
		
		private long thisId;
		private Value object;
		private List<NamedValue> events = new ArrayList<>();
		private boolean newInstance = false;
		
		public InstanceEffects(long thisId) {
			this.thisId = thisId;
		}
		
		private Value getObject() {
			if (object == null) {
				object = Value.object(SideEffects.this.testId, thisId, SideEffects.this.captureEnd)
						.select()._execute(SideEffects.this.cnn());
			}
			return object;
		}
		
		public long getThisId() {
			return thisId;
		}
		
		@Override
		protected void lazyInitialize() throws Exception {
			getObject();
			super.lazyInitialize();
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			return new InstanceEffectsValue(this);
		}
		
		@Override
		public String getName() {
			String s = getObject().getLongString();
			if (newInstance) s = "* " + s;
			return s;
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
	
	public static class FieldEffect extends FieldValue {
		
		private final List<FieldValue> reads = new ArrayList<>();
		private long thisId;
		
		public FieldEffect() {
		}
		
		public long getThisId() {
			return thisId;
		}
		
		public void addRead(FieldValue read) {
			reads.add(read);
		}
		
		public List<FieldValue> getReads() {
			return reads;
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			return new FieldEffectValue(this, super.fetchValue());
		}
	}
	
	private static class FieldEffectValue extends ValueWithChildren {
		
		private final FieldEffect ie;
		private final Value value;
		
		public FieldEffectValue(FieldEffect ie, Value value) {
			this.ie = ie;
			this.value = value;
		}
		
		@Override
		protected NamedValue[] fetchChildren() throws Exception {
			return ie.reads.toArray(new NamedValue[0]);
		}

		@Override
		public String getShortString() {
			return value.getShortString();
		}

		@Override
		public String getLongString() {
			return value.getLongString();
		}
	}
}

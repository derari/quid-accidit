package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.view.ViewR;
import static org.cthul.miro.DSL.*;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.db.NamedValueDao;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.ArrayHistoryQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.FieldQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.ItemQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.ObjHistoryQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.VarHistoryQuery;
import de.hpi.accidit.eclipse.model.db.NamedValueDao.VarQuery;

public class NamedValue extends ModelBase implements NamedEntity {
	
	public static final ViewR<VarQuery> VARIABLE_VIEW = NamedValueDao.VARIABLE_VIEW;
	public static final ViewR<FieldQuery> FIELD_VIEW = NamedValueDao.FIELD_VIEW;
	public static final ViewR<ItemQuery> ARRAY_ITEM_VIEW = NamedValueDao.ARRAY_ITEM_VIEW;
	public static final ViewR<VarHistoryQuery> VARIABLE_HISTORY_VIEW = NamedValueDao.VARIABLE_HISTORY_VIEW;
	public static final ViewR<ObjHistoryQuery> OBJECT_HISTORY_VIEW = NamedValueDao.OBJECT_HISTORY_VIEW;
	public static final ViewR<ArrayHistoryQuery> ARRAY_HISTORY_VIEW = NamedValueDao.ARRAY_HISTORY_VIEW;

	protected int testId;
	protected long step;
//	protected long callStep;
	protected long valueStep;
	protected long nextChangeStep;
	protected long nextGetStep = -1;
	protected long lastGetStep = -1;
	
	protected int id = -1;
	protected String name;
	protected Value value;
	protected String method;
	
	private Value owner;
	
	public NamedValue() { }
	
	public NamedValue(MiConnection cnn) {
		super(cnn);
	}

	public NamedValue(String name) {
		super();
		this.name = name;
		value = new Value.Primitive('V', 0);
		valueStep = -1;
		nextChangeStep = -1;
	}
	
	public NamedValue(String name, Value value) {
		this.name = name;
		this.value = value;
		valueStep = -1;
		nextChangeStep = -1;
	}

	public int getTestId() {
		return testId;
	}
	
	public long getStep() {
		return step;
	}
	
	public long getValueStep() {
		return valueStep;
	}
	
	public Value getOwner() {
		return owner;
	}
	
	public void setOwner(Value owner) {
		this.owner = owner;
	}
	
//	public long getCallStep() {
//		return callStep;
//	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		//Thread.sleep(500);
		if (value == null) {
			value = fetchValue();
		}
		if (value == null) {
			value = new Value.Primitive(valueStep + "/" + step + "/" + nextChangeStep);
		}
		value.beInitialized();
	}
	
	protected Value fetchValue() throws Exception {
		return new Value.Primitive(valueStep + "/" + step + "/" + nextChangeStep);
	}
	
	public synchronized Value getValue() {
		if (!beInitialized() || !isInitSuccess()) {
			Throwable t = getInitException();
			if (t != null) {
				t.printStackTrace(System.err);
				value = new Value.Primitive(getInitException());
			} else {
				value = new Value.Primitive("-no data-");
			}
		}
		return value;	
	}
	
	public boolean isActiveValue() {
		if (id == -1) {
			// some dummy value, assume active
			return true;
		}
		if (isGetTraced()) {
			if (nextGetStep == -1) {
				// value is never read again
				return false;
			}
			if (nextChangeStep != -1 && nextChangeStep < nextGetStep) {
				// value is changed before next read
				return false;
			}
		}
		if (valueStep == -1 || valueStep > step) {
			// value is in future
			return isActiveBeforeValueStep();
		}
		return true;
	}
	
	protected boolean isActiveBeforeValueStep() {
		return false;
	}
	
	protected boolean isGetTraced() {
		return true;
	}
	
//	public NamedValue[] previewChildren() {
//		if (value == null) return null;
//		return value.previewChildren();
//	}

	private boolean stepOver(long value, long oldStep, long newStep) {
		if (value == oldStep) {
			return value != newStep;
		} else if (value < oldStep) {
			return value >= newStep;
		} else { // value > oldStep
			return value <= newStep;
		}
	}
//	
//	public boolean valueUpdateNeeded(long newStep) {
//		return valueUpdateNeeded(step, newStep);
//	}
	
	public boolean updateNeeded(long newStep) {
		return valueUpdateNeeded(step, newStep) || minorUpdateNeeded(step, newStep);
	}
	
	private boolean valueUpdateNeeded(long oldStep, long newStep) {
		return (nextChangeStep != -1 && stepOver(nextChangeStep, oldStep, newStep))
				|| (valueStep != -1 && stepOver(valueStep, oldStep, newStep));
	}
	
	private boolean minorUpdateNeeded(long oldStep, long newStep) {
		return (nextGetStep != -1 && stepOver(nextGetStep, oldStep, newStep))
				|| (lastGetStep != -1 && stepOver(lastGetStep, oldStep, newStep));
	}
	
	public boolean setStep(long newStep) {
		assert !updateNeeded(newStep);
		step = newStep;
		if (!isInitialized()) {
			return true;
		}
		if (!setValueStep(newStep)) {
			return false;
		}
//		if (valueUpdateNeeded(oldStep, newStep) || minorUpdateNeeded(oldStep, newStep)) {
//			value = null;
//			reInitialize();
//			return false;
//		}
		return true;
	}

	private boolean setValueStep(long newStep) {
		if (value.updateNeeded(newStep)) {
			value = null;
			reInitialize();
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name + 
				"@" + Integer.toHexString(hashCode()) +
				(value == null ? "" : " " + Integer.toHexString(value.hashCode()));
	}
	
	/**
	 * Dummy value for the root of the tree,
	 * not actually "named".
	 */
	public static class MethodFrameValue extends NamedValue {

		private long call;
		
		public MethodFrameValue(MiConnection cnn, int testId, long call, long step) {
			super(cnn);
			this.testId = testId;
			this.call = call;
			this.step = step;
			this.valueStep = -1;
			this.nextChangeStep = -1;
		}

		@Override
		protected Value fetchValue() throws Exception {
			return new Value.MethodSnapshot(cnn(), testId, call, step);
		}	
	}
	
	public static class VariableValue extends NamedValue { 
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				return new Value.Primitive("--> " + nextChangeStep);
			}
			return select()
					.from(Value.ofVariable(id, testId, valueStep, step))
					.execute(DatabaseConnector.cnn());
		}
		
		@Override
		protected boolean isGetTraced() {
			return false;
		}
	}
	
	public static class FieldValue extends NamedValue {
		
		public FieldValue() {
		}
		
		public FieldValue(int testId, int fieldId, long valueStep, boolean valueIsPut, String name) {
			this.testId = testId;
			this.id = fieldId;
			this.valueStep = valueStep;
			this.step = valueStep;
			this.valueIsPut = valueIsPut;
			this.name = name;
		}
		
		private boolean valueIsPut;
		
		public boolean isPut() {
			return valueIsPut;
		}
		
		public int getFieldId() {
			return getId();
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				if (nextGetStep != -1 && 
						(nextChangeStep == -1 || nextGetStep < nextChangeStep)) {
					// assume the next get will tell us the value
					valueStep = nextGetStep;
					valueIsPut = false;
				} else {
					// no value yet
					return new Value.Primitive("--> " + nextChangeStep);
				}
			}
			
			Value v = select()
				.from(Value.ofField(valueIsPut, id, testId, valueStep, step))
				.execute(DatabaseConnector.cnn());
			if (v != null) return v;
			
			System.err.println("TODO: fix this");
			v = select()
				.from(Value.ofField(!valueIsPut, id, testId, valueStep, step))
				.execute(DatabaseConnector.cnn());
			if (v != null) return v;
			
			return null;
		}
		
		@Override
		protected boolean isActiveBeforeValueStep() {
			return !valueIsPut;
		}
	}
	
	public static class ItemValue extends NamedValue {
		
		private boolean valueIsPut;
		
		public boolean isPut() {
			return valueIsPut;
		}
		
		@Override
		public String getName() {
			return String.valueOf(id);
		}
		
		@Override
		protected Value fetchValue() throws Exception {
			if (valueStep == -1) {
				if (nextGetStep != -1 && 
						(nextChangeStep == -1 || nextGetStep < nextChangeStep)) {
					// assume the next get will tell us the value
					valueStep = nextGetStep;
					valueIsPut = false;
				} else {
					// no value yet
					return new Value.Primitive("--> " + nextChangeStep);
				}
			}
			return select().from(Value.ofArray(valueIsPut, id, testId, valueStep, step))
					.execute(DatabaseConnector.cnn());
		}
		
		@Override
		protected boolean isActiveBeforeValueStep() {
			return !valueIsPut;
		}
	}
	
	public static class VariableHistory extends NamedValue {
		public VariableHistory(MiConnection cnn, int testId, long callStep, int varId) {
			super("-", new Value.VariableHistory(cnn, testId, callStep, varId));
		}
	}

	public static class ObjectHistory extends NamedValue {
		public ObjectHistory(MiConnection cnn, int testId, long callStep, int thisId) {
			super("-", new Value.ObjectHistory(cnn, testId, callStep, thisId));
		}
	}
	
	public static class ArrayHistory extends NamedValue {
		public ArrayHistory(MiConnection cnn, int testId, long callStep, int thisId) {
			super("-", new Value.ArrayHistory(cnn, testId, callStep, thisId));
		}
	}
}

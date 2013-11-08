package de.hpi.accidit.eclipse.model.db;

import java.sql.SQLException;

import org.cthul.miro.dsl.QueryView;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.MappedQueryString;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.EntityInitializer;

import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Value.Primitive;

public class ValueDao extends ModelDaoBase {
	
	public static View<MappedQueryString<Value>> this_inInvocation(int testId, long callStep, long step) {
		return new QueryView<>(MAPPING, 
					"SELECT t.`testId`, 'L' AS `primType`, COALESCE(t.`thisId`, 0) AS `valueId`, o.`arrayLength`, y.`name` AS `typeName` " +
					"FROM `CallTrace` t " +
					"LEFT OUTER JOIN `ObjectTrace` o " +
					  "ON t.`testId` = o.`testId` AND t.`thisId` = o.`id`" +
					"LEFT OUTER JOIN `Type` y " +
					  "ON y.`id` = o.`typeId` " +
					"WHERE t.`testId` = ? AND t.`step` = ?", 
					testId, callStep)
			.configure(SET_CONNECTION)
			.configure(new SetStepAdapter(step));
	}
	
	public static View<MappedQueryString<Value>> ofVariable(int varId, int testId, long valueStep, long step) {
		return new ValueQuery("VariableTrace", "variableId", varId, testId, valueStep)
					.configure(new SetStepAdapter(step));
	}

	public static View<MappedQueryString<Value>> ofField(boolean put, int fieldId, int testId, long valueStep, long step) {
		return new ValueQuery(put ? "PutTrace" : "GetTrace", "fieldId", fieldId, testId, valueStep)
					.configure(new SetStepAdapter(step));
	}
	
	public static View<MappedQueryString<Value>> ofArray(boolean put, int index, int testId, long valueStep, long step) {
		return new ValueQuery(put ? "ArrayPutTrace" : "ArrayGetTrace", "index", index, testId, valueStep)
					.configure(new SetStepAdapter(step));
	}

	private static final String[] C_PARAMS = {"testId", "primType", "valueId"};
	
	private static final Mapping<Value> MAPPING = new ReflectiveMapping<Value>(Value.class, ObjectSnapshot.class, null) {
		
		protected String[] getConstructorParameters() {
			return C_PARAMS;
		};
		protected Value newRecord(Object[] args) {
			int testId = (Integer) args[0];
			char primType = ((String) args[1]).charAt(0);
			long valueId = (Long) args[2];
			return Value.newValue(testId, primType, valueId);
		}
		protected void injectField(Value record, String field, Object value) throws SQLException {
			if (record instanceof Primitive) return;
			for (String s: C_PARAMS) {
				if (field.equals(s)) return;
			}
			super.injectField(record, field, value);
		};
	};
	
	protected static class ValueQuery extends QueryView<Value> {

		public ValueQuery(String table, String idField, int id, int testId, long step) {
			super(MAPPING, 
					"SELECT t.`testId`, t.`primType`, t.`valueId`, o.`arrayLength`, y.`name` AS `typeName` " +
					"FROM `" + table + "` t " +
					"LEFT OUTER JOIN `ObjectTrace` o " +
					"ON t.`primType` = 'L' AND t.`testId` = o.`testId` AND t.`valueId` = o.`id` " +
					"LEFT OUTER JOIN `Type` y " +
					"ON y.`id` = o.`typeId` " +
					"WHERE t.`" + idField + "` = ? " +
					  "AND t.`testId` = ? AND t.`step` = ?", 
				id, testId, step);
			configure(SET_CONNECTION);
		}
	}
		
	private static class SetStepAdapter implements EntityInitializer<Value> {
		
		private final long step;
		
		public SetStepAdapter(long step) {
			this.step = step;
		}

		@Override
		public void apply(Value entity) throws SQLException {
			MAPPING.setField(entity, "step", step);
		}

		@Override
		public void complete() throws SQLException { }

		@Override
		public void close() throws SQLException { }		
	}
}

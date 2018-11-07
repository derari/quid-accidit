package de.hpi.accidit.eclipse.model.db;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.db.MiException;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.SelectBuilder;
import org.cthul.miro.sql.SqlBuilder.Code;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;
import org.cthul.miro.sql.syntax.MiSqlParser;
import org.cthul.miro.util.XBiConsumer;

import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;

public class ValueDao extends ModelDaoBase<Value, ValueDao> {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<Value, ?> sql = schema.getMappingBuilder(Value.class);
		ModelDaoBase.init(sql);
		Arrays.asList("testId", "primType", "valueId").forEach(k -> {
			sql.key(k).require(k).readOnly();
		});
		Arrays.asList("step", "arrayLength", "typeName").forEach(a -> {
			sql.require(a).set(writeField(a));
		});
		sql.constructor(args -> {
			int testId = (Integer) args[0];
			char primType = ((String) args[1]).charAt(0);
			long valueId = (Long) args[2];
			return Value.newValue(testId, primType, valueId);				
		});
		sql.attributes("o.`arrayLength`, y.`name` AS `typeName`")
		   .join("LEFT `Type` y ON y.`id` = o.`typeId`")
			// code that joins ObjectTrace as o
		   .selectSnippet("o", (s,a) -> s.include((Code) a[0]))
		   // queries a table as t
		   .selectSnippet("t", (s,a) -> s.from().id((String) a[0]).ql(" t")) 
		   // filters by some attribute of t
		   .selectSnippet("t.f=", (s,a) -> s.where().ql("t.").id((String) a[0]).sql(" = ?", a[1]))
		   ;
	}
	
	public ValueDao(ValueDao source) {
		super(source);
	}

	public ValueDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Value.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "arrayLength", "typeName");
	}
	
	private static final Code<SelectBuilder> VALUE_QUERY = MiSqlParser.parsePartialSelect(
			"SELECT t.`testId`, t.`primType`, t.`valueId` " +
			"LEFT JOIN `ObjectTrace` o ON t.`primType` = 'L' AND t.`testId` = o.`testId` AND t.`valueId` = o.`id`");
	
	protected ValueDao valueQuery(String table, String idField, int id, int testId, long valueStep, long step) {
		return doSafe(me -> me
				.snippet("t", table)
				.snippet("o", VALUE_QUERY)
				.snippet("t.f=", idField, id)
				.where("t.`testId` = ? AND t.`step` = ?", testId, valueStep)
				.setStep(step)
			);
	}
	
	protected ValueDao setStep(long step) {
		return setUp(MappingKey.SET, "step", step);
	}
	
	public ValueDao ofVariable(int varId, int testId, long valueStep, long step) {
		return valueQuery("VariableTrace", "variableId", varId, testId, valueStep, step);
	}
	
	public ValueDao ofField(boolean put, int fieldId, int testId, long valueStep, long step) {
		return valueQuery(put ? "PutTrace" : "GetTrace", "fieldId", fieldId, testId, valueStep, step);
	}

	public ValueDao ofArray(boolean put, int index, int testId, long valueStep, long step) {
		return valueQuery(put ? "ArrayPutTrace" : "ArrayGetTrace", "index", index, testId, valueStep, step);
	}
	
	private static final Code<SelectBuilder> OF_OBJECT_QUERY = MiSqlParser.parsePartialSelect(
			"SELECT o.`testId`, 'L' AS `primType`, o.`id` AS `valueId` " +
			"FROM `ObjectTrace` o");

	public ValueDao ofObject(int testId, long thisId, long step) {
		return doSafe(me -> me
				.snippet("o", OF_OBJECT_QUERY)
				.where("o.`testId` = ? AND o.`id` = ?", testId, thisId)
				.setStep(step)
			);
	}
	
	private static final Code<SelectBuilder> INV_THIS_QUERY = MiSqlParser.parsePartialSelect(
			"SELECT t.`testId`, 'L' AS `primType`, COALESCE(t.`thisId`, 0) AS `valueId`" +
			"FROM `CallTrace` t "+
			"LEFT JOIN `ObjectTrace` o ON t.`testId` = o.`testId` AND t.`thisId` = o.`id`");
	
	public ValueDao this_inInvocation(int testId, long callStep, long step) {
		return doSafe(me -> me
				.snippet("o", INV_THIS_QUERY)
				.where("t.`testId` = ? AND t.`step` = ?", testId, callStep)
				.setStep(step)
			);
	}

	private static final Code<SelectBuilder> INV_RESULT_QUERY = MiSqlParser.parsePartialSelect(
			"SELECT t.`testId`, t.`exitStep` AS `step`, e.`primType` AS `primType`, e.`valueId` AS `valueId`" +
			"FROM `CallTrace` t " +
			"JOIN `ExitTrace` e ON e.`testId` = t.`testId` AND e.`step` = t.`exitStep` " +
			"LEFT JOIN `ObjectTrace` o ON t.`testId` = o.`testId` AND e.`valueId` = o.`id`");
	
	public ValueDao result_ofInvocation(int testId, long callStep) {
		return doSafe(me -> me
				.snippet("o", INV_RESULT_QUERY)
				.where("t.`testId` = ? AND t.`step` = ?", testId, callStep)
				.setUp(MappingKey.LOAD, "step")
			);
	}
	
	private static XBiConsumer<Value, ?, MiException> writeField(String name) {
		try {
			Field f = ObjectSnapshot.class.getDeclaredField(name);
			f.setAccessible(true);
			return (v, o) -> {
				try {
					if (v instanceof ObjectSnapshot) f.set(v, o);
				} catch (ReflectiveOperationException e) {
					throw new MiException(e);
				}
			};
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(e);
		}
	}
}

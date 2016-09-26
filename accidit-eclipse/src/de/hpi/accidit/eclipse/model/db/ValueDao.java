package de.hpi.accidit.eclipse.model.db;

import java.util.Arrays;
import java.util.List;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.db.MiException;
import org.cthul.miro.db.MiResultSet;
import org.cthul.miro.entity.EntityConfiguration;
import org.cthul.miro.entity.EntityInitializer;
import org.cthul.miro.entity.InitializationBuilder;
import org.cthul.miro.entity.map.EntityProperties;
import org.cthul.miro.graph.GraphApi;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlSchema;
import org.cthul.miro.sql.set.MappedSqlType;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;

public class ValueDao extends ModelDaoBase<Value, ValueDao> {
	
	public static void init(MappedSqlSchema schema) {
//		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(ObjectOccurranceDao.class);
//		ModelDaoBase.init(sql);
//		sql.attributes("`thisId`, MIN(`step`) AS `first`, MAX(`step`) AS `last`");
	}
	
	public ValueDao(ValueDao source) {
		super(source);
	}

	public ValueDao(MiConnection cnn) {
		super(cnn, TYPE.getSelectLayer());
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		withConnection(DatabaseConnector.cnn());
		configureWith(TYPE.getAttributeReader(null, Arrays.asList("arrayLength", "typeName")));
//		setUp(MappingKey.FETCH, "testId");
	}
	
	protected ValueDao valueQuery(String table, String idField, int id, int testId, long step) {
		return sql	(sql -> sql
				.select().sql("t.`testId`, t.`primType`, t.`valueId`, o.`arrayLength`, y.`name` AS `typeName`")
				.from().id(table).ql(" t")
				.leftJoin().id("ObjectTrace").sql(" o ON t.`primType` = 'L' AND t.`testId` = o.`testId` AND t.`valueId` = o.`id`")
				.leftJoin().id("Type").sql(" y ON y.`id` = o.`typeId`")
				.where().ql("t.").id(idField).sql(" = ? AND t.`testId` = ? AND t.`step` = ?", id, testId, step))
			.loadObjectAttributes();
	}
	
	protected ValueDao setStep(long step) {
		return initializeWith(injectField(ObjectSnapshot.class, "step", step));
	}
	
	protected ValueDao loadObjectAttributes() {
		return this;
//		return configureWith((EntityConfiguration) ATTRIBUTES.newConfiguration(Arrays.asList("arrayLength", "typeName")));
	}
	
	public ValueDao ofVariable(int varId, int testId, long valueStep, long step) {
		return valueQuery("VariableTrace", "variableId", varId, testId, valueStep).setStep(step);
	}
	
	public ValueDao ofField(boolean put, int fieldId, int testId, long valueStep, long step) {
		return valueQuery(put ? "PutTrace" : "GetTrace", "fieldId", fieldId, testId, valueStep).setStep(step);
	}

	public ValueDao ofArray(boolean put, int index, int testId, long valueStep, long step) {
		return valueQuery(put ? "ArrayPutTrace" : "ArrayGetTrace", "index", index, testId, valueStep).setStep(step);
	}

	public ValueDao ofObject(int testId, long thisId, long step) {
		return sql(sql -> sql
				.select().sql("o.`testId`, 'L' AS `primType`, o.`id` AS `valueId`, o.`arrayLength`, y.`name` AS `typeName`")
				.from().id("ObjectTrace").ql(" o")
				.leftJoin().id("Type").sql(" y ON y.`id` = o.`typeId`")
				.where().sql("o.`testId` = ? AND o.`id` = ?", testId, thisId))
			.setStep(step)
			.loadObjectAttributes();
	}
	
	public ValueDao this_inInvocation(int testId, long callStep, long step) {
		return sql(sql -> sql
			.select()
				.sql("t.`testId`, 'L' AS `primType`, COALESCE(t.`thisId`, 0) AS `valueId`, o.`arrayLength`, y.`name` AS `typeName`")
			.from().id("CallTrace").ql(" t")
			.leftJoin().id("ObjectTrace").sql(" o ON t.`testId` = o.`testId` AND t.`thisId` = o.`id`")
			.leftJoin().id("Type").sql(" y ON y.`id` = o.`typeId`")
			.where().sql("t.`testId` = ? AND t.`step` = ?", testId, callStep))
		.setStep(step)
		.loadObjectAttributes();
	}

	public ValueDao result_ofInvocation(int testId, long callStep) {
		return sql(sql -> sql
			.select()
				.sql("t.`testId`, t.`exitStep` AS `step`, e.`primType` AS `primType`, e.`valueId` AS `valueId`, o.`arrayLength`, y.`name` AS `typeName`")
			.from().id("CallTrace").ql(" t")
			.join().id("ExitTrace").sql("e ON e.`testId` = t.`testId` AND e.`step` = t.`exitStep`")
			.leftJoin().id("ObjectTrace").sql(" o ON t.`testId` = o.`testId` AND e.`valueId` = o.`id` AND e.`primType` = 'L'")
			.leftJoin().id("Type").sql(" y ON y.`id` = o.`typeId`")
			.where().sql("t.`testId` = ? AND t.`step` = ?", testId, callStep))
		.loadObjectAttributes();
	}
	
	
	private static final MappedSqlType<Value> TYPE = new MappedSqlType<Value>(Value.class) {
		{
			ModelDaoBase.init(this);
			Arrays.asList("testId", "primType", "valueId").forEach(k -> {
				key(k).require(k).readOnly();
			});
			constructor(args -> {
				int testId = (Integer) args[0];
				char primType = ((String) args[1]).charAt(0);
				long valueId = (Long) args[2];
				return Value.newValue(testId, primType, valueId);				
			});
		}
		@Override
		protected EntityConfiguration<Value> createAttributeReader(GraphApi graph, List<?> attributes) {
			return (rs, b) -> this.newInitializer(rs, b, attributes);
		}
		protected void newInitializer(MiResultSet rs, InitializationBuilder<? extends Value> builder, List<?> attributes) throws MiException {
			EntityInitializer<ObjectSnapshot> ei = builder.nestedInitializer(b -> ATTRIBUTES.newInitializer(rs, null, flattenStr(attributes), b));
			builder.addName("Init ObjectSnapshot")
				.addInitializer(v -> {
					if (v instanceof ObjectSnapshot) ei.apply((ObjectSnapshot) v);
				}); 
		};
	};
	
	private static final EntityProperties<ObjectSnapshot,?> ATTRIBUTES = EntityProperties
			.build(ObjectSnapshot.class)
			.require("arrayLength").field("arrayLength")
			.require("typeName").field("typeName");
}

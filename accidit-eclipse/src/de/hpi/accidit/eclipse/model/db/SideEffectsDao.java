package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.entity.EntityConfiguration;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.SideEffects.FieldEffect;

public class SideEffectsDao extends ModelDaoBase<FieldEffect, SideEffectsDao> {
	
	protected static void init(MappedSqlSchema schema) {
		ModelDaoBase.init(schema.getMappingBuilder(FieldEffect.class));
	}
	
	private int testId = -1;
	
	protected SideEffectsDao(SideEffectsDao source) {
		super(source);
		this.testId = source.testId;
	}

	public SideEffectsDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(FieldEffect.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		sql(sql -> sql
				.select().sql("sideEffect.`step` AS `valueStep`, sideEffect.`thisId`, f.`id`, f.`name`, nextRead.`step` AS `nextGetStep`")
				.from().id("Field").ql(" f")
				.groupBy().sql("sideEffect.`step`, sideEffect.`thisId`, sideEffect.`fieldId`, nextRead.`step`"));
		setUp(MappingKey.SET, sf -> sf.set("valueIsPut", true));
		configureWith(FIELD_EFFECT_READS);
	}

	public SideEffectsDao inTest(int testId) {
		return doSafe(me -> me.testId = testId)
				.setUp(MappingKey.SET, sf -> sf.set("testId", testId));
	}

	public SideEffectsDao captureBetween(long start, long end) {
		return sql(sql -> sql
				.join().sql("(SELECT MAX(`step`) AS `step`, `thisId`, `fieldId` " +
					"FROM `PutTrace` WHERE `testId` = ? AND `step` BETWEEN ? AND ? " + // testId, capStart, capEnd 
					"GROUP BY `thisId`, `fieldId`) sideEffect ON f.`id` = sideEffect.`fieldId`", testId, start, end));
	}

	public SideEffectsDao targetBetween(long start, long end) {
		return sql(sql -> sql
				.leftJoin().sql(
						"`PutTrace` nextChange ON nextChange.`testId` = ? AND nextChange.`step` > sideEffect.`step` AND nextChange.`step` <= ? " + // testId, tgtEnd
						"AND nextChange.`thisId` = sideEffect.`thisId` AND nextChange.`fieldId` = sideEffect.`fieldId` ", testId, end)
				.join().sql(
						"`GetTrace` nextRead ON nextRead.`testId` = ? AND nextRead.`thisId` = sideEffect.`thisId` AND nextRead.`fieldId` = sideEffect.`fieldId` " + //testId
						"AND nextRead.`step` > sideEffect.`step` AND nextRead.`step` > ? ", start) // tgtStart
				.having().sql("nextRead.`step` < COALESCE(MIN(nextChange.step), ?);", end)); // tgtEnd
	}
	
	private static final EntityConfiguration<FieldEffect> FIELD_EFFECT_READS = (rs, b) -> {
		int iThis = rs.findColumn("thisId");
		int iField = rs.findColumn("id");
		int iStep = rs.findColumn("nextGetStep");
		b.addInitializer(fe -> {
			rs.previous();
			while (rs.next() 
					&& rs.getLong(iThis) == fe.getThisId() 
					&& rs.getInt(iField) == fe.getFieldId() ) {
				FieldValue read = new FieldValue(fe.getTestId(), fe.getFieldId(), rs.getLong(iStep), false, fe.getName());
				fe.addRead(read);
			}
			rs.previous();
		});
	};
	
//	private static final Mapping<FieldEffect> FIELD_EFFECT_MAPPING = new ReflectiveMapping<>(FieldEffect.class);
//	
//	public static final ViewR<FieldSE> FIELDS = Views.build().r(FieldSE.class);
//	
//	public static class FieldSE {
//
//		private int testId = -1;
//		private long capStart = -1;
//		private long capEnd = -1;
//		private long tgtStart = -1;
//		private long tgtEnd = -1;
//		
//		public FieldSE(String[] fields) {
//		}
//		
//		public FieldSE inTest(int testId) {
//			this.testId = testId;
//			return this;
//		}
//		
//		public FieldSE captureBetween(long start, long end) {
//			capStart = start;
//			capEnd = end;
//			return this;
//		}
//		
//		public FieldSE targetBetween(long start, long end) {
//			tgtStart = start;
//			tgtEnd = end;
//			return this;
//		}
//		
//		public Results<FieldEffect> _execute(MiConnection cnn) {
//			return Views.query(FIELD_EFFECT_MAPPING, FIELD_EFFECT_QUERY, 
//					testId, capStart, capEnd,
//					testId, tgtEnd,
//					testId, tgtStart, tgtEnd)
//				.configure(CfgSetField.newInstance("testId", testId))
//				.configure(CfgSetField.newInstance("valueIsPut", true))
//				.configure(FIELD_EFFECT_READS)
//				.select("valueStep", "thisId", "id", "name", "nextGetStep")._execute(cnn);
//		}
//	}
//	
//	private static final String FIELD_EFFECT_QUERY = 

}

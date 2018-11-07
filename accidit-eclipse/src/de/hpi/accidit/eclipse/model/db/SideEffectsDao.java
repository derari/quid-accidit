package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.entity.EntityConfiguration;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.SideEffects.FieldEffect;

public class SideEffectsDao extends ModelDaoBase<FieldEffect, SideEffectsDao> {
	
	protected static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?, ?> sql = schema.getMappingBuilder(FieldEffect.class);
		ModelDaoBase.init(sql);
		sql.attributes("sideEffect.`step` AS `valueStep`, sideEffect.`thisId`, f.`id`, f.`name`, nextRead.`step` AS `nextGetStep`");
		sql.from("`Field` f");
		sql.selectSnippet("sideEffect", (s, a) -> s
			.join("(SELECT MAX(`step`) AS `step`, `thisId`, `fieldId` " +
				"FROM `PutTrace` WHERE `testId` = ? AND `step` BETWEEN ? AND ? " + // testId, capStart, capEnd 
				"GROUP BY `thisId`, `fieldId`) sideEffect ON f.`id` = sideEffect.`fieldId`", a)
			.groupBy("sideEffect.`step`, sideEffect.`thisId`, sideEffect.`fieldId`, nextRead.`step`"));
		
//		return sql(sql -> sql
//				.join().sql("(SELECT MAX(`step`) AS `step`, `thisId`, `fieldId` " +
//					"FROM `PutTrace` WHERE `testId` = ? AND `step` BETWEEN ? AND ? " + // testId, capStart, capEnd 
//					"GROUP BY `thisId`, `fieldId`) sideEffect ON f.`id` = sideEffect.`fieldId`", testId, start, end));
//
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
//		sql(sql -> sql
//				.select().sql("sideEffect.`step` AS `valueStep`, sideEffect.`thisId`, f.`id`, f.`name`, nextRead.`step` AS `nextGetStep`")
//				.from().id("Field").ql(" f")
//				.groupBy().sql("sideEffect.`step`, sideEffect.`thisId`, sideEffect.`fieldId`, nextRead.`step`"));
		setUp(MappingKey.SET, sf -> sf.set("valueIsPut", true));
//	"sideEffect.`step` AS `valueStep`, sideEffect.`thisId`, f.`id`, f.`name`, nextRead.`step` AS `nextGetStep`");
		configureWith(FIELD_EFFECT_READS);
	}

	public SideEffectsDao inTest(int testId) {
		return doSafe(me -> me.testId = testId)
				.setUp(MappingKey.SET, sf -> sf.set("testId", testId));
	}

	public SideEffectsDao captureBetween(long start, long end) {
		if (testId < 0) {
			throw new IllegalStateException("Set testId first");
		}
		return snippet("sideEffect", testId, start, end);
//		return sql(sql -> sql
//				.join().sql("(SELECT MAX(`step`) AS `step`, `thisId`, `fieldId` " +
//					"FROM `PutTrace` WHERE `testId` = ? AND `step` BETWEEN ? AND ? " + // testId, capStart, capEnd 
//					"GROUP BY `thisId`, `fieldId`) sideEffect ON f.`id` = sideEffect.`fieldId`", testId, start, end));
	}

	public SideEffectsDao targetBetween(long start, long end) {
		return sql(sql -> sql
				.leftJoin(
						"`PutTrace` nextChange ON nextChange.`testId` = ? AND nextChange.`step` > sideEffect.`step` AND nextChange.`step` <= ? " + // testId, tgtEnd
						"AND nextChange.`thisId` = sideEffect.`thisId` AND nextChange.`fieldId` = sideEffect.`fieldId` ", testId, end)
				.join(
						"`GetTrace` nextRead ON nextRead.`testId` = ? AND nextRead.`thisId` = sideEffect.`thisId` AND nextRead.`fieldId` = sideEffect.`fieldId` " + //testId
						"AND nextRead.`step` > sideEffect.`step` AND nextRead.`step` > ? ", start) // tgtStart
				.having("nextRead.`step` < COALESCE(MIN(nextChange.step), ?);", end)); // tgtEnd
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
				FieldValue read = new FieldValue(fe.getTestId(), fe.getThisId(), fe.getFieldId(), rs.getLong(iStep), false, fe.getName());
				fe.addRead(read);
			}
			rs.previous();
		});
	};
}

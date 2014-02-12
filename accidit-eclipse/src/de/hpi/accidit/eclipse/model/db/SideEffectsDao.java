package de.hpi.accidit.eclipse.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dml.AbstractMappedSelect;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.Results;
import org.cthul.miro.util.CfgSetField;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.FieldEvent;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.SideEffects.FieldEffect;

public class SideEffectsDao extends ModelDaoBase {
	
	private static final SimpleEntityConfig<FieldEffect> FIELD_EFFECT_READS = new SimpleEntityConfig<FieldEffect>() {
		@Override
		protected void apply(ResultSet rs, FieldEffect fe) throws SQLException {
			int iThis = getFieldIndex(rs, "thisId");
			int iField = getFieldIndex(rs, "fieldId");
			int iStep = getFieldIndex(rs, "read");
			rs.previous();
			while (rs.next() 
					&& rs.getLong(iThis) == fe.getThisId() 
					&& rs.getInt(iField) == fe.getFieldId() ) {
				
				FieldValue read = new FieldValue(fe.getTestId(), fe.getFieldId(), rs.getLong(iStep), false, fe.getName());
				fe.addRead(read);
			}
			rs.previous();
		}
	};
	
	private static final Mapping<FieldEffect> FIELD_EFFECT_MAPPING = new ReflectiveMapping<>(FieldEffect.class);
	
	public static final ViewR<FieldSE> FIELDS = Views.build().r(FieldSE.class);
	
	public static class FieldSE {

		private long testId = -1;
		private long capStart = -1;
		private long capEnd = -1;
		private long tgtStart = -1;
		private long tgtEnd = -1;
		
		public FieldSE(String[] fields) {
		}
		
		public FieldSE inTest(long testId) {
			this.testId = testId;
			return this;
		}
		
		public FieldSE captureBetween(long start, long end) {
			capStart = start;
			capEnd = end;
			return this;
		}
		
		public FieldSE targetBetween(long start, long end) {
			tgtStart = start;
			tgtEnd = end;
			return this;
		}
		
		public Results<FieldEffect> _execute(MiConnection cnn) {
			return Views.query(FIELD_EFFECT_MAPPING, FIELD_EFFECT_QUERY, 
					testId, capStart, capEnd,
					testId, tgtStart, tgtEnd,
					testId, tgtStart, tgtEnd)
				.configure(CfgSetField.newInstance("testId", testId))
				.configure(FIELD_EFFECT_READS)
				.select()._execute(cnn);
		}
	}
	
	private static final String FIELD_EFFECT_QUERY = 
			"SELECT sideEffect.`step` AS `valueStep`, sideEffect.`thisId`, f.`id`, f.`name`, nextRead.`step` AS `read` "+
			"FROM `Field` f " +
			"JOIN (SELECT MAX(`step`) AS `step`, `thisId`, `fieldId` " +
					"FROM `PutTrace` WHERE `testId` = ? AND `step` BETWEEN ? AND ? " + // testId, capStart, capEnd 
					"GROUP BY `thisId`, `fieldId`) sideEffect ON f.`id` = sideEffect.`fieldId`" +
			"LEFT JOIN `PutTrace` nextChange ON nextChange.`testId` = ? AND nextChange.`step` BETWEEN ? AND ? " + // testId, tgtStart, tgtEnd
					"AND nextChange.`thisId` = sideEffect.`thisId` AND nextChange.`fieldId` = sideEffect.`fieldId` " +
			"JOIN `GetTrace` nextRead ON nextRead.`testId` = ? AND nextRead.`thisId` = sideEffect.`thisId` AND nextRead.`fieldId` = sideEffect.`fieldId` " + //testId
			"GROUP BY sideEffect.`step`, sideEffect.`thisId`, sideEffect.`fieldId`, nextRead.`step` "+
			"HAVING nextRead.`step` BETWEEN ? AND COALESCE(MIN(nextChange.step), ?);"; // tgtStart, tgtEnd
}

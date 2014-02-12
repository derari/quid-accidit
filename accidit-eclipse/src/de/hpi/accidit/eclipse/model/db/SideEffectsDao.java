package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.dml.AbstractMappedSelect;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.result.Results;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;

public class SideEffectsDao extends ModelDaoBase {
	
	public static final ViewR<FieldSE> FIELDS = Views.build().r(FieldSE.class);

	public static final MappedTemplateProvider<FieldValue> FIELD_SE_TEMPLATE = new MappedDataQueryTemplateProvider<FieldValue>(NamedValueDao.FIELD_MAPPING) {{
		attributes("m.`name`, m.`id`");
		using("last_and_next")
			.select("lastPut.`step` AS `valueStep`")
			.select("1 AS `valueIsPut`")
			.select("COALESCE(nextPut.`step`, -1) AS `nextChangeStep`")
			.select("COALESCE(nextGet.`step`, -1) AS `nextGetStep`")
			;
		
		table("`Field` m");
		
		join("JOIN " +
				"(SELECT MAX(`step`) AS `step`, `fieldId`, `thisId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `step` >= ? AND `step` <= ? " +
				 "GROUP BY `thisId`, `fieldId`) " +
			 "lastPut ON lastPut.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId`, `thisId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `step` >= ? AND `step` <= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextPut ON nextPut.`fieldId` = m.`id` AND nextPut.`thisId` = lastPut.`thisId`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId`, `thisId` " +
				 "FROM `GetTrace` " +
				 "WHERE `testId` = ? AND `step` >= ? AND `step` <= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextGet ON nextGet.`fieldId` = m.`id` AND nextGet.`thisId` = lastPut.`thisId`");
		
		using("lastPut", "nextPut", "nextGet")
			.where("last_and_next", 
				     "(lastPut.`step` IS NOT NULL " +
				   "AND nextGet.`step` IS NOT NULL " +
				   "AND (nextPut.`step` IS NULL OR nextPut.`step` > nextGet.`step`))");
		always().orderBy("m.`id`");
		always().configure("cfgCnn", SET_CONNECTION);
	}};
	
	
	public static class FieldSE extends AbstractMappedSelect<FieldValue, Results<FieldValue>, FieldSE> {

		private long testId = -1;
		
		public FieldSE(String[] fields) {
			super(FIELD_SE_TEMPLATE, Results.<FieldValue>getBuilder(), fields);
		}
		
		public FieldSE inTest(long testId) {
			this.testId = testId;
			return this;
		}
		
		public FieldSE captureBetween(long start, long end) {
			put("lastPut", testId, start, end);
			return this;
		}
		
		public FieldSE targetBetween(long start, long end) {
			put("nextPut", testId, start, end);
			put("nextGet", testId, start, end);
			return this;
		}
	}
}

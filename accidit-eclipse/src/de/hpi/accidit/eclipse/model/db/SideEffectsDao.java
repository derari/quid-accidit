package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.View;
import org.cthul.miro.graph.GraphQuery;
import org.cthul.miro.graph.GraphQueryTemplate;
import org.cthul.miro.graph.SelectByKey;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;

public class SideEffectsDao extends ModelDaoBase {

	public static final GraphQueryTemplate<FieldValue> FIELD_SE_TEMPLATE = new GraphQueryTemplate<FieldValue>(NamedValueDao.FIELD_MAPPING) {{
		select("m.`name`, m.`id`");
		using("last_and_next")
			.select("lastPut.`step` AS `valueStep`")
			.select("1 AS `valueIsPut`")
			.select("COALESCE(nextPut.`step`, -1) AS `nextChangeStep`")
			.select("COALESCE(nextGet.`step`, -1) AS `nextGetStep`")
//			.select("COALESCE(lastGet.`step`, -1) AS `lastGetStep`")
			;
		
		from("`Field` m");
		
		join("JOIN " +
				"(SELECT MAX(`step`) AS `step`, `fieldId`, `thisId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `step` < ? " +
				 "GROUP BY `fieldId`) " +
			 "lastPut ON lastPut.`fieldId` = m.`id`");
//		join("LEFT OUTER JOIN " +
//				"(SELECT MAX(`step`) AS `step`, `fieldId` " +
//				 "FROM `GetTrace` " +
//				 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
//				 "GROUP BY `fieldId`) " +
//			 "lastGet ON lastGet.`fieldId` = m.`id`");
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
//				   "OR lastGet.`step` IS NOT NULL " +
				   "AND nextGet.`step` IS NOT NULL)");
		always().orderBy("m.`id`");
		always().configure("cfgCnn", SET_CONNECTION);
		
	}};
	
	
	public static class FieldSE extends GraphQuery<FieldValue> {

		private long testId = -1;
		
		public FieldSE(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
			super(cnn, NamedValueDao.FIELD_MAPPING, FIELD_SE_TEMPLATE, view);
			select(fields);
		}
		
		public FieldSE inTest(long testId) {
			this.testId = testId;
			return this;
		}
		
		public FieldSE captureBetween(long start, long end) {
			put("lastPut");
			return this;
		}
		
	}
	
}

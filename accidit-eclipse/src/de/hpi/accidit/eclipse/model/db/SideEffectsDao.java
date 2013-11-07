package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.View;
import org.cthul.miro.graph.GraphQuery;
import org.cthul.miro.graph.GraphQueryTemplate;
import org.cthul.miro.graph.SelectByKey;
import org.cthul.miro.map.MappedStatement;
import org.cthul.miro.map.Mapping;

import sun.misc.JavaLangAccess;

import de.hpi.accidit.eclipse.model.FieldEvent;

public class SideEffectsDao extends ModelDaoBase {

	public static final GraphQueryTemplate<FieldEvent> T_FIELD = new GraphQueryTemplate<FieldEvent>(FieldEventDao.MAPPING) {{
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
				"(SELECT MAX(`step`) AS `step`, `fieldId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
				 "GROUP BY `fieldId`) " +
			 "lastPut ON lastPut.`fieldId` = m.`id`");
//		join("LEFT OUTER JOIN " +
//				"(SELECT MAX(`step`) AS `step`, `fieldId` " +
//				 "FROM `GetTrace` " +
//				 "WHERE `testId` = ? AND `thisId` = ? AND `step` < ? " +
//				 "GROUP BY `fieldId`) " +
//			 "lastGet ON lastGet.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId` " +
				 "FROM `PutTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? AND `step` <= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextPut ON nextPut.`fieldId` = m.`id`");
		join("LEFT OUTER JOIN " +
				"(SELECT MIN(`step`) AS `step`, `fieldId` " +
				 "FROM `GetTrace` " +
				 "WHERE `testId` = ? AND `thisId` = ? AND `step` >= ? AND `step` <= ? " +
				 "GROUP BY `fieldId`) " +
			 "nextGet ON nextGet.`fieldId` = m.`id`");
		
		using("lastPut", "lastGet", "nextPut", "nextGet")
			.where("last_and_next", 
				     "(lastPut.`step` IS NOT NULL " +
//				   "OR lastGet.`step` IS NOT NULL " +
				   "AND nextGet.`step` IS NOT NULL)");
		always().orderBy("m.`id`");
		always().configure("cfgCnn", SET_CONNECTION);
		
	}};
	
	
//	public static class FieldSE extends GraphQuery<FieldEvent> {
//
//		public FieldSE(MiConnection cnn, String[] fields, View<? extends SelectByKey<?>> view) {
//			super(cnn, FieldEventDao.MAPPING, OBJ_HISTORY_TEMPLATE, view);
//			select(fields);
//		}
//
//		
//		
//		
//	}
	
}

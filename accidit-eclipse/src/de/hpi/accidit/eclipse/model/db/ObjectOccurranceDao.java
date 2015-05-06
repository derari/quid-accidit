package de.hpi.accidit.eclipse.model.db;

import java.sql.ResultSet;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.MiConnection;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.Results;
import org.cthul.miro.util.CfgSetField;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;
import de.hpi.accidit.eclipse.model.ObjectOccurrance;
import de.hpi.accidit.eclipse.model.SideEffects.FieldEffect;
import de.hpi.accidit.eclipse.model.SideEffects.InstanceEffects;

public class ObjectOccurranceDao extends ModelDaoBase {
	
	private static final Mapping<ObjectOccurrance> MAPPING = new ReflectiveMapping<>(ObjectOccurrance.class);
	
	public static final ViewR<ObjectsQuery> OBJECTS = Views.build().r(ObjectsQuery.class);
	
	public static class ObjectsQuery {

		private int testId = -1;
		private long capEnd = Long.MAX_VALUE;
		private long tgtStart = 0;
		private List<InstanceEffects> instances = new ArrayList<>();
		
		public ObjectsQuery(String[] fields) {
		}
		
		public ObjectsQuery inTest(int testId) {
			this.testId = testId;
			return this;
		}
		
		public ObjectsQuery firstBefore(long step) {
			this.capEnd = step;
			return this;
		}
		
		public ObjectsQuery lastAfter(long step) {
			this.tgtStart = step;
			return this;
		}
		
		public Results<ObjectOccurrance> _execute(MiConnection cnn) {
//			StringBuilder idParams = new StringBuilder(instances.size()*3);
//			for (int i = 0; i < instances.size(); i++) {
//				idParams.append("?, ");
//			}
//			idParams.setLength(idParams.length()-2);
			String query = OBJECTS_QUERY;//.replace("??", idParams);
			
//			List<Object> arguments = new ArrayList<>(instances.size()*2+2);
//			arguments.add(testId);
//			arguments.add(step);
//			for (InstanceEffects ie: instances) {
//				arguments.add(ie.getThisId());
//			}
//			arguments.add(testId);
//			arguments.add(step);
//			for (InstanceEffects ie: instances) {
//				arguments.add(ie.getThisId());
//			}
			
			return Views.query(MAPPING, query,
					testId, capEnd, tgtStart, 
					testId, capEnd, tgtStart)
				.select()._execute(cnn);
		}
	}
	
	private static final String OBJECTS_QUERY = 
			"SELECT `thisId`, MIN(`step`) AS `first`, MAX(`step`) AS `last` "+
			"FROM (" +
				"SELECT `thisId`, `step` " +
				"FROM `PutTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " + // AND `thisId` IN (??) testId, capEnd, thisIds
			"UNION " +
				"SELECT `thisId`, `step` " +
				"FROM `CallTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " + // AND `thisId` IN (??) testId, capEnd, thisIds
			") o " +
			"GROUP BY `thisId`";
}

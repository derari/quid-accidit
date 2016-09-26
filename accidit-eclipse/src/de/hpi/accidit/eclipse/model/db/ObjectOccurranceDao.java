package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.graph.TypeBuilder;
import org.cthul.miro.result.Results.Action;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.ObjectOccurrance;

public class ObjectOccurranceDao extends ModelDaoBase<ObjectOccurrance, ObjectOccurranceDao> {
	
	protected static void init(MappedSqlSchema schema) {
//		ModelDaoBase.init(schema.getMappingBuilder(ObjectOccurranceDao.class));
	}
	
	private int testId = -1;
	private long capEnd = Long.MAX_VALUE;
	private long tgtStart = 0;
	
	protected ObjectOccurranceDao(ObjectOccurranceDao source) {
		super(source);
		this.testId = source.testId;
		this.capEnd = source.capEnd;
		this.tgtStart = source.tgtStart;
	}

	public ObjectOccurranceDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(ObjectOccurrance.class));
	}

	@Override
	protected void initialize() {
		super.initialize();
		sql(sql -> sql.groupBy().sql("`thisId`"));
	}
	
	private ObjectOccurranceDao finish() {
		return sql(sql -> 
				sql.from().sql(objectsTable(), 
						testId, capEnd, tgtStart, 
						testId, capEnd, tgtStart));
	}

	@Override
	public Action<ObjectOccurrance> result() {
		return finish().superResult();
	}
	
	private Action<ObjectOccurrance> superResult() {
		return super.result();
	}
	
	public ObjectOccurranceDao inTest(int testId) {
		return doSafe(me -> me.testId = testId);
	}
	
	public ObjectOccurranceDao firstBefore(long step) {
		return doSafe(me -> me.capEnd = step);
	}
	
	public ObjectOccurranceDao lastAfter(long step) {
		return doSafe(me -> me.tgtStart = step);
	}
	
//	public static class ObjectsQuery {
//
//		private int testId = -1;
//		private long capEnd = Long.MAX_VALUE;
//		private long tgtStart = 0;
//		private List<InstanceEffects> instances = new ArrayList<>();
//		
//		public ObjectsQuery(String[] fields) {
//		}
//		
//
//		
//		public Results<ObjectOccurrance> _execute(MiConnection cnn) {
////			StringBuilder idParams = new StringBuilder(instances.size()*3);
////			for (int i = 0; i < instances.size(); i++) {
////				idParams.append("?, ");
////			}
////			idParams.setLength(idParams.length()-2);
//			String query = OBJECTS_QUERY;//.replace("??", idParams);
//			
////			List<Object> arguments = new ArrayList<>(instances.size()*2+2);
////			arguments.add(testId);
////			arguments.add(step);
////			for (InstanceEffects ie: instances) {
////				arguments.add(ie.getThisId());
////			}
////			arguments.add(testId);
////			arguments.add(step);
////			for (InstanceEffects ie: instances) {
////				arguments.add(ie.getThisId());
////			}
//			
//			return Views.query(MAPPING, query,
//					testId, capEnd, tgtStart, 
//					testId, capEnd, tgtStart)
//				.select()._execute(cnn);
//		}
//	}
	
	private static String objectsTable() { 
		return 
			"(SELECT `thisId`, `step` " +
				"FROM `PutTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " + // AND `thisId` IN (??) testId, capEnd, thisIds
			"UNION " +
				"SELECT `thisId`, `step` " +
				"FROM `CallTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " + // AND `thisId` IN (??) testId, capEnd, thisIds
			") o ";
	}
}

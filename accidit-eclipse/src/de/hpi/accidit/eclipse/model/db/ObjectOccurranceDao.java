package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
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
		sql(sql -> sql.groupBy("`thisId`"));
	}
	
	protected ObjectOccurranceDao finish() {
		return sql(sql -> 
				sql.from(objectsTable(), 
						testId, capEnd, tgtStart, 
						testId, capEnd, tgtStart));
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
	
	private static String objectsTable() { 
		return 
			"(SELECT `thisId`, `step` " +
				"FROM `PutTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " +
			"UNION " +
				"SELECT `thisId`, `step` " +
				"FROM `CallTrace` " +
				"WHERE `testId` = ? AND (`step` < ? OR `step` > ?) " +
			") o ";
	}
}

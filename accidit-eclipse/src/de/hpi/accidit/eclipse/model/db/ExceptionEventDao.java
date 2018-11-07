package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.ExceptionEvent;

public class ExceptionEventDao extends TraceElementDaoBase<ExceptionEvent, ExceptionEventDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sqlThrow = schema.newMapping("EEThrow", ExceptionEvent.class);
		TraceElementDaoBase.init(sqlThrow);
		sqlThrow.sql("SELECT e.`callStep` FROM `ThrowTrace` e");
		
		MappedSqlBuilder<?,?> sqlCatch = schema.newMapping("EECatch", ExceptionEvent.class);
		TraceElementDaoBase.init(sqlCatch);
		sqlCatch.sql("SELECT e.`callStep` FROM `CatchTrace` e");
	}

	private final boolean throwTrace;
	
	protected ExceptionEventDao(ExceptionEventDao source) {
		super(source);
		this.throwTrace = source.throwTrace;
	}

	public ExceptionEventDao(MiConnection cnn, MappedSqlSchema schema, boolean throwTrace) {
		super(cnn, schema.getSelectLayer(throwTrace ? "EEThrow" : "EECatch"));
		this.throwTrace = throwTrace;
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		initializeWith(ee -> ee.isThrow = throwTrace);
	}
	
	public ExceptionEventDao lastBefore(int testId, long step) {
		return doSafe(me -> me
				.setUp(MappingKey.PROPERTY_FILTER, "testId", testId)
				.sql("WHERE e.`step` < ? ORDER BY e.`step` DESC", step));
	}
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.ExceptionEvent;

public class ExceptionEventDao extends TraceElementDaoBase<ExceptionEvent, ExceptionEventDao> {
	
	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(ExceptionEvent.class);
		TraceElementDaoBase.init(sql);
		sql.attribute("e.`callStep`");
		sql.selectSnippet("e", s -> {});
	}

	private final boolean throwTrace;
	
	protected ExceptionEventDao(ExceptionEventDao source) {
		super(source);
		this.throwTrace = source.throwTrace;
	}

	public ExceptionEventDao(MiConnection cnn, MappedSqlSchema schema, boolean throwTrace) {
		super(cnn, schema.getSelectLayer(ExceptionEvent.class));
		this.throwTrace = throwTrace;
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		String table = throwTrace ? "ThrowTrace" : "CatchTrace";
		sql(sql -> sql.from().id(table).ql(" e"));
		setUp(MappingKey.SET, sf -> sf.set("isThrow", throwTrace));
	}	
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.FieldEvent;

public class FieldEventDao extends TraceElementDaoBase<FieldEvent, FieldEventDao> {

	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(FieldEvent.class);
		TraceElementDaoBase.init(sql);
		sql.attribute("e.`callStep`");
		sql.from("`PutTrace` e");
	}
	
	protected FieldEventDao(TraceElementDaoBase<FieldEvent, FieldEventDao> source) {
		super(source);
	}

	public FieldEventDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(FieldEvent.class));
	}
}

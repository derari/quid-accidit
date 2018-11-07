package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.sql.set.MappedSqlBuilder;
import org.cthul.miro.sql.set.MappedSqlSchema;

import de.hpi.accidit.eclipse.model.VariableEvent;

public class VariableEventDao extends TraceElementDaoBase<VariableEvent, VariableEventDao> {

	public static void init(MappedSqlSchema schema) {
		MappedSqlBuilder<?,?> sql = schema.getMappingBuilder(VariableEvent.class);
		TraceElementDaoBase.init(sql);
		sql.sql("SELECT e.`callStep` FROM `VariableTrace` e");
	}
	
	protected VariableEventDao(ModelDaoBase<VariableEvent, VariableEventDao> source) {
		super(source);
	}

	public VariableEventDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(VariableEvent.class));
	}
}

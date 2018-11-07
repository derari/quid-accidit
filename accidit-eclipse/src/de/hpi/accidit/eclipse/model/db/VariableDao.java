package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.request.template.TemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlSchema;
import org.cthul.miro.sql.set.SqlEntitySet;
import org.cthul.miro.sql.template.SqlTemplatesBuilder;

import de.hpi.accidit.eclipse.model.Method;
import de.hpi.accidit.eclipse.model.Variable;

public class VariableDao extends SqlEntitySet<Variable, VariableDao> {

	public static void init(MappedSqlSchema schema) {
		SqlTemplatesBuilder<?> sql = schema.getMappingBuilder(Variable.class);
		sql.attributes("v.`id`, v.`name`")
			.from("`Variable` v");
	}
	
	public VariableDao(VariableDao source) {
		super(source);
	}

	
	public VariableDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Variable.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "id", "name");
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<Variable, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		snippetLayer.once("Method", qry -> qry.getStatement().join().id("Method").sql(" m ON v.`methodId` = m.`id`"));
		snippetLayer.once("CallTrace", qry -> qry.getStatement().join().id("CallTrace").sql(" c ON m.`id` = c.`methodId`"));
	}
	
	public VariableDao inCall(int testId, long callStep) {
		return doSafe(me -> me
				.build("Method")
				.build("CallTrace")
				.sql(sql -> sql.where().sql("c.`testId` = ? AND c.`step` = ?", testId, callStep)));
	}
	
	public VariableDao orderById() {
		return sql(sql -> sql.orderBy().sql("v.`id`"));
	}
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlSchema;
import org.cthul.miro.sql.set.SqlEntitySet;
import org.cthul.miro.sql.template.SqlTemplatesBuilder;

import de.hpi.accidit.eclipse.model.Type;

public class TypeDao extends SqlEntitySet<Type, TypeDao> {

	public static void init(MappedSqlSchema schema) {
		SqlTemplatesBuilder<?> sql = schema.getMappingBuilder(Type.class);
		sql.attributes("t.`name`")
			.from("`Type` t");
	}
	
	protected TypeDao(TypeDao source) {
		super(source);
	}

	public TypeDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Type.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "name");
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<Type, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		snippetLayer.once("ObjectTrace", qry -> qry.getStatement()
				.join().id("ObjectTrace").ql(" o")
					.on().sql("t.`id` = o.`typeId`"));
		snippetLayer.setUp("ofObject", (qry, a) -> qry.getStatement()
				.where().sql("o.`testId` = ? AND o.`thisId`= ?", a));
	}
	
	public TypeDao ofObject(int testId, long thisId) {
		return doSafe(me -> me
				.snippet("ObjectTrace")
				.snippet("ofObject", testId, thisId));
	}
}

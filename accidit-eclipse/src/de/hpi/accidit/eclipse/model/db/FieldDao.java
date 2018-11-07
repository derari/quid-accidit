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

import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.NamedValue.FieldValue;

public class FieldDao extends SqlEntitySet<Field, FieldDao> {
	
	public static void init(MappedSqlSchema schema) {
		SqlTemplatesBuilder<?> sql = schema.getMappingBuilder(Field.class);
		sql.attributes("f.`name`, f.`id`")
			.from("`Field` f");
	}
	
	protected FieldDao(FieldDao source) {
		super(source);
	}

	public FieldDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Field.class));
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "name", "id");
		sql(sql -> sql
				.groupBy().sql("f.`id`, f.`name`")
				.orderBy().sql("f.`name`"));
//		compose(c -> c.require(MappingKey.MKey.LOAD_ALL));
	}
	
	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<Field, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
		snippetLayer.setUp("evt", (qry, a) -> qry.getStatement().join().sql(
				"(SELECT `fieldId` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId` " +
				" UNION " +
				" SELECT `fieldId` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId`) evt", a)
			.on().sql("f.`id` = evt.`fieldId`"));
	}
	
	public FieldDao ofType(long tId) {
		return sql(sql -> sql.where().sql("t.`id` = ?", tId));
	}
	
	public FieldDao ofObject(int testId, long thisId) {
		return build("evt", testId, thisId, testId, thisId);
	}
	
	public FieldDao orderById() {
		return sql(sql -> sql.orderBy().sql("f.`id`"));
	}
}

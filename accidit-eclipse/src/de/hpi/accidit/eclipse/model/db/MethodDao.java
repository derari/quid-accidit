package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.template.TemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlSchema;
import org.cthul.miro.sql.set.SqlEntitySet;
import org.cthul.miro.sql.template.SqlTemplatesBuilder;

import de.hpi.accidit.eclipse.model.Field;
import de.hpi.accidit.eclipse.model.Method;

public class MethodDao extends SqlEntitySet<Method, MethodDao> {
	
	public static void init(MappedSqlSchema schema) {
		SqlTemplatesBuilder<?> sql = schema.getMappingBuilder(Method.class);
		sql.attributes("m.`id`, m.`name`, m.`signature`, t.`name` AS `type`")
			.from("`Method` m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
	}
	
	public MethodDao(MethodDao source) {
		super(source);
	}

	
	public MethodDao(MiConnection cnn, MappedSqlSchema schema) {
		super(cnn, schema.getSelectLayer(Method.class));
	}

	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "id", "name", "signature", "type");
	}
	
	public MethodDao byId(long id) {
		if (id < 0)  {
			throw new IllegalArgumentException(String.valueOf(id));
		}
		return setUp(MappingKey.PROPERTY_FILTER, "id", id);
	}
	
//		@Require("t")
//		@Where("t.`name` = ? AND m.`name` = ? AND m.`signature` = ?")
//		Query declaredAs(String type, String name, String signature);
	
}

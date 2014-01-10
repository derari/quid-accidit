package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Put;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.QueryWithResult;
import org.cthul.miro.result.Results;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.Field;

public class FieldDao {
	
	public static final Mapping<Field> MAPPING = new ReflectiveMapping<>(Field.class);
	
	private static final MappedTemplateProvider<Field> TEMPLATE = new MappedDataQueryTemplateProvider<Field>(MAPPING) {{
		attributes("f.`id`, f.`name`");
		table("`Field` f");
//		join("`Type` t ON f.`declaringTypeId` = t.`id`");
		join("evt", "JOIN " +
				"(SELECT `fieldId` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId` " +
				" UNION " +
				" SELECT `fieldId` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId`) " +
			"evt ON f.`id` = evt.`fieldId`");
		always().groupBy("f.`id`, f.`name`");
		always().orderBy("f.`name`");
	}};
	
	public static final ViewR<Query> VIEW = Views.build(TEMPLATE).r(Query.class).build();
	
	public static interface Query extends QueryWithResult<Results<Field>> {
		
//		@Require("t")
//		@Where("t.`id` = ?")
//		Query ofType(long tId);
		
		@Put(value="evt", mapArgs={0, 1, 0, 1})
		Query ofObject(long testId, long thisId);
		
		@OrderBy("f.`id`")
		Query orderById();
	}
	
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedMappedStatement;
import org.cthul.miro.at.AnnotatedQueryTemplate;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Put;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.Field;

public class FieldDao {
	
	public static final Mapping<Field> MAPPING = new ReflectiveMapping<>(Field.class);
	
	private static final AnnotatedQueryTemplate<Field> TEMPLATE = new AnnotatedQueryTemplate<Field>() {{
		select("f.`id`, f.`name`");
		from("`Field` f");
//		join("`Type` t ON f.`declaringTypeId` = t.`id`");
		join("evt", "JOIN " +
				"(SELECT `fieldId` FROM `PutTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId` " +
				" UNION " +
				" SELECT `fieldId` FROM `GetTrace` WHERE `testId` = ? AND `thisId` = ? GROUP BY `fieldId`) " +
			"evt ON f.`id` = evt.`fieldId`");
		always().groupBy("f.`id`", "f.`name`");
		always().orderBy("f.`name`");
	}};
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING, TEMPLATE);
	
	public static interface Query extends AnnotatedMappedStatement<Field> {
		
//		@Require("t")
//		@Where("t.`id` = ?")
//		Query ofType(long tId);
		
		@Put(value="evt", mapArgs={0, 1, 0, 1})
		Query ofObject(long testId, long thisId);
		
		@OrderBy("f.`id`")
		Query orderById();
	}
	
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedMappedStatement;
import org.cthul.miro.at.AnnotatedQueryTemplate;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Require;
import org.cthul.miro.at.Where;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.Field;

public class FieldDao {
	
	private static final Mapping<Field> MAPPING = new ReflectiveMapping<>(Field.class);
	
	private static final AnnotatedQueryTemplate<Field> TEMPLATE = new AnnotatedQueryTemplate<Field>() {{
		select("f.`id`, f.`name`");
		from("`Field` f");
		join("`Type` t ON f.`declaringTypeId` = t.`id`");
		using("t")
			.join("`ObjectTrace` o ON t.`id` = o.`typeId`");
	}};
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING, TEMPLATE);
	
	public static interface Query extends AnnotatedMappedStatement<Field> {
		
		@Require("t")
		@Where("t.`id` = ?")
		Query ofType(long tId);
		
		@Require("o")
		@Where("o.`testId` = ? AND o.`id` = ?")
		Query ofObject(long testId, long thisId);
		
		@OrderBy("f.`id`")
		Query orderById();
	}
	
}

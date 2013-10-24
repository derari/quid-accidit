package de.hpi.accidit.eclipse.model;

import org.cthul.miro.at.*;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

public class Field implements NamedEntity {

	private int id;
	private String name;
	
	public Field() {
	}
	
	public Field(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
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
		Query ofType(long mId);
		
		@Require("o")
		@Where("o.`testId` = ? AND o.`id` = ?")
		Query ofObject(long testId, long thisId);
		
		@OrderBy("f.`id`")
		Query orderById();
	}
	
}

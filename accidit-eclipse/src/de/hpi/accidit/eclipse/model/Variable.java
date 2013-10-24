package de.hpi.accidit.eclipse.model;

import org.cthul.miro.at.*;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

public class Variable implements NamedEntity {

	private int id;
	private String name;
	
	public Variable() {
	}
	
	public Variable(int id, String name) {
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
	
	private static final Mapping<Variable> MAPPING = new ReflectiveMapping<>(Variable.class);
	
	private static final AnnotatedQueryTemplate<Variable> TEMPLATE = new AnnotatedQueryTemplate<Variable>() {{
		select("v.`id`, v.`name`");
		from("`Variable` v");
		join("`Method` m ON v.`methodId` = m.`id`");
		using("m")
			.join("`CallTrace` c ON m.`id` = c.`methodId`");
	}};
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING, TEMPLATE);
	
	public static interface Query extends AnnotatedMappedStatement<Variable> {
		
		@Require("m")
		@Where("m.`id` = ?")
		Query inMethod(long mId);
		
		@Require("c")
		@Where("c.`testId` = ? AND c.`step` = ?")
		Query inCall(long testId, long callStep);
		
		@OrderBy("v.`id`")
		Query orderById();
	}
	
}

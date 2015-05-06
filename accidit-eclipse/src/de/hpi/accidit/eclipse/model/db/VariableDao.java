package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.OrderBy;
import org.cthul.miro.at.Require;
import org.cthul.miro.at.Where;
import org.cthul.miro.dml.MappedDataQueryTemplateProvider;
import org.cthul.miro.map.MappedTemplateProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.QueryWithResult;
import org.cthul.miro.result.Results;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.Variable;

public class VariableDao {

private static final Mapping<Variable> MAPPING = new ReflectiveMapping<>(Variable.class);
	
	private static final MappedTemplateProvider<Variable> TEMPLATE = new MappedDataQueryTemplateProvider<Variable>(MAPPING) {{
		attributes("v.`id`, v.`name`");
		table("`Variable` v");
		join("`Method` m ON v.`methodId` = m.`id`");
		using("m")
			.join("`CallTrace` c ON m.`id` = c.`methodId`");
	}};
	
	public static final ViewR<Query> VIEW = Views.build(TEMPLATE).r(Query.class).build();
	
	public static interface Query extends QueryWithResult<Results<Variable>> {
		
		@Require("m")
		@Where("m.`id` = ?")
		Query inMethod(long mId);
		
		@Require("c")
		@Where("c.`testId` = ? AND c.`step` = ?")
		Query inCall(long testId, long callStep);
		
		@OrderBy("id")
		Query orderById();
	}
}

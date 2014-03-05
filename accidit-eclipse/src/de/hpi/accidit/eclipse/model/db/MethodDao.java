package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.From;
import org.cthul.miro.at.Join;
import org.cthul.miro.at.Require;
import org.cthul.miro.at.Select;
import org.cthul.miro.at.Where;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.QueryWithResult;
import org.cthul.miro.result.Results;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.Method;

public class MethodDao {
	
	public static final Mapping<Method> MAPPING = new ReflectiveMapping<>(Method.class);

	public static final ViewR<Query> VIEW = Views.build(MAPPING).r(Query.class);
	
	@Select("m.`id`, m.`name`, m.`signature`, t.`name` AS `type`")
	@From("`Method` m")
	@Join("`Type` t ON m.`declaringTypeId` = t.`id`")
	public static interface Query extends QueryWithResult<Results<Method>> {
		
		@Require("t")
		@Where("t.`name` = ? AND m.`name` = ? AND m.`signature` = ?")
		Query declaredAs(String type, String name, String signature);
	}
	
}

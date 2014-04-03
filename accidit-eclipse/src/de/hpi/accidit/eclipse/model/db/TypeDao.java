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

import de.hpi.accidit.eclipse.model.Type;

public class TypeDao {

	private static final Mapping<Type> MAPPING = new ReflectiveMapping<>(Type.class);
	
	public static final ViewR<Query> VIEW = Views.build(MAPPING).r(Query.class);
	 
	@Select("t.`name`")
	@From("`Type` t")
	@Join("`ObjectTrace` o ON t.`id` = o.`typeId`")
	public static interface Query extends QueryWithResult<Results<Type>> {
		
		@Require("o")
		@Where("o.`testId` = ? AND o.`thisId`= ?")
		public Query ofObject(long testId, long thisId);
	}
}

package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedMappedStatement;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.From;
import org.cthul.miro.at.Join;
import org.cthul.miro.at.Require;
import org.cthul.miro.at.Select;
import org.cthul.miro.at.Where;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.Type;

public class TypeDao {

	private static final Mapping<Type> MAPPING = new ReflectiveMapping<>(Type.class);
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING);
	 
	@Select("t.`name`")
	@From("`Type` t")
	@Join("`ObjectTrace` o ON t.`id` = o.`typeId`")
	public static interface Query extends AnnotatedMappedStatement<Type> {
		
		@Require("o")
		@Where("o.`testId` = ? AND o.`thisId`= ?")
		public Query ofObject(long testId, long thisId);
	}
}

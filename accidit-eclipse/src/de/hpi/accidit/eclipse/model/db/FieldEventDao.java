package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.From;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.FieldEvent;

public class FieldEventDao extends TraceElementDaoBase {

	public static final Mapping<FieldEvent> MAPPING = new ReflectiveMapping<>(FieldEvent.class);
	
	public static final View<PutQuery> PUT = new AnnotatedView<>(PutQuery.class, MAPPING);
	
	@From("`PutTrace` e")
	public static interface PutQuery extends Query<FieldEvent, PutQuery> {
		
	}
}

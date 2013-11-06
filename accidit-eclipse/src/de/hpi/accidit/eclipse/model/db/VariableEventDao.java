package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.From;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

import de.hpi.accidit.eclipse.model.VariableEvent;

public class VariableEventDao extends TraceElementDaoBase {

private static final Mapping<VariableEvent> MAPPING = new ReflectiveMapping<>(VariableEvent.class);
	
	public static final View<PutQuery> PUT = new AnnotatedView<>(PutQuery.class, MAPPING);
	
	@From("`VariableTrace` e")
	public static interface PutQuery extends Query<VariableEvent, PutQuery> {
		
	}
}

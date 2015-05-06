package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.From;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.VariableEvent;

public class VariableEventDao extends TraceElementDaoBase {

private static final Mapping<VariableEvent> MAPPING = new ReflectiveMapping<>(VariableEvent.class);
	
	public static final ViewR<PutQuery> PUT = Views.build(MAPPING).r(PutQuery.class);
	
	@From("`VariableTrace` e")
	public static interface PutQuery extends Query<VariableEvent, PutQuery> {
		
	}
}

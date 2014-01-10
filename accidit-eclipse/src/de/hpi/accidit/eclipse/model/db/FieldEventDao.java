package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.From;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;

import de.hpi.accidit.eclipse.model.FieldEvent;

public class FieldEventDao extends TraceElementDaoBase {

	public static final Mapping<FieldEvent> MAPPING = new ReflectiveMapping<>(FieldEvent.class);
	
	public static final ViewR<PutQuery> PUT = Views.build(MAPPING).r(PutQuery.class).build();
	
	@From("`PutTrace` e")
	public static interface PutQuery extends Query<FieldEvent, PutQuery> {
		
	}
}

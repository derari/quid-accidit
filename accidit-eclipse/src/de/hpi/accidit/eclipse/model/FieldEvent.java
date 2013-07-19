package de.hpi.accidit.eclipse.model;

import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.MappedQuery;
import org.cthul.miro.util.QueryView;
import org.cthul.miro.util.ReflectiveMapping;


public class FieldEvent extends TraceElement {

	private static final Mapping<FieldEvent> MAPPING = new ReflectiveMapping<>(FieldEvent.class);
	
	public static View<MappedQuery<FieldEvent>> put_inInvocation(int testId, long callStep) {
		return new QueryView<>(MAPPING, 
				"SELECT `line`, `step` " +
				"FROM `PutTrace` " +
				"WHERE `testId` = ? AND `callStep` = ? " +
				"ORDER BY `step`", 
				testId, callStep);
	}
	
}

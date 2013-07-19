package de.hpi.accidit.eclipse.model;

import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.MappedQuery;
import org.cthul.miro.util.QueryView;
import org.cthul.miro.util.ReflectiveMapping;

public class VariableEvent extends TraceElement {

	private static final Mapping<VariableEvent> MAPPING = new ReflectiveMapping<>(VariableEvent.class);
	
	public static View<MappedQuery<VariableEvent>> inInvocation(int testId, long callStep) {
		return new QueryView<>(MAPPING, 
				"SELECT `line`, `step` " +
				"FROM `VariableTrace` " +
				"WHERE `testId` = ? AND `callStep` = ? " +
				"ORDER BY `step`", 
				testId, callStep);
	}
	
}

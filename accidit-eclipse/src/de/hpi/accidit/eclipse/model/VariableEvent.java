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
	
//	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
//	
//	private static final QueryTemplate<VariableEvent> TEMPLATE = new TETemplate<VariableEvent>() {{
//		from("`VariableTrace` f");
//	}};
//	
//	public static class Query extends QueryWithTemplate<VariableEvent> {
//		public Query(MiConnection cnn, String[] select) {
//			super(cnn, MAPPING, TEMPLATE);
//			select(select);
//		}
//		public Query where() {
//			return this;
//		}
//		public Query inInvocation(int testId, long callStep) {
//			where("testId_EQ", testId);
//			where("callStep_EQ", callStep);
//			orderBy("o_step");
//			return this;
//		}
//	}
	
}

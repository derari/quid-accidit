package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.QueryTemplate;
import org.cthul.miro.dsl.QueryWithTemplate;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;

public class VariableEvent extends TraceElement {

	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final Mapping<VariableEvent> MAPPING = new ReflectiveMapping<>(VariableEvent.class);
	
	private static final QueryTemplate<VariableEvent> TEMPLATE = new TETemplate<VariableEvent>() {{
		from("`VariableTrace` f");
	}};
	
	public static class Query extends QueryWithTemplate<VariableEvent> {
		public Query(MiConnection cnn, String[] select) {
			super(cnn, MAPPING, TEMPLATE);
			select_keys(select);
		}
		public Query where() {
			return this;
		}
		public Query inInvocation(int testId, long callStep) {
			where_key("testId_EQ", testId);
			where_key("callStep_EQ", callStep);
			orderBy_key("o_step");
			return this;
		}
	}
	
}

package de.hpi.accidit.eclipse.model;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.util.QueryFactoryView;
import de.hpi.accidit.orm.util.ReflectiveMapping;

public class VariableEvent extends TraceElement {

	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final Mapping<VariableEvent> MAPPING = new ReflectiveMapping<>(VariableEvent.class);
	
	private static final QueryTemplate<VariableEvent> TEMPLATE = new TETemplate<VariableEvent>() {{
		from("VariableTrace f");
	}};
	
	public static class Query extends QueryBuilder<VariableEvent> {
		public Query(OConnection cnn, String[] select) {
			super(cnn, TEMPLATE, MAPPING);
			select(select);
		}
		public Query where() {
			return this;
		}
		public Query inInvocation(int testId, long callStep) {
			where("testId_EQ", testId);
			where("callStep_EQ", callStep);
			orderBy("o_step");
			return this;
		}
	}
	
}

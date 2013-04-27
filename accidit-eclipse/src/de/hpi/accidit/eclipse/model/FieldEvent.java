package de.hpi.accidit.eclipse.model;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.util.QueryFactoryView;
import de.hpi.accidit.orm.util.ReflectiveMapping;

public class FieldEvent extends TraceElement {

	public static final View<PutQuery> PUT_VIEW = new QueryFactoryView<>(PutQuery.class);
	
	private static final Mapping<FieldEvent> MAPPING = new ReflectiveMapping<>(FieldEvent.class);
	
	private static class FeTemplate extends TETemplate<FieldEvent> {{
		
	}};
	
	private static final QueryTemplate<FieldEvent> PUT_TEMPLATE = new FeTemplate() {{
		from("PutTrace f");
	}};
	
	public static class PutQuery extends QueryBuilder<FieldEvent> {
		public PutQuery(OConnection cnn, String[] select) {
			super(cnn, PUT_TEMPLATE, MAPPING);
			select(select);
		}
		public PutQuery where() {
			return this;
		}
		public PutQuery inInvocation(int testId, long callStep) {
			where("testId_EQ", testId);
			where("callStep_EQ", callStep);
			orderBy("o_step");
			return this;
		}
	}
	
}

package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.QueryTemplate;
import org.cthul.miro.dsl.QueryWithTemplate;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;


public class FieldEvent extends TraceElement {

	public static final View<PutQuery> PUT_VIEW = new QueryFactoryView<>(PutQuery.class);
	
	private static final Mapping<FieldEvent> MAPPING = new ReflectiveMapping<>(FieldEvent.class);
	
	private static class FeTemplate extends TETemplate<FieldEvent> {{
		
	}};
	
	private static final QueryTemplate<FieldEvent> PUT_TEMPLATE = new FeTemplate() {{
		from("PutTrace f");
	}};
	
	public static class PutQuery extends QueryWithTemplate<FieldEvent> {
		public PutQuery(MiConnection cnn, String[] select) {
			super(cnn, MAPPING, PUT_TEMPLATE);
			select_keys(select);
		}
		public PutQuery where() {
			return this;
		}
		public PutQuery inInvocation(int testId, long callStep) {
			where_key("testId_EQ", testId);
			where_key("callStep_EQ", callStep);
			orderBy_key("o_step");
			return this;
		}
	}
	
}

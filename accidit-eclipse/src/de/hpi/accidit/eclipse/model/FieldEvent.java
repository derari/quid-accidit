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
	
//	public static final View<PutQuery> PUT_VIEW = new QueryFactoryView<>(PutQuery.class);
//	
//	private static class FeTemplate extends TETemplate<FieldEvent> {{
//		
//	}};
//	
//	private static final QueryTemplate<FieldEvent> PUT_TEMPLATE = new FeTemplate() {{
//		from("`PutTrace` f");
//	}};
//	
//	public static class PutQuery extends QueryWithTemplate<FieldEvent> {
//		public PutQuery(MiConnection cnn, String[] select) {
//			super(cnn, MAPPING, PUT_TEMPLATE);
//			select(select);
//		}
//		public PutQuery where() {
//			return this;
//		}
//		public PutQuery inInvocation(int testId, long callStep) {
//			where("testId_EQ", testId);
//			where("callStep_EQ", callStep);
//			orderBy("o_step");
//			return this;
//		}
//	}
	
}

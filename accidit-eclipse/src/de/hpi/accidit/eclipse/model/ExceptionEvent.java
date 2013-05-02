package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapterFactory;
import de.hpi.accidit.orm.util.QueryFactoryView;
import de.hpi.accidit.orm.util.ReflectiveMapping;

public class ExceptionEvent extends TraceElement {

	public boolean isThrow;
	
	public ExceptionEvent() {
	}
	
	@Override
	public String getImage() {
		return isThrow ? "trace_throw.png" : "trace_catch.png";
	}
	
	@Override
	public String getShortText() {
		return isThrow ? "throw" : "catch";
	}
	
	public static final View<CatchQuery> CATCH_VIEW = new QueryFactoryView<>(CatchQuery.class);
	public static final View<ThrowQuery> THROW_VIEW = new QueryFactoryView<>(ThrowQuery.class);
	
	private static final Mapping<ExceptionEvent> MAPPING = new ReflectiveMapping<>(ExceptionEvent.class);
	
	private static class EeTemplate extends TETemplate<ExceptionEvent> {{
	}};
	
	private static final QueryTemplate<ExceptionEvent> THROW_TEMPLATE = new EeTemplate() {{
		from("ThrowTrace f");
	}};
	
	public static class ThrowQuery extends QueryBuilder<ExceptionEvent> {
		public ThrowQuery(OConnection cnn, String[] select) {
			super(cnn, THROW_TEMPLATE, MAPPING);
			select(select);
			apply(new SetIsThrow(true));
		}
		public ThrowQuery where() {
			return this;
		}
		public ThrowQuery inInvocation(Invocation inv) {
			where("testId_EQ", inv.testId);
			where("callStep_EQ", inv.step);
			orderBy("o_step");
			apply(new SetParentAdapter(inv));
			return this;
		}
	}
	
	private static final QueryTemplate<ExceptionEvent> CATCH_TEMPLATE = new EeTemplate() {{
		from("CatchTrace f");
	}};
	
	public static class CatchQuery extends QueryBuilder<ExceptionEvent> {
		public CatchQuery(OConnection cnn, String[] select) {
			super(cnn, CATCH_TEMPLATE, MAPPING);
			select(select);
			apply(new SetIsThrow(false));
		}
		public CatchQuery where() {
			return this;
		}
		public CatchQuery inInvocation(Invocation inv) {
			where("testId_EQ", inv.testId);
			where("callStep_EQ", inv.step);
			orderBy("o_step");
			apply(new SetParentAdapter(inv));
			return this;
		}
	}
	
	private static class SetIsThrow implements ValueAdapterFactory<ExceptionEvent>, ValueAdapter<ExceptionEvent> {

		private boolean isThrow;
		
		public SetIsThrow(boolean isThrow) {
			this.isThrow = isThrow;
		}

		@Override
		public void initialize(ResultSet rs) throws SQLException {
		}

		@Override
		public void apply(ExceptionEvent entity) throws SQLException {
			entity.isThrow = this.isThrow;
		}

		@Override
		public void complete() throws SQLException {
		}

		@Override
		public ValueAdapter<ExceptionEvent> newAdapter(
				Mapping<ExceptionEvent> mapping, OConnection cnn,
				List<String> attributes) {
			return this;
		}
		
	}
	
}

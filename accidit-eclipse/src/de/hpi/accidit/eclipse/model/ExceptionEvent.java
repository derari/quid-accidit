package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.QueryTemplate;
import org.cthul.miro.dsl.QueryWithTemplate;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ResultBuilder.ValueAdapter;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;

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
		from("`ThrowTrace` f");
	}};
	
	public static class ThrowQuery extends QueryWithTemplate<ExceptionEvent> {
		public ThrowQuery(MiConnection cnn, String[] select) {
			super(cnn, MAPPING, THROW_TEMPLATE);
			select_keys(select);
			adapter(new SetIsThrow(true));
		}
		public ThrowQuery where() {
			return this;
		}
		public ThrowQuery inInvocation(Invocation inv) {
			where_key("testId_EQ", inv.testId);
			where_key("callStep_EQ", inv.step);
			orderBy_key("o_step");
			adapter(new SetParentAdapter(inv));
			return this;
		}
	}
	
	private static final QueryTemplate<ExceptionEvent> CATCH_TEMPLATE = new EeTemplate() {{
		from("`CatchTrace` f");
	}};
	
	public static class CatchQuery extends QueryWithTemplate<ExceptionEvent> {
		public CatchQuery(MiConnection cnn, String[] select) {
			super(cnn, MAPPING, CATCH_TEMPLATE);
			select_keys(select);
			adapter(new SetIsThrow(false));
		}
		public CatchQuery where() {
			return this;
		}
		public CatchQuery inInvocation(Invocation inv) {
			where_key("testId_EQ", inv.testId);
			where_key("callStep_EQ", inv.step);
			orderBy_key("o_step");
			adapter(new SetParentAdapter(inv));
			return this;
		}
	}
	
	private static class SetIsThrow implements ValueAdapter<ExceptionEvent> {

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
		public void close() throws SQLException {
		}
		
	}
	
}
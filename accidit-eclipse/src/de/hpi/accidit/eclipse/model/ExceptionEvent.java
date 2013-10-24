package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.at.Always;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.Config;
import org.cthul.miro.at.From;
import org.cthul.miro.at.MiQuery;
import org.cthul.miro.at.More;
import org.cthul.miro.dsl.QueryView;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.result.EntityInitializer;
import org.cthul.objects.instance.Arg;

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
	
	private static final Mapping<ExceptionEvent> MAPPING = new ReflectiveMapping<>(ExceptionEvent.class);
	
	public static View<ThrowQuery> THROW = new AnnotatedView<>(ThrowQuery.class, MAPPING);
	public static View<CatchQuery> CATCH = new AnnotatedView<>(CatchQuery.class, MAPPING);
	
	@From("`ThrowTrace` e")
	@Always(@More(
		config=@Config(impl=SetIsThrow.class, args=@Arg(x=true))
	))
	public static interface ThrowQuery extends Query<ExceptionEvent, ThrowQuery> {
		
	}
	
	@From("`CatchTrace` e")
	@Always(@More(
		config=@Config(impl=SetIsThrow.class, args=@Arg(x=false))
	))
	public static interface CatchQuery extends Query<ExceptionEvent, CatchQuery> {
		
	}
	
	public static class SetIsThrow implements EntityInitializer<ExceptionEvent> {

		private boolean isThrow;
		
		public SetIsThrow(boolean isThrow) {
			this.isThrow = isThrow;
		}

		@Override
		public void apply(ExceptionEvent entity) throws SQLException {
			entity.isThrow = isThrow;
		}

		@Override
		public void complete() throws SQLException {
		}

		@Override
		public void close() throws SQLException {
		}
	}
}

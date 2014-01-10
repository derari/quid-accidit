package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.at.Always;
import org.cthul.miro.at.Config;
import org.cthul.miro.at.From;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;
import org.cthul.miro.util.CfgSetField;
import org.cthul.miro.view.ViewR;
import org.cthul.miro.view.Views;
import org.cthul.objects.instance.Arg;

import de.hpi.accidit.eclipse.model.ExceptionEvent;

public class ExceptionEventDao extends TraceElementDaoBase {

	private static final Mapping<ExceptionEvent> MAPPING = new ReflectiveMapping<>(ExceptionEvent.class);
	
	public static ViewR<ThrowQuery> THROW = Views.build(MAPPING).r(ThrowQuery.class).build();
	public static ViewR<CatchQuery> CATCH = Views.build(MAPPING).r(CatchQuery.class).build();
	
	@From("`ThrowTrace` e")
	@Always(
		config=@Config(impl=CfgSetField.class, args={@Arg(str="isThrow"), @Arg(x=true)})
	)
	public static interface ThrowQuery extends Query<ExceptionEvent, ThrowQuery> {
		
	}
	
	@From("`CatchTrace` e")
	@Always(
		config=@Config(impl=CfgSetField.class, args={@Arg(str="isThrow"), @Arg(x=false)})
	)
	public static interface CatchQuery extends Query<ExceptionEvent, CatchQuery> {
		
	}
//	
//	public static class SetIsThrow implements EntityInitializer<ExceptionEvent> {
//
//		private boolean isThrow;
//		
//		public SetIsThrow(boolean isThrow) {
//			this.isThrow = isThrow;
//		}
//
//		@Override
//		public void apply(ExceptionEvent entity) throws SQLException {
//			entity.isThrow = isThrow;
//		}
//
//		@Override
//		public void complete() throws SQLException {
//		}
//
//		@Override
//		public void close() throws SQLException {
//		}
//	}
}

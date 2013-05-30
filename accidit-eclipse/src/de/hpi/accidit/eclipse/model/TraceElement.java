package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.cthul.miro.MiConnection;
import org.cthul.miro.dsl.QueryTemplate;
import org.cthul.miro.dsl.ValueAdapterFactory;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ResultBuilder;
import org.cthul.miro.map.ResultBuilder.ValueAdapter;


public class TraceElement extends ModelBase implements Comparable<TraceElement> {

	public Invocation parent;
	public int line;
	public long step;
	
	@Override
	public int compareTo(TraceElement o) {
		int c = Long.compare(step, o.step);
		if (c != 0) return c;
		return deepCompare(o);
	}

	protected int deepCompare(TraceElement o) {
		return 0;
	}
	
	public String getImage() {
		return "";
	}
	
	public String getShortText() {
		return "";
	}

	
	protected static class TETemplate<V extends TraceElement> extends QueryTemplate<V> {{
		select("f.`line`", "f.`step`");
		where("testId_EQ", "f.`testId` = ?");
		where("callStep_EQ", "f.`callStep` = ?");
		orderBy("o_step", "f.`step`");
	}};
	
	protected static class SetParentAdapter 
					implements ResultBuilder.ValueAdapter<TraceElement> {
		
		private final Invocation parent;
		
		public SetParentAdapter(Invocation parent) {
			this.parent = parent;
		}
		
		@Override
		public void initialize(ResultSet rs) throws SQLException {}
		
		@Override
		public void apply(TraceElement entity) throws SQLException {
			entity.parent = parent;
		}
		
		@Override
		public void complete() throws SQLException {}

		@Override
		public void close() throws SQLException {}
	}
	
}
